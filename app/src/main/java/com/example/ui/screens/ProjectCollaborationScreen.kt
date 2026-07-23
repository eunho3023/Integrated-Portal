package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCollaborationScreen(viewModel: MainViewModel) {
    val projectGroups by viewModel.projectGroups.collectAsStateWithLifecycle()
    val projectTasks by viewModel.projectTasks.collectAsStateWithLifecycle()
    val projectResources by viewModel.projectResources.collectAsStateWithLifecycle()
    val projectEvaluations by viewModel.projectEvaluations.collectAsStateWithLifecycle()

    val currentUser = viewModel.currentUser
    val allUsersList by viewModel.allUsers.collectAsStateWithLifecycle()
    val rosterList by viewModel.rosterNames.collectAsStateWithLifecycle()

    val registeredUsersList = remember(allUsersList, rosterList, currentUser) {
        val fromUsers = allUsersList.map { it.displayName }.filter { it.isNotBlank() }
        val fromRoster = rosterList.map { it.name }.filter { it.isNotBlank() }
        val current = listOfNotNull(currentUser?.displayName)
        val defaultCandidates = listOf("김철수", "이영희", "박민수", "정수진", "강하늘")
        (current + fromUsers + fromRoster + defaultCandidates).distinct().filter { it.isNotBlank() }
    }

    // Auto select first project if none selected
    LaunchedEffect(projectGroups) {
        if (viewModel.selectedProjectId == null && projectGroups.isNotEmpty()) {
            viewModel.selectedProjectId = projectGroups.firstOrNull()?.id
        }
    }

    val selectedGroup = projectGroups.find { it.id == viewModel.selectedProjectId }

    var activeSubTab by remember { mutableStateOf("tasks") } // "tasks", "schedule", "resources", "eval"

    var showNewProjectModal by remember { mutableStateOf(false) }
    var showNewTaskModal by remember { mutableStateOf(false) }
    var showNewResourceModal by remember { mutableStateOf(false) }

    val mainScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(mainScrollState)
                .padding(12.dp)
        ) {
        // Top Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = NeonCyan)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🤝 모둠 프로젝트 & 기여도 측정",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpaceText
                            )
                            Text(
                                text = "조별과제 역할분담, 일정, 자료공유 및 상호평가 리포트",
                                fontSize = 11.sp,
                                color = SpaceTextSoft
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Project Selector Horizontal Scroll
                if (projectGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 모둠 프로젝트가 없습니다. '새 모둠 생성' 버튼을 눌러 프로젝트를 시작해보세요!",
                            fontSize = 12.sp,
                            color = SpaceTextSoft,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        projectGroups.forEach { group ->
                            val isSelected = group.id == viewModel.selectedProjectId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent
                                ),
                                modifier = Modifier.clickable { viewModel.selectedProjectId = group.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${group.subject}] ${group.title}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else SpaceText
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedGroup == null) {
            // Empty State view with demo button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelSolid),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(48.dp))
                        Text("선택된 프로젝트가 없습니다", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "오른쪽 아래 '+' 버튼을 눌러 새로운 조별과제/모둠 프로젝트를 생성하고 협업을 시작해보세요.",
                            color = SpaceTextSoft,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Active Project Details & Subtabs
            val currentTasks = projectTasks.filter { it.projectId == selectedGroup.id }
            val currentResources = projectResources.filter { it.projectId == selectedGroup.id }
            val currentEvaluations = projectEvaluations.filter { it.projectId == selectedGroup.id }

            val completedTasksCount = currentTasks.count { it.status == "완료" }
            val totalTasksCount = currentTasks.size
            val progressPercent = if (totalTasksCount > 0) (completedTasksCount.toFloat() / totalTasksCount.toFloat()) else 0f

            // Project Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelGlass),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NeonPurple.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = selectedGroup.subject,
                                        color = NeonPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedGroup.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpaceText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👥 팀원: ${selectedGroup.membersData} | 📅 제출기한: ${selectedGroup.dueDate}",
                                fontSize = 11.sp,
                                color = SpaceTextSoft
                            )
                        }

                        IconButton(onClick = { viewModel.deleteProjectGroup(selectedGroup) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Group", tint = NeonRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("전체 과제 진행률", fontSize = 11.sp, color = SpaceTextSoft)
                        Text(
                            "${(progressPercent * 100).toInt()}% (${completedTasksCount}/${totalTasksCount}건 완료)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progressPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val subTabs = listOf(
                    "tasks" to "📋 역할&할일",
                    "schedule" to "📅 일정현황",
                    "resources" to "📂 자료공유",
                    "eval" to "📊 기여도&평가"
                )

                subTabs.forEach { (tabKey, tabLabel) ->
                    val isTabSelected = activeSubTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTabSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (isTabSelected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { activeSubTab = tabKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 11.5.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTabSelected) NeonCyan else SpaceTextSoft
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Tab Contents
            Box(modifier = Modifier.fillMaxWidth()) {
                when (activeSubTab) {
                    "tasks" -> TasksSubTab(
                        tasks = currentTasks,
                        groupMembers = selectedGroup.membersData.split(",").map { it.trim() },
                        onAddTaskClick = { showNewTaskModal = true },
                        onUpdateStatus = { task, newStatus -> viewModel.updateProjectTaskStatus(task, newStatus) },
                        onDeleteTask = { task -> viewModel.deleteProjectTask(task) }
                    )
                    "schedule" -> ScheduleSubTab(
                        tasks = currentTasks,
                        groupMembers = selectedGroup.membersData.split(",").map { it.trim() }
                    )
                    "resources" -> ResourcesSubTab(
                        resources = currentResources,
                        onAddResourceClick = { showNewResourceModal = true },
                        onDeleteResource = { res -> viewModel.deleteProjectResource(res) }
                    )
                    "eval" -> ContributionEvalSubTab(
                        group = selectedGroup,
                        tasks = currentTasks,
                        resources = currentResources,
                        evaluations = currentEvaluations,
                        currentUser = currentUser,
                        onSubmitEval = { target, r, q, c, comment ->
                            viewModel.submitProjectEvaluation(
                                selectedGroup.id, target, r, q, c, comment
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }

    // Floating Action Button at Bottom Right (like in QaScreen)
    FloatingActionButton(
        onClick = { showNewProjectModal = true },
        containerColor = NeonCyan,
        contentColor = Color.Black,
        shape = CircleShape,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("새 모둠 생성", fontWeight = FontWeight.Bold)
        }
    }

    // Modal 1: Create New Project Group
    if (showNewProjectModal) {
        NewProjectDialog(
            defaultUser = currentUser?.displayName ?: "",
            registeredUsers = registeredUsersList,
            onDismiss = { showNewProjectModal = false },
            onCreate = { title, subject, dueDate, members ->
                viewModel.createProjectGroup(title, subject, dueDate, members)
                showNewProjectModal = false
            }
        )
    }

    // Modal 2: Add New Task
    if (showNewTaskModal && selectedGroup != null) {
        NewTaskDialog(
            groupMembers = selectedGroup.membersData.split(",").map { it.trim() },
            onDismiss = { showNewTaskModal = false },
            onCreate = { taskName, assignee, role, dueDate, weight ->
                viewModel.addProjectTask(selectedGroup.id, taskName, assignee, role, dueDate, weight)
                showNewTaskModal = false
            }
        )
    }

    // Modal 3: Add New Resource
    if (showNewResourceModal && selectedGroup != null) {
        NewResourceDialog(
            onDismiss = { showNewResourceModal = false },
            onCreate = { title, linkContent, resType ->
                viewModel.addProjectResource(selectedGroup.id, title, linkContent, resType)
                showNewResourceModal = false
            }
        )
    }
}
}

@Composable
fun TasksSubTab(
    tasks: List<ProjectTaskEntity>,
    groupMembers: List<String>,
    onAddTaskClick: () -> Unit,
    onUpdateStatus: (ProjectTaskEntity, String) -> Unit,
    onDeleteTask: (ProjectTaskEntity) -> Unit
) {
    var filterRole by remember { mutableStateOf("전체") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋 역할 분담 및 과제 목록", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)
            Button(
                onClick = onAddTaskClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("할일 등록", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter
        val roleCategories = listOf("전체", "총괄/리더", "자료조사", "PPT제작", "발표", "보고서작성", "기타")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            roleCategories.forEach { role ->
                val isSelected = filterRole == role
                FilterChip(
                    selected = isSelected,
                    onClick = { filterRole = role },
                    label = { Text(role, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple.copy(alpha = 0.25f),
                        selectedLabelColor = NeonPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val filteredTasks = tasks.filter { filterRole == "전체" || it.roleCategory == filterRole }

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 역할 및 과제가 없습니다.\n'할일 등록' 버튼을 눌러 팀원별 역할을 지정해보세요!",
                    color = SpaceTextSoft,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredTasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onUpdateStatus = { newStatus -> onUpdateStatus(task, newStatus) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: ProjectTaskEntity,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (task.status) {
        "완료" -> NeonGreen
        "진행중" -> NeonAmber
        else -> SpaceTextSoft
    }

    val roleBgColor = when (task.roleCategory) {
        "총괄/리더" -> NeonRed.copy(alpha = 0.2f)
        "자료조사" -> NeonCyan.copy(alpha = 0.2f)
        "PPT제작" -> NeonMagenta.copy(alpha = 0.2f)
        "발표" -> NeonGreen.copy(alpha = 0.2f)
        "보고서작성" -> NeonPurple.copy(alpha = 0.2f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelSolid),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = roleBgColor
                    ) {
                        Text(
                            text = task.roleCategory,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpaceText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "👤 ${task.assigneeName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⭐ 중요도 ${task.contributionWeight}",
                        fontSize = 10.sp,
                        color = NeonAmber
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = SpaceTextSoft, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.taskName,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SpaceText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 마감일: ${task.dueDate}",
                    fontSize = 11.sp,
                    color = SpaceTextSoft
                )

                // Quick Status Toggle Button
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("대기", "진행중", "완료").forEach { st ->
                        val isCurr = task.status == st
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurr) statusColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurr) statusColor else Color.Transparent),
                            modifier = Modifier.clickable { onUpdateStatus(st) }
                        ) {
                            Text(
                                text = st,
                                fontSize = 10.sp,
                                fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurr) statusColor else SpaceTextSoft,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSubTab(
    tasks: List<ProjectTaskEntity>,
    groupMembers: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📅 일정 및 팀원별 진행 현황", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)

        // Member progress breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("👥 팀원별 과제 이행률", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                groupMembers.forEach { member ->
                    val memberTasks = tasks.filter { it.assigneeName == member }
                    val memberCompleted = memberTasks.count { it.status == "완료" }
                    val memberTotal = memberTasks.size
                    val memberRatio = if (memberTotal > 0) memberCompleted.toFloat() / memberTotal else 0f

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 $member",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SpaceText
                            )
                            Text(
                                text = "$memberCompleted / $memberTotal 건 완료 (${(memberRatio * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = if (memberRatio == 1f && memberTotal > 0) NeonGreen else SpaceTextSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = memberRatio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (memberRatio == 1f && memberTotal > 0) NeonGreen else NeonCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // Timeline Schedule list sorted by date
        Text("⏱️ 타임라인 일정 목록", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SpaceText)

        val sortedTasks = tasks.sortedBy { it.dueDate }

        if (sortedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("등록된 과제 일정이 없습니다.", fontSize = 12.sp, color = SpaceTextSoft)
            }
        } else {
            sortedTasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelSolid)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (task.status == "완료") Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (task.status == "완료") NeonGreen else NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = task.taskName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SpaceText
                            )
                            Text(
                                text = "담당: ${task.assigneeName} (${task.roleCategory})",
                                fontSize = 10.5.sp,
                                color = SpaceTextSoft
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = task.dueDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcesSubTab(
    resources: List<ProjectResourceEntity>,
    onAddResourceClick: () -> Unit,
    onDeleteResource: (ProjectResourceEntity) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📂 자료 & 링크 공유함", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)
            Button(
                onClick = onAddResourceClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("자료 공유", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (resources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "공유된 자료가 없습니다.\n참고 자료 링크, 보고서 초안, 발표 자료 등을 팀원들과 공유해보세요!",
                    color = SpaceTextSoft,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resources.forEach { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PanelSolid),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NeonCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = res.resourceType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "업로드: ${res.uploaderName} (${res.date})",
                                        fontSize = 10.5.sp,
                                        color = SpaceTextSoft
                                    )
                                }

                                IconButton(onClick = { onDeleteResource(res) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = SpaceTextSoft, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = res.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpaceText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = res.linkOrContent,
                                    fontSize = 11.5.sp,
                                    color = SpaceTextSoft,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(res.linkOrContent))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = SpaceText),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("복사하기", fontSize = 10.sp)
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
fun ContributionEvalSubTab(
    group: ProjectGroupEntity,
    tasks: List<ProjectTaskEntity>,
    resources: List<ProjectResourceEntity>,
    evaluations: List<ProjectEvaluationEntity>,
    currentUser: UserEntity?,
    onSubmitEval: (targetMember: String, respScore: Int, qualScore: Int, collabScore: Int, comment: String) -> Unit
) {
    val members = group.membersData.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    var selectedTargetMember by remember { mutableStateOf(members.firstOrNull { it != currentUser?.displayName } ?: members.firstOrNull() ?: "") }
    var respScore by remember { mutableStateOf(5) }
    var qualScore by remember { mutableStateOf(5) }
    var collabScore by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    if (validationErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMsg = null },
            title = { Text("⚠️ 입력 내용 확인", color = NeonMagenta, fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorMsg ?: "", color = SpaceText) },
            confirmButton = {
                Button(
                    onClick = { validationErrorMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("📊 기여도 레포트 & 팀원 상호평가", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)

        // 1. Peer Evaluation Input Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelSolid),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ 동료 상호평가 작성", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonMagenta)

                Text("평가 대상 팀원 선택", fontSize = 11.5.sp, color = SpaceTextSoft)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { m ->
                        val isSel = m == selectedTargetMember
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) NeonMagenta.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonMagenta else Color.Transparent),
                            modifier = Modifier.clickable { selectedTargetMember = m }
                        ) {
                            Text(
                                text = "👤 $m",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) NeonMagenta else SpaceText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Rating 1: Responsibility
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1. 책임감 & 약속 준수", fontSize = 11.5.sp, color = SpaceText)
                        Text("${respScore}점 / 5점", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }
                    RatingStarRow(score = respScore, onSelect = { respScore = it })
                }

                // Rating 2: Quality
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2. 과제 성과물 완성도", fontSize = 11.5.sp, color = SpaceText)
                        Text("${qualScore}점 / 5점", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }
                    RatingStarRow(score = qualScore, onSelect = { qualScore = it })
                }

                // Rating 3: Collaboration
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3. 소통 및 협력 태도", fontSize = 11.5.sp, color = SpaceText)
                        Text("${collabScore}점 / 5점", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }
                    RatingStarRow(score = collabScore, onSelect = { collabScore = it })
                }

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("팀원에 대한 칭찬이나 피드백 한 줄 작성 (선택)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SpaceText,
                        unfocusedTextColor = SpaceText
                    )
                )

                Button(
                    onClick = {
                        if (selectedTargetMember.isEmpty()) {
                            validationErrorMsg = "평가 대상 팀원을 선택해주세요!"
                        } else {
                            onSubmitEval(selectedTargetMember, respScore, qualScore, collabScore, commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("상호평가 제출하기", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 2. Calculated Contribution Objective Dashboard
        Text("🏆 팀원별 객관적 기여도 산출 리포트", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SpaceText)

        // Calculate contribution scores per member
        val memberScores = members.map { member ->
            val memberCompletedTasks = tasks.filter { it.assigneeName == member && it.status == "완료" }
            val memberTaskWeightSum = memberCompletedTasks.sumOf { it.contributionWeight }
            val totalTaskWeightSum = tasks.sumOf { it.contributionWeight }.coerceAtLeast(1)

            val taskScoreComponent = (memberTaskWeightSum.toFloat() / totalTaskWeightSum.toFloat()) * 50f

            val memberResourceCount = resources.count { it.uploaderName == member }
            val totalResourceCount = resources.size.coerceAtLeast(1)
            val resourceScoreComponent = (memberResourceCount.toFloat() / totalResourceCount.toFloat()) * 20f

            val memberEvals = evaluations.filter { it.targetMemberName == member }
            val avgPeerScore = if (memberEvals.isNotEmpty()) {
                memberEvals.map { (it.responsibilityScore + it.qualityScore + it.collaborationScore) / 3.0 }.average().toFloat()
            } else 5.0f

            val peerScoreComponent = (avgPeerScore / 5.0f) * 30f

            val totalRawScore = taskScoreComponent + resourceScoreComponent + peerScoreComponent

            MemberContribData(
                memberName = member,
                completedTasksCount = memberCompletedTasks.size,
                totalAssignedTasksCount = tasks.count { it.assigneeName == member },
                sharedResourceCount = memberResourceCount,
                avgPeerRating = avgPeerScore,
                rawScore = totalRawScore,
                comments = memberEvals.map { it.comment }.filter { it.isNotBlank() }
            )
        }

        val grandTotalScore = memberScores.sumOf { it.rawScore.toDouble() }.coerceAtLeast(1.0).toFloat()

        memberScores.forEach { data ->
            val contribPercent = (data.rawScore / grandTotalScore) * 100f

            // Auto-badge selection
            val badge = when {
                contribPercent >= 35f -> "🏆 MVP 핵심기여왕"
                data.sharedResourceCount >= 2 -> "📊 자료수집 대장"
                data.completedTasksCount >= 2 -> "📝 성실 과제이행자"
                data.avgPeerRating >= 4.8f -> "🤝 최고 협력자"
                else -> "⭐ 열정 팀원"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelSolid),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤 ${data.memberName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpaceText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "기여도 ${String.format(Locale.getDefault(), "%.1f", contribPercent)}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = (contribPercent / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📌 과제 완료: ${data.completedTasksCount}/${data.totalAssignedTasksCount}건", fontSize = 11.sp, color = SpaceTextSoft)
                        Text("📂 자료 공유: ${data.sharedResourceCount}건", fontSize = 11.sp, color = SpaceTextSoft)
                        Text("⭐ 동료 평점: ${String.format(Locale.getDefault(), "%.1f", data.avgPeerRating)} / 5.0", fontSize = 11.sp, color = SpaceTextSoft)
                    }

                    if (data.comments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("💬 팀원 피드백:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NeonMagenta)
                        data.comments.forEach { c ->
                            Text(" • \"$c\"", fontSize = 10.5.sp, color = SpaceTextSoft)
                        }
                    }
                }
            }
        }
    }
}

data class MemberContribData(
    val memberName: String,
    val completedTasksCount: Int,
    val totalAssignedTasksCount: Int,
    val sharedResourceCount: Int,
    val avgPeerRating: Float,
    val rawScore: Float,
    val comments: List<String>
)

@Composable
fun RatingStarRow(score: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { i ->
            Icon(
                imageVector = if (i <= score) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = null,
                tint = if (i <= score) NeonAmber else SpaceTextSoft,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSelect(i) }
            )
        }
    }
}

@Composable
fun NewProjectDialog(
    defaultUser: String,
    registeredUsers: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCreate: (title: String, subject: String, dueDate: String, members: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var title by remember { mutableStateOf("") }
    var selectedSubjectCategory by remember { mutableStateOf("과학") }
    var customSubjectInput by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2026-08-30") }

    val userList = remember(registeredUsers, defaultUser) {
        val base = if (registeredUsers.isNotEmpty()) registeredUsers else listOf("김철수", "이영희", "박민수", "정수진", "강하늘")
        if (defaultUser.isNotEmpty() && !base.contains(defaultUser)) {
            listOf(defaultUser) + base
        } else {
            base
        }
    }

    var selectedMembers by remember {
        mutableStateOf(
            if (defaultUser.isNotEmpty()) {
                listOf(defaultUser, "김철수", "이영희").intersect(userList.toSet()).toList().ifEmpty { listOf(defaultUser) }
            } else {
                userList.take(3)
            }
        )
    }

    var membersInputText by remember {
        mutableStateOf(selectedMembers.joinToString(", "))
    }

    fun toggleUserSelection(userName: String) {
        val current = selectedMembers.toMutableList()
        if (current.contains(userName)) {
            current.remove(userName)
        } else {
            current.add(userName)
        }
        selectedMembers = current
        membersInputText = current.joinToString(", ")
    }

    val subjectList = listOf("과학", "국어", "수학", "영어", "사회", "음악", "미술", "체육", "기술·가정", "정보", "기타")
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    if (validationErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMsg = null },
            title = { Text("⚠️ 입력 내용 확인", color = NeonCyan, fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorMsg ?: "", color = SpaceText) },
            confirmButton = {
                Button(
                    onClick = { validationErrorMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚀 새 모둠 프로젝트 생성", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 📚 과목 선택 카테고리 한 묶음 Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelSolid),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Text("📚 과목 카테고리 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                            Text(
                                text = if (selectedSubjectCategory == "기타") (customSubjectInput.ifBlank { "기타(직접입력)" }) else selectedSubjectCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SpaceTextSoft
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjectList.forEach { s ->
                                val isSel = (s == selectedSubjectCategory)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSel) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else Color.Transparent),
                                    modifier = Modifier.clickable {
                                        selectedSubjectCategory = s
                                        if (s != "기타") {
                                            customSubjectInput = ""
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isSel) "✓ $s" else s,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) NeonCyan else SpaceText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // 기타 선택 시 과목명 직접 입력창 표시
                        AnimatedVisibility(visible = (selectedSubjectCategory == "기타")) {
                            OutlinedTextField(
                                value = customSubjectInput,
                                onValueChange = { customSubjectInput = it },
                                label = { Text("기타 과목명 직접 입력") },
                                placeholder = { Text("예: 한문, 서양사, 프로그래밍 등") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    focusedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("프로젝트 / 조별과제 제목") },
                    placeholder = { Text("예: 생태계 보전 조사 프로젝트") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val parts = dueDate.split("-")
                                if (parts.size == 3) {
                                    calendar.set(Calendar.YEAR, parts[0].toInt())
                                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                    calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                }
                            } catch (e: Exception) {}

                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    dueDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("최종 제출 마감일 (날짜 선택 📅)") },
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "날짜 선택", tint = NeonCyan)
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

                // Selection list for registered members
                Text("👥 회원가입/등록 조원 선택 (클릭 시 추가/제외)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    userList.forEach { userName ->
                        val isSelected = selectedMembers.contains(userName)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else Color.Transparent
                            ),
                            modifier = Modifier.clickable { toggleUserSelection(userName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "✅ $userName" else "👤 $userName",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else SpaceText
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = membersInputText,
                    onValueChange = {
                        membersInputText = it
                        selectedMembers = it.split(",").map { name -> name.trim() }.filter { name -> name.isNotEmpty() }
                    },
                    label = { Text("선택된 조원 목록 (직접 입력/수정 가능)") },
                    placeholder = { Text("예: 김철수, 이영희, 박민수") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSubject = if (selectedSubjectCategory == "기타") customSubjectInput.trim() else selectedSubjectCategory
                    if (title.isBlank()) {
                        validationErrorMsg = "프로젝트 제목을 입력해주세요!"
                    } else if (finalSubject.isBlank()) {
                        validationErrorMsg = "과목을 선택하거나 기타 과목명을 직접 입력해주세요!"
                    } else if (membersInputText.isBlank()) {
                        validationErrorMsg = "조원 이름을 1명 이상 선택하거나 입력해주세요!"
                    } else {
                        onCreate(title, finalSubject, dueDate, membersInputText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("생성하기", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = SpaceTextSoft)
            }
        }
    )
}

@Composable
fun NewTaskDialog(
    groupMembers: List<String>,
    onDismiss: () -> Unit,
    onCreate: (taskName: String, assignee: String, role: String, dueDate: String, weight: Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var taskName by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf(groupMembers.firstOrNull() ?: "") }
    var roleCategory by remember { mutableStateOf("자료조사") }
    var dueDate by remember { mutableStateOf("2026-08-15") }
    var weight by remember { mutableStateOf(3) }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    val roles = listOf("총괄/리더", "자료조사", "PPT제작", "발표", "보고서작성", "기타")

    if (validationErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMsg = null },
            title = { Text("⚠️ 입력 내용 확인", color = NeonPurple, fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorMsg ?: "", color = SpaceText) },
            confirmButton = {
                Button(
                    onClick = { validationErrorMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📌 새 역할 및 과제 등록", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("과제 내용") },
                    placeholder = { Text("예: 2장 환경오염 관련 논문 자료 3건 요약") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("역할 카테고리 선택", fontSize = 11.5.sp, color = SpaceTextSoft)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roles.forEach { r ->
                        val isSel = r == roleCategory
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) NeonPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonPurple else Color.Transparent),
                            modifier = Modifier.clickable { roleCategory = r }
                        ) {
                            Text(r, fontSize = 11.sp, color = if (isSel) NeonPurple else SpaceText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Text("담당 조원 선택", fontSize = 11.5.sp, color = SpaceTextSoft)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupMembers.forEach { m ->
                        val isSel = m == assignee
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else Color.Transparent),
                            modifier = Modifier.clickable { assignee = m }
                        ) {
                            Text("👤 $m", fontSize = 11.sp, color = if (isSel) NeonCyan else SpaceText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val parts = dueDate.split("-")
                                if (parts.size == 3) {
                                    calendar.set(Calendar.YEAR, parts[0].toInt())
                                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                    calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                }
                            } catch (e: Exception) {}

                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    dueDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("과제 마감일 (날짜 선택 📅)") },
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "날짜 선택", tint = NeonPurple)
                        },
                        colors = TextFieldDefaults.colors(
                            disabledContainerColor = Color.Transparent,
                            disabledTextColor = SpaceText,
                            disabledLabelColor = SpaceTextSoft,
                            disabledTrailingIconColor = NeonPurple,
                            disabledIndicatorColor = SpaceTextSoft.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("기여 가중치 (중요도 ⭐1~5)", fontSize = 11.5.sp, color = SpaceTextSoft)
                RatingStarRow(score = weight, onSelect = { weight = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isBlank()) {
                        validationErrorMsg = "과제 내용을 입력해주세요!"
                    } else if (assignee.isBlank()) {
                        validationErrorMsg = "담당 조원을 선택해주세요!"
                    } else {
                        onCreate(taskName, assignee, roleCategory, dueDate, weight)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White)
            ) {
                Text("등록하기", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = SpaceTextSoft)
            }
        }
    )
}

@Composable
fun NewResourceDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, linkContent: String, resType: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var linkContent by remember { mutableStateOf("") }
    var resourceType by remember { mutableStateOf("참고자료/링크") }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    val types = listOf("참고자료/링크", "보고서초안", "발표자료", "회의록")

    if (validationErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMsg = null },
            title = { Text("⚠️ 입력 내용 확인", color = NeonCyan, fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorMsg ?: "", color = SpaceText) },
            confirmButton = {
                Button(
                    onClick = { validationErrorMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📂 공유 자료 등록", color = SpaceText, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("자료 제목") },
                    placeholder = { Text("예: 환경부 2026 생태계 통계 자료") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("자료 구분", fontSize = 11.5.sp, color = SpaceTextSoft)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.forEach { t ->
                        val isSel = t == resourceType
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else Color.Transparent),
                            modifier = Modifier.clickable { resourceType = t }
                        ) {
                            Text(t, fontSize = 11.sp, color = if (isSel) NeonCyan else SpaceText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = linkContent,
                    onValueChange = { linkContent = it },
                    label = { Text("웹 URL 또는 자료 요약 내용") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationErrorMsg = "자료 제목을 입력해주세요!"
                    } else if (linkContent.isBlank()) {
                        validationErrorMsg = "웹 URL 또는 요약 내용을 입력해주세요!"
                    } else {
                        onCreate(title, linkContent, resourceType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("공유하기", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = SpaceTextSoft)
            }
        }
    )
}
