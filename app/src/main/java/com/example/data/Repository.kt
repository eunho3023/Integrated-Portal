package com.example.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot

suspend fun <T> Task<T>?.awaitTask(): T? = if (this == null) null else suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
        }
    }
}

class AppRepository(private val appDao: AppDao) {
    private val db = try {
        FirebaseDatabase.getInstance("https://integrated-portal-ea306-default-rtdb.firebaseio.com/")
    } catch (e: Exception) {
        Log.e("RealtimeDBSync", "Firebase Database initialization failed", e)
        null
    }
    private val ref get() = db?.reference

    private fun mapSnapshotToUser(snapshot: DataSnapshot): UserEntity {
        val uid = snapshot.key ?: ""
        val username = snapshot.child("username").value?.toString() ?: ""
        val displayName = snapshot.child("displayName").value?.toString() ?: ""
        val role = snapshot.child("role").value?.toString() ?: ""
        val schoolId = snapshot.child("schoolId").value?.toString() ?: ""
        val createdAt = snapshot.child("createdAt").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
        val phoneNumber = snapshot.child("phoneNumber").value?.toString() ?: ""
        val password = snapshot.child("password").value?.toString() ?: ""
        return UserEntity(uid, username, displayName, role, schoolId, createdAt, phoneNumber, password)
    }

    private fun mapSnapshotToSchool(snapshot: DataSnapshot): SchoolEntity {
        val schoolId = snapshot.key ?: ""
        val name = snapshot.child("name").value?.toString() ?: ""
        val inviteCode = snapshot.child("inviteCode").value?.toString() ?: ""
        val createdAt = snapshot.child("createdAt").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
        return SchoolEntity(schoolId, name, inviteCode, createdAt)
    }

    private fun mapSnapshotToInviteCode(snapshot: DataSnapshot): InviteCodeEntity {
        val inviteCode = snapshot.key ?: ""
        val schoolId = snapshot.child("schoolId").value?.toString() ?: ""
        return InviteCodeEntity(inviteCode, schoolId)
    }

