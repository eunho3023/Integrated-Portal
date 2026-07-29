package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE displayName = :displayName AND phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneAndName(displayName: String, phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND displayName = :displayName AND phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByUsernamePhoneAndName(username: String, displayName: String, phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    // Schools
    @Query("SELECT * FROM schools WHERE schoolId = :schoolId")
    suspend fun getSchoolById(schoolId: String): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity)

    @Query("SELECT * FROM schools WHERE inviteCode = :inviteCode")
    suspend fun getSchoolByInviteCode(inviteCode: String): SchoolEntity?

    @Query("SELECT * FROM schools")
    fun getAllSchoolsFlow(): Flow<List<SchoolEntity>>

    // Rentals
    @Query("SELECT * FROM rentals WHERE schoolId = :schoolId")
    fun getRentalsFlow(schoolId: String): Flow<List<RentalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRental(rental: RentalEntity)

    @Delete
    suspend fun deleteRental(rental: RentalEntity)

    @Query("DELETE FROM rentals WHERE schoolId = :schoolId")
    suspend fun clearRentals(schoolId: String)

    // Suggestions
    @Query("SELECT * FROM suggestions WHERE schoolId = :schoolId")
    fun getSuggestionsFlow(schoolId: String): Flow<List<SuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: SuggestionEntity)

    @Delete
    suspend fun deleteSuggestion(suggestion: SuggestionEntity)

    @Query("DELETE FROM suggestions WHERE schoolId = :schoolId")
    suspend fun clearSuggestions(schoolId: String)

    // Roster names
    @Query("SELECT * FROM roster_names WHERE schoolId = :schoolId")
    fun getRosterNamesFlow(schoolId: String): Flow<List<RosterNameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRosterName(rosterName: RosterNameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRosterNames(rosterNames: List<RosterNameEntity>)

    // Uniform checks
    @Query("SELECT * FROM uniform_checks WHERE schoolId = :schoolId")
    fun getUniformChecksFlow(schoolId: String): Flow<List<UniformCheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniformCheck(check: UniformCheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniformChecks(checks: List<UniformCheckEntity>)

    @Query("DELETE FROM uniform_checks WHERE schoolId = :schoolId")
    suspend fun clearUniformChecks(schoolId: String)

    @Query("DELETE FROM uniform_checks WHERE id = :id")
    suspend fun deleteUniformCheck(id: Long)

    @Query("SELECT * FROM uniform_checks WHERE id = :id")
    suspend fun getUniformCheckById(id: Long): UniformCheckEntity?

    // Attendances
    @Query("SELECT * FROM attendances WHERE schoolId = :schoolId")
    fun getAttendancesFlow(schoolId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Query("DELETE FROM attendances WHERE schoolId = :schoolId")
    suspend fun clearAttendances(schoolId: String)

    @Query("DELETE FROM attendances WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    @Query("SELECT * FROM attendances WHERE id = :id")
    suspend fun getAttendanceById(id: Long): AttendanceEntity?

    // Merit logs
    @Query("SELECT * FROM merit_logs WHERE schoolId = :schoolId")
    fun getMeritLogsFlow(schoolId: String): Flow<List<MeritLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeritLog(log: MeritLogEntity)

    @Delete
    suspend fun deleteMeritLog(log: MeritLogEntity)

    @Query("DELETE FROM merit_logs WHERE schoolId = :schoolId")
    suspend fun clearMeritLogs(schoolId: String)

    // Clean zones
    @Query("SELECT * FROM clean_zones WHERE schoolId = :schoolId")
    fun getCleanZonesFlow(schoolId: String): Flow<List<CleanZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanZone(zone: CleanZoneEntity)

    @Delete
    suspend fun deleteCleanZone(zone: CleanZoneEntity)

    @Query("UPDATE clean_zones SET doneDate = '' WHERE schoolId = :schoolId")
    suspend fun resetCleanZonesToday(schoolId: String)

    // Funds
    @Query("SELECT * FROM funds WHERE schoolId = :schoolId")
    fun getFundsFlow(schoolId: String): Flow<List<FundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFund(fund: FundEntity)

    @Delete
    suspend fun deleteFund(fund: FundEntity)

    @Query("DELETE FROM funds WHERE schoolId = :schoolId")
    suspend fun clearFunds(schoolId: String)

    // Votes
    @Query("SELECT * FROM votes WHERE schoolId = :schoolId")
    fun getVotesFlow(schoolId: String): Flow<List<VoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: VoteEntity)

    // Lost items
    @Query("SELECT * FROM lost_items WHERE schoolId = :schoolId")
    fun getLostItemsFlow(schoolId: String): Flow<List<LostItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLostItem(item: LostItemEntity)

    @Query("DELETE FROM lost_items WHERE schoolId = :schoolId")
    suspend fun clearLostItems(schoolId: String)

    // Item stocks
    @Query("SELECT * FROM item_stocks WHERE schoolId = :schoolId")
    fun getItemStocksFlow(schoolId: String): Flow<List<ItemStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemStock(stock: ItemStockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemStocks(stocks: List<ItemStockEntity>)

    @Delete
    suspend fun deleteItemStock(stock: ItemStockEntity)

    // Invite codes
    @Query("SELECT * FROM invite_codes WHERE inviteCode = :inviteCode")
    suspend fun getInviteCode(inviteCode: String): InviteCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInviteCode(inviteCode: InviteCodeEntity)

    // Wrong Answers (오답노트)
    @Query("SELECT * FROM wrong_answers WHERE schoolId = :schoolId ORDER BY date DESC")
    fun getWrongAnswersFlow(schoolId: String): Flow<List<WrongAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrongAnswer(item: WrongAnswerEntity)

    @Delete
    suspend fun deleteWrongAnswer(item: WrongAnswerEntity)

    // Questions (Q&A 서비스)
    @Query("SELECT * FROM questions WHERE isPublic = 1 OR schoolId = :schoolId ORDER BY date DESC")
    fun getQuestionsFlow(schoolId: String): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(item: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(item: QuestionEntity)

    // Project Groups (모둠 프로젝트)
    @Query("SELECT * FROM project_groups WHERE schoolId = :schoolId ORDER BY id DESC")
    fun getProjectGroupsFlow(schoolId: String): Flow<List<ProjectGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectGroup(group: ProjectGroupEntity)

    @Delete
    suspend fun deleteProjectGroup(group: ProjectGroupEntity)

    // Project Tasks (역할분담 & 일정/할일)
    @Query("SELECT * FROM project_tasks WHERE schoolId = :schoolId ORDER BY dueDate ASC")
    fun getProjectTasksFlow(schoolId: String): Flow<List<ProjectTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectTask(task: ProjectTaskEntity)

    @Delete
    suspend fun deleteProjectTask(task: ProjectTaskEntity)

    // Project Resources (자료 공유)
    @Query("SELECT * FROM project_resources WHERE schoolId = :schoolId ORDER BY date DESC")
    fun getProjectResourcesFlow(schoolId: String): Flow<List<ProjectResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectResource(resource: ProjectResourceEntity)

    @Delete
    suspend fun deleteProjectResource(resource: ProjectResourceEntity)

    // Project Evaluations (상호평가 & 기여도)
    @Query("SELECT * FROM project_evaluations WHERE schoolId = :schoolId ORDER BY date DESC")
    fun getProjectEvaluationsFlow(schoolId: String): Flow<List<ProjectEvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectEvaluation(evaluation: ProjectEvaluationEntity)

    @Delete
    suspend fun deleteProjectEvaluation(evaluation: ProjectEvaluationEntity)
}
