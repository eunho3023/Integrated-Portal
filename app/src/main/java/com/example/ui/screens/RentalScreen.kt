package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import java.util.Calendar
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusDirection
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
import com.example.data.RentalEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun RentalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val rentals by viewModel.rentals.collectAsState()
    val stocks by viewModel.itemStocks.collectAsState()
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var rentGrade by remember { mutableStateOf("1학년") }
    var rentClass by remember { mutableStateOf("1반") }
    var rentNum by remember { mutableStateOf("") }
    var rentName by remember { mutableStateOf("") }
    var rentItem by remember { mutableStateOf("") }
    var rentReturnDate by remember { mutableStateOf(viewModel.todayDateString) }

    // Dialog state
    var showUnlockDialog by remember { mutableStateOf(false) }
    var passcodeText by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf("") }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("5") }

    var showUpdateStockDialog by remember { mutableStateOf(false) }
    var stockToUpdateName by remember { mutableStateOf("") }
    var stockToUpdateQty by remember { mutableStateOf("") }

    // Seed selected item
    LaunchedEffect(stocks) {
        if (stocks.isNotEmpty()) {
            if (rentItem.isEmpty() || !stocks.any { it.itemName == rentItem }) {
                rentItem = stocks.first().itemName
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedKeep(14.dp)
    ) {
        // 1. Stock Status Chips
        Text(
            text = "📊 실시간 재고 현황",
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            stocks.forEach { stock ->
                val rentedCount = rentals.count { it.item == stock.itemName && it.status == "대여중" }
                val remaining = (stock.totalQty - rentedCount).coerceAtLeast(0)

                val (levelColor, levelLabel) = when {
                    remaining <= 0 -> NeonRed to "OUT"
                    remaining <= (stock.totalQty * 0.2f).toInt() -> NeonAmber to "LOW"
                    else -> NeonGreen to "OK"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(PanelSolid)
                        .border(1.dp, levelColor.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                        .clickable {
                            if (viewModel.rentAdminUnlocked) {
                                stockToUpdateName = stock.itemName
                                stockToUpdateQty = stock.totalQty.toString()
                                showUpdateStockDialog = true
                            } else {
                                viewModel.showToast("🔓 관리자 메뉴를 먼저 해제하면 수량을 변경할 수 있습니다.")
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stock.itemName, color = SpaceText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$remaining",
                            color = levelColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ ${stock.totalQty}",
                            color = SpaceTextSoft,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Rental Registration Form
        GlassmorphicCard(accentColor = NeonCyan) {
            Text(
                text = "📝 물품 대여 신청",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grade Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                    ) {
                        Text(rentGrade, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("1학년", "2학년", "3학년").forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = { rentGrade = it; expanded = false }
                            )
                        }
                    }
                }

                // Class Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                    ) {
                        Text(rentClass, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (1..10).map { "${it}반" }.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = { rentClass = it; expanded = false }
                            )
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
                    value = rentNum,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) rentNum = it },
                    label = { Text("번호", fontSize = 11.sp) },
                    placeholder = { Text("예: 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = rentName,
                    onValueChange = { rentName = it },
                    label = { Text("이름", fontSize = 11.sp) },
                    placeholder = { Text("홍길동") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    ),
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Item Dropdown Selector
            Text("대여 물품 선택", color = SpaceTextSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            var showCustomItemInput by remember { mutableStateOf(false) }

            if (showCustomItemInput) {
                OutlinedTextField(
                    value = rentItem,
                    onValueChange = { rentItem = it },
                    label = { Text("직접 대여 물품명 입력", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SpaceTextSoft
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showCustomItemInput = false }) {
                            Icon(Icons.Default.Close, contentDescription = "목록으로 전환", tint = SpaceTextSoft)
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rentItem.ifEmpty { "대여 물품 선택" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(PanelSolid)
                    ) {
                        if (stocks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("등록된 수량 정보가 없습니다 (직접 입력 가능)", color = SpaceTextSoft, fontSize = 12.sp) },
                                onClick = { expanded = false; showCustomItemInput = true }
                            )
                        } else {
                            stocks.forEach { stock ->
                                val currentActiveCount = rentals.count { it.item == stock.itemName && it.status == "대여중" }
                                val remainQty = stock.totalQty - currentActiveCount
                                val isOutOfStock = remainQty <= 0
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stock.itemName,
                                                color = if (isOutOfStock) NeonRed else SpaceText,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isOutOfStock) "(잔여: 0개 - 품절)" else "(잔여: ${remainQty}개)",
                                                fontSize = 11.sp,
                                                color = if (isOutOfStock) NeonRed else NeonGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        rentItem = stock.itemName
                                        expanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = SpaceTextSoft.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("✏️ 직접 물품명 입력하기", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    showCustomItemInput = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        try {
                            val parts = rentReturnDate.split("-")
                            if (parts.size == 3) {
                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                            }
                        } catch (e: Exception) {}

                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                rentReturnDate = formattedDate
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                OutlinedTextField(
                    value = rentReturnDate,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("반납 예정일 (날짜 선택)", fontSize = 11.sp) },
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "날짜 선택", tint = NeonCyan)
                    },
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.Transparent,
                        disabledTextColor = SpaceText,
                        disabledLabelColor = SpaceTextSoft,
                        disabledTrailingIconColor = NeonCyan,
                        disabledIndicatorColor = SpaceTextSoft.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (rentNum.isEmpty() || rentName.isEmpty() || rentItem.isEmpty()) {
                        viewModel.showToast("⚠️ 모든 빈칸을 기입해 주세요.")
                        return@Button
                    }
                    val itemStockObj = stocks.find { it.itemName == rentItem }
                    val rented = rentals.count { it.item == rentItem && it.status == "대여중" }
                    val total = itemStockObj?.totalQty ?: 0
                    if (total - rented <= 0) {
                        viewModel.showToast("❌ 현재 '$rentItem'의 잔여 수량이 부족합니다.")
                        return@Button
                    }

                    viewModel.addRental(
                        grade = rentGrade,
                        classNum = rentClass,
                        num = rentNum,
                        name = rentName,
                        item = rentItem,
                        rentDate = viewModel.todayDateString,
                        returnDate = rentReturnDate
                    )
                    // Reset inputs
                    rentNum = ""
                    rentName = ""
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("대여 등록", fontWeight = FontWeight.Bold)
            }
        }

        // 3. Admin Tools Section
        if (!viewModel.rentAdminUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showUnlockDialog = true }
                    .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .background(NeonAmber.copy(alpha = 0.05f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔓 관리자 메뉴 열기 (교사 비밀번호)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            GlassmorphicCard(accentColor = NeonAmber) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚙️ 관리자 설정 (잠금해제됨)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { viewModel.rentAdminUnlocked = false }) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = NeonAmber)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    // CSV Copy
                    Button(
                        onClick = {
                            if (rentals.isEmpty()) {
                                viewModel.showToast("내보낼 대여 명단이 없습니다.")
                                return@Button
                            }
                            val csv = buildString {
                                append("학년,반,번호,이름,물품,대여일,반납예정일,상태\n")
                                rentals.forEach {
                                    append("${it.grade},${it.classNum},${it.num}번,${it.name},${it.item},${it.rentDate},${it.returnDate},${it.status}\n")
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Rentals CSV", csv))
                            viewModel.showToast("📋 대여 명단 CSV 데이터가 클립보드에 복사되었습니다! (엑셀 붙여넣기 가능)")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV 복사", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share CSV Text
                    Button(
                        onClick = {
                            if (rentals.isEmpty()) {
                                viewModel.showToast("공유할 대여 명단이 없습니다.")
                                return@Button
                            }
                            val csv = buildString {
                                append("학년,반,번호,이름,물품,대여일,반납예정일,상태\n")
                                rentals.forEach {
                                    append("${it.grade},${it.classNum},${it.num}번,${it.name},${it.item},${it.rentDate},${it.returnDate},${it.status}\n")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "대여_명단_내역")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(intent, "명단 공유"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("공유하기", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Add Item
                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f), contentColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("물품 추가", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Reset Data
                    Button(
                        onClick = { viewModel.clearRentals() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("내역 초기화", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Search & Filter Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.rentSearchKeyword,
                onValueChange = { viewModel.rentSearchKeyword = it },
                placeholder = { Text("이름, 반, 물품 검색...", fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PanelSolid,
                    unfocusedContainerColor = PanelSolid,
                    focusedTextColor = SpaceText,
                    unfocusedTextColor = SpaceText
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.8f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpaceTextSoft) }
            )

            // Filter dropdown
            Box(modifier = Modifier.weight(1f)) {
                var filterExpanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { filterExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                ) {
                    Text(viewModel.rentStatusFilter, fontSize = 12.sp)
                }
                DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                    listOf("전체", "대여중", "반납완료").forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = { viewModel.rentStatusFilter = it; filterExpanded = false }
                        )
                    }
                }
            }
        }

        // 5. Rental Data Table List
        val filteredRentals = rentals.filter { item ->
            val matchStatus = viewModel.rentStatusFilter == "전체" || item.status == viewModel.rentStatusFilter
            val matchKeyword = viewModel.rentSearchKeyword.isEmpty() ||
                    item.name.contains(viewModel.rentSearchKeyword) ||
                    item.item.contains(viewModel.rentSearchKeyword) ||
                    item.classNum.contains(viewModel.rentSearchKeyword)
            matchStatus && matchKeyword
        }

        if (filteredRentals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (rentals.isEmpty()) "📂 등록된 대여 명단 기록이 없습니다." else "🔍 조건에 일치하는 대여 내역이 없습니다.",
                    color = SpaceTextSoft,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                // Table header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141F32))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("대여자 정보", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("대여 물품", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                    Text("반납일", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                    Text("상태 / 관리", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End)
                }

                // Table rows
                filteredRentals.forEachIndexed { index, rental ->
                    val isOverdue = rental.status == "대여중" && rental.returnDate < viewModel.todayDateString

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isOverdue) NeonRed.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Info column
                        Column(modifier = Modifier.weight(2f)) {
                            Text("${rental.grade} ${rental.classNum} ${rental.num}번", color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(rental.name, color = SpaceTextSoft, fontSize = 11.sp)
                        }

                        // Item column
                        Text(rental.item, color = SpaceText, fontSize = 13.sp, modifier = Modifier.weight(1.5f))

                        // Return Date column
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(rental.returnDate, color = if (isOverdue) NeonRed else SpaceTextSoft, fontSize = 11.sp, fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal)
                            if (isOverdue) {
                                Text("연체 중", color = NeonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action button column
                        Box(
                            modifier = Modifier.weight(1.8f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (rental.status == "대여중") {
                                Button(
                                    onClick = { viewModel.returnRental(rental) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("반납완료", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "✅ 반납완료",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }

                    if (index < filteredRentals.lastIndex) {
                        HorizontalDivider(color = Color(0x228CAEC6))
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODAL DIALOGS
    // -------------------------------------------------------------
    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("🔓 관리자 모드 잠금 해제", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("관리자(교사) 비밀번호를 입력하세요.", color = SpaceTextSoft, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passcodeText,
                        onValueChange = { passcodeText = it },
                        placeholder = { Text("기본값: 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PanelSolid,
                            unfocusedContainerColor = PanelSolid,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                    if (passcodeError.isNotEmpty()) {
                        Text(passcodeError, color = NeonRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (passcodeText == viewModel.ADMIN_PASSWORD) {
                        viewModel.rentAdminUnlocked = true
                        showUnlockDialog = false
                        passcodeText = ""
                        passcodeError = ""
                        viewModel.showToast("🔓 대여 관리자 권한이 활성화되었습니다.")
                    } else {
                        passcodeError = "❌ 비밀번호가 올바르지 않습니다."
                    }
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }) {
                    Text("확인", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false; passcodeText = ""; passcodeError = "" }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("🎒 새 물품 등록", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("추가할 물품 명칭과 최초 보유량을 기입하세요.", color = SpaceTextSoft, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = { Text("🎒 보조가방, ⚾ 야구배트 등") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { if (it.all { c -> c.isDigit() }) newItemQty = it },
                        label = { Text("보유 수량") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val qty = newItemQty.toIntOrNull() ?: 0
                            if (newItemName.isEmpty() || qty <= 0) {
                                viewModel.showToast("물품 정보가 바르지 않습니다.")
                                return@KeyboardActions
                            }
                            viewModel.addStockItem(newItemName, qty)
                            rentItem = newItemName
                            viewModel.showToast("🎒 '$newItemName' 물품이 추가되었습니다!")
                            newItemName = ""
                            newItemQty = "5"
                        }),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = newItemQty.toIntOrNull() ?: 0
                    if (newItemName.isEmpty() || qty <= 0) {
                        viewModel.showToast("물품 정보가 바르지 않습니다.")
                        return@TextButton
                    }
                    viewModel.addStockItem(newItemName, qty)
                    rentItem = newItemName
                    viewModel.showToast("🎒 '$newItemName' 물품이 추가되었습니다!")
                    newItemName = ""
                    newItemQty = "5"
                }) {
                    Text("추가", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("닫기", color = SpaceTextSoft)
                }
            }
        )
    }

    if (showUpdateStockDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateStockDialog = false },
            title = { Text("⚙️ 재고 수정 / 삭제", color = SpaceText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("[$stockToUpdateName] 의 총 보유 수량을 변경하거나 물품 자체를 영구 삭제합니다.", color = SpaceTextSoft, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = stockToUpdateQty,
                        onValueChange = { if (it.all { c -> c.isDigit() }) stockToUpdateQty = it },
                        label = { Text("보유 수량") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = PanelSolid, unfocusedContainerColor = PanelSolid, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.deleteStockItem(stockToUpdateName)
                            showUpdateStockDialog = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("물품 영구 삭제", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = stockToUpdateQty.toIntOrNull()
                    if (qty != null && qty >= 0) {
                        viewModel.updateStockQty(stockToUpdateName, qty)
                        showUpdateStockDialog = false
                    }
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }) {
                    Text("수량 변경", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateStockDialog = false }) {
                    Text("취소", color = SpaceTextSoft)
                }
            }
        )
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PanelGlass
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}

// FlowRow support
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val mainAxisSpacingPx = mainAxisSpacing.roundToPx()
        val crossAxisSpacingPx = crossAxisSpacing.roundToPx()

        val lines = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val lineHeights = mutableListOf<Int>()

        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentLineWidth = 0
        var currentLineHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val spacing = if (currentLine.isEmpty()) 0 else mainAxisSpacingPx

            if (currentLineWidth + spacing + placeable.width <= constraints.maxWidth) {
                currentLine.add(placeable)
                currentLineWidth += spacing + placeable.width
                currentLineHeight = maxOf(currentLineHeight, placeable.height)
            } else {
                lines.add(currentLine)
                lineHeights.add(currentLineHeight)

                currentLine = mutableListOf(placeable)
                currentLineWidth = placeable.width
                currentLineHeight = placeable.height
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
            lineHeights.add(currentLineHeight)
        }

        val width = constraints.maxWidth
        val height = (lineHeights.sum() + (lines.size - 1).coerceAtLeast(0) * crossAxisSpacingPx)
            .coerceAtMost(constraints.maxHeight)

        layout(width, height) {
            var yOffset = 0
            lines.forEachIndexed { lineIndex, line ->
                var xOffset = 0
                line.forEach { placeable ->
                    placeable.placeRelative(xOffset, yOffset)
                    xOffset += placeable.width + mainAxisSpacingPx
                }
                yOffset += lineHeights[lineIndex] + crossAxisSpacingPx
            }
        }
    }
}

// Spaced Arrangement helper
fun Arrangement.spacedKeep(space: androidx.compose.ui.unit.Dp): Arrangement.Vertical =
    Arrangement.spacedBy(space)
