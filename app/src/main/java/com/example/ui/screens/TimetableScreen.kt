package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun TimetableScreen(viewModel: MainViewModel) {
    val user = viewModel.currentUser
    val school = viewModel.currentSchool
    val timetables by viewModel.timetableList.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("월") }
    var selectedPeriod by remember { mutableStateOf(1) }
    var editSubject by remember { mutableStateOf("") }
    var editTeacher by remember { mutableStateOf("") }
    var editClassroom by remember { mutableStateOf("") }

    val days = listOf("월", "화", "수", "목", "금")
    val periods = (1..7).toList()

    // Map timetable by Day and Period for quick lookup
    val timetableMap = remember(timetables) {
        timetables.associateBy { "${it.dayOfWeek}-${it.period}" }
    }

    LaunchedEffect(timetables) {
        if (timetables.isEmpty() && user != null) {
            viewModel.seedDefaultTimetableForClass()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan)
                        Text(
                            text = "📅 학급 시간표 센터",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.seedDefaultTimetableForClass() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("표준 시간표 초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "소속: ${school?.name ?: "미지정"} | ${user?.grade ?: "1"}학년 ${user?.classNum ?: "1"}반 (${user?.num ?: "1"}번 ${user?.displayName ?: ""})",
                    color = SpaceText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "💡 각 교시를 클릭하면 과목명, 담당 선생님, 교실 정보를 수정할 수 있습니다.",
                    color = SpaceTextSoft,
                    fontSize = 11.5.sp
                )
            }
        }

        // Timetable Grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelGlass),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Days Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("교시", color = SpaceTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    days.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${day}요일", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = BorderGlow.copy(alpha = 0.2f))

                // Periods Rows
                periods.forEach { period ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Period Label Column
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(56.dp)
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${period}교시", color = SpaceTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        days.forEach { day ->
                            val entry = timetableMap["$day-$period"]
                            val hasSubject = entry != null && entry.subject.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (hasSubject) NeonPurple.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.02f)
                                    )
                                    .border(
                                        1.dp,
                                        if (hasSubject) NeonPurple.copy(alpha = 0.35f)
                                        else BorderGlow.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedDay = day
                                        selectedPeriod = period
                                        editSubject = entry?.subject ?: ""
                                        editTeacher = entry?.teacherName ?: ""
                                        editClassroom = entry?.classroom ?: ""
                                        showEditDialog = true
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasSubject) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = entry!!.subject,
                                            color = SpaceText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        if (entry.teacherName.isNotEmpty()) {
                                            Text(
                                                text = entry.teacherName,
                                                color = SpaceTextSoft,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add subject",
                                        tint = SpaceTextSoft.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Timetable Dialog Modal
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "✏️ ${selectedDay}요일 ${selectedPeriod}교시 과목 수정",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editSubject,
                        onValueChange = { editSubject = it },
                        label = { Text("과목명 (예: 국어, 수학, 영어)") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTeacher,
                        onValueChange = { editTeacher = it },
                        label = { Text("담당 선생님 이름 (선택)") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editClassroom,
                        onValueChange = { editClassroom = it },
                        label = { Text("강의실 / 교실 (선택)") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTimetableEntry(
                            dayOfWeek = selectedDay,
                            period = selectedPeriod,
                            subject = editSubject,
                            teacherName = editTeacher,
                            classroom = editClassroom
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditDialog = false },
                    border = BorderStroke(1.dp, SpaceTextSoft)
                ) {
                    Text("취소", color = SpaceText)
                }
            },
            containerColor = PanelSolid,
            shape = RoundedCornerShape(14.dp)
        )
    }
}
