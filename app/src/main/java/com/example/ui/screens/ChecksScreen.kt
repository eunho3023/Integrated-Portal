@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecksScreen(
    viewModel: MainViewModel,
    tabId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (tabId) {
        "uniform-tab" -> UniformCheckTab(viewModel)
        "attend-tab" -> AttendanceCheckTab(viewModel)
        "merit-tab" -> MeritCheckTab(viewModel)
    }
}

// -----------------------------------------------------------------
// UNIFORM CHECK TAB
// -----------------------------------------------------------------
@Composable
fun UniformCheckTab(viewModel: MainViewModel) {
    val checks by viewModel.uniformChecks.collectAsState()
    val roster by viewModel.rosterNames.collectAsState()
    val context = LocalContext.current

    var pwText by remember { mutableStateOf("") }
    var selectedGradeLocal by remember { mutableStateOf(viewModel.selectedGrade) }
    var selectedClassLocal by remember { mutableStateOf(viewModel.selectedClass) }

    var bulkNamesText by remember { mutableStateOf("") }
    var showBulkNamesDialog by remember { mutableStateOf(false) }

    val activeRoster = viewModel.getRosterForClass(selectedGradeLocal, selectedClassLocal)
    val todayClassChecks = checks.filter {
        it.grade == selectedGradeLocal && it.classNum == selectedClassLocal && it.date == viewModel.todayDateString
    }
    val wearCount = todayClassChecks.count { it.status == "착용" }
    val notWearCount = todayClassChecks.count { it.status == "미착용" }
    val uncheckedCount = (24 - wearCount - notWearCount).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Unlock Panel
        if (!viewModel.uniformUnlocked) {
            GlassmorphicCard(accentColor = NeonAmber) {
                Text("🧥 학생회 및 교사 전용 메뉴", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("교복 착용 상태를 점검하고 명렬표를 등록하려면 전용 확인번호가 필요합니다.", color = SpaceTextSoft, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pwText,
                        onValueChange = { pwText = it },
                        placeholder = { Text("확인 번호 입력") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (pwText == viewModel.STAFF_PASSWORD || pwText == viewModel.ADMIN_PASSWORD) {
                                viewModel.uniformUnlocked = true
                                pwText = ""
                                viewModel.showToast("🔓 교복 점검 메뉴 권한이 활성화되었습니다.")
                            } else {
                                viewModel.showToast("❌ 비밀번호가 올바르지 않습니다.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("해제", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        } else {
            // Main Controls
            GlassmorphicCard(accentColor = NeonAmber) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋 학급 점검 설정", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { viewModel.uniformUnlocked = false }) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonAmber)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedGradeLocal, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("1학년", "2학년", "3학년").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedGradeLocal = it; expanded = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedClassLocal, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            (1..10).map { "${it}반" }.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedClassLocal = it; expanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.bulkSetUniformStatus(selectedGradeLocal, selectedClassLocal, "착용") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("전체 착용", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.bulkSetUniformStatus(selectedGradeLocal, selectedClassLocal, "미착용") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("전체 미착용", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showBulkNamesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f), contentColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("이름 일괄등록", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.clearUniformData() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("전체초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Summary Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅ 착용", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("$wearCount 명", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚫 미착용", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("$notWearCount 명", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .border(1.dp, BorderGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⬜ 미점검", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("$uncheckedCount 명", color = SpaceTextSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Roster Table Form 1..24
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141F32))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("번호", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("학생 이름", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("상태 체크", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(3f), textAlign = TextAlign.End)
                }

                activeRoster.forEachIndexed { idx, student ->
                    val checkObj = todayClassChecks.find { it.num == student.num.toString() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${student.num}번", color = SpaceTextSoft, fontSize = 12.5.sp, modifier = Modifier.weight(1f))

                        var tempName by remember(student.name) { mutableStateOf(student.name) }
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = {
                                tempName = it
                                viewModel.saveRosterName(selectedGradeLocal, selectedClassLocal, student.num, it)
                            },
                            placeholder = { Text("이름") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp)
                        )

                        Row(
                            modifier = Modifier.weight(3f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.setUniformStatus(selectedGradeLocal, selectedClassLocal, student.num.toString(), tempName, "착용") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (checkObj?.status == "착용") NeonGreen else Color.White.copy(alpha = 0.05f),
                                    contentColor = if (checkObj?.status == "착용") Color.Black else SpaceTextSoft
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("착용", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.setUniformStatus(selectedGradeLocal, selectedClassLocal, student.num.toString(), tempName, "미착용") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (checkObj?.status == "미착용") NeonRed else Color.White.copy(alpha = 0.05f),
                                    contentColor = if (checkObj?.status == "미착용") Color.Black else SpaceTextSoft
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("미착용", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (idx < activeRoster.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }

            // Cross-class general log
            val crossClassChecks = checks.filter { it.date == viewModel.todayDateString }
            if (crossClassChecks.isNotEmpty()) {
                Text("📊 오늘 전체 교복 점검 미착용자 목록", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                ) {
                    crossClassChecks.filter { it.status == "미착용" }.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.grade} ${item.classNum} ${item.num}번 ${item.name}", color = SpaceText, fontSize = 13.sp)
                            IconButton(onClick = { viewModel.deleteUniformCheck(item.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkNamesDialog) {
        AlertDialog(
            onDismissRequest = { showBulkNamesDialog = false },
            title = { Text("이름 일괄 등록 (1번부터 순서대로)", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("쉼표(,) 혹은 줄바꿈으로 이름을 24개 구분하여 기입하세요.", color = SpaceTextSoft, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bulkNamesText,
                        onValueChange = { bulkNamesText = it },
                        placeholder = { Text("홍길동, 김철수, 이영희...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = bulkNamesText.split(Regex("[,\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.saveRosterNamesBulk(selectedGradeLocal, selectedClassLocal, list)
                    showBulkNamesDialog = false
                    bulkNamesText = ""
                }) {
                    Text("등록", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkNamesDialog = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// ATTENDANCE CHECK TAB
// -----------------------------------------------------------------
@Composable
fun AttendanceCheckTab(viewModel: MainViewModel) {
    val attendances by viewModel.attendances.collectAsState()
    var pwText by remember { mutableStateOf("") }
    var selectedGradeLocal by remember { mutableStateOf(viewModel.attendSelectedGrade) }
    var selectedClassLocal by remember { mutableStateOf(viewModel.attendSelectedClass) }

    val activeRoster = viewModel.getRosterForClass(selectedGradeLocal, selectedClassLocal)
    val todayClassChecks = attendances.filter {
        it.grade == selectedGradeLocal && it.classNum == selectedClassLocal && it.date == viewModel.todayDateString
    }
    val presentCount = todayClassChecks.count { it.status == "출석" }
    val lateCount = todayClassChecks.count { it.status == "지각" }
    val absentCount = todayClassChecks.count { it.status == "결석" }
    val uncheckedCount = (24 - presentCount - lateCount - absentCount).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Unlock Panel
        if (!viewModel.attendUnlocked) {
            GlassmorphicCard(accentColor = NeonGreen) {
                Text("📝 교사 전용 출결 관리", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("학생들의 출석, 지각, 결석 상태를 체크하려면 비밀번호가 필요합니다.", color = SpaceTextSoft, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pwText,
                        onValueChange = { pwText = it },
                        placeholder = { Text("확인 번호 입력") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (pwText == viewModel.STAFF_PASSWORD || pwText == viewModel.ADMIN_PASSWORD) {
                                viewModel.attendUnlocked = true
                                pwText = ""
                                viewModel.showToast("🔓 출결 관리 권한이 활성화되었습니다.")
                            } else {
                                viewModel.showToast("❌ 비밀번호가 올바르지 않습니다.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("해제", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        } else {
            // Main Controls
            GlassmorphicCard(accentColor = NeonGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋 학급 출결 설정", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { viewModel.attendUnlocked = false }) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonGreen)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedGradeLocal, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("1학년", "2학년", "3학년").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedGradeLocal = it; expanded = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedClassLocal, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            (1..10).map { "${it}반" }.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedClassLocal = it; expanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.bulkSetAttendanceStatus(selectedGradeLocal, selectedClassLocal, "출석") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("전체 출석", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.clearAttendData() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("전체초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Status Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("출석" to presentCount, "지각" to lateCount, "결석" to absentCount, "미점검" to uncheckedCount).forEach { pair ->
                    val color = when (pair.first) {
                        "출석" -> NeonGreen
                        "지각" -> NeonAmber
                        "결석" -> NeonRed
                        else -> SpaceTextSoft
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PanelSolid)
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pair.first, color = SpaceTextSoft, fontSize = 10.5.sp)
                            Text("${pair.second} 명", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Attendance list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141F32))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("번호", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("학생 이름", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("출결 체크", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(4f), textAlign = TextAlign.End)
                }

                activeRoster.forEachIndexed { idx, student ->
                    val checkObj = todayClassChecks.find { it.num == student.num.toString() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${student.num}번", color = SpaceTextSoft, fontSize = 12.5.sp, modifier = Modifier.weight(1f))

                        var tempName by remember(student.name) { mutableStateOf(student.name) }
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = {
                                tempName = it
                                viewModel.saveRosterName(selectedGradeLocal, selectedClassLocal, student.num, it)
                            },
                            placeholder = { Text("이름") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp)
                        )

                        Row(
                            modifier = Modifier.weight(4f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("출석", "지각", "결석").forEach { status ->
                                val color = when (status) {
                                    "출석" -> NeonGreen
                                    "지각" -> NeonAmber
                                    else -> NeonRed
                                }

                                Button(
                                    onClick = { viewModel.setAttendanceStatus(selectedGradeLocal, selectedClassLocal, student.num.toString(), tempName, status) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (checkObj?.status == status) color else Color.White.copy(alpha = 0.05f),
                                        contentColor = if (checkObj?.status == status) Color.Black else SpaceTextSoft
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp).padding(horizontal = 2.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (idx < activeRoster.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MERIT POINTS TAB
// -----------------------------------------------------------------
@Composable
fun MeritCheckTab(viewModel: MainViewModel) {
    val meritLogs by viewModel.meritLogs.collectAsState()
    var pwText by remember { mutableStateOf("") }

    var logGrade by remember { mutableStateOf("1학년") }
    var logClass by remember { mutableStateOf("1반") }
    var logNum by remember { mutableStateOf("") }
    var logName by remember { mutableStateOf("") }
    var logScore by remember { mutableStateOf("") }
    var logReason by remember { mutableStateOf("") }

    // Leaderboard logic
    val leaderboard = remember(meritLogs) {
        val totals = mutableMapOf<String, Triple<String, String, Int>>() // Name -> Triple(Grade, Class, TotalScore)
        meritLogs.forEach { log ->
            val key = "${log.grade}-${log.classNum}-${log.num}-${log.name}"
            val existing = totals[key] ?: Triple(log.grade, log.classNum, 0)
            totals[key] = Triple(existing.first, existing.second, existing.third + log.score)
        }
        totals.entries
            .map { entry ->
                val parts = entry.key.split("-")
                val name = parts.last()
                val num = parts[2]
                LeaderboardRow(entry.value.first, entry.value.second, num, name, entry.value.third)
            }
            .sortedByDescending { it.totalScore }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Public Leaderboard
        Text("🏆 학급 누적 상벌점 순위 (실시간)", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        if (leaderboard.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("아직 부여된 상벌점 내역이 없습니다.", color = SpaceTextSoft, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141F32))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("순위", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("학년/반", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                    Text("이름", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                    Text("누계 점수", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }

                leaderboard.forEachIndexed { index, row ->
                    val rank = index + 1
                    val rankColor = when (rank) {
                        1 -> NeonAmber
                        2 -> Color(0xFFCCC2DC)
                        3 -> Color(0xFFE0A86F)
                        else -> SpaceTextSoft
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (rank <= 3) "🏆 $rank" else "$rank",
                            color = rankColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${row.grade} ${row.classNum} ${row.num}번", color = SpaceText, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                        Text(row.name, color = SpaceText, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                        Text(
                            text = if (row.totalScore > 0) "+${row.totalScore}" else "${row.totalScore}",
                            color = if (row.totalScore >= 0) NeonGreen else NeonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                    }

                    if (index < leaderboard.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }
        }

        // Pinned Lock for teacher log entry
        if (!viewModel.meritUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Open quick dialog or simple prompt
                    }
                    .border(1.dp, NeonPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .background(NeonPurple.copy(alpha = 0.05f))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🔒 상벌점 등록은 교사 전용 권한입니다.", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.widthIn(max = 280.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pwText,
                            onValueChange = { pwText = it },
                            placeholder = { Text("교사 패스코드") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            modifier = Modifier.height(48.dp).weight(1.2f)
                        )
                        Button(
                            onClick = {
                                if (pwText == viewModel.ADMIN_PASSWORD) {
                                    viewModel.meritUnlocked = true
                                    pwText = ""
                                    viewModel.showToast("🔓 상벌점 기록 권한이 해제되었습니다.")
                                } else {
                                    viewModel.showToast("❌ 비밀번호 오류.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("확인", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Teacher input form
            GlassmorphicCard(accentColor = NeonPurple) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ 상벌점 가감 등록", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { viewModel.meritUnlocked = false }) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonPurple)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(logGrade, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("1학년", "2학년", "3학년").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { logGrade = it; expanded = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(logClass, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            (1..10).map { "${it}반" }.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { logClass = it; expanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = logNum,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) logNum = it },
                        placeholder = { Text("예: 5") },
                        label = { Text("번호", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = logName,
                        onValueChange = { logName = it },
                        placeholder = { Text("홍길동") },
                        label = { Text("이름", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = logScore,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) logScore = it },
                        placeholder = { Text("상점은 3, 벌점은 -2") },
                        label = { Text("점수", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = logReason,
                        onValueChange = { logReason = it },
                        placeholder = { Text("예: 수업태도 우수, 무단외출 등") },
                        label = { Text("부여 사유", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(2f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val scoreInt = logScore.toIntOrNull()
                        if (logNum.isEmpty() || logName.isEmpty() || scoreInt == null || logReason.isEmpty()) {
                            viewModel.showToast("⚠️ 정확한 정보를 입력해 주세요.")
                            return@Button
                        }
                        viewModel.addMeritLog(logGrade, logClass, logNum, logName, scoreInt, logReason)
                        logNum = ""
                        logName = ""
                        logScore = ""
                        logReason = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("상벌점 가감 등록 완료", fontWeight = FontWeight.Bold)
                }
            }

            // Detailed Logs
            Text("📋 부여 내역 로그", color = SpaceTextSoft, fontSize = 13.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                meritLogs.reversed().forEachIndexed { index, log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${log.grade} ${log.classNum} ${log.num}번 ${log.name}", color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(log.reason, color = SpaceTextSoft, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (log.score > 0) "+${log.score}점" else "${log.score}점",
                                color = if (log.score > 0) NeonGreen else NeonRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(onClick = { viewModel.deleteMeritLog(log) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (index < meritLogs.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }
        }
    }
}

data class LeaderboardRow(
    val grade: String,
    val classNum: String,
    val num: String,
    val name: String,
    val totalScore: Int
)