    // Users
    suspend fun getUserById(uid: String): UserEntity? {
        val local = appDao.getUserById(uid)
        if (local != null) return local
        try {
            val snapshot = ref?.child("users")?.child(uid)?.get()?.awaitTask()
            if (snapshot != null && snapshot.exists()) {
                val user = mapSnapshotToUser(snapshot)
                appDao.insertUser(user)
                return user
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting user by ID from Firebase", e)
        }
        return null
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        val local = appDao.getUserByUsername(username)
        if (local != null) return local
        try {
            val snapshot = ref?.child("users")
                ?.orderByChild("username")
                ?.equalTo(username)
                ?.get()
                ?.awaitTask()
            if (snapshot != null && snapshot.exists() && snapshot.hasChildren()) {
                val child = snapshot.children.firstOrNull()
                if (child != null) {
                    val user = mapSnapshotToUser(child)
                    appDao.insertUser(user)
                    if (user.schoolId.isNotEmpty()) {
                        getSchoolById(user.schoolId)
                    }
                    return user
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting user by username from Firebase", e)
        }
        return null
    }

    suspend fun getUserByPhoneAndName(displayName: String, phoneNumber: String): UserEntity? {
        val local = appDao.getUserByPhoneAndName(displayName, phoneNumber)
        if (local != null) return local
        try {
            val snapshot = ref?.child("users")
                ?.orderByChild("displayName")
                ?.equalTo(displayName)
                ?.get()
                ?.awaitTask()
            if (snapshot != null && snapshot.exists() && snapshot.hasChildren()) {
                for (child in snapshot.children) {
                    val user = mapSnapshotToUser(child)
                    if (user.phoneNumber == phoneNumber) {
                        appDao.insertUser(user)
                        if (user.schoolId.isNotEmpty()) {
                            getSchoolById(user.schoolId)
                        }
                        return user
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting user by phone and name", e)
        }
        return null
    }

    suspend fun getUserByUsernamePhoneAndName(username: String, displayName: String, phoneNumber: String): UserEntity? {
        val local = appDao.getUserByUsernamePhoneAndName(username, displayName, phoneNumber)
        if (local != null) return local
        try {
            val snapshot = ref?.child("users")
                ?.orderByChild("username")
                ?.equalTo(username)
                ?.get()
                ?.awaitTask()
            if (snapshot != null && snapshot.exists() && snapshot.hasChildren()) {
                val child = snapshot.children.firstOrNull()
                if (child != null) {
                    val user = mapSnapshotToUser(child)
                    if (user.displayName == displayName && user.phoneNumber == phoneNumber) {
                        appDao.insertUser(user)
                        if (user.schoolId.isNotEmpty()) {
                            getSchoolById(user.schoolId)
                        }
                        return user
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting user by username phone and name", e)
        }
        return null
    }
    
    fun getAllUsersFlow(): Flow<List<UserEntity>> = appDao.getAllUsersFlow()

    suspend fun insertUser(user: UserEntity) {
        appDao.insertUser(user)
        try {
            val userMap = mapOf(
                "uid" to user.uid,
                "username" to user.username,
                "displayName" to user.displayName,
                "role" to user.role,
                "schoolId" to user.schoolId,
                "createdAt" to user.createdAt,
                "phoneNumber" to user.phoneNumber,
                "password" to user.password
            )
            ref?.child("users")?.child(user.uid)?.setValue(userMap)?.awaitTask()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting user", e)
        }
    }
    
    suspend fun deleteUser(user: UserEntity) {
        appDao.deleteUser(user)
        try {
            ref?.child("users")?.child(user.uid)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting user", e)
        }
    }

    // Schools
    suspend fun getSchoolById(schoolId: String): SchoolEntity? {
        val local = appDao.getSchoolById(schoolId)
        if (local != null) return local
        try {
            val snapshot = ref?.child("schools")?.child(schoolId)?.get()?.awaitTask()
            if (snapshot != null && snapshot.exists()) {
                val school = mapSnapshotToSchool(snapshot)
                appDao.insertSchool(school)
                return school
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting school by ID from Firebase", e)
        }
        return null
    }
    
    suspend fun insertSchool(school: SchoolEntity) {
        appDao.insertSchool(school)
        try {
            val schoolMap = mapOf(
                "schoolId" to school.schoolId,
                "name" to school.name,
                "inviteCode" to school.inviteCode,
                "createdAt" to school.createdAt
            )
            ref?.child("schools")?.child(school.schoolId)?.setValue(schoolMap)?.awaitTask()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting school", e)
        }
    }
    
    suspend fun getSchoolByInviteCode(inviteCode: String): SchoolEntity? {
        val local = appDao.getSchoolByInviteCode(inviteCode)
        if (local != null) return local
        try {
            val snapshot = ref?.child("schools")
                ?.orderByChild("inviteCode")
                ?.equalTo(inviteCode)
                ?.get()
                ?.awaitTask()
            if (snapshot != null && snapshot.exists() && snapshot.hasChildren()) {
                val child = snapshot.children.firstOrNull()
                if (child != null) {
                    val school = mapSnapshotToSchool(child)
                    appDao.insertSchool(school)
                    return school
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting school by invite code from Firebase", e)
        }
        return null
    }
    fun getAllSchoolsFlow(): Flow<List<SchoolEntity>> = appDao.getAllSchoolsFlow()

    // Rentals
    fun getRentalsFlow(schoolId: String): Flow<List<RentalEntity>> = appDao.getRentalsFlow(schoolId)
    
    suspend fun insertRental(rental: RentalEntity) {
        appDao.insertRental(rental)
        try {
            ref?.child("rentals")?.child(rental.schoolId)?.child(rental.id.toString())?.setValue(rental)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting rental", e)
        }
    }
    
    suspend fun deleteRental(rental: RentalEntity) {
        appDao.deleteRental(rental)
        try {
            ref?.child("rentals")?.child(rental.schoolId)?.child(rental.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting rental", e)
        }
    }
    
    suspend fun clearRentals(schoolId: String) {
        appDao.clearRentals(schoolId)
        try {
            ref?.child("rentals")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing rentals", e)
        }
    }

    // Suggestions
    fun getSuggestionsFlow(schoolId: String): Flow<List<SuggestionEntity>> = appDao.getSuggestionsFlow(schoolId)
    
    suspend fun insertSuggestion(suggestion: SuggestionEntity) {
        appDao.insertSuggestion(suggestion)
        try {
            ref?.child("suggestions")?.child(suggestion.schoolId)?.child(suggestion.id.toString())?.setValue(suggestion)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting suggestion", e)
        }
    }

    suspend fun deleteSuggestion(suggestion: SuggestionEntity) {
        appDao.deleteSuggestion(suggestion)
        try {
            ref?.child("suggestions")?.child(suggestion.schoolId)?.child(suggestion.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting suggestion", e)
        }
    }
    
    suspend fun clearSuggestions(schoolId: String) {
        appDao.clearSuggestions(schoolId)
        try {
            ref?.child("suggestions")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing suggestions", e)
        }
    }

    // Roster names
    fun getRosterNamesFlow(schoolId: String): Flow<List<RosterNameEntity>> = appDao.getRosterNamesFlow(schoolId)
    
    suspend fun insertRosterName(rosterName: RosterNameEntity) {
        appDao.insertRosterName(rosterName)
        try {
            ref?.child("roster_names")?.child(rosterName.schoolId)?.child(rosterName.id)?.setValue(rosterName)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting roster name", e)
        }
    }
    
    suspend fun insertRosterNames(rosterNames: List<RosterNameEntity>) {
        appDao.insertRosterNames(rosterNames)
        try {
            for (rosterName in rosterNames) {
                ref?.child("roster_names")?.child(rosterName.schoolId)?.child(rosterName.id)?.setValue(rosterName)
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting roster names", e)
        }
    }

    // Uniform checks
    fun getUniformChecksFlow(schoolId: String): Flow<List<UniformCheckEntity>> = appDao.getUniformChecksFlow(schoolId)
    
    suspend fun insertUniformCheck(check: UniformCheckEntity) {
        appDao.insertUniformCheck(check)
        try {
            ref?.child("uniform_checks")?.child(check.schoolId)?.child(check.id.toString())?.setValue(check)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting uniform check", e)
        }
    }
    
    suspend fun insertUniformChecks(checks: List<UniformCheckEntity>) {
        appDao.insertUniformChecks(checks)
        try {
            for (check in checks) {
                ref?.child("uniform_checks")?.child(check.schoolId)?.child(check.id.toString())?.setValue(check)
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting uniform checks", e)
        }
    }
    
    suspend fun clearUniformChecks(schoolId: String) {
        appDao.clearUniformChecks(schoolId)
        try {
            ref?.child("uniform_checks")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing uniform checks", e)
        }
    }
    
    suspend fun deleteUniformCheck(id: Long) {
        val check = appDao.getUniformCheckById(id)
        appDao.deleteUniformCheck(id)
        if (check != null) {
            try {
                ref?.child("uniform_checks")?.child(check.schoolId)?.child(id.toString())?.removeValue()
            } catch (e: Exception) {
                Log.e("RealtimeDBSync", "Error deleting uniform check", e)
            }
        }
    }

    // Attendances
    fun getAttendancesFlow(schoolId: String): Flow<List<AttendanceEntity>> = appDao.getAttendancesFlow(schoolId)
    
    suspend fun insertAttendance(attendance: AttendanceEntity) {
        appDao.insertAttendance(attendance)
        try {
            ref?.child("attendances")?.child(attendance.schoolId)?.child(attendance.id.toString())?.setValue(attendance)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting attendance", e)
        }
    }
    
    suspend fun insertAttendances(attendances: List<AttendanceEntity>) {
        appDao.insertAttendances(attendances)
        try {
            for (attendance in attendances) {
                ref?.child("attendances")?.child(attendance.schoolId)?.child(attendance.id.toString())?.setValue(attendance)
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting attendances", e)
        }
    }
    
    suspend fun clearAttendances(schoolId: String) {
        appDao.clearAttendances(schoolId)
        try {
            ref?.child("attendances")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing attendances", e)
        }
    }
    
    suspend fun deleteAttendance(id: Long) {
        val attendance = appDao.getAttendanceById(id)
        appDao.deleteAttendance(id)
        if (attendance != null) {
            try {
                ref?.child("attendances")?.child(attendance.schoolId)?.child(id.toString())?.removeValue()
            } catch (e: Exception) {
                Log.e("RealtimeDBSync", "Error deleting attendance", e)
            }
        }
    }

    // Merit logs
    fun getMeritLogsFlow(schoolId: String): Flow<List<MeritLogEntity>> = appDao.getMeritLogsFlow(schoolId)
    
    suspend fun insertMeritLog(log: MeritLogEntity) {
        appDao.insertMeritLog(log)
        try {
            ref?.child("merit_logs")?.child(log.schoolId)?.child(log.id.toString())?.setValue(log)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting merit log", e)
        }
    }
    
    suspend fun deleteMeritLog(log: MeritLogEntity) {
        appDao.deleteMeritLog(log)
        try {
            ref?.child("merit_logs")?.child(log.schoolId)?.child(log.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting merit log", e)
        }
    }
    
    suspend fun clearMeritLogs(schoolId: String) {
        appDao.clearMeritLogs(schoolId)
        try {
            ref?.child("merit_logs")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing merit logs", e)
        }
    }

    // Clean zones
    fun getCleanZonesFlow(schoolId: String): Flow<List<CleanZoneEntity>> = appDao.getCleanZonesFlow(schoolId)
    
    suspend fun insertCleanZone(zone: CleanZoneEntity) {
        appDao.insertCleanZone(zone)
        try {
            ref?.child("clean_zones")?.child(zone.schoolId)?.child(zone.id.toString())?.setValue(zone)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting clean zone", e)
        }
    }
    
    suspend fun deleteCleanZone(zone: CleanZoneEntity) {
        appDao.deleteCleanZone(zone)
        try {
            ref?.child("clean_zones")?.child(zone.schoolId)?.child(zone.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting clean zone", e)
        }
    }
    
    suspend fun resetCleanZonesToday(schoolId: String) {
        appDao.resetCleanZonesToday(schoolId)
        try {
            ref?.child("clean_zones")?.child(schoolId)?.get()?.addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    child.ref.child("doneDate").setValue("")
                }
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error resetting clean zones", e)
        }
    }

    // Funds
    fun getFundsFlow(schoolId: String): Flow<List<FundEntity>> = appDao.getFundsFlow(schoolId)
    
    suspend fun insertFund(fund: FundEntity) {
        appDao.insertFund(fund)
        try {
            ref?.child("funds")?.child(fund.schoolId)?.child(fund.id.toString())?.setValue(fund)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting fund", e)
        }
    }
    
    suspend fun deleteFund(fund: FundEntity) {
        appDao.deleteFund(fund)
        try {
            ref?.child("funds")?.child(fund.schoolId)?.child(fund.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting fund", e)
        }
    }
    
    suspend fun clearFunds(schoolId: String) {
        appDao.clearFunds(schoolId)
        try {
            ref?.child("funds")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing funds", e)
        }
    }

    // Votes
    fun getVotesFlow(schoolId: String): Flow<List<VoteEntity>> = appDao.getVotesFlow(schoolId)
    
    suspend fun insertVote(vote: VoteEntity) {
        appDao.insertVote(vote)
        try {
            ref?.child("votes")?.child(vote.schoolId)?.child(vote.id.toString())?.setValue(vote)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting vote", e)
        }
    }

    // Lost items
    fun getLostItemsFlow(schoolId: String): Flow<List<LostItemEntity>> = appDao.getLostItemsFlow(schoolId)
    
    suspend fun insertLostItem(item: LostItemEntity) {
        appDao.insertLostItem(item)
        try {
            ref?.child("lost_items")?.child(item.schoolId)?.child(item.id.toString())?.setValue(item)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting lost item", e)
        }
    }
    
    suspend fun clearLostItems(schoolId: String) {
        appDao.clearLostItems(schoolId)
        try {
            ref?.child("lost_items")?.child(schoolId)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error clearing lost items", e)
        }
    }

    // Item stocks
    fun getItemStocksFlow(schoolId: String): Flow<List<ItemStockEntity>> = appDao.getItemStocksFlow(schoolId)
    
    suspend fun insertItemStock(stock: ItemStockEntity) {
        appDao.insertItemStock(stock)
        try {
            ref?.child("item_stocks")?.child(stock.schoolId)?.child(stock.id)?.setValue(stock)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting item stock", e)
        }
    }
    
    suspend fun insertItemStocks(stocks: List<ItemStockEntity>) {
        appDao.insertItemStocks(stocks)
        try {
            for (stock in stocks) {
                ref?.child("item_stocks")?.child(stock.schoolId)?.child(stock.id)?.setValue(stock)
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting item stocks", e)
        }
    }
    
    suspend fun deleteItemStock(stock: ItemStockEntity) {
        appDao.deleteItemStock(stock)
        try {
            ref?.child("item_stocks")?.child(stock.schoolId)?.child(stock.id)?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting item stock", e)
        }
    }

    // Invite codes
    suspend fun getInviteCode(inviteCode: String): InviteCodeEntity? {
        val local = appDao.getInviteCode(inviteCode)
        if (local != null) return local
        try {
            val snapshot = ref?.child("invite_codes")?.child(inviteCode)?.get()?.awaitTask()
            if (snapshot != null && snapshot.exists()) {
                val invite = mapSnapshotToInviteCode(snapshot)
                appDao.insertInviteCode(invite)
                if (invite.schoolId.isNotEmpty()) {
                    getSchoolById(invite.schoolId)
                }
                return invite
            }
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error getting invite code from Firebase", e)
        }
        return null
    }
    
    suspend fun insertInviteCode(inviteCode: InviteCodeEntity) {
        appDao.insertInviteCode(inviteCode)
        try {
            val inviteMap = mapOf(
                "inviteCode" to inviteCode.inviteCode,
                "schoolId" to inviteCode.schoolId
            )
            ref?.child("invite_codes")?.child(inviteCode.inviteCode)?.setValue(inviteMap)?.awaitTask()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting invite code", e)
        }
    }

    // Wrong Answers (오답노트)
    fun getWrongAnswersFlow(schoolId: String): Flow<List<WrongAnswerEntity>> = appDao.getWrongAnswersFlow(schoolId)

    suspend fun insertWrongAnswer(item: WrongAnswerEntity) {
        appDao.insertWrongAnswer(item)
        try {
            ref?.child("wrong_answers")?.child(item.schoolId)?.child(item.id.toString())?.setValue(item)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting wrong answer", e)
        }
    }

    suspend fun deleteWrongAnswer(item: WrongAnswerEntity) {
        appDao.deleteWrongAnswer(item)
        try {
            ref?.child("wrong_answers")?.child(item.schoolId)?.child(item.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting wrong answer", e)
        }
    }

    // Questions (Q&A 서비스)
    fun getQuestionsFlow(schoolId: String): Flow<List<QuestionEntity>> = appDao.getQuestionsFlow(schoolId)

    suspend fun insertQuestion(item: QuestionEntity) {
        appDao.insertQuestion(item)
        try {
            ref?.child("questions")?.child(item.schoolId)?.child(item.id.toString())?.setValue(item)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting question", e)
        }
    }

    suspend fun deleteQuestion(item: QuestionEntity) {
        appDao.deleteQuestion(item)
        try {
            ref?.child("questions")?.child(item.schoolId)?.child(item.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting question", e)
        }
    }

    // Project Groups
    fun getProjectGroupsFlow(schoolId: String): Flow<List<ProjectGroupEntity>> = appDao.getProjectGroupsFlow(schoolId)

    suspend fun insertProjectGroup(group: ProjectGroupEntity) {
        appDao.insertProjectGroup(group)
        try {
            ref?.child("project_groups")?.child(group.schoolId)?.child(group.id.toString())?.setValue(group)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting project group", e)
        }
    }

    suspend fun deleteProjectGroup(group: ProjectGroupEntity) {
        appDao.deleteProjectGroup(group)
        try {
            ref?.child("project_groups")?.child(group.schoolId)?.child(group.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting project group", e)
        }
    }

    // Project Tasks
    fun getProjectTasksFlow(schoolId: String): Flow<List<ProjectTaskEntity>> = appDao.getProjectTasksFlow(schoolId)

    suspend fun insertProjectTask(task: ProjectTaskEntity) {
        appDao.insertProjectTask(task)
        try {
            ref?.child("project_tasks")?.child(task.schoolId)?.child(task.id.toString())?.setValue(task)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting project task", e)
        }
    }

    suspend fun deleteProjectTask(task: ProjectTaskEntity) {
        appDao.deleteProjectTask(task)
        try {
            ref?.child("project_tasks")?.child(task.schoolId)?.child(task.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting project task", e)
        }
    }

    // Project Resources
    fun getProjectResourcesFlow(schoolId: String): Flow<List<ProjectResourceEntity>> = appDao.getProjectResourcesFlow(schoolId)

    suspend fun insertProjectResource(resource: ProjectResourceEntity) {
        appDao.insertProjectResource(resource)
        try {
            ref?.child("project_resources")?.child(resource.schoolId)?.child(resource.id.toString())?.setValue(resource)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting project resource", e)
        }
    }

    suspend fun deleteProjectResource(resource: ProjectResourceEntity) {
        appDao.deleteProjectResource(resource)
        try {
            ref?.child("project_resources")?.child(resource.schoolId)?.child(resource.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting project resource", e)
        }
    }

    // Project Evaluations
    fun getProjectEvaluationsFlow(schoolId: String): Flow<List<ProjectEvaluationEntity>> = appDao.getProjectEvaluationsFlow(schoolId)

    suspend fun insertProjectEvaluation(evaluation: ProjectEvaluationEntity) {
        appDao.insertProjectEvaluation(evaluation)
        try {
            ref?.child("project_evaluations")?.child(evaluation.schoolId)?.child(evaluation.id.toString())?.setValue(evaluation)
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error inserting project evaluation", e)
        }
    }

    suspend fun deleteProjectEvaluation(evaluation: ProjectEvaluationEntity) {
        appDao.deleteProjectEvaluation(evaluation)
        try {
            ref?.child("project_evaluations")?.child(evaluation.schoolId)?.child(evaluation.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e("RealtimeDBSync", "Error deleting project evaluation", e)
        }
    }
}
