package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuestionEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.questions.collectAsState()

    var selectedLevelFilter by remember { mutableStateOf("전체") }
    var selectedSubjectFilter by remember { mutableStateOf("전체") }
    var showAddModal by remember { mutableStateOf(false) }

    var selectedQuestionForAnswer by remember { mutableStateOf<QuestionEntity?>(null) }
    var humanAnswerInput by remember { mutableStateOf("") }

    var selectedQuestionForViewDetail by remember { mutableStateOf<QuestionEntity?>(null) }

    // Form states
    var subjectInput by remember { mutableStateOf("수학") }
    var levelInput by remember { mutableStateOf("기본") }
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var isPublicInput by remember { mutableStateOf(true) }

    val currentUid = viewModel.currentUser?.uid ?: ""
    val currentUserRole = viewModel.currentUser?.role ?: ""

    val levels = listOf("전체", "기초", "기본", "심화")
    val levelOptions = listOf("기초", "기본", "심화")
    val subjects = listOf("전체", "수학", "영어", "국어", "과학", "사회", "기타")
    val subjectOptions = listOf("수학", "영어", "국어", "과학", "사회", "기타")

    val filteredList = questions.filter { q ->
        (selectedLevelFilter == "전체" || q.level == selectedLevelFilter) &&
        (selectedSubjectFilter == "전체" || q.subject == selectedSubjectFilter) &&
        (q.isPublic || q.authorUid == currentUid || currentUserRole == "teacher")
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PanelSolid)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.25f),
                                    NeonGreen.copy(alpha = 0.15f),
                                    PanelSolid
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NeonCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = NeonCyan)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🙋 실시간 수준별 Q&A 매칭",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpaceText,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "수준별 질문 등록 & AI 튜터 답변",
                                    fontSize = 11.sp,
                                    color = SpaceTextSoft,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level status cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PanelGlass)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🟢 기초 레벨", fontSize = 10.sp, color = SpaceTextSoft)
                                Text("${questions.count { it.level == "기초" }}건", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PanelGlass)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🟡 기본 레벨", fontSize = 10.sp, color = SpaceTextSoft)
                                Text("${questions.count { it.level == "기본" }}건", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PanelGlass)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔴 심화 레벨", fontSize = 10.sp, color = SpaceTextSoft)
                                Text("${questions.count { it.level == "심화" }}건", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters: Level & Subject
        Text("수준별 & 과목별 매칭 필터", fontSize = 12.sp, color = SpaceTextSoft, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            levels.forEach { lvl ->
                val selected = selectedLevelFilter == lvl
                val chipColor = when(lvl) {
                    "기초" -> NeonGreen
                    "기본" -> NeonAmber
                    "심화" -> NeonRed
                    else -> NeonCyan
                }
                FilterChip(
                    selected = selected,
                    onClick = { selectedLevelFilter = lvl },
                    label = { Text(if (lvl == "전체") "전체 난이도" else "$lvl 난이도", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = if (chipColor == NeonAmber || chipColor == NeonCyan) Color.Black else Color.White,
                        containerColor = PanelGlass,
                        labelColor = SpaceText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subjects.forEach { subj ->
                val selected = selectedSubjectFilter == subj
                FilterChip(
                    selected = selected,
                    onClick = { selectedSubjectFilter = subj },
                    label = { Text(subj, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = PanelGlass,
                        labelColor = SpaceText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PanelGlass)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(48.dp), tint = SpaceTextSoft)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("조건에 해당되는 Q&A 질문이 없습니다.", fontSize = 14.sp, color = SpaceTextSoft)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("새로운 질문을 올려 AI 튜터와 교사/동료의 맞춤 답변을 받아보세요!", fontSize = 12.sp, color = NeonCyan)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredList.forEach { q ->
                    QuestionCard(
                        item = q,
                        onAiAnswerClick = { viewModel.answerQuestionWithGemini(q) },
                        onHumanAnswerClick = { selectedQuestionForAnswer = q },
                        onViewDetailClick = { selectedQuestionForViewDetail = q },
                        onDeleteClick = { viewModel.deleteQuestion(q) }
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(70.dp))
        }

        FloatingActionButton(
            onClick = { showAddModal = true },
            containerColor = NeonCyan,
            contentColor = Color.Black,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "질문하기", tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("질문하기", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
            }
        }
    }

    // Modal: Add New Question
    if (showAddModal) {
        AlertDialog(
            onDismissRequest = { showAddModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isEmpty() || contentInput.isEmpty()) {
                            viewModel.showToast("⚠️ 질문 제목과 내용을 모두 입력해 주세요.")
                            return@Button
                        }
                        viewModel.addQuestion(
                            subject = subjectInput,
                            level = levelInput,
                            title = titleInput,
                            content = contentInput,
                            isPublic = isPublicInput
                        )
                        titleInput = ""
                        contentInput = ""
                        isPublicInput = true
                        showAddModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("질문 등록하기", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModal = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("수준별 Q&A 질문 작성", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔒 공개 범위 선택", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isPublicInput,
                            onClick = { isPublicInput = true },
                            label = { Text("🌐 전체 공개", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = PanelGlass,
                                labelColor = SpaceText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isPublicInput,
                            onClick = { isPublicInput = false },
                            label = { Text("🔒 비공개 (AI 전용)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White,
                                containerColor = PanelGlass,
                                labelColor = SpaceText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PanelGlass,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isPublicInput) {
                                "📢 전체 공개: 가입한 모든 사용자(교사, 동료)와 AI가 답변을 확인할 수 있습니다."
                            } else {
                                "🔒 비공개: 나만 확인할 수 있으며, AI 튜터가 전용으로 맞춤 답변을 제공합니다."
                            },
                            fontSize = 11.sp,
                            color = SpaceTextSoft,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("질문 난이도 수준", fontSize = 11.sp, color = SpaceTextSoft)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        levelOptions.forEach { lvl ->
                            FilterChip(
                                selected = levelInput == lvl,
                                onClick = { levelInput = lvl },
                                label = { Text(lvl, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("과목 선택", fontSize = 11.sp, color = SpaceTextSoft)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjectOptions.forEach { subj ->
                            FilterChip(
                                selected = subjectInput == subj,
                                onClick = { subjectInput = subj },
                                label = { Text(subj, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("질문 제목") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("상세 질문 내용 (풀다가 막힌 부분을 상세히 적어주세요)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            },
            containerColor = PanelSolid
        )
    }

    // Modal: Human Write Answer
    selectedQuestionForAnswer?.let { q ->
        AlertDialog(
            onDismissRequest = { selectedQuestionForAnswer = null },
            confirmButton = {
                Button(
                    onClick = {
                        if (humanAnswerInput.isEmpty()) {
                            viewModel.showToast("⚠️ 답변 내용을 입력해 주세요.")
                            return@Button
                        }
                        viewModel.addHumanAnswerToQuestion(q, humanAnswerInput)
                        humanAnswerInput = ""
                        selectedQuestionForAnswer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("답변 등록", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedQuestionForAnswer = null }) {
                    Text("취소", color = SpaceTextSoft)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💬 답변 작성 (선생님/동료)", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("질문: ${q.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpaceText)
                    OutlinedTextField(
                        value = humanAnswerInput,
                        onValueChange = { humanAnswerInput = it },
                        label = { Text("친절하고 상세한 답변을 작성해 주세요") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            },
            containerColor = PanelSolid
        )
    }

    // Modal: View Question Detail & All Answers
    selectedQuestionForViewDetail?.let { q ->
        AlertDialog(
            onDismissRequest = { selectedQuestionForViewDetail = null },
            confirmButton = {
                Button(
                    onClick = { selectedQuestionForViewDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("닫기", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Q&A 상세 및 답변 목록", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PanelGlass,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when(q.level) {
                                        "기초" -> NeonGreen.copy(alpha = 0.2f)
                                        "기본" -> NeonAmber.copy(alpha = 0.2f)
                                        else -> NeonRed.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = "${q.level} 난이도",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when(q.level) {
                                            "기초" -> NeonGreen
                                            "기본" -> NeonAmber
                                            else -> NeonRed
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("[${q.subject}]", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(q.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpaceText)
                            Text("작성자: ${q.authorName} | 일자: ${q.date}", fontSize = 11.sp, color = SpaceTextSoft)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(q.content, fontSize = 13.sp, color = SpaceText)
                        }
                    }

                    Text("💬 등록된 답변 (${if (q.answersData.isEmpty()) 0 else q.answersData.split("---").size}개)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                    if (q.answersData.isEmpty()) {
                        Text("아직 등록된 답변이 없습니다. AI 튜터에게 1초 답변을 받아보세요!", fontSize = 12.sp, color = SpaceTextSoft)
                    } else {
                        val answerBlocks = q.answersData.split("---")
                        answerBlocks.forEach { block ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PanelGlass,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = block.trim(),
                                    fontSize = 12.sp,
                                    color = SpaceText,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = PanelSolid
        )
    }
}

@Composable
fun QuestionCard(
    item: QuestionEntity,
    onAiAnswerClick: () -> Unit,
    onHumanAnswerClick: () -> Unit,
    onViewDetailClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val levelColor = when(item.level) {
        "기초" -> NeonGreen
        "기본" -> NeonAmber
        else -> NeonRed
    }

    val answerCount = if (item.answersData.isEmpty()) 0 else item.answersData.split("---").size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelSolid)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.isPublic) NeonCyan.copy(alpha = 0.2f) else NeonPurple.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (item.isPublic) "🌐 전체공개" else "🔒 비공개 AI전용",
                            color = if (item.isPublic) NeonCyan else NeonPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = levelColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${item.level} 수준",
                            color = levelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("[${item.subject}]", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpaceText,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = SpaceTextSoft)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.content,
                fontSize = 13.sp,
                color = SpaceTextSoft,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("작성자: ${item.authorName} (${item.date})", fontSize = 11.sp, color = SpaceTextSoft)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (answerCount > 0) NeonGreen.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (answerCount > 0) "답변 완료 ($answerCount)" else "답변 대기",
                        fontSize = 10.sp,
                        color = if (answerCount > 0) NeonGreen else NeonAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SpaceTextSoft.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.isPublic) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onAiAnswerClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⚡ AI 즉시 답변", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = onHumanAnswerClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("💬 직접 답변", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                        }
                    }
                } else {
                    // Private Q&A (AI only)
                    Button(
                        onClick = onAiAnswerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔒 AI 튜터 맞춤 답변 요청 / 재생성", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onViewDetailClick,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    Text("📖 답변 상세 내역 보기 ($answerCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
