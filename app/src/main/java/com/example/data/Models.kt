package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val displayName: String,
    val role: String, // student, leader, staff, teacher
    val schoolId: String,
    val createdAt: Long,
    val phoneNumber: String = "",
    val password: String = "",
    val grade: String = "1",
    val classNum: String = "1",
    val num: String = "1"
)

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val schoolId: String,
    val name: String,
    val inviteCode: String,
    val createdAt: Long
)

@Entity(tableName = "rentals")
data class RentalEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val num: String,
    val name: String,
    val item: String,
    val rentDate: String,
    val returnDate: String,
    val status: String // 대여중, 반납완료
)

@Entity(tableName = "suggestions")
data class SuggestionEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val type: String, // 온라인, 오프라인
    val studentId: String,
    val content: String,
    val status: String, // 접수대기, 처리중, 완료
    val comment: String
)

@Entity(tableName = "roster_names")
data class RosterNameEntity(
    @PrimaryKey val id: String, // grade-class-num (e.g., "1학년-1반-5")
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val num: Int,
    val name: String
)

@Entity(tableName = "uniform_checks")
data class UniformCheckEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val num: String,
    val name: String,
    val status: String, // 착용, 미착용
    val date: String
)

@Entity(tableName = "attendances")
data class AttendanceEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val num: String,
    val name: String,
    val status: String, // 출석, 지각, 결석
    val date: String
)

@Entity(tableName = "merit_logs")
data class MeritLogEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val num: String,
    val name: String,
    val score: Int,
    val reason: String,
    val date: String
)

@Entity(tableName = "clean_zones")
data class CleanZoneEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val zone: String,
    val assignee: String,
    val doneDate: String
)

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val type: String, // 수입, 지출
    val title: String,
    val amount: Int,
    val memo: String,
    val date: String
)

@Entity(tableName = "votes")
data class VoteEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val question: String,
    val grade: String,
    val classNum: String,
    val status: String, // open, closed
    val date: String,
    val optionsData: String, // Comma-separated OptionName::VoteCount
    val votersData: String // Comma-separated student ids
)

@Entity(tableName = "lost_items")
data class LostItemEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val name: String,
    val location: String,
    val date: String,
    val status: String, // 보관중, 찾아감
    val claimant: String,
    val photoBase64: String,
    val fileName: String,
    val fileDataBase64: String
)

@Entity(tableName = "item_stocks")
data class ItemStockEntity(
    @PrimaryKey val id: String, // schoolId-itemName
    val schoolId: String,
    val itemName: String,
    val totalQty: Int
)

@Entity(tableName = "invite_codes")
data class InviteCodeEntity(
    @PrimaryKey val inviteCode: String,
    val schoolId: String
)

@Entity(tableName = "wrong_answers")
data class WrongAnswerEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val studentUid: String,
    val studentName: String,
    val subject: String,
    val problemTitle: String,
    val problemDescription: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val errorReason: String,
    val date: String,
    val aiAnalysis: String = "",
    val aiSimilarQuestion: String = ""
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val authorUid: String,
    val authorName: String,
    val subject: String,
    val level: String, // 기초, 기본, 심화
    val title: String,
    val content: String,
    val status: String, // 답변대기, 답변완료
    val date: String,
    val answersData: String = "", // Serialized pipe/newline separated answers
    val isPublic: Boolean = true // 전체공개 여부 (true: 전체공개, false: 비공개 AI 전용)
)

@Entity(tableName = "project_groups")
data class ProjectGroupEntity(
    @PrimaryKey val id: Long,
    val schoolId: String,
    val title: String,
    val subject: String,
    val dueDate: String,
    val membersData: String, // Comma-separated member names
    val status: String = "진행중", // 진행중, 완료
    val createdAt: String
)

@Entity(tableName = "project_tasks")
data class ProjectTaskEntity(
    @PrimaryKey val id: Long,
    val projectId: Long,
    val schoolId: String,
    val taskName: String,
    val assigneeName: String,
    val roleCategory: String, // 총괄/리더, 자료조사, PPT제작, 발표, 보고서작성, 기타
    val dueDate: String,
    val status: String = "대기", // 대기, 진행중, 완료
    val contributionWeight: Int = 3 // 1~5
)

@Entity(tableName = "project_resources")
data class ProjectResourceEntity(
    @PrimaryKey val id: Long,
    val projectId: Long,
    val schoolId: String,
    val uploaderName: String,
    val title: String,
    val linkOrContent: String,
    val resourceType: String, // 참고자료/링크, 보고서초안, 발표자료, 회의록
    val date: String
)

@Entity(tableName = "project_evaluations")
data class ProjectEvaluationEntity(
    @PrimaryKey val id: Long,
    val projectId: Long,
    val schoolId: String,
    val evaluatorName: String,
    val targetMemberName: String,
    val responsibilityScore: Int, // 1~5
    val qualityScore: Int, // 1~5
    val collaborationScore: Int, // 1~5
    val comment: String,
    val date: String
)

@Entity(tableName = "timetables")
data class TimetableEntity(
    @PrimaryKey val id: String, // e.g. "schoolId-grade-classNum-day-period"
    val schoolId: String,
    val grade: String,
    val classNum: String,
    val dayOfWeek: String, // 월, 화, 수, 목, 금
    val period: Int, // 1~7교시
    val subject: String,
    val teacherName: String = "",
    val classroom: String = ""
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String, // "myUid-friendUid"
    val myUid: String,
    val friendUid: String,
    val friendName: String,
    val friendPhone: String = "",
    val addedAt: Long = System.currentTimeMillis()
)


