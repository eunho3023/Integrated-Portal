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
import com.example.data.WrongAnswerEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLearningScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wrongAnswers by viewModel.wrongAnswers.collectAsState()

    var selectedSubjectFilter by remember { mutableStateOf("전체") }
    var showAddModal by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<WrongAnswerEntity?>(null) }

    // Form inputs for adding a wrong answer
    var subjectInput by remember { mutableStateOf("수학") }
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var studentAnswerInput by remember { mutableStateOf("") }
    var correctAnswerInput by remember { mutableStateOf("") }
    var errorReasonInput by remember { mutableStateOf("개념 미흡") }

    val subjects = listOf("전체", "수학", "영어", "국어", "과학", "사회", "기타")
    val subjectOptions = listOf("수학", "영어", "국어", "과학", "사회", "기타")
    val errorReasons = listOf("개념 미흡", "계산 실수", "문제 오해", "시간 부족", "공식 암기 부족")

    val filteredList = if (selectedSubjectFilter == "전체") wrongAnswers else wrongAnswers.filter { it.subject == selectedSubjectFilter }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Card
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
                                    NeonPurple.copy(alpha = 0.25f),
                                    NeonCyan.copy(alpha = 0.15f),
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
                                color = NeonPurple.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonPurple)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🤖 AI 오답노트 & 취약점 분석",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpaceText,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Gemini AI 약점 진단 리포트",
                                    fontSize = 11.sp,
                                    color = SpaceTextSoft,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PanelGlass)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("총 등록 오답", fontSize = 11.sp, color = SpaceTextSoft)
                                Text("${wrongAnswers.size}건", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PanelGlass)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("AI 분석 완료", fontSize = 11.sp, color = SpaceTextSoft)
                                Text("${wrongAnswers.count { it.aiAnalysis.isNotEmpty() }}건", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Filter Bar
        Text("과목별 오답노트 필터", fontSize = 12.sp, color = SpaceTextSoft, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subject ->
                val selected = selectedSubjectFilter == subject
                FilterChip(
                    selected = selected,
                    onClick = { selectedSubjectFilter = subject },
                    label = { Text(subject, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = Color.White,
                        containerColor = PanelGlass,
                        labelColor = SpaceText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Empty state
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
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SpaceTextSoft
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedSubjectFilter == "전체") "등록된 오답노트가 없습니다." else "[$selectedSubjectFilter] 과목에 등록된 오답노트가 없습니다.",
                        fontSize = 14.sp,
                        color = SpaceTextSoft
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "우측 상단 '+ 오답 등록' 버튼을 눌러 오답을 등록하고 AI 분석을 받아보세요!",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Wrong Answer Item List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredList.forEach { item ->
                    WrongAnswerCard(
                        item = item,
                        onAnalyzeClick = { viewModel.analyzeWrongAnswerWithGemini(item) },
                        onDeleteClick = { viewModel.deleteWrongAnswer(item) },
                        onDetailClick = { selectedItemForDetail = item }
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(70.dp))
        }

        FloatingActionButton(
            onClick = { showAddModal = true },
            containerColor = NeonPurple,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "오답 등록")
                Spacer(modifier = Modifier.width(6.dp))
                Text("오답 등록", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Modal Dialog: Add Wrong Answer
    if (showAddModal) {
        AlertDialog(
            onDismissRequest = { showAddModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isEmpty() || descriptionInput.isEmpty()) {
                            viewModel.showToast("⚠️ 문제 제목과 문제 내용을 입력해주세요.")
                            return@Button
                        }
                        viewModel.addWrongAnswer(
                            subject = subjectInput,
                            problemTitle = titleInput,
                            problemDescription = descriptionInput,
                            studentAnswer = studentAnswerInput,
                            correctAnswer = correctAnswerInput,
                            errorReason = errorReasonInput
                        )
                        titleInput = ""
                        descriptionInput = ""
                        studentAnswerInput = ""
                        correctAnswerInput = ""
                        showAddModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("오답 등록하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModal = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = NeonPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("신규 오답 등록", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                        label = { Text("문제 제목 (예: 이차방정식 근의 공식 문제)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("문제 내용 및 상황 설명") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = studentAnswerInput,
                        onValueChange = { studentAnswerInput = it },
                        label = { Text("내가 적은 오답") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = correctAnswerInput,
                        onValueChange = { correctAnswerInput = it },
                        label = { Text("정답") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("오답 주요 원인", fontSize = 11.sp, color = SpaceTextSoft)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        errorReasons.forEach { reason ->
                            FilterChip(
                                selected = errorReasonInput == reason,
                                onClick = { errorReasonInput = reason },
                                label = { Text(reason, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            containerColor = PanelSolid
        )
    }

    // Modal Dialog: View Full AI Report Detail
    selectedItemForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            confirmButton = {
                Button(
                    onClick = { selectedItemForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🤖 AI 맞춤 취약점 상세 분석", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📌 [${item.subject}] ${item.problemTitle}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)
                            Text("등록 학생: ${item.studentName} | 일자: ${item.date}", fontSize = 11.sp, color = SpaceTextSoft)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("문제: ${item.problemDescription}", fontSize = 12.sp, color = SpaceText)
                            Text("오답: ${item.studentAnswer} | 정답: ${item.correctAnswer}", fontSize = 12.sp, color = NeonAmber)
                        }
                    }

                    if (item.aiAnalysis.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("아직 AI 분석이 진행되지 않았습니다.", color = SpaceTextSoft, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.analyzeWrongAnswerWithGemini(item)
                                        selectedItemForDetail = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                ) {
                                    Text("지금 Gemini AI 분석 시작", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text("💡 Gemini AI 취약점 진단 & 클리닉", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PanelGlass,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.aiAnalysis,
                                fontSize = 12.sp,
                                color = SpaceText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (item.aiSimilarQuestion.isNotEmpty()) {
                            Text("📝 AI 추천 유사 연습 문제", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PanelGlass,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.aiSimilarQuestion,
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
fun WrongAnswerCard(
    item: WrongAnswerEntity,
    onAnalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDetailClick: () -> Unit
) {
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
                        color = NeonPurple.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.subject,
                            color = NeonPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.problemTitle,
                        fontSize = 15.sp,
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
                text = item.problemDescription,
                fontSize = 13.sp,
                color = SpaceTextSoft,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "❌ 오답: ${item.studentAnswer}",
                        fontSize = 12.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "⭕ 정답: ${item.correctAnswer}",
                        fontSize = 12.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NeonAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "원인: ${item.errorReason}",
                        fontSize = 10.sp,
                        color = NeonAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SpaceTextSoft.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.aiAnalysis.isNotEmpty()) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI 분석 리포트 완료", fontSize = 12.sp, color = NeonGreen, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onDetailClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PanelGlass),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("상세 분석서 보기", fontSize = 11.sp, color = NeonCyan)
                    }
                } else {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI 분석 대기중", fontSize = 12.sp, color = NeonAmber, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onAnalyzeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🤖 AI 맞춤 분석", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
