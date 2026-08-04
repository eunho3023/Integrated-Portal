package com.example.ui.screens

import android.app.Dialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.*

@Composable
fun ClassroomScreens(
    viewModel: MainViewModel,
    tabId: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (tabId) {
            "clean-tab" -> CleanTab(viewModel)
            "fund-tab" -> FundTab(viewModel)
            "vote-tab" -> VoteTab(viewModel)
            "seat-tab" -> SeatTab(viewModel)
            "lost-tab" -> LostTab(viewModel)
        }
    }
}

// -----------------------------------------------------------------
// CLEANING DUTY SCREEN
// -----------------------------------------------------------------
@Composable
fun CleanTab(viewModel: MainViewModel) {
    val cleanZones by viewModel.cleanZones.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newZoneName by remember { mutableStateOf("") }
    var newZoneAssignee by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧹 구역별 청소 당번표", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("구역 추가", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { viewModel.resetCleanZonesToday() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = SpaceTextSoft),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("오늘 완료 초기화", fontSize = 11.sp)
                }
            }
        }

        if (cleanZones.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("🧹 등록된 청소 구역이 없습니다. 구역 추가를 눌러보세요.", color = SpaceTextSoft, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, NeonRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                cleanZones.forEachIndexed { index, zone ->
                    val isDone = zone.doneDate == viewModel.todayDateString

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(zone.zone, color = SpaceText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            var assigneeText by remember(zone.assignee) { mutableStateOf(zone.assignee) }
                            OutlinedTextField(
                                value = assigneeText,
                                onValueChange = {
                                    assigneeText = it
                                    viewModel.updateCleanAssignee(zone, it)
                                },
                                placeholder = { Text("당번 입력") },
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceTextSoft),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.height(44.dp).fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1.2f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.toggleCleanZoneStatus(zone) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDone) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                    contentColor = if (isDone) NeonGreen else SpaceTextSoft
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(if (isDone) "✅ 완료됨" else "☐ 미완료", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(onClick = { viewModel.deleteCleanZone(zone) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (index < cleanZones.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("🧹 새 청소 구역 등록", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newZoneName,
                        onValueChange = { newZoneName = it },
                        placeholder = { Text("예: 복도 창틀, 교실 칠판") },
                        label = { Text("청소 구역 명칭") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                    OutlinedTextField(
                        value = newZoneAssignee,
                        onValueChange = { newZoneAssignee = it },
                        placeholder = { Text("예: 홍길동, 김철수") },
                        label = { Text("청소 담당 학생") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                }
            },
            confirmButton = {
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                TextButton(onClick = {
                    if (newZoneName.isEmpty()) return@TextButton
                    viewModel.addCleanZone(newZoneName, newZoneAssignee)
                    showAddDialog = false
                    newZoneName = ""
                    newZoneAssignee = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }) {
                    Text("추가", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// CLASS EXPENSES SCREEN
// -----------------------------------------------------------------
@Composable
fun FundTab(viewModel: MainViewModel) {
    val funds by viewModel.funds.collectAsState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var pwText by remember { mutableStateOf("") }

    var fType by remember { mutableStateOf("수입") }
    var fTitle by remember { mutableStateOf("") }
    var fAmount by remember { mutableStateOf("") }
    var fMemo by remember { mutableStateOf("") }

    val incomeTotal = funds.filter { it.type == "수입" }.sumOf { it.amount }
    val expenseTotal = funds.filter { it.type == "지출" }.sumOf { it.amount }
    val balance = incomeTotal - expenseTotal

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Unlock Panel (학생 계정에게는 안 뜨게 처리, 비밀번호 없이 해제)
        val isStudent = viewModel.currentUser?.role == "student"
        if (!viewModel.fundUnlocked) {
            if (!isStudent) {
                GlassmorphicCard(accentColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("💰 학생회 및 교사 전용 학급비 장부", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("비밀번호 입력 없이 장부를 열람 및 관리합니다.", color = SpaceTextSoft, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.fundUnlocked = true
                                viewModel.showToast("🔓 학급비 장부 권한이 활성화되었습니다.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("열기", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            } else {
                GlassmorphicCard(accentColor = NeonCyan) {
                    Text("🔒 학급비 장부는 학생회 및 교사 전용 메뉴입니다.", color = SpaceTextSoft, fontSize = 12.5.sp)
                }
            }
        } else {
            // Dashboard Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰 누적 수입", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("+${incomeTotal.toLocaleString()}원", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💸 누적 지출", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("-${expenseTotal.toLocaleString()}원", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelSolid)
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏦 현재 잔액", color = SpaceTextSoft, fontSize = 11.sp)
                        Text("${balance.toLocaleString()}원", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Expense log input
            GlassmorphicCard(accentColor = NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰 학급 자금 변동 내역 등록", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { viewModel.fundUnlocked = false }) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonCyan)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type selector
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(fType, color = SpaceText)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("수입", "지출").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { fType = it; expanded = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fTitle,
                        onValueChange = { fTitle = it },
                        label = { Text("항목명", fontSize = 11.sp) },
                        placeholder = { Text("회비 납부 / 간식 구입") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) fAmount = it },
                        label = { Text("금액 (원)", fontSize = 11.sp) },
                        placeholder = { Text("5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fMemo,
                        onValueChange = { fMemo = it },
                        label = { Text("비고 (기타 메모)", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val amt = fAmount.toIntOrNull()
                        if (fTitle.isEmpty() || amt == null || amt <= 0) {
                            viewModel.showToast("⚠️ 정확한 항목명과 금액을 기입하세요.")
                            return@Button
                        }
                        viewModel.addFund(fType, fTitle, amt, fMemo)
                        fTitle = ""
                        fAmount = ""
                        fMemo = ""
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("내역 기입 및 자금 변동 저장", fontWeight = FontWeight.Bold)
                }
            }

            // Funds List
            Text("📋 자금 내역 장부", color = SpaceTextSoft, fontSize = 13.sp)
            if (funds.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("아직 등록된 입출금 기록이 없습니다.", color = SpaceTextSoft, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .border(1.dp, BorderGlow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    funds.reversed().forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val badgeColor = if (item.type == "수입") NeonGreen else NeonRed
                                    Text(
                                        text = item.type,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.title, color = SpaceText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                }
                                if (item.memo.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.memo, color = SpaceTextSoft, fontSize = 11.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (item.type == "수입") "+${item.amount.toLocaleString()}원" else "-${item.amount.toLocaleString()}원",
                                    color = if (item.type == "수입") NeonGreen else NeonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                IconButton(onClick = { viewModel.deleteFund(item) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (index < funds.lastIndex) {
                            HorizontalDivider(color = Color(0x118CAEC6))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// CLASS VOTE / POLLS SCREEN
// -----------------------------------------------------------------
@Composable
fun VoteTab(viewModel: MainViewModel) {
    val votes by viewModel.votes.collectAsState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var selectedGradeLocal by remember { mutableStateOf(viewModel.voteSelectedGrade) }
    var selectedClassLocal by remember { mutableStateOf(viewModel.voteSelectedClass) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var voteQuestion by remember { mutableStateOf("") }
    var voteOptionsText by remember { mutableStateOf("") }
    var voteAccessCode by remember { mutableStateOf("") }

    val filteredVotes = votes.filter {
        it.grade == selectedGradeLocal && it.classNum == selectedClassLocal
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗳️ 학급 투표 및 의사결정", color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.15f), contentColor = NeonMagenta),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("새 투표 등록", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Filter Grade/Class
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${selectedGradeLocal}학년", color = SpaceText)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("1", "2", "3").forEach {
                        DropdownMenuItem(text = { Text("${it}학년") }, onClick = { selectedGradeLocal = it; expanded = false })
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${selectedClassLocal}반", color = SpaceText)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (1..10).map { "${it}" }.forEach {
                        DropdownMenuItem(text = { Text("${it}반") }, onClick = { selectedClassLocal = it; expanded = false })
                    }
                }
            }
        }

        if (filteredVotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("🗳️ 선택된 학급에 개설된 투표 안건이 없습니다.", color = SpaceTextSoft, fontSize = 13.sp)
            }
        } else {
            filteredVotes.reversed().forEach { vote ->
                val totalVotes = viewModel.parseVoteOptions(vote.optionsData).sumOf { it.second }
                val isClosed = vote.status == "closed"

                GlassmorphicCard(accentColor = if (isClosed) SpaceTextSoft else NeonMagenta) {
                    Text(
                        text = if (isClosed) "🔒 [마감] ${vote.question}" else "🗳️ ${vote.question}",
                        color = if (isClosed) SpaceTextSoft else SpaceText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text("📅 ${vote.date} 등록 · 총 $totalVotes 표", color = SpaceTextSoft, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(10.dp))

                    val parsedOptions = viewModel.parseVoteOptions(vote.optionsData)
                    parsedOptions.forEachIndexed { optIdx, option ->
                        val ratio = if (totalVotes > 0) option.second.toFloat() / totalVotes else 0f
                        val percentage = (ratio * 100).toInt()

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option.first, color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${option.second}표 ($percentage%)", color = SpaceTextSoft, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(if (isClosed) SpaceTextSoft else NeonCyan)
                                )
                            }

                            if (!isClosed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        val studentId = sPromptStudentIdForVote(context = viewModel.getApplication())
                                        if (studentId.isNotEmpty()) {
                                            viewModel.castVote(vote, optIdx, studentId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = SpaceText),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("이 항목에 투표", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!isClosed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val inputPw = sPromptPasscode(viewModel.getApplication(), "투표를 마감하시겠습니까? 권한 비밀번호(실장/학생회/교사)를 기입하세요.")
                                if (inputPw == viewModel.STAFF_PASSWORD || inputPw == viewModel.ADMIN_PASSWORD || inputPw == viewModel.CLASS_LEADER_PASSWORD) {
                                    viewModel.closeVote(vote)
                                } else if (inputPw.isNotEmpty()) {
                                    viewModel.showToast("❌ 비밀번호 불일치.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.12f), contentColor = NeonRed),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("투표 종료 및 마감", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("🗳️ 새 투표 개설", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("개설 비밀번호와 문항, 쉼표로 나열한 선택지를 입력하세요.", color = SpaceTextSoft, fontSize = 12.sp)
                    OutlinedTextField(
                        value = voteAccessCode,
                        onValueChange = { voteAccessCode = it },
                        placeholder = { Text("비밀번호 (실장 5678, 학생회 0000 등)") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                    OutlinedTextField(
                        value = voteQuestion,
                        onValueChange = { voteQuestion = it },
                        placeholder = { Text("예: 축제 장기자랑 준비곡 결정") },
                        label = { Text("투표 안건 질문") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                    OutlinedTextField(
                        value = voteOptionsText,
                        onValueChange = { voteOptionsText = it },
                        placeholder = { Text("예: 밴드합주, 댄스무대, 보컬독창") },
                        label = { Text("선택지 (쉼표 구분)") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (voteAccessCode != viewModel.STAFF_PASSWORD && voteAccessCode != viewModel.ADMIN_PASSWORD && voteAccessCode != viewModel.CLASS_LEADER_PASSWORD) {
                        viewModel.showToast("❌ 개설 권한 비밀번호 오류.")
                        return@TextButton
                    }
                    val opts = voteOptionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (voteQuestion.isEmpty() || opts.size < 2) {
                        viewModel.showToast("⚠️ 안건 및 선택지 2개 이상 기입요망.")
                        return@TextButton
                    }
                    viewModel.addVote(voteQuestion, selectedGradeLocal, selectedClassLocal, opts)
                    showCreateDialog = false
                    voteQuestion = ""
                    voteOptionsText = ""
                    voteAccessCode = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }) {
                    Text("투표 등록", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// SEAT ARRANGEMENT SCREEN
// -----------------------------------------------------------------
@Composable
fun SeatTab(viewModel: MainViewModel) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🪑 자리 배치표 제어", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        GlassmorphicCard(accentColor = NeonAmber) {
            Text("학생 명단 기입", color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = viewModel.seatNamesText,
                onValueChange = { viewModel.seatNamesText = it },
                placeholder = { Text("홍길동, 김철수, 이영희 (쉼표 또는 줄바꿈 구분)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("배치 열 수:", color = SpaceTextSoft, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(4, 5, 6).forEach { col ->
                        OutlinedButton(
                            onClick = { viewModel.seatCols = col },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (viewModel.seatCols == col) NeonAmber.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (viewModel.seatCols == col) NeonAmber else SpaceTextSoft
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(horizontal = 2.dp).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("${col}열", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.shuffleSeats()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("랜덤 배치 실행", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        viewModel.clearSeats()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = SpaceTextSoft),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("지우기")
                }
            }
        }

        // Draw Seat Grid
        val grid = viewModel.activeSeatGrid
        if (grid == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("배치된 교실 자리가 없습니다. 학생 이름을 기입하고 배치 버튼을 실행해 주세요.", color = SpaceTextSoft, fontSize = 12.5.sp, textAlign = TextAlign.Center)
            }
        } else {
            Text("🪑 배치 레이아웃 (${viewModel.seatCols}열 구성)", color = SpaceTextSoft, fontSize = 13.sp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                val cols = viewModel.seatCols
                val rows = (grid.size + cols - 1) / cols

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            if (idx < grid.size) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, BorderGlow.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${idx + 1}번 자리", color = SpaceTextSoft, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(grid[idx], color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// LOST & FOUND SCREEN
// -----------------------------------------------------------------
@Composable
fun LostTab(viewModel: MainViewModel) {
    val lostItems by viewModel.lostItems.collectAsState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current

    var lName by remember { mutableStateOf("") }
    var lLoc by remember { mutableStateOf("") }
    var lDate by remember { mutableStateOf(viewModel.todayDateString) }
    var pwText by remember { mutableStateOf("") }

    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = remember(context) {
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                lDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDayOfMonth)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🔍 분실물 보관 센터", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        // Submit Form
        GlassmorphicCard(accentColor = NeonGreen) {
            Text("📥 분실물 습득 등록", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = lName,
                onValueChange = { lName = it },
                placeholder = { Text("검정 장우산, 가디건 등") },
                label = { Text("습득 물품명") },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = lLoc,
                    onValueChange = { lLoc = it },
                    placeholder = { Text("2층 복도, 체육관 등", fontSize = 12.sp) },
                    label = { Text("습득 장소", fontSize = 11.sp) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                    modifier = Modifier.weight(0.8f)
                )

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = lDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("습득 일자 📅", fontSize = 11.sp) },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "날짜 선택", tint = NeonGreen)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText,
                            focusedIndicatorColor = NeonGreen,
                            unfocusedIndicatorColor = SpaceTextSoft
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (lName.isEmpty()) {
                        viewModel.showToast("⚠️ 보관 물품명을 기입해 주세요.")
                        return@Button
                    }
                    viewModel.addLostItem(lName, lLoc, lDate, "")
                    lName = ""
                    lLoc = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("보관 등록 완료", fontWeight = FontWeight.Bold)
            }
        }

        // Admin lock (학생 계정 안 보이게 처리, 비밀번호 없이 해제)
        val isStudent = viewModel.currentUser?.role == "student"
        if (!isStudent) {
            if (!viewModel.lostAdminUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.lostAdminUnlocked = true
                            viewModel.showToast("🔓 분실물 관리 모드가 활성화되었습니다.")
                        }
                        .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .background(NeonAmber.copy(alpha = 0.05f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔓 분실물 관리자 모드 열기", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔓 관리자 모드 활성화됨", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.clearLostData() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed)
                    ) {
                        Text("대장 전체초기화")
                    }
                }
            }
        }

        // Lost items list
        if (lostItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("🔍 현재 보관실에 영치된 분실물이 없습니다.", color = SpaceTextSoft, fontSize = 12.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                lostItems.reversed().forEachIndexed { index, item ->
                    val isClaimed = item.status == "찾아감"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, color = SpaceText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("📍 장소: ${item.location} · 습득: ${item.date}", color = SpaceTextSoft, fontSize = 11.5.sp)
                            if (isClaimed) {
                                Text("Claimant: ${item.claimant}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val badgeColor = if (isClaimed) NeonGreen else NeonAmber
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(item.status, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (!isClaimed) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val claimantName = sPromptClaimant(viewModel.getApplication())
                                        if (claimantName.isNotEmpty()) {
                                            viewModel.claimLostItem(item, claimantName)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("찾아감 처리", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (index < lostItems.lastIndex) {
                        HorizontalDivider(color = Color(0x118CAEC6))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MOCK POPUP SYSTEM STUBS
// -----------------------------------------------------------------
private fun sPromptStudentIdForVote(context: Context): String {
    // Simple mock helper: In real Android prompt dialog would launch, let's auto-generate a valid ID index
    return "105" + (10..99).random()
}

private fun sPromptPasscode(context: Context, msg: String): String {
    // Default override fallback passcode input
    return "0000"
}

private fun sPromptClaimant(context: Context): String {
    return listOf("3-2 김민수", "1-5 이혜원", "2-3 박현우").random()
}

// Simple locale number formatting helper
fun Int.toLocaleString(): String {
    return String.format("%,d", this)
}
