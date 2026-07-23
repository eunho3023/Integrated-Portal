package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SuggestionEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun SuggestScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val suggestions by viewModel.suggestions.collectAsState()
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var sType by remember { mutableStateOf("🌐 온라인") }
    var sStudentId by remember { mutableStateOf("") }
    var sContent by remember { mutableStateOf("") }

    // Viewing unlock passcode states
    var myStudentIdSearch by remember { mutableStateOf("") }
    var adminPasswordText by remember { mutableStateOf("") }

    // Dialog state
    var showCommentDialog by remember { mutableStateOf<SuggestionEntity?>(null) }
    var commentText by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf<SuggestionEntity?>(null) }
    var editContentText by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf<SuggestionEntity?>(null) }

    val pendingCount = suggestions.count { it.status == "접수대기" }
    val processingCount = suggestions.count { it.status == "처리중" }
    val completedCount = suggestions.count { it.status == "완료" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedKeep(14.dp)
    ) {
        // 1. Suggestion Summary Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    Text("🕓 접수대기", color = SpaceTextSoft, fontSize = 11.sp)
                    Text("$pendingCount 건", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelSolid)
                    .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚙️ 처리중", color = SpaceTextSoft, fontSize = 11.sp)
                    Text("$processingCount 건", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

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
                    Text("✅ 완료됨", color = SpaceTextSoft, fontSize = 11.sp)
                    Text("$completedCount 건", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // 2. Submit Suggestion Form
        GlassmorphicCard(accentColor = NeonMagenta) {
            Text(
                text = "💡 건의 등록",
                color = NeonMagenta,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown type
                Box(modifier = Modifier.weight(1f)) {
                    var typeExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                    ) {
                        Text(sType, fontSize = 11.sp)
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("🌐 온라인", "📮 오프라인").forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = { sType = it; typeExpanded = false }
                            )
                        }
                    }
                }

                // Student ID Input for safety key
                OutlinedTextField(
                    value = sStudentId,
                    onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) sStudentId = it },
                    placeholder = { Text("예: 10513") },
                    label = { Text("학번 (확인키)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    ),
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = sContent,
                onValueChange = { sContent = it },
                label = { Text("건의 내용 (다른 학생에게는 전면 익명 보장)", fontSize = 11.sp) },
                placeholder = { Text("건의 사항을 자세히 적어주세요...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = SpaceText,
                    unfocusedTextColor = SpaceText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (sStudentId.isEmpty() || sContent.isEmpty()) {
                        viewModel.showToast("⚠️ 학번과 내용을 적어주세요.")
                        return@Button
                    }
                    viewModel.addSuggestion(sType, sStudentId, sContent)
                    sContent = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("건의사항 제출하기", fontWeight = FontWeight.Bold)
            }
        }

        // 3. Locking, Viewing & Mode Toggle Panel
        GlassmorphicCard(accentColor = BorderGlow) {
            Text(
                text = "🔍 건의내역 조회 및 열람",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // View Mine
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("내 건의 조회 (학번 입력)", color = SpaceTextSoft, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = myStudentIdSearch,
                        onValueChange = { if (it.all { c -> c.isDigit() }) myStudentIdSearch = it },
                        placeholder = { Text("학번 입력") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PanelSolid,
                            unfocusedContainerColor = PanelSolid,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                }

                Button(
                    onClick = {
                        if (myStudentIdSearch.isEmpty()) {
                            viewModel.showToast("학번을 입력해 주세요.")
                            return@Button
                        }
                        viewModel.suggestViewMode = "mine"
                        viewModel.suggestStudentIdSearch = myStudentIdSearch
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.15f), contentColor = NeonMagenta),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("조회", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // View Admin
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("전체 열람 (교사 관리자)", color = SpaceTextSoft, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = adminPasswordText,
                        onValueChange = { adminPasswordText = it },
                        placeholder = { Text("비밀번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PanelSolid,
                            unfocusedContainerColor = PanelSolid,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                }

                Button(
                    onClick = {
                        if (adminPasswordText == viewModel.ADMIN_PASSWORD) {
                            viewModel.suggestViewMode = "admin"
                            adminPasswordText = ""
                            viewModel.showToast("🔓 전체 건의 목록이 열람되었습니다.")
                        } else {
                            viewModel.showToast("❌ 비밀번호가 올바르지 않습니다.")
                        }
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.15f), contentColor = NeonAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("열람", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (viewModel.suggestViewMode != "none") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = if (viewModel.suggestViewMode == "admin") "전체 교사 열람 모드" else "내 건의 단독 조회 (${viewModel.suggestStudentIdSearch})"
                    Text("👁️ 활성 상태: $statusText", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    TextButton(
                        onClick = {
                            viewModel.suggestViewMode = "none"
                            myStudentIdSearch = ""
                            viewModel.suggestStudentIdSearch = ""
                        }
                    ) {
                        Text("🙈 목록 닫기/숨기기", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Exporters & Data Erasers for Admin view
        if (viewModel.suggestViewMode == "admin") {
            GlassmorphicCard(accentColor = NeonAmber) {
                Text("🔐 관리자 전용 제어", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy CSV
                    Button(
                        onClick = {
                            if (suggestions.isEmpty()) {
                                viewModel.showToast("내보낼 건의함이 비어 있습니다.")
                                return@Button
                            }
                            val csv = buildString {
                                append("번호,구분,내용,상태,조치답변\n")
                                suggestions.forEachIndexed { i, s ->
                                    append("${i + 1},${s.type},${s.content},${s.status},${s.comment}\n")
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Suggestions CSV", csv))
                            viewModel.showToast("📋 건의 명단 CSV 데이터가 클립보드에 복사되었습니다!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("CSV 복사", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Clear
                    Button(
                        onClick = { viewModel.clearSuggestions() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("내역 전체초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Display Suggestions Log Table
        if (viewModel.suggestViewMode == "none") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒 개인정보 보호와 완전 익명을 위해\n학번 또는 교사 비밀번호를 기입해야 내역이 조회됩니다.",
                    color = SpaceTextSoft,
                    textAlign = TextAlign.Center,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            val visibleSuggestions = if (viewModel.suggestViewMode == "admin") {
                suggestions
            } else {
                suggestions.filter { it.studentId == viewModel.suggestStudentIdSearch }
            }

            if (visibleSuggestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📂 조회 가능한 건의사항 기록이 없습니다.", color = SpaceTextSoft, fontSize = 13.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelSolid)
                        .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141F32))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("구분 / 내용", color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(3f))
                        Text("상태 / 관리", color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                    }

                    visibleSuggestions.forEachIndexed { idx, suggest ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                // Content
                                Column(modifier = Modifier.weight(3f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = suggest.type,
                                            color = if (suggest.type.contains("온라인")) NeonCyan else NeonMagenta,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        if (viewModel.suggestViewMode == "admin") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "학번: ${suggest.studentId}",
                                                color = SpaceTextSoft,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(suggest.content, color = SpaceText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier.weight(2f),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    val (color, text) = when (suggest.status) {
                                        "접수대기" -> NeonRed to "접수대기"
                                        "처리중" -> NeonAmber to "처리중"
                                        else -> NeonGreen to "처리완료"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(color.copy(alpha = 0.12f))
                                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Reply Comment block
                            if (suggest.comment.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .padding(10.dp)
                                ) {
                                    Text("💬 교사 답변:", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(suggest.comment, color = SpaceText, fontSize = 12.sp)
                                }
                            }

                            // Actions Row (Student/Author edit & delete + Admin management)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Author / Student actions (edit / delete)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            showEditDialog = suggest
                                            editContentText = suggest.content
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.12f), contentColor = NeonCyan),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("✏️ 수정", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            showDeleteDialog = suggest
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.12f), contentColor = NeonRed),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("🗑️ 삭제", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Admin Actions
                                if (viewModel.suggestViewMode == "admin") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (suggest.status == "접수대기") {
                                            Button(
                                                onClick = { viewModel.updateSuggestionStatus(suggest, "처리중") },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.12f), contentColor = NeonAmber),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("처리 시작", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (suggest.status == "처리중") {
                                            Button(
                                                onClick = { viewModel.updateSuggestionStatus(suggest, "완료") },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.12f), contentColor = NeonGreen),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("조치 완료", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (suggest.status == "완료") {
                                            Button(
                                                onClick = {
                                                    showCommentDialog = suggest
                                                    commentText = suggest.comment
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.12f), contentColor = NeonPurple),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text(if (suggest.comment.isEmpty()) "답변 달기" else "답변 수정", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (idx < visibleSuggestions.lastIndex) {
                            HorizontalDivider(color = Color(0x228CAEC6))
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // ANSWER DIALOG
    // -------------------------------------------------------------
    showCommentDialog?.let { activeSuggest ->
        AlertDialog(
            onDismissRequest = { showCommentDialog = null },
            title = { Text("💬 건의 조치 사항 답변", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("이 건의에 남길 교사 의견 및 조치 내역을 입력하세요.", color = SpaceTextSoft, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("예: 검토 후 우산 거치대를 추가 설치하기로 결정하였습니다.") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addSuggestionComment(activeSuggest, commentText.trim())
                    showCommentDialog = null
                    commentText = ""
                }) {
                    Text("답변 저장", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = null; commentText = "" }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // EDIT DIALOG
    // -------------------------------------------------------------
    showEditDialog?.let { activeSuggest ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("✏️ 건의 내용 수정", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("수정할 건의 내용을 입력하세요.", color = SpaceTextSoft, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editContentText,
                        onValueChange = { editContentText = it },
                        placeholder = { Text("수정할 내용...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editContentText.isNotBlank()) {
                        viewModel.editSuggestionContent(activeSuggest, editContentText.trim())
                        showEditDialog = null
                        editContentText = ""
                    }
                }) {
                    Text("수정 완료", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null; editContentText = "" }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // -------------------------------------------------------------
    showDeleteDialog?.let { activeSuggest ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("🗑️ 건의 사항 삭제", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Text("해당 건의 사항을 정말 삭제하시겠습니까?\n이 작업은 복구할 수 없습니다.", color = SpaceTextSoft, fontSize = 12.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSuggestion(activeSuggest)
                    showDeleteDialog = null
                }) {
                    Text("삭제", color = NeonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }
}
