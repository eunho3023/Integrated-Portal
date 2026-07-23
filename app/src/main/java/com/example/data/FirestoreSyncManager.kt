package com.example.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirestoreSyncManager(private val appDao: AppDao) {
    private val db = try {
        FirebaseDatabase.getInstance("https://integrated-portal-ea306-default-rtdb.firebaseio.com/")
    } catch (e: Exception) {
        Log.e("FirestoreSyncManager", "Firebase Database initialization failed", e)
        null
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<RegisteredListener>()
    private var activeSchoolId: String? = null

    private interface RegisteredListener {
        fun remove()
    }

    fun startSync(schoolId: String) {
        if (schoolId.isEmpty()) return
        if (activeSchoolId == schoolId) return
        stopSync()
        activeSchoolId = schoolId
        Log.d("RealtimeSync", "Starting real-time synchronization for schoolId: $schoolId")

        // 1. Sync Schools
        listenToSingleValue("schools", schoolId) { snapshot ->
            val name = snapshot.child("name").value?.toString() ?: ""
            val inviteCode = snapshot.child("inviteCode").value?.toString() ?: ""
            val createdAt = snapshot.child("createdAt").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
            val school = SchoolEntity(
                schoolId = snapshot.key ?: "",
                name = name,
                inviteCode = inviteCode,
                createdAt = createdAt
            )
            scope.launch {
                appDao.insertSchool(school)
            }
        }

        // 2. Sync Rentals
        listenToCollection(
            path = "rentals",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val rentSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val num = snapshot.child("num").value?.toString() ?: ""
                val name = snapshot.child("name").value?.toString() ?: ""
                val item = snapshot.child("item").value?.toString() ?: ""
                val rentDate = snapshot.child("rentDate").value?.toString() ?: ""
                val returnDate = snapshot.child("returnDate").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val rental = RentalEntity(id, rentSchoolId, grade, classNum, num, name, item, rentDate, returnDate, status)
                scope.launch {
                    appDao.insertRental(rental)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val rentSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val rental = RentalEntity(id, rentSchoolId, "", "", "", "", "", "", "", "")
                scope.launch {
                    appDao.deleteRental(rental)
                }
            }
        )

        // 3. Sync Suggestions
        listenToCollection(
            path = "suggestions",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val sugSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val type = snapshot.child("type").value?.toString() ?: ""
                val studentId = snapshot.child("studentId").value?.toString() ?: ""
                val content = snapshot.child("content").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val comment = snapshot.child("comment").value?.toString() ?: ""
                val suggestion = SuggestionEntity(id, sugSchoolId, type, studentId, content, status, comment)
                scope.launch {
                    appDao.insertSuggestion(suggestion)
                }
            },
            onRemoved = {
                // Read-only / handled on database clear
            }
        )

        // 4. Sync Roster Names
        listenToCollection(
            path = "roster_names",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key ?: return@listenToCollection
                val rSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val num = snapshot.child("num").value?.toString()?.toIntOrNull() ?: 0
                val name = snapshot.child("name").value?.toString() ?: ""
                val roster = RosterNameEntity(id, rSchoolId, grade, classNum, num, name)
                scope.launch {
                    appDao.insertRosterName(roster)
                }
            },
            onRemoved = {
                // Not usually removed individually
            }
        )

        // 5. Sync Uniform Checks
        listenToCollection(
            path = "uniform_checks",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val ucSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val num = snapshot.child("num").value?.toString() ?: ""
                val name = snapshot.child("name").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val check = UniformCheckEntity(id, ucSchoolId, grade, classNum, num, name, status, date)
                scope.launch {
                    appDao.insertUniformCheck(check)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                scope.launch {
                    appDao.deleteUniformCheck(id)
                }
            }
        )

        // 6. Sync Attendances
        listenToCollection(
            path = "attendances",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val attSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val num = snapshot.child("num").value?.toString() ?: ""
                val name = snapshot.child("name").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val attendance = AttendanceEntity(id, attSchoolId, grade, classNum, num, name, status, date)
                scope.launch {
                    appDao.insertAttendance(attendance)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                scope.launch {
                    appDao.deleteAttendance(id)
                }
            }
        )

        // 7. Sync Merit Logs
        listenToCollection(
            path = "merit_logs",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val mSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val num = snapshot.child("num").value?.toString() ?: ""
                val name = snapshot.child("name").value?.toString() ?: ""
                val score = snapshot.child("score").value?.toString()?.toIntOrNull() ?: 0
                val reason = snapshot.child("reason").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val log = MeritLogEntity(id, mSchoolId, grade, classNum, num, name, score, reason, date)
                scope.launch {
                    appDao.insertMeritLog(log)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val log = MeritLogEntity(id, "", "", "", "", "", 0, "", "")
                scope.launch {
                    appDao.deleteMeritLog(log)
                }
            }
        )

        // 8. Sync Clean Zones
        listenToCollection(
            path = "clean_zones",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val czSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val zone = snapshot.child("zone").value?.toString() ?: ""
                val assignee = snapshot.child("assignee").value?.toString() ?: ""
                val doneDate = snapshot.child("doneDate").value?.toString() ?: ""
                val cleanZone = CleanZoneEntity(id, czSchoolId, zone, assignee, doneDate)
                scope.launch {
                    appDao.insertCleanZone(cleanZone)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val cleanZone = CleanZoneEntity(id, "", "", "", "")
                scope.launch {
                    appDao.deleteCleanZone(cleanZone)
                }
            }
        )

        // 9. Sync Funds
        listenToCollection(
            path = "funds",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val fSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val type = snapshot.child("type").value?.toString() ?: ""
                val title = snapshot.child("title").value?.toString() ?: ""
                val amount = snapshot.child("amount").value?.toString()?.toIntOrNull() ?: 0
                val memo = snapshot.child("memo").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val fund = FundEntity(id, fSchoolId, type, title, amount, memo, date)
                scope.launch {
                    appDao.insertFund(fund)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val fund = FundEntity(id, "", "", "", 0, "", "")
                scope.launch {
                    appDao.deleteFund(fund)
                }
            }
        )

        // 10. Sync Votes
        listenToCollection(
            path = "votes",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val vSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val question = snapshot.child("question").value?.toString() ?: ""
                val grade = snapshot.child("grade").value?.toString() ?: ""
                val classNum = snapshot.child("classNum").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val optionsData = snapshot.child("optionsData").value?.toString() ?: ""
                val votersData = snapshot.child("votersData").value?.toString() ?: ""
                val vote = VoteEntity(id, vSchoolId, question, grade, classNum, status, date, optionsData, votersData)
                scope.launch {
                    appDao.insertVote(vote)
                }
            },
            onRemoved = {
                // Handled on refresh / database reset
            }
        )

        // 11. Sync Lost Items
        listenToCollection(
            path = "lost_items",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val liSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val name = snapshot.child("name").value?.toString() ?: ""
                val location = snapshot.child("location").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val claimant = snapshot.child("claimant").value?.toString() ?: ""
                val photoBase64 = snapshot.child("photoBase64").value?.toString() ?: ""
                val fileName = snapshot.child("fileName").value?.toString() ?: ""
                val fileDataBase64 = snapshot.child("fileDataBase64").value?.toString() ?: ""
                val item = LostItemEntity(id, liSchoolId, name, location, date, status, claimant, photoBase64, fileName, fileDataBase64)
                scope.launch {
                    appDao.insertLostItem(item)
                }
            },
            onRemoved = {
                // Handled on refresh / database reset
            }
        )

        // 12. Sync Item Stocks
        listenToCollection(
            path = "item_stocks",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key ?: return@listenToCollection
                val isSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val itemName = snapshot.child("itemName").value?.toString() ?: ""
                val totalQty = snapshot.child("totalQty").value?.toString()?.toIntOrNull() ?: 0
                val stock = ItemStockEntity(id, isSchoolId, itemName, totalQty)
                scope.launch {
                    appDao.insertItemStock(stock)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key ?: return@listenToCollection
                val stock = ItemStockEntity(id, "", "", 0)
                scope.launch {
                    appDao.deleteItemStock(stock)
                }
            }
        )

        // Sync Wrong Answers
        listenToCollection(
            path = "wrong_answers",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val waSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val studentUid = snapshot.child("studentUid").value?.toString() ?: ""
                val studentName = snapshot.child("studentName").value?.toString() ?: ""
                val subject = snapshot.child("subject").value?.toString() ?: ""
                val problemTitle = snapshot.child("problemTitle").value?.toString() ?: ""
                val problemDescription = snapshot.child("problemDescription").value?.toString() ?: ""
                val studentAnswer = snapshot.child("studentAnswer").value?.toString() ?: ""
                val correctAnswer = snapshot.child("correctAnswer").value?.toString() ?: ""
                val errorReason = snapshot.child("errorReason").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val aiAnalysis = snapshot.child("aiAnalysis").value?.toString() ?: ""
                val aiSimilarQuestion = snapshot.child("aiSimilarQuestion").value?.toString() ?: ""

                val wrongAnswer = WrongAnswerEntity(
                    id, waSchoolId, studentUid, studentName, subject, problemTitle,
                    problemDescription, studentAnswer, correctAnswer, errorReason, date, aiAnalysis, aiSimilarQuestion
                )
                scope.launch {
                    appDao.insertWrongAnswer(wrongAnswer)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val wrongAnswer = WrongAnswerEntity(id, "", "", "", "", "", "", "", "", "", "", "", "")
                scope.launch {
                    appDao.deleteWrongAnswer(wrongAnswer)
                }
            }
        )

        // Sync Questions
        listenToCollection(
            path = "questions",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val qSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val authorUid = snapshot.child("authorUid").value?.toString() ?: ""
                val authorName = snapshot.child("authorName").value?.toString() ?: ""
                val subject = snapshot.child("subject").value?.toString() ?: ""
                val level = snapshot.child("level").value?.toString() ?: ""
                val title = snapshot.child("title").value?.toString() ?: ""
                val content = snapshot.child("content").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""
                val answersData = snapshot.child("answersData").value?.toString() ?: ""
                val isPublic = snapshot.child("isPublic").value?.toString()?.toBooleanStrictOrNull() ?: true

                val question = QuestionEntity(
                    id, qSchoolId, authorUid, authorName, subject, level, title, content, status, date, answersData, isPublic
                )
                scope.launch {
                    appDao.insertQuestion(question)
                }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val question = QuestionEntity(id, "", "", "", "", "", "", "", "", "", "", true)
                scope.launch {
                    appDao.deleteQuestion(question)
                }
            }
        )

        // Sync Project Groups
        listenToCollection(
            path = "project_groups",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val pSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val title = snapshot.child("title").value?.toString() ?: ""
                val subject = snapshot.child("subject").value?.toString() ?: ""
                val dueDate = snapshot.child("dueDate").value?.toString() ?: ""
                val membersData = snapshot.child("membersData").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: "진행중"
                val createdAt = snapshot.child("createdAt").value?.toString() ?: ""

                val group = ProjectGroupEntity(id, pSchoolId, title, subject, dueDate, membersData, status, createdAt)
                scope.launch { appDao.insertProjectGroup(group) }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val group = ProjectGroupEntity(id, "", "", "", "", "", "", "")
                scope.launch { appDao.deleteProjectGroup(group) }
            }
        )

        // Sync Project Tasks
        listenToCollection(
            path = "project_tasks",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val projectId = snapshot.child("projectId").value?.toString()?.toLongOrNull() ?: 0L
                val tSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val taskName = snapshot.child("taskName").value?.toString() ?: ""
                val assigneeName = snapshot.child("assigneeName").value?.toString() ?: ""
                val roleCategory = snapshot.child("roleCategory").value?.toString() ?: ""
                val dueDate = snapshot.child("dueDate").value?.toString() ?: ""
                val status = snapshot.child("status").value?.toString() ?: "대기"
                val contributionWeight = snapshot.child("contributionWeight").value?.toString()?.toIntOrNull() ?: 3

                val task = ProjectTaskEntity(id, projectId, tSchoolId, taskName, assigneeName, roleCategory, dueDate, status, contributionWeight)
                scope.launch { appDao.insertProjectTask(task) }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val task = ProjectTaskEntity(id, 0L, "", "", "", "", "", "대기", 3)
                scope.launch { appDao.deleteProjectTask(task) }
            }
        )

        // Sync Project Resources
        listenToCollection(
            path = "project_resources",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val projectId = snapshot.child("projectId").value?.toString()?.toLongOrNull() ?: 0L
                val rSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val uploaderName = snapshot.child("uploaderName").value?.toString() ?: ""
                val title = snapshot.child("title").value?.toString() ?: ""
                val linkOrContent = snapshot.child("linkOrContent").value?.toString() ?: ""
                val resourceType = snapshot.child("resourceType").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""

                val resource = ProjectResourceEntity(id, projectId, rSchoolId, uploaderName, title, linkOrContent, resourceType, date)
                scope.launch { appDao.insertProjectResource(resource) }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val resource = ProjectResourceEntity(id, 0L, "", "", "", "", "", "")
                scope.launch { appDao.deleteProjectResource(resource) }
            }
        )

        // Sync Project Evaluations
        listenToCollection(
            path = "project_evaluations",
            schoolId = schoolId,
            onAddedOrChanged = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val projectId = snapshot.child("projectId").value?.toString()?.toLongOrNull() ?: 0L
                val eSchoolId = snapshot.child("schoolId").value?.toString() ?: ""
                val evaluatorName = snapshot.child("evaluatorName").value?.toString() ?: ""
                val targetMemberName = snapshot.child("targetMemberName").value?.toString() ?: ""
                val responsibilityScore = snapshot.child("responsibilityScore").value?.toString()?.toIntOrNull() ?: 5
                val qualityScore = snapshot.child("qualityScore").value?.toString()?.toIntOrNull() ?: 5
                val collaborationScore = snapshot.child("collaborationScore").value?.toString()?.toIntOrNull() ?: 5
                val comment = snapshot.child("comment").value?.toString() ?: ""
                val date = snapshot.child("date").value?.toString() ?: ""

                val eval = ProjectEvaluationEntity(id, projectId, eSchoolId, evaluatorName, targetMemberName, responsibilityScore, qualityScore, collaborationScore, comment, date)
                scope.launch { appDao.insertProjectEvaluation(eval) }
            },
            onRemoved = { snapshot ->
                val id = snapshot.key?.toLongOrNull() ?: return@listenToCollection
                val eval = ProjectEvaluationEntity(id, 0L, "", "", "", 5, 5, 5, "", "")
                scope.launch { appDao.deleteProjectEvaluation(eval) }
            }
        )
    }

    private fun listenToCollection(
        path: String,
        schoolId: String,
        onAddedOrChanged: (com.google.firebase.database.DataSnapshot) -> Unit,
        onRemoved: (com.google.firebase.database.DataSnapshot) -> Unit
    ) {
        val database = db ?: return
        val targetRef = database.getReference(path).child(schoolId)
        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                onAddedOrChanged(snapshot)
            }
            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                onAddedOrChanged(snapshot)
            }
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                onRemoved(snapshot)
            }
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.w("RealtimeSync", "Listen cancelled for path: $path", error.toException())
            }
        }
        targetRef.addChildEventListener(listener)
        listeners.add(object : RegisteredListener {
            override fun remove() {
                targetRef.removeEventListener(listener)
            }
        })
    }

    private fun listenToSingleValue(
        path: String,
        schoolId: String,
        onChanged: (com.google.firebase.database.DataSnapshot) -> Unit
    ) {
        val database = db ?: return
        val targetRef = database.getReference(path).child(schoolId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists()) {
                    onChanged(snapshot)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.w("RealtimeSync", "Listen cancelled for path: $path", error.toException())
            }
        }
        targetRef.addValueEventListener(listener)
        listeners.add(object : RegisteredListener {
            override fun remove() {
                targetRef.removeEventListener(listener)
            }
        })
    }

    fun stopSync() {
        Log.d("RealtimeSync", "Stopping Realtime Database synchronization")
        for (listener in listeners) {
            listener.remove()
        }
        listeners.clear()
        activeSchoolId = null
    }
}
