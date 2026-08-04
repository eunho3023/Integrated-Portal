package com.example

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInit", "Failed to initialize FirebaseApp", e)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppScaffold()
            }
        }
    }
}

@Composable
fun AppScaffold() {
    val viewModel: MainViewModel = viewModel()
    val currentUser = viewModel.currentUser
    val currentSchool = viewModel.currentSchool
    var isWithdrawDialogVisible by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showExitAppDialog by remember { mutableStateOf(false) }
    var showApkDownloadInfoDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // 📱 뒤로가기 버튼(BackHandler) 처리: 드로어 닫기, 모달 닫기, 이전 탭으로 이동 또는 앱 종료 확인 알림창
    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (viewModel.activeChatPeerId != null) {
            viewModel.activeChatPeerId = null
        } else if (viewModel.isAuthModalVisible) {
            viewModel.isAuthModalVisible = false
        } else if (viewModel.navigateBack()) {
            // Navigated back to previous tab item
        } else {
            showExitAppDialog = true
        }
    }

    if (showExitAppDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showExitAppDialog = false },
            title = { Text("🚪 앱 종료 확인", color = NeonCyan, fontWeight = FontWeight.Bold) },
            text = { Text("스마트 학급 알리미 앱을 종료하시겠습니까?", color = SpaceText) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitAppDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White)
                ) {
                    Text("종료", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitAppDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                ) {
                    Text("취소")
                }
            }
        )
    }

    val tabs = listOf(
        "home-tab" to "🏠 홈",
        "timetable-tab" to "📅 학급 시간표",
        "vulnerability-tab" to "🤖 AI 오답노트 & 취약점 분석",
        "qa-tab" to "🙋 실시간 수준별 Q&A 매칭",
        "project-tab" to "🤝 모둠 프로젝트 & 기여도",
        "rent-tab" to "📦 대여 대장",
        "suggest-tab" to "💬 익명 건의",
        "uniform-tab" to "🧥 교복 점검",
        "attend-tab" to "📝 출석 점검",
        "merit-tab" to "⭐ 상벌점 가감",
        "clean-tab" to "🧹 청소 당번",
        "fund-tab" to "💰 학급 장부",
        "vote-tab" to "🗳️ 투표 안건",
        "seat-tab" to "🪑 자리 배치",
        "lost-tab" to "🔍 분실물 보관"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SpaceBackground,
                drawerContentColor = SpaceText,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PanelSolid)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🏫 메뉴 전체 카테고리", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("이동할 기능 메뉴를 선택하세요", color = SpaceTextSoft, fontSize = 11.sp)
                        }
                        IconButton(onClick = { coroutineScope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = SpaceTextSoft)
                        }
                    }

                    HorizontalDivider(color = Color(0x228CAEC6))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEach { tab ->
                            val isActive = viewModel.activeTab == tab.first
                            val accentColor = when (tab.first) {
                                "home-tab" -> NeonGreen
                                "vulnerability-tab" -> NeonPurple
                                "qa-tab", "rent-tab", "fund-tab", "project-tab" -> NeonCyan
                                "suggest-tab", "vote-tab" -> NeonMagenta
                                "uniform-tab", "seat-tab" -> NeonAmber
                                "attend-tab", "lost-tab" -> NeonGreen
                                "merit-tab" -> NeonPurple
                                else -> NeonRed
                            }

                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = tab.second,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) accentColor else SpaceText,
                                        fontSize = 13.sp
                                    )
                                },
                                selected = isActive,
                                onClick = {
                                    viewModel.selectTab(tab.first)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Paint cosmic background
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(SpaceBackground, SpaceDarkGrad)
                        )
                    )

                    // Draw cybernetic grid
                    val gridSpacing = 40.dp.toPx()
                    val lineAlpha = 0.05f

                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = NeonCyan.copy(alpha = lineAlpha),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }

                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = NeonCyan.copy(alpha = lineAlpha),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            val isLandscape = maxWidth > maxHeight
            val maxContentWidth = if (isLandscape) 1100.dp else 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = maxContentWidth)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = if (isLandscape) 20.dp else 12.dp)
            ) {
                // 1. App Header with Left Navigation Bar Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top-Left Navigation Drawer Toggle Button
                        Button(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", modifier = Modifier.size(16.dp))
                                Text("메뉴", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "🏫 통합 교육행정 포털",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            val currentTabLabel = tabs.find { it.first == viewModel.activeTab }?.second ?: ""
                            Text(
                                text = "현재: $currentTabLabel",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val isSynced = currentUser != null && currentUser.schoolId.isNotEmpty()
                    val badgeColor = if (isSynced) {
                        if (viewModel.isFirebaseConnected) NeonGreen else NeonRed
                    } else {
                        SpaceTextSoft
                    }
                    val badgeText = if (isSynced) {
                        if (viewModel.isFirebaseConnected) "FIREBASE // ONLINE ⚡" else "FIREBASE // OFFLINE ⚠️"
                    } else {
                        "LOCAL // OFFLINE 🛡️"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(PanelSolid)
                            .border(
                                1.dp,
                                badgeColor.copy(alpha = 0.6f),
                                RoundedCornerShape(99.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

            // 2. Auth Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelGlass)
                    .border(1.dp, BorderGlow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentUser == null) {
                    Text(
                        text = "🔒 로그인이 되어 있지 않습니다.",
                        color = SpaceTextSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { viewModel.isAuthModalVisible = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("로그인 / 가입", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val roleLabelText = when {
                        currentUser.username == "admin" || currentUser.role == "admin" -> "👑 총괄 관리자"
                        currentUser.role == "teacher" -> "교사(관리자)"
                        currentUser.role == "staff" -> "학생회/부장"
                        currentUser.role == "leader" -> "반 실장"
                        else -> "학생"
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(
                            text = "👤 ${currentUser.displayName} (${roleLabelText})",
                            color = NeonCyan,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "🏫 소속: ${currentSchool?.name ?: "지정안됨"} (초대코드: ${currentSchool?.inviteCode ?: ""})",
                            color = SpaceTextSoft,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f), contentColor = NeonCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("✏️ 정보수정", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showLogoutConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = SpaceText),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("로그아웃", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { isWithdrawDialogVisible = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f), contentColor = NeonRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("회원탈퇴", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Tab navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    val isActive = viewModel.activeTab == tab.first
                    val accentColor = when (tab.first) {
                        "home-tab" -> NeonGreen
                        "vulnerability-tab" -> NeonPurple
                        "qa-tab", "rent-tab", "fund-tab", "project-tab" -> NeonCyan
                        "suggest-tab", "vote-tab" -> NeonMagenta
                        "uniform-tab", "seat-tab" -> NeonAmber
                        "attend-tab", "lost-tab" -> NeonGreen
                        "merit-tab" -> NeonPurple
                        else -> NeonRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isActive) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectTab(tab.first) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.second,
                            color = if (isActive) accentColor else SpaceTextSoft,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Inner scroll content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (viewModel.activeTab == "home-tab") {
                    HomeScreen(viewModel)
                } else if (currentUser == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("🔒 로그인이 필요한 메뉴입니다", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("포털 내 본 기능 서비스를 이용하시려면 로그인이 필요합니다.\n로그인하시거나 '🏠 홈' 메뉴에서 시계 및 공지사항을 확인하세요.", color = SpaceTextSoft, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.isAuthModalVisible = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("로그인 / 회원가입 하러가기", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    when (viewModel.activeTab) {
                        "timetable-tab" -> com.example.ui.screens.TimetableScreen(viewModel)
                        "vulnerability-tab" -> AiLearningScreen(viewModel)
                        "qa-tab" -> QaScreen(viewModel)
                        "project-tab" -> ProjectCollaborationScreen(viewModel)
                        "rent-tab" -> RentalScreen(viewModel)
                        "suggest-tab" -> SuggestScreen(viewModel)
                        "uniform-tab", "attend-tab", "merit-tab" -> ChecksScreen(viewModel, viewModel.activeTab)
                        else -> ClassroomScreens(viewModel, viewModel.activeTab)
                    }
                }
            }
        }

        // Toasts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 14.dp, end = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                viewModel.toasts.forEach { toast ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.95f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(toast, color = SpaceText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Overlays

        if (viewModel.isAuthModalVisible) {
            AuthModalDialog(viewModel = viewModel)
        }

        if (showEditProfileDialog && currentUser != null) {
            EditProfileModalDialog(
                viewModel = viewModel,
                onDismiss = { showEditProfileDialog = false }
            )
        }

        if (showLogoutConfirmDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showLogoutConfirmDialog = false }) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight()
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = PanelSolid),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🔒 로그아웃 확인",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "정말 로그아웃 하시겠습니까?",
                            color = SpaceText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showLogoutConfirmDialog = false },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, SpaceTextSoft.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                            ) {
                                Text("취소", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showLogoutConfirmDialog = false
                                    viewModel.logout()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                            ) {
                                Text("로그아웃", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (isWithdrawDialogVisible) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { isWithdrawDialogVisible = false }) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight()
                        .border(1.dp, NeonRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = PanelSolid),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚠️ 회원 탈퇴 확인",
                            color = NeonRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "정말로 탈퇴하시겠습니까?\n탈퇴 시 계정 정보 및 가입 데이터가 영구히 삭제되며 복구할 수 없습니다.",
                            color = SpaceText,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isWithdrawDialogVisible = false },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, SpaceTextSoft.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceText)
                            ) {
                                Text("취소", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isWithdrawDialogVisible = false
                                    viewModel.withdrawUser()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White)
                            ) {
                                Text("탈퇴", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// -----------------------------------------------------------------
// CAMERA PREVIEW & AUDIO RECORD MONITOR HELPERS
// -----------------------------------------------------------------
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Camera initialization failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Camera update failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}

@Composable
fun AudioMicLevelMonitor(
    isMuted: Boolean,
    onAmplitudeChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(isMuted) {
        if (isMuted) {
            onAmplitudeChanged(0f)
            return@LaunchedEffect
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) return@withContext

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize
                )
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    val buffer = ShortArray(minBufferSize)
                    audioRecord.startRecording()

                    while (isActive && !isMuted) {
                        val readSize = audioRecord.read(buffer, 0, buffer.size)
                        if (readSize > 0) {
                            var maxAmp = 0
                            for (i in 0 until readSize) {
                                val absVal = kotlin.math.abs(buffer[i].toInt())
                                if (absVal > maxAmp) maxAmp = absVal
                            }
                            val norm = (maxAmp / 32767f).coerceIn(0f, 1f)
                            onAmplitudeChanged(norm)
                        }
                        delay(60)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioRecord", "Failed recording audio", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }
}


// -----------------------------------------------------------------
// SIMULATED STREAM ON-AIR BROADCAST OVERLAY
// -----------------------------------------------------------------
@Composable
fun SimulatedBroadcastOverlay(viewModel: MainViewModel) {
    val activeStream = viewModel.activeLiveStream ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground.copy(alpha = 0.95f))
            .clickable { /* prevent bubble clicks */ }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ON-AIR", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(activeStream.title, color = SpaceText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("[시청대상: ${activeStream.audienceScope}]", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Text("👀 시청자 ${activeStream.viewers}명", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelSolid)
                    .border(1.dp, BorderGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = NeonRed, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("실시간 스트리밍 송출 중", color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("주파수 피드가 암호화 연결되어 원격 시청자 단말기로 송출됩니다.", color = SpaceTextSoft, fontSize = 11.sp)
                }
            }

            Button(
                onClick = { viewModel.stopLiveBroadcast() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("방송 송출 종료", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------
// PEER CHAT & MULTI-RECIPIENT SMS POPUP WINDOW PANEL
// -----------------------------------------------------------------
@Composable
fun SimulatedChatDialog(viewModel: MainViewModel) {
    val messages = viewModel.chatMessages
    val allUsersList by viewModel.allUsers.collectAsStateWithLifecycle()
    val selectedRecipients = viewModel.selectedMessageRecipients
    var memberSearchQuery by remember { mutableStateOf("") }
    var chatText by remember { mutableStateOf("") }

    // Attachment state
    var pendingAttachmentName by remember { mutableStateOf<String?>(null) }
    var pendingAttachmentIsImage by remember { mutableStateOf(false) }

    // Message Edit State
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editingMessageText by remember { mutableStateOf("") }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Filter members matching search query
    val matchedMembers = remember(allUsersList, memberSearchQuery) {
        if (memberSearchQuery.isBlank()) {
            allUsersList.take(10)
        } else {
            allUsersList.filter {
                it.displayName.contains(memberSearchQuery, ignoreCase = true) ||
                it.username.contains(memberSearchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(memberSearchQuery, ignoreCase = true) ||
                it.role.contains(memberSearchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { viewModel.closeChat() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clickable { /* prevent closing when clicking inside card */ }
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📩 회원 검색 & 문자/메시지 센터",
                                color = SpaceText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                        Text(
                            text = "가입된 회원을 검색해 1:1 또는 그룹 대화를 나누고 5분 내 수정/삭제 및 파일 전송을 하세요",
                            color = SpaceTextSoft,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = { viewModel.closeChat() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = SpaceTextSoft)
                    }
                }

                HorizontalDivider(color = Color(0x228CAEC6), modifier = Modifier.padding(vertical = 4.dp))

                // Member Search & Multi-Selection Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        placeholder = { Text("🔍 회원가입 한 사람 이름/아이디/전화번호 검색...", fontSize = 11.5.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (memberSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { memberSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = SpaceTextSoft, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Member Search Results List (Chips)
                    if (matchedMembers.isEmpty()) {
                        Text(
                            text = "🔍 검색 결과가 없습니다.",
                            color = SpaceTextSoft,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchedMembers, key = { it.uid }) { user ->
                                val isSelected = selectedRecipients.any { it.uid == user.uid }
                                val isFriend = viewModel.isFriend(user.uid)
                                val roleText = when (user.role) {
                                    "teacher" -> "교사"
                                    "staff" -> "학생회"
                                    "leader" -> "실장"
                                    "admin" -> "관리자"
                                    else -> "학생"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleMessageRecipient(user) },
                                        label = {
                                            Text(
                                                text = "${if (isFriend) "⭐ " else ""}${user.displayName} ($roleText)${if (isSelected) " ✓" else ""}",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = PanelGlass,
                                            labelColor = SpaceText
                                        )
                                    )
                                    // Friend Add / Remove Button
                                    IconButton(
                                        onClick = {
                                            if (isFriend) {
                                                viewModel.removeFriend(user.uid, user.displayName)
                                            } else {
                                                viewModel.addFriend(user.uid, user.displayName, user.phoneNumber)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text(if (isFriend) "⭐" else "➕", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Selected Recipients Bar
                    if (selectedRecipients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 대화 수신 대상 (총 ${selectedRecipients.size}명 지정):",
                                color = NeonCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "전체 해제 ✕",
                                color = NeonRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.clearMessageRecipients() }
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedRecipients.forEach { target ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(target.displayName, color = NeonCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)

                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "삭제",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(12.dp).clickable { viewModel.removeMessageRecipient(target) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Log List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.from == "me"
                        val isSys = msg.from == "system"
                        val canEditOrDelete = isMe && viewModel.canEditOrDeleteMessage(msg.timestamp)

                        if (isSys) {
                            Text(
                                text = msg.text,
                                color = SpaceTextSoft,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 290.dp)
                                        .border(
                                            1.dp,
                                            if (isMe) NeonCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isMe) "📤 ${msg.fromName}" else "📩 ${msg.fromName}",
                                                color = if (isMe) NeonCyan else NeonAmber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (msg.isEdited) {
                                                Text("(수정됨)", color = SpaceTextSoft, fontSize = 9.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Attachment view if exists
                                        if (msg.fileName != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.3f),
                                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        if (msg.isImage) Icons.Default.Image else Icons.Default.AttachFile,
                                                        contentDescription = null,
                                                        tint = NeonGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Column {
                                                        Text(msg.fileName, color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text(if (msg.isImage) "📷 첨부 이미지 파일" else "📄 첨부 문서 파일", color = SpaceTextSoft, fontSize = 9.5.sp)
                                                    }
                                                }
                                            }
                                        }

                                        if (msg.text.isNotEmpty()) {
                                            Text(
                                                text = msg.text,
                                                color = SpaceText,
                                                fontSize = 12.5.sp,
                                                lineHeight = 17.sp,
                                                softWrap = true
                                            )
                                        }

                                        // Edit & Delete Action Buttons (within 5 minutes)
                                        if (canEditOrDelete) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text(
                                                    text = "✏️ 수정",
                                                    color = NeonCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        editingMessageId = msg.id
                                                        editingMessageText = msg.text
                                                    }
                                                )
                                                Text(
                                                    text = "🗑️ 삭제",
                                                    color = NeonRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        viewModel.deleteChatMessage(msg.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending Attachment Indicator Bar
                if (pendingAttachmentName != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(if (pendingAttachmentIsImage) Icons.Default.Image else Icons.Default.AttachFile, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Text("📎 첨부예정: ${pendingAttachmentName}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { pendingAttachmentName = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "취소", tint = NeonRed)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Input Bar with Image & File Attachment Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attachment options: Image / File
                    IconButton(
                        onClick = {
                            pendingAttachmentName = "사진_수업자료_${System.currentTimeMillis() % 1000}.png"
                            pendingAttachmentIsImage = true
                            viewModel.showToast("📷 이미지가 전송 첨부 파일에 선택되었습니다.")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "사진 첨부", tint = NeonGreen)
                    }

                    IconButton(
                        onClick = {
                            pendingAttachmentName = "과제제출_문서_${System.currentTimeMillis() % 1000}.pdf"
                            pendingAttachmentIsImage = false
                            viewModel.showToast("📄 파일 문서가 전송 첨부 파일에 선택되었습니다.")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "파일 첨부", tint = NeonCyan)
                    }

                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        placeholder = { Text("문자/메시지 입력...", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                        maxLines = 2,
                        singleLine = false,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (chatText.trim().isEmpty() && pendingAttachmentName == null) return@Button
                            viewModel.sendChatMessage(
                                text = chatText.trim(),
                                fileName = pendingAttachmentName,
                                isImage = pendingAttachmentIsImage
                            )
                            chatText = ""
                            pendingAttachmentName = null
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (selectedRecipients.size > 1) "📤 다중전송" else "📤 전송",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ✏️ Message Edit Dialog (within 5 minutes)
        if (editingMessageId != null) {
            AlertDialog(
                onDismissRequest = { editingMessageId = null },
                title = { Text("✏️ 메시지 내용 수정 (5분 이내)", color = SpaceText, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        Text("전송 후 5분 이내의 메시지만 수정할 수 있습니다.", color = SpaceTextSoft, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editingMessageText,
                            onValueChange = { editingMessageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SpaceText, unfocusedTextColor = SpaceText)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            editingMessageId?.let { id ->
                                viewModel.editChatMessage(id, editingMessageText)
                            }
                            editingMessageId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                    ) {
                        Text("수정 완료", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMessageId = null }) {
                        Text("취소", color = SpaceTextSoft)
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------
// EDIT PROFILE MODAL DIALOG
// -----------------------------------------------------------------
@Composable
fun EditProfileModalDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentUser = viewModel.currentUser ?: return
    val currentSchool = viewModel.currentSchool

    var displayNameText by remember { mutableStateOf(currentUser.displayName) }
    var phoneNumberText by remember { mutableStateOf(currentUser.phoneNumber) }
    var passwordText by remember { mutableStateOf(currentUser.password) }
    var inviteCodeText by remember { mutableStateOf(currentSchool?.inviteCode ?: "") }
    var gradeText by remember { mutableStateOf(currentUser.grade) }
    var classNumText by remember { mutableStateOf(currentUser.classNum) }
    var numText by remember { mutableStateOf(currentUser.num) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clickable { /* prevent closing when clicking inside */ }
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Text("✏️ 회원 로그인 정보 수정", color = SpaceText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = SpaceTextSoft)
                    }
                }

                HorizontalDivider(color = Color(0x228CAEC6))

                // Username (Read only)
                OutlinedTextField(
                    value = currentUser.username,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("아이디 (변경 불가)", fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.White.copy(alpha = 0.03f),
                        disabledTextColor = SpaceTextSoft,
                        disabledLabelColor = SpaceTextSoft
                    )
                )

                // Display Name
                OutlinedTextField(
                    value = displayNameText,
                    onValueChange = { displayNameText = it },
                    label = { Text("이름 / 닉네임", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    )
                )

                // Phone Number
                OutlinedTextField(
                    value = phoneNumberText,
                    onValueChange = { input -> phoneNumberText = formatPhoneNumber(input) },
                    label = { Text("전화번호 (휴대폰 번호)", fontSize = 11.5.sp) },
                    placeholder = { Text("010-XXXX-XXXX", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    )
                )

                // Grade / Class / Number Row
                Text("🏫 학년 / 반 / 번호 정보 수정", color = NeonCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = { gradeText = it },
                        label = { Text("학년", fontSize = 11.sp) },
                        placeholder = { Text("1", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                    OutlinedTextField(
                        value = classNumText,
                        onValueChange = { classNumText = it },
                        label = { Text("반", fontSize = 11.sp) },
                        placeholder = { Text("1", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                    OutlinedTextField(
                        value = numText,
                        onValueChange = { numText = it },
                        label = { Text("번호", fontSize = 11.sp) },
                        placeholder = { Text("15", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = SpaceText,
                            unfocusedTextColor = SpaceText
                        )
                    )
                }

                // Password / PIN
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("비밀번호 / PIN 번호", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    )
                )

                // School Invite Code
                OutlinedTextField(
                    value = inviteCodeText,
                    onValueChange = { inviteCodeText = it },
                    label = { Text("소속 학교/학원 초대코드 (선택)", fontSize = 11.5.sp) },
                    placeholder = { Text("초대코드 입력 시 해당 학교로 소속 전환", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = SpaceText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }

                    Button(
                        onClick = {
                            viewModel.updateCurrentUserInfo(
                                newDisplayName = displayNameText,
                                newPhoneNumber = phoneNumberText,
                                newPassword = passwordText,
                                inviteCode = inviteCodeText,
                                newGrade = gradeText,
                                newClassNum = classNumText,
                                newNum = numText,
                                onSuccess = onDismiss
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💾 저장하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(11)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}

// -----------------------------------------------------------------
// AUTHENTICATIONS SCREEN DIALOG
// -----------------------------------------------------------------
@Composable
fun AuthModalDialog(viewModel: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("student") }
    var roleCode by remember { mutableStateOf("") }

    var schoolMode by remember { mutableStateOf("join") } // "create" or "join"
    var inviteCode by remember { mutableStateOf("") }

    // Grade, Class, Number fields for Registration
    var grade by remember { mutableStateOf("1") }
    var classNum by remember { mutableStateOf("1") }
    var num by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf("") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // School classification and office of education states
    var selectedOffice by remember { mutableStateOf("전체") }
    var selectedType by remember { mutableStateOf("전체") }
    var schoolSearchQuery by remember { mutableStateOf("") }
    var inputSchoolName by remember { mutableStateOf("") }

    val combinedSchoolName = remember(selectedOffice, selectedType, inputSchoolName) {
        if (inputSchoolName.isEmpty()) "" else {
            val officeToUse = if (selectedOffice == "전체") "경기도교육청" else selectedOffice
            val typeToUse = if (selectedType == "전체") "고등학교" else selectedType
            "[$officeToUse / $typeToUse] $inputSchoolName"
        }
    }

    // Password-protected test mode states
    var phoneNumber by remember { mutableStateOf("") }

    // Phone verification states
    var isVerificationCodeSent by remember { mutableStateOf(false) }
    var sentVerificationCode by remember { mutableStateOf("") }
    var verificationInput by remember { mutableStateOf("") }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var verificationMsg by remember { mutableStateOf("") }

    val onPerformLogin = {
        errorMsg = ""
        if (username.isBlank()) {
            errorMsg = "아이디를 입력해 주세요."
        } else if (password.isBlank()) {
            errorMsg = "비밀번호를 입력해 주세요."
        } else {
            viewModel.login(
                username = username.trim(),
                pin = password.trim(),
                onError = { errorMsg = it }
            )
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // ID/Password recovery states
    var recoveryMode by remember { mutableStateOf<String?>(null) } // null, "id", "password"
    var recoveryName by remember { mutableStateOf("") }
    var recoveryPhone by remember { mutableStateOf("") }
    var recoveryUsername by remember { mutableStateOf("") }
    var recoveryResult by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .clickable { /* block */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
                .clickable { /* block */ }
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (recoveryMode != null) {
                    // --- ID/PASSWORD RECOVERY PANEL ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (recoveryMode == "id") "🔍 아이디 찾기" else "🔍 비밀번호 찾기",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { viewModel.isAuthModalVisible = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = SpaceTextSoft)
                        }
                    }

                    Text(
                        text = if (recoveryMode == "id") "가입 시 입력하신 이름과 전화번호를 입력해 주세요." else "가입 시 입력하신 아이디, 이름, 전화번호를 입력해 주세요.",
                        color = SpaceTextSoft,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color(0x118CAEC6))

                    if (recoveryMode == "password") {
                        OutlinedTextField(
                            value = recoveryUsername,
                            onValueChange = { recoveryUsername = it },
                            placeholder = { Text("아이디 입력") },
                            label = { Text("아이디") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = recoveryName,
                        onValueChange = { recoveryName = it },
                        placeholder = { Text("본인 실명 입력") },
                        label = { Text("이름") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recoveryPhone,
                        onValueChange = { input -> recoveryPhone = formatPhoneNumber(input) },
                        placeholder = { Text("010-XXXX-XXXX") },
                        label = { Text("전화번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (recoveryResult.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (recoveryResult.contains("❌")) Color(0x33FF5555) else NeonCyan.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (recoveryResult.contains("❌")) NeonRed.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = recoveryResult,
                                color = if (recoveryResult.contains("❌")) NeonRed else SpaceText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (recoveryMode == "id") {
                                viewModel.findId(recoveryName, recoveryPhone) { result ->
                                    recoveryResult = result
                                }
                            } else {
                                viewModel.findPassword(recoveryUsername, recoveryName, recoveryPhone) { result ->
                                    recoveryResult = result
                                }
                            }
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (recoveryMode == "id") "아이디 찾기 실행" else "비밀번호 찾기 실행", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            recoveryMode = null
                            recoveryResult = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("로그인 화면으로 돌아가기", color = SpaceTextSoft, fontSize = 11.5.sp)
                    }
                } else {
                    // --- NORMAL LOGIN / REGISTER PANEL ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "🔐 계정 만들기" else "🔐 로그인",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { viewModel.isAuthModalVisible = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = SpaceTextSoft)
                        }
                    }

                    Text(
                        text = "학교(조직)별로 데이터가 완벽 분리되어 저장됩니다. 계속하려면 가입 및 로그인이 필요합니다.",
                        color = SpaceTextSoft,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("영문/숫자 아이디") },
                        label = { Text("아이디") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("4자 이상의 숫자/비밀번호") },
                        label = { Text("비밀번호") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isRegisterMode) {
                                    onPerformLogin()
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isRegisterMode) {
                        // Auto Login + ID/PW Find Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = viewModel.isAutoLoginEnabled,
                                onCheckedChange = { viewModel.setAutoLogin(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonCyan,
                                    uncheckedColor = SpaceTextSoft,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Text(
                                text = "자동 로그인",
                                color = SpaceText,
                                fontSize = 11.5.sp,
                                modifier = Modifier.clickable { viewModel.setAutoLogin(!viewModel.isAutoLoginEnabled) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "아이디 찾기",
                                color = NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        recoveryMode = "id"
                                        recoveryResult = ""
                                        recoveryName = ""
                                        recoveryPhone = ""
                                    }
                            )
                            Text(
                                text = "|",
                                color = SpaceTextSoft.copy(alpha = 0.3f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "비밀번호 찾기",
                                color = NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        recoveryMode = "password"
                                        recoveryResult = ""
                                        recoveryName = ""
                                        recoveryPhone = ""
                                        recoveryUsername = ""
                                    }
                            )
                        }
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = { Text("예: 홍길동") },
                            label = { Text("표시할 본인 이름") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // --- PHONE NUMBER INPUT SECTION ---
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input -> phoneNumber = formatPhoneNumber(input) },
                            placeholder = { Text("010-XXXX-XXXX") },
                            label = { Text("본인 전화번호") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("역할 선택", color = SpaceTextSoft, fontSize = 11.sp)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expanded by remember { mutableStateOf(false) }
                            val label = when (role) {
                                "admin" -> "👑 총괄 최고 관리자"
                                "teacher" -> "교사 (관리자)"
                                "staff" -> "학생회 / 부장 교사"
                                "leader" -> "반 실장 / 부실장"
                                else -> "학생 (일반)"
                            }
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(label, color = SpaceText)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(
                                    "student" to "학생 (일반)",
                                    "leader" to "반 실장/부실장",
                                    "staff" to "학생회/부장",
                                    "teacher" to "교사 (관리자)",
                                    "admin" to "👑 총괄 최고 관리자"
                                ).forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.second) },
                                        onClick = { role = pair.first; expanded = false }
                                    )
                                }
                            }
                        }

                        if (role != "student") {
                            OutlinedTextField(
                                value = roleCode,
                                onValueChange = { roleCode = it },
                                placeholder = { Text("인증 번호 (교사/관리자: 1234, 학생회: 0000)") },
                                label = { Text("역할 확인 코드") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // --- UNIFIED SCHOOL & ACADEMY SEARCH SECTION ---
                        Text("🏫 소속 학교/학원 지정 (검색 및 가입)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // 1. Filter dropdowns (Metropolitan/Provincial Office & Classification)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ATPT Office Selector
                            var officeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.0f)) {
                                OutlinedButton(
                                    onClick = { officeExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SpaceTextSoft.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (selectedOffice == "전체") "시도교육청: 전체" else selectedOffice,
                                            color = SpaceText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SpaceTextSoft, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = officeExpanded,
                                    onDismissRequest = { officeExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)
                                ) {
                                    listOf(
                                        "전체", "서울특별시교육청", "부산광역시교육청", "대구광역시교육청", "인천광역시교육청",
                                        "광주광역시교육청", "대전광역시교육청", "울산광역시교육청", "세종특별자치시교육청",
                                        "경기도교육청", "강원특별자치도교육청", "충청북도교육청", "충청남도교육청",
                                        "전북특별자치도교육청", "전라남도교육청", "경상북도교육청", "경상남도교육청",
                                        "제주특별자치도교육청"
                                    ).forEach { office ->
                                        DropdownMenuItem(
                                            text = { Text(office, fontSize = 11.sp) },
                                            onClick = {
                                                selectedOffice = office
                                                officeExpanded = false
                                                viewModel.searchSchoolsFromNeis(schoolSearchQuery, office, selectedType)
                                            }
                                        )
                                    }
                                }
                            }

                            // Classification Selector (School Type & Academies)
                            var typeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(0.9f)) {
                                OutlinedButton(
                                    onClick = { typeExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SpaceTextSoft.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (selectedType == "전체") "분류: 전체" else selectedType,
                                            color = SpaceText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SpaceTextSoft, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    listOf("전체", "초등학교", "중학교", "고등학교", "학원", "대학교", "특수학교", "특성화고등학교", "대안학교", "기타").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type, fontSize = 11.sp) },
                                            onClick = {
                                                selectedType = type
                                                typeExpanded = false
                                                viewModel.searchSchoolsFromNeis(schoolSearchQuery, selectedOffice, type)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. School/Academy Name Search Input Field
                        val schoolsList by viewModel.allSchools.collectAsState()
                        var showSearchDropdown by remember { mutableStateOf(true) }

                        val filteredSchools = remember(schoolSearchQuery, selectedOffice, selectedType, schoolsList) {
                            schoolsList.filter { school ->
                                val matchesQuery = schoolSearchQuery.isBlank() || school.name.contains(schoolSearchQuery, ignoreCase = true)
                                val matchesOffice = selectedOffice == "전체" || school.name.contains(selectedOffice)
                                val matchesType = selectedType == "전체" || (if (selectedType == "학원") school.name.contains("학원") else school.name.contains(selectedType))
                                matchesQuery && matchesOffice && matchesType
                            }.take(30)
                        }

                        OutlinedTextField(
                            value = schoolSearchQuery,
                            onValueChange = {
                                schoolSearchQuery = it
                                showSearchDropdown = true
                                viewModel.searchSchoolsFromNeis(it, selectedOffice, selectedType)
                            },
                            placeholder = { Text("학교 또는 학원 이름 입력 (예: 경기고, 대성)") },
                            label = { Text("학교/학원명 검색") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpaceTextSoft) },
                            trailingIcon = {
                                if (schoolSearchQuery.isNotEmpty() || !showSearchDropdown) {
                                    IconButton(onClick = {
                                        schoolSearchQuery = ""
                                        showSearchDropdown = true
                                    }) {
                                        Icon(
                                            if (showSearchDropdown) Icons.Default.Clear else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = SpaceTextSoft
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { showSearchDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SpaceTextSoft)
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = SpaceText,
                                unfocusedTextColor = SpaceText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3. Search Results Dropdown List
                        if (showSearchDropdown && (schoolSearchQuery.isNotEmpty() || selectedOffice != "전체" || selectedType != "전체")) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.95f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    if (filteredSchools.isEmpty()) {
                                        Text(
                                            text = "일치하는 학교나 학원이 없습니다.",
                                            color = SpaceTextSoft,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    } else {
                                        filteredSchools.forEach { school ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        schoolMode = "join"
                                                        inviteCode = school.inviteCode
                                                        schoolSearchQuery = school.name
                                                        inputSchoolName = ""
                                                        showSearchDropdown = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(school.name, color = SpaceText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("초대코드: ${school.inviteCode}", color = NeonCyan, fontSize = 10.sp)
                                                }
                                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            }
                                            HorizontalDivider(color = Color(0x118CAEC6))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 4. Status Indicator Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (schoolSearchQuery.isNotEmpty() && inviteCode.isNotEmpty()) NeonCyan.copy(alpha = 0.08f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (schoolSearchQuery.isNotEmpty() && inviteCode.isNotEmpty()) NeonCyan.copy(alpha = 0.25f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                if (schoolSearchQuery.isNotEmpty() && inviteCode.isNotEmpty()) {
                                    Text("✅ 가입할 소속 선택됨", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("명칭: $schoolSearchQuery", color = SpaceText, fontSize = 12.sp)
                                    Text("초대코드: $inviteCode", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("⚠️ 가입할 학교나 학원을 검색하여 선택해 주세요.", color = NeonAmber, fontSize = 11.sp)
                                }
                            }
                        }

                        // Invite code manual text field
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            placeholder = { Text("SEL... 또는 ACA... 코드") },
                            label = { Text("소속 학교/학원 초대 코드") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Grade, Class, and Number Selection Row (Enforce digits for Number!)
                        Text("🏫 학년 / 반 / 번호 입력", color = NeonCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            var gradeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { gradeExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("${grade}학년", color = SpaceText, fontSize = 11.sp)
                                }
                                DropdownMenu(expanded = gradeExpanded, onDismissRequest = { gradeExpanded = false }) {
                                    (1..6).forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text("${g}학년", fontSize = 11.sp) },
                                            onClick = { grade = g.toString(); gradeExpanded = false }
                                        )
                                    }
                                }
                            }

                            var classExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { classExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("${classNum}반", color = SpaceText, fontSize = 11.sp)
                                }
                                DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                                    (1..15).forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text("${c}반", fontSize = 11.sp) },
                                            onClick = { classNum = c.toString(); classExpanded = false }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = num,
                                onValueChange = { input ->
                                    // ENFORCE NUMERIC-ONLY INPUT FOR NUMBER
                                    num = input.filter { it.isDigit() }
                                },
                                placeholder = { Text("숫자") },
                                label = { Text("번호") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = SpaceText,
                                    unfocusedTextColor = SpaceText
                                ),
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            errorMsg = ""
                            if (isRegisterMode) {
                                if (phoneNumber.isBlank()) {
                                    errorMsg = "전화번호를 입력해 주세요."
                                    return@Button
                                }
                                viewModel.signup(
                                    username = username.trim(),
                                    pin = password.trim(),
                                    displayName = displayName.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    role = role,
                                    roleCode = roleCode.trim(),
                                    schoolMode = schoolMode,
                                    schoolName = combinedSchoolName.trim(),
                                    inviteCode = inviteCode.trim(),
                                    grade = grade,
                                    classNum = classNum,
                                    num = if (num.isNotBlank()) num else "1",
                                    onError = { errorMsg = it }
                                )
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            } else {
                                onPerformLogin()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isRegisterMode) "가입 및 로그인 실행" else "로그인", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMsg = ""
                            isVerificationCodeSent = false
                            sentVerificationCode = ""
                            verificationInput = ""
                            isPhoneVerified = false
                            verificationMsg = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegisterMode) "이미 계정이 있으신가요? 로그인하기" else "처음이신가요? 새 계정 만들기",
                            color = NeonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MASTER ADMIN SCHOOL & ACADEMY CONTROL PANEL
// -----------------------------------------------------------------
@Composable
fun MasterAdminSchoolControlPanel(viewModel: MainViewModel) {
    val allSchools by viewModel.allSchools.collectAsState()
    val activeSchoolId = viewModel.effectiveSchoolId
    val isOverridden = viewModel.masterAdminSelectedSchoolId != null
    
    val currentActiveSchool = remember(allSchools, activeSchoolId) {
        allSchools.find { it.schoolId == activeSchoolId } ?: viewModel.currentSchool
    }

    var showSchoolPickerModal by remember { mutableStateOf(false) }
    var showEditSchoolModal by remember { mutableStateOf(false) }
    var showCreateSchoolModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PanelSolid),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "총괄 관리자: 학교/학원 개별 제어 센터",
                        color = NeonAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOverridden) {
                    Surface(
                        color = NeonMagenta.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonMagenta)
                    ) {
                        Text(
                            text = "개별 선택 작동 중",
                            color = NeonMagenta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x228CAEC6))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("현재 관람 및 편집 중인 소속:", color = SpaceTextSoft, fontSize = 11.sp)
                    Text(
                        text = currentActiveSchool?.name ?: "지정된 소속 없음",
                        color = SpaceText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "초대코드: ${currentActiveSchool?.inviteCode ?: "-"} | ID: ${currentActiveSchool?.schoolId ?: "-"}",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { showSchoolPickerModal = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("🏫 소속 전환", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showEditSchoolModal = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NeonAmber),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("✏️ 정보 수정", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showCreateSchoolModal = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("➕ 신규 등록", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isOverridden) {
                TextButton(
                    onClick = { viewModel.resetMasterAdminSchoolSelection() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("↺ 본인 소속 학교/학원으로 원복", color = SpaceTextSoft, fontSize = 11.5.sp)
                }
            }
        }
    }

    if (showSchoolPickerModal) {
        MasterAdminSchoolPickerModal(
            viewModel = viewModel,
            allSchools = allSchools,
            activeSchoolId = activeSchoolId,
            onSelectSchool = { school ->
                viewModel.selectSchoolForMasterAdmin(school.schoolId)
                showSchoolPickerModal = false
            },
            onDismiss = { showSchoolPickerModal = false }
        )
    }

    if (showEditSchoolModal && currentActiveSchool != null) {
        MasterAdminEditSchoolModal(
            school = currentActiveSchool,
            onSave = { newName, newCode ->
                viewModel.updateSchoolByMasterAdmin(currentActiveSchool.schoolId, newName, newCode)
                showEditSchoolModal = false
            },
            onDismiss = { showEditSchoolModal = false }
        )
    }

    if (showCreateSchoolModal) {
        MasterAdminCreateSchoolModal(
            onCreate = { name, code ->
                viewModel.createSchoolByMasterAdmin(name, code)
                showCreateSchoolModal = false
            },
            onDismiss = { showCreateSchoolModal = false }
        )
    }
}

@Composable
fun MasterAdminSchoolPickerModal(
    viewModel: MainViewModel,
    allSchools: List<com.example.data.SchoolEntity>,
    activeSchoolId: String,
    onSelectSchool: (com.example.data.SchoolEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("전체") }
    var selectedType by remember { mutableStateOf("전체") }

    val regions = listOf("전체", "서울", "경기", "인천", "부산", "대구", "광주", "대전", "세종", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")
    val types = listOf("전체", "고등학교", "중학교", "초등학교", "학원")

    val officeFullName = remember(selectedRegion) {
        when (selectedRegion) {
            "서울" -> "서울특별시교육청"
            "부산" -> "부산광역시교육청"
            "대구" -> "대구광역시교육청"
            "인천" -> "인천광역시교육청"
            "광주" -> "광주광역시교육청"
            "대전" -> "대전광역시교육청"
            "울산" -> "울산광역시교육청"
            "세종" -> "세종특별자치시교육청"
            "경기" -> "경기도교육청"
            "강원" -> "강원특별자치도교육청"
            "충북" -> "충청북도교육청"
            "충남" -> "충청남도교육청"
            "전북" -> "전북특별자치도교육청"
            "전남" -> "전라남도교육청"
            "경북" -> "경상북도교육청"
            "경남" -> "경상남도교육청"
            "제주" -> "제주특별자치도교육청"
            else -> "전체"
        }
    }

    // Trigger NEIS API search when search query or filters change
    LaunchedEffect(searchQuery, selectedRegion, selectedType) {
        if (searchQuery.trim().length >= 2 || selectedRegion != "전체" || selectedType != "전체") {
            viewModel.searchSchoolsFromNeis(
                query = searchQuery,
                officeName = officeFullName,
                schoolType = selectedType
            )
        }
    }

    val filtered = remember(allSchools, searchQuery, selectedRegion, selectedType) {
        allSchools.filter { school ->
            val matchQuery = searchQuery.isBlank() || school.name.contains(searchQuery, ignoreCase = true) || school.inviteCode.contains(searchQuery, ignoreCase = true)
            val matchRegion = selectedRegion == "전체" || school.name.contains(selectedRegion)
            val matchType = selectedType == "전체" || (if (selectedType == "학원") school.name.contains("학원") else school.name.contains(selectedType))
            matchQuery && matchRegion && matchType
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clickable { /* stop propagation */ }
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🏫 전국 학교 & 학원 전환 (총 ${allSchools.size}개 등록)", color = SpaceText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("검색어를 입력하면 교육청 NEIS API에서 실시간 조회됩니다", color = SpaceTextSoft, fontSize = 10.5.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = SpaceTextSoft)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("전국 학교/학원명 검색 (예: 서울고, 대성학원)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = SpaceTextSoft)
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (searchQuery.trim().isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.addAndSelectSchool(searchQuery.trim())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("➕ '${searchQuery.trim()}' (으)로 즉시 신규 등록 & 소속 전환", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Region filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    regions.forEach { reg ->
                        val selected = selectedRegion == reg
                        FilterChip(
                            selected = selected,
                            onClick = { selectedRegion = reg },
                            label = { Text(reg, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = PanelGlass,
                                labelColor = SpaceText
                            )
                        )
                    }
                }

                // Type filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.forEach { typ ->
                        val selected = selectedType == typ
                        FilterChip(
                            selected = selected,
                            onClick = { selectedType = typ },
                            label = { Text(typ, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (typ == "학원") NeonAmber else NeonGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = PanelGlass,
                                labelColor = SpaceText
                            )
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("검색 결과가 없습니다", color = SpaceTextSoft, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    viewModel.searchSchoolsFromNeis(searchQuery, officeFullName, selectedType)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🌐 전국 NEIS API 조회 실행", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.schoolId }) { school ->
                            val isCurrent = school.schoolId == activeSchoolId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSchool(school) }
                                    .border(
                                        1.dp,
                                        if (isCurrent) NeonCyan else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = school.name,
                                            color = if (isCurrent) NeonCyan else SpaceText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "초대코드: ${school.inviteCode} | ID: ${school.schoolId}",
                                            color = SpaceTextSoft,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isCurrent) {
                                        Surface(
                                            color = NeonCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, NeonCyan)
                                        ) {
                                            Text(
                                                text = "현재 선택됨 ✅",
                                                color = NeonCyan,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterAdminEditSchoolModal(
    school: com.example.data.SchoolEntity,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(school.name) }
    var inviteCode by remember { mutableStateOf(school.inviteCode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable { }
                .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✏️ 학교/학원 정보 수정", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("학교/학원 명칭") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("초대 코드") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("취소", color = SpaceTextSoft)
                    }
                    Button(
                        onClick = { onSave(name, inviteCode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("저장하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MasterAdminCreateSchoolModal(
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("SEL" + (1000..9999).random().toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable { }
                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("➕ 신규 학교/학원 등록", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("학교/학원 명칭") },
                    placeholder = { Text("예: [서울] 미래고등학교 또는 [학원] 대성학원") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("초대 코드") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = SpaceText, unfocusedTextColor = SpaceText),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("취소", color = SpaceTextSoft)
                    }
                    Button(
                        onClick = { onCreate(name, inviteCode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("등록하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// HOME SCREEN (WITH REAL-TIME DIGITAL CLOCK & QUICK PORTAL ACCESS)
// -----------------------------------------------------------------
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val sdfTime = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREAN) }
    val sdfDate = remember { java.text.SimpleDateFormat("yyyy년 MM월 dd일 (E)", java.util.Locale.KOREAN) }
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    val greetingText = when {
        hour in 6..11 -> "좋은 아침입니다! ☀️\n활기찬 하루를 시작해 보세요."
        hour in 12..17 -> "즐거운 오후입니다! ☕\n오늘 하루도 힘내세요!"
        hour in 18..21 -> "편안한 저녁 시간입니다! ✨\n하루를 기분 좋게 마무리해 보세요."
        else -> "고요한 밤입니다! 🌙\n오늘 하루도 정말 수고 많으셨습니다."
    }

    val currentUser = viewModel.currentUser
    val currentSchool = viewModel.currentSchool
    val context = androidx.compose.ui.platform.LocalContext.current

    var showMealDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glowing Digital Clock Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGlow.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = sdfDate.format(currentTime),
                    color = SpaceTextSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = sdfTime.format(currentTime),
                    color = NeonCyan,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = NeonCyan.copy(alpha = 0.6f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = 14f
                        )
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderGlow.copy(alpha = 0.2f)
                )

                Text(
                    text = greetingText,
                    color = SpaceText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Quick Access Cards Grid for Logged-In Users or Guidance Card
        if (currentUser != null) {
            if (viewModel.isTeacher() || viewModel.isAdmin()) {
                MasterAdminSchoolControlPanel(viewModel)
            }

            // User & School Registration Info Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏫 ${currentSchool?.name ?: "소속 학교 미지정"}",
                                color = NeonCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "👤 ${currentUser.displayName} 학생 | ${currentUser.grade}학년 ${currentUser.classNum}반 ${currentUser.num}번",
                                color = SpaceText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "초대 코드: ${currentSchool?.inviteCode ?: "-"}",
                                color = SpaceTextSoft,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(color = BorderGlow.copy(alpha = 0.2f))

                    // Buttons for Meal Check and Map Linking
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Meal Check Button
                        Button(
                            onClick = {
                                viewModel.fetchSchoolMeal()
                                showMealDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.2f), contentColor = NeonAmber),
                            border = BorderStroke(1.dp, NeonAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Text("🍱 급식 확인하기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Map Application Integration Button
                        Button(
                            onClick = {
                                val rawName = currentSchool?.name ?: "학교"
                                val targetSchool = viewModel.cleanSchoolNameForMap(rawName)
                                val query = android.net.Uri.encode(targetSchool)
                                val geoIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                                val chooser = android.content.Intent.createChooser(geoIntent, "학교 위치를 찾아볼 지도 앱을 선택하세요 (카카오맵 / 네이버지도 / 구글지도)")
                                try {
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    viewModel.showToast("⚠️ 실행 가능한 지도 앱이 설치되어 있지 않습니다.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f), contentColor = NeonGreen),
                            border = BorderStroke(1.dp, NeonGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Text("🗺️ 학교 위치 지도", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = PanelGlass),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 바로가기 퀵 메뉴",
                            color = SpaceText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = { viewModel.selectTab("timetable-tab") }) {
                            Text("📅 내 학급 시간표 보기 >", color = NeonCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick access items
                    val quickTabs = listOf(
                        Triple("timetable-tab", "📅 학급 시간표", NeonCyan),
                        Triple("rent-tab", "📦 대여 대장", NeonCyan),
                        Triple("suggest-tab", "💬 익명 건의", NeonMagenta),
                        Triple("attend-tab", "📝 출석 점검", NeonGreen),
                        Triple("uniform-tab", "🧥 교복 점검", NeonAmber),
                        Triple("merit-tab", "⭐ 상벌점 가감", NeonPurple)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickTabs.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (tabId, label, color) ->
                                    Button(
                                        onClick = { viewModel.selectTab(tabId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = color.copy(alpha = 0.12f),
                                            contentColor = color
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Welcome Guidance Card for logged out
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGlow.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = PanelGlass),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🛠️ 포털 주요 제공 기능",
                        color = NeonPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val items = listOf(
                        "📦 통합 대여 대장" to "학교 비품, 교복 등 대여 물품을 스마트하게 관리하고 승인하세요.",
                        "💬 익명 소통 창구" to "신원 노출 걱정 없는 100% 실시간 익명 건의함으로 학교 생활을 개선하세요.",
                        "📊 편리한 학급 도구" to "출석 체크, 교복 상태 점검, 벌점 관리, 청소 당번, 학급 장부, 자리 배치까지!",
                        "🔍 스마트 분실물 센터" to "잃어버린 물건을 실시간 사진 및 장소 정보로 쉽게 찾으세요."
                    )

                    items.forEach { (title, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("▪", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Column {
                                Text(title, color = SpaceText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(desc, color = SpaceTextSoft, fontSize = 11.5.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.isAuthModalVisible = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen.copy(alpha = 0.15f),
                    contentColor = NeonGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("로그인 / 회원가입하고 시작하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showMealDialog) {
        MealCheckModalDialog(
            viewModel = viewModel,
            schoolName = currentSchool?.name ?: "소속 학교",
            onDismiss = { showMealDialog = false }
        )
    }
}

@Composable
fun MealCheckModalDialog(
    viewModel: MainViewModel,
    schoolName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🍱 학교 급식 날짜별 메뉴 조회", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🏫 $schoolName",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Date Navigation Control Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = PanelGlass)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.setMealDateAndFetch(-1) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.2f), contentColor = NeonAmber),
                                border = BorderStroke(1.dp, NeonAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("◀ 이전 날", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = viewModel.selectedMealDateFormatted,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { viewModel.setMealDateAndFetch(1) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(alpha = 0.2f), contentColor = NeonAmber),
                                border = BorderStroke(1.dp, NeonAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("다음 날 ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📅 오늘 날짜로 이동",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                                        viewModel.setMealDateAndFetch(0, today)
                                    }
                                    .padding(vertical = 2.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

                if (viewModel.schoolMealInfoMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.schoolMealInfoMessage,
                        color = SpaceTextSoft,
                        fontSize = 11.sp
                    )
                }

                HorizontalDivider(color = BorderGlow.copy(alpha = 0.2f))

                if (viewModel.isMealLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonAmber, modifier = Modifier.size(28.dp))
                    }
                } else if (viewModel.schoolMealMenu.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.9f))
                    ) {
                        Text(
                            text = "해당 날짜의 급식 정보가 준비 중이거나 주말/휴업일입니다.",
                            color = SpaceText,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.schoolMealMenu.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, NeonAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = PanelSolid.copy(alpha = 0.9f))
                            ) {
                                Text(
                                    text = item,
                                    color = SpaceText,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black)
            ) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PanelSolid,
        shape = RoundedCornerShape(16.dp)
    )
}



