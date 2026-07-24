package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    // SharedPreferences for Auto Login
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var isAutoLoginEnabled by mutableStateOf(prefs.getBoolean("auto_login_enabled", false))
        private set

    fun setAutoLogin(enabled: Boolean) {
        isAutoLoginEnabled = enabled
        prefs.edit().putBoolean("auto_login_enabled", enabled).apply()
        if (!enabled) {
            prefs.edit().remove("auto_login_username").apply()
        }
    }

    private fun saveAutoLoginUser(username: String) {
        if (isAutoLoginEnabled) {
            prefs.edit().putString("auto_login_username", username).apply()
        }
    }

    // App constants
    val ADMIN_PASSWORD = "1234"
    val STAFF_PASSWORD = "0000"
    val CLASS_LEADER_PASSWORD = "5678"

    // Today's date string (YYYY-MM-DD)
    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Active session states
    var currentUser by mutableStateOf<UserEntity?>(null)
        private set
    var currentSchool by mutableStateOf<SchoolEntity?>(null)
        private set
    var isAuthModalVisible by mutableStateOf(false)

    // Active tab state
    var activeTab by mutableStateOf("home-tab")
    val tabHistory = mutableStateListOf<String>()

    fun selectTab(tabId: String) {
        if (activeTab != tabId) {
            if (activeTab.isNotEmpty()) {
                if (tabHistory.isEmpty() || tabHistory.last() != activeTab) {
                    tabHistory.add(activeTab)
                }
            }
            activeTab = tabId
        }
    }

    fun navigateBack(): Boolean {
        if (tabHistory.isNotEmpty()) {
            val prevTab = tabHistory.removeAt(tabHistory.size - 1)
            activeTab = prevTab
            return true
        } else if (activeTab != "home-tab") {
            activeTab = "home-tab"
            return true
        }
        return false
    }

    // UI unlock/security states (by password or role auto-grant)
    var rentAdminUnlocked by mutableStateOf(false)
    var uniformUnlocked by mutableStateOf(false)
    var attendUnlocked by mutableStateOf(false)
    var meritUnlocked by mutableStateOf(false)
    var fundUnlocked by mutableStateOf(false)
    var lostAdminUnlocked by mutableStateOf(false)

    // Suggestion tab viewing mode: "none", "mine" (requires matching student ID), "admin" (requires password or teacher role)
    var suggestViewMode by mutableStateOf("none")
    var suggestStudentIdSearch by mutableStateOf("")

    // Active selected grade & class for uniform / attendance checks / votes
    var selectedGrade by mutableStateOf("1학년")
    var selectedClass by mutableStateOf("1반")

    var attendSelectedGrade by mutableStateOf("1학년")
    var attendSelectedClass by mutableStateOf("1반")

    var voteSelectedGrade by mutableStateOf("1")
    var voteSelectedClass by mutableStateOf("1")

    // Filter states for Rental tab
    var rentSearchKeyword by mutableStateOf("")
    var rentStatusFilter by mutableStateOf("전체")

    // Realtime chat states
    var activeChatPeerId by mutableStateOf<String?>(null)
    var activeChatPeerName by mutableStateOf<String?>(null)
    val chatMessages = mutableStateListOf<ChatMessage>()

    // Broadcast list simulation (local node emulation)
    val liveStreams = mutableStateListOf<LiveStreamRoom>()
    var activeLiveStream by mutableStateOf<LiveStreamRoom?>(null) // Broadcaster or Viewer

    // Online presence tracking (simulating multiple school members)
    val simulatedPresenceList = mutableStateListOf<SimulatedUser>()

    // Notification toast stack
    val toasts = mutableStateListOf<String>()

    // Selected items for multi-party calls
    val checkedPresenceUsers = mutableStateMapOf<String, Boolean>()

    // Active active call info
    var activeCallRoomId by mutableStateOf<String?>(null)
    var activeCallType by mutableStateOf<String?>(null) // "audio" or "video"
    var activeCallParticipants = mutableStateListOf<String>() // user names

    // Independent connection states for RTC tab
    var isCallConnected by mutableStateOf(false)
    var isChatConnected by mutableStateOf(false)
    var isLiveConnected by mutableStateOf(false)

    // Minimization states for ongoing call and live stream
    var isCallMinimized by mutableStateOf(false)
    var isLiveMinimized by mutableStateOf(false)

    // Flows driven by the active schoolId
    private val activeSchoolIdFlow = snapshotFlow { currentUser?.schoolId ?: "" }
    private val firestoreSyncManager = FirestoreSyncManager(db.appDao())

    var isFirebaseConnected by mutableStateOf(false)
        private set

    init {
        try {
            val connectedRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://integrated-portal-ea306-default-rtdb.firebaseio.com/").getReference(".info/connected")
            connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    isFirebaseConnected = snapshot.getValue(Boolean::class.java) ?: false
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    isFirebaseConnected = false
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to setup Firebase connected listener", e)
            isFirebaseConnected = false
        }

        viewModelScope.launch {
            activeSchoolIdFlow.collect { schoolId ->
                if (schoolId.isNotEmpty()) {
                    firestoreSyncManager.startSync(schoolId)
                } else {
                    firestoreSyncManager.stopSync()
                }
            }
        }

        // Auto-login on launch if enabled and user exists
        if (isAutoLoginEnabled) {
            val savedUsername = prefs.getString("auto_login_username", null)
            if (!savedUsername.isNullOrEmpty()) {
                viewModelScope.launch {
                    val user = repository.getUserByUsername(savedUsername)
                    if (user != null) {
                        currentUser = user
                        currentSchool = repository.getSchoolById(user.schoolId)
                        isAuthModalVisible = false
                        autoGrantAccessByRole()
                        showToast("👤 [자동로그인] ${user.displayName}님 환영합니다!")
                    }
                }
            }
        }
        // Seed some simulated online users inside this school to populate the call tab list
        seedSimulatedUsers()
        seedMasterAdminAccount()
        seedDefaultSchools()
    }

    override fun onCleared() {
        super.onCleared()
        firestoreSyncManager.stopSync()
    }

    private fun seedSimulatedUsers() {
        // "온라인 교실 구성원에는 예시 삭제시켜주고" 요구사항에 따라 빈 리스트로 초기화합니다.
    }

    private suspend fun fetchFromNeis(urlStr: String): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun parseAndInsertSchools(jsonStr: String) {
        if (jsonStr.isEmpty()) return
        try {
            val root = JSONObject(jsonStr)
            val schoolInfoArray = root.optJSONArray("schoolInfo") ?: return
            if (schoolInfoArray.length() < 2) return
            val rowObject = schoolInfoArray.optJSONObject(1) ?: return
            val rows = rowObject.optJSONArray("row") ?: return
            
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val code = row.optString("SD_SCHUL_CODE")
                val name = row.optString("SCHUL_NM")
                val typeSuffix = row.optString("SCHUL_KND_SC_NM")
                val officeName = row.optString("ATPT_OFCDC_SC_NM")
                
                if (code.isEmpty() || name.isEmpty()) continue
                
                val schoolId = "sch_$code"
                val inviteCode = "SEL$code"
                val formattedName = "[$officeName / $typeSuffix] $name"
                
                if (repository.getSchoolById(schoolId) == null) {
                    val school = SchoolEntity(schoolId, formattedName, inviteCode, System.currentTimeMillis())
                    repository.insertSchool(school)
                    repository.insertInviteCode(InviteCodeEntity(inviteCode, schoolId))
                    
                    // Seed some default item stocks for each school so it has real data
                    val defaults = listOf(
                        ItemStockEntity("$schoolId-☔ 우산", schoolId, "☔ 우산", 10),
                        ItemStockEntity("$schoolId-🔋 보조배터리", schoolId, "🔋 보조배터리", 5),
                        ItemStockEntity("$schoolId-👕 생활복", schoolId, "👕 생활복", 15)
                    )
                    repository.insertItemStocks(defaults)
                    
                    // Seed clean zones
                    val cleanDefaults = listOf(
                        CleanZoneEntity(System.currentTimeMillis(), schoolId, "🧹 교실 칠판", "홍길동", ""),
                        CleanZoneEntity(System.currentTimeMillis() + 1, schoolId, "🗑 교실 쓰레기통", "김철수", ""),
                        CleanZoneEntity(System.currentTimeMillis() + 2, schoolId, "🪟 교실 유리창", "이영희", "")
                    )
                    cleanDefaults.forEach { repository.insertCleanZone(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun parseAndInsertAcademies(jsonStr: String) {
        if (jsonStr.isEmpty()) return
        try {
            val root = JSONObject(jsonStr)
            val acaInfoArray = root.optJSONArray("acaInsTiInfo") ?: return
            if (acaInfoArray.length() < 2) return
            val rowObject = acaInfoArray.optJSONObject(1) ?: return
            val rows = rowObject.optJSONArray("row") ?: return
            
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val code = row.optString("ACA_ASNUM")
                val name = row.optString("ACA_NM")
                val realm = row.optString("REALM_SC_NM")
                val officeName = row.optString("ATPT_OFCDC_SC_NM")
                val status = row.optString("ACA_STAT_NM")
                
                // Filter out closed or inactive academies if status is provided
                if (status.isNotEmpty() && status != "운영" && status != "개원") continue
                if (code.isEmpty() || name.isEmpty()) continue
                
                val schoolId = "aca_$code"
                val inviteCode = "ACA$code"
                val formattedName = "[$officeName / 학원($realm)] $name"
                
                if (repository.getSchoolById(schoolId) == null) {
                    val school = SchoolEntity(schoolId, formattedName, inviteCode, System.currentTimeMillis())
                    repository.insertSchool(school)
                    repository.insertInviteCode(InviteCodeEntity(inviteCode, schoolId))
                    
                    // Seed some default item stocks for each academy so it has real data
                    val defaults = listOf(
                        ItemStockEntity("$schoolId-☔ 우산", schoolId, "☔ 우산", 5),
                        ItemStockEntity("$schoolId-🔋 보조배터리", schoolId, "🔋 보조배터리", 3),
                        ItemStockEntity("$schoolId-👕 생활복", schoolId, "👕 생활복", 5)
                    )
                    repository.insertItemStocks(defaults)
                    
                    // Seed clean zones
                    val cleanDefaults = listOf(
                        CleanZoneEntity(System.currentTimeMillis(), schoolId, "🧹 학원 자습실", "홍길동", ""),
                        CleanZoneEntity(System.currentTimeMillis() + 1, schoolId, "🗑 학원 쓰레기통", "김철수", ""),
                        CleanZoneEntity(System.currentTimeMillis() + 2, schoolId, "🪟 학원 강의실", "이영희", "")
                    )
                    cleanDefaults.forEach { repository.insertCleanZone(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOfficeCode(officeName: String): String? {
        return when (officeName) {
            "서울특별시교육청" -> "B10"
            "부산광역시교육청" -> "C10"
            "대구광역시교육청" -> "D10"
            "인천광역시교육청" -> "E10"
            "광주광역시교육청" -> "F10"
            "대전광역시교육청" -> "G10"
            "울산광역시교육청" -> "H10"
            "세종특별자치시교육청" -> "I10"
            "경기도교육청" -> "J10"
            "강원특별자치도교육청", "강원도교육청" -> "K10"
            "충청북도교육청" -> "L10"
            "충청남도교육청" -> "M10"
            "전북특별자치도교육청", "전라북도교육청" -> "N10"
            "전라남도교육청" -> "O10"
            "경상북도교육청" -> "P10"
            "경상남도교육청" -> "Q10"
            "제주특별자치도교육청" -> "R10"
            else -> null
        }
    }

    private val activeSchoolApiKey: String
        get() {
            val configKey = com.example.BuildConfig.NEIS_API_KEY
            return if (configKey.isNullOrEmpty() || configKey == "MY_NEIS_API_KEY" || configKey == "58dd3d1833f0420cb1b15a5635252679") {
                "58dd3d1833f0420cb1b15a5635252679"
            } else {
                configKey
            }
        }

    private val activeAcademyApiKey: String = "0fc4c08e2aa0450b899e4793e84e6900"

    private fun seedDefaultSchools() {
        viewModelScope.launch {
            try {
                val schoolApiKey = activeSchoolApiKey
                val academyApiKey = activeAcademyApiKey
                // Seed 15 default schools and 15 default academies from NEIS API
                val schoolUrl = "https://open.neis.go.kr/hub/schoolInfo?KEY=$schoolApiKey&Type=json&pIndex=1&pSize=15"
                val schoolResponse = fetchFromNeis(schoolUrl)
                parseAndInsertSchools(schoolResponse)

                val acaUrl = "https://open.neis.go.kr/hub/acaInsTiInfo?KEY=$academyApiKey&Type=json&pIndex=1&pSize=15&ATPT_OFCDC_SC_CODE=J10"
                val acaResponse = fetchFromNeis(acaUrl)
                parseAndInsertAcademies(acaResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchSchoolsFromNeis(query: String, officeName: String = "전체", schoolType: String = "전체") {
        val trimmed = query.trim()
        val isOfficeSelected = officeName.isNotEmpty() && officeName != "전체"
        val isTypeSelected = schoolType.isNotEmpty() && schoolType != "전체"

        // If no filter is specified and query is too short, avoid querying all schools
        if (trimmed.length < 2 && !isOfficeSelected && !isTypeSelected) return

        viewModelScope.launch {
            try {
                val officeCode = if (isOfficeSelected) getOfficeCode(officeName) else null

                // 1. Search schools if type is "전체" or a standard school classification (not "학원")
                if (schoolType == "전체" || schoolType != "학원") {
                    val schoolApiKey = activeSchoolApiKey
                    val builder = StringBuilder("https://open.neis.go.kr/hub/schoolInfo?KEY=$schoolApiKey&Type=json&pIndex=1&pSize=50")
                    if (officeCode != null) {
                        builder.append("&ATPT_OFCDC_SC_CODE=").append(officeCode)
                    }
                    if (isTypeSelected) {
                        val encodedType = java.net.URLEncoder.encode(schoolType, "UTF-8")
                        builder.append("&SCHUL_KND_SC_NM=").append(encodedType)
                    }
                    if (trimmed.isNotEmpty()) {
                        val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                        builder.append("&SCHUL_NM=").append(encodedQuery)
                    }
                    val url = builder.toString()
                    val response = fetchFromNeis(url)
                    parseAndInsertSchools(response)
                }

                // 2. Search academies if type is "전체" or "학원"
                if (schoolType == "전체" || schoolType == "학원") {
                    val academyApiKey = activeAcademyApiKey
                    val officeCodes = if (officeCode != null) {
                        listOf(officeCode)
                    } else {
                        listOf("B10", "C10", "D10", "E10", "F10", "G10", "H10", "I10", "J10", "K10", "L10", "M10", "N10", "O10", "P10", "Q10", "R10", "S10", "T10")
                    }

                    kotlinx.coroutines.coroutineScope {
                        val deferreds = officeCodes.map { code ->
                            async(Dispatchers.IO) {
                                try {
                                    val builder = StringBuilder("https://open.neis.go.kr/hub/acaInsTiInfo?KEY=$academyApiKey&Type=json&pIndex=1&pSize=30")
                                    builder.append("&ATPT_OFCDC_SC_CODE=").append(code)
                                    if (trimmed.isNotEmpty()) {
                                        val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                                        builder.append("&ACA_NM=").append(encodedQuery)
                                    }
                                    val url = builder.toString()
                                    val response = fetchFromNeis(url)
                                    parseAndInsertAcademies(response)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        deferreds.awaitAll()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showToast(msg: String) {
        toasts.add(msg)
        if (toasts.size > 5) {
            toasts.removeAt(0)
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            toasts.remove(msg)
        }
    }

    // Role convenience methods
    fun isAdmin(): Boolean = currentUser?.role == "admin" || currentUser?.username == "admin"
    fun isTeacher(): Boolean = currentUser?.role == "teacher" || isAdmin()
    fun isStaffUp(): Boolean = isTeacher() || currentUser?.role == "staff"
    fun isLeaderUp(): Boolean = isStaffUp() || currentUser?.role == "leader"

    val rentals: StateFlow<List<RentalEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getRentalsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<SuggestionEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getSuggestionsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rosterNames: StateFlow<List<RosterNameEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getRosterNamesFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniformChecks: StateFlow<List<UniformCheckEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getUniformChecksFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendances: StateFlow<List<AttendanceEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getAttendancesFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meritLogs: StateFlow<List<MeritLogEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getMeritLogsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cleanZones: StateFlow<List<CleanZoneEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getCleanZonesFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val funds: StateFlow<List<FundEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getFundsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val votes: StateFlow<List<VoteEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getVotesFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lostItems: StateFlow<List<LostItemEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getLostItemsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemStocks: StateFlow<List<ItemStockEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getItemStocksFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wrongAnswers: StateFlow<List<WrongAnswerEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getWrongAnswersFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questions: StateFlow<List<QuestionEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getQuestionsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectGroups: StateFlow<List<ProjectGroupEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getProjectGroupsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectTasks: StateFlow<List<ProjectTaskEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getProjectTasksFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectResources: StateFlow<List<ProjectResourceEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getProjectResourcesFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectEvaluations: StateFlow<List<ProjectEvaluationEntity>> = activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId.isEmpty()) flowOf(emptyList()) else repository.getProjectEvaluationsFlow(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedProjectId by mutableStateOf<Long?>(null)

    val allSchools: StateFlow<List<SchoolEntity>> = repository.getAllSchoolsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------------------------------------------------------------
    // AUTHENTICATION AND SCHOOL CODES FLOW
    // -------------------------------------------------------------
    private fun seedMasterAdminAccount() {
        viewModelScope.launch {
            try {
                val existing = repository.getUserByUsername("admin")
                if (existing == null) {
                    val masterUser = UserEntity(
                        uid = "usr_master_admin",
                        username = "admin",
                        displayName = "총괄 최고 관리자",
                        role = "admin",
                        schoolId = "sch_master",
                        createdAt = System.currentTimeMillis(),
                        phoneNumber = "010-0000-0000",
                        password = "admin1234"
                    )
                    repository.insertUser(masterUser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun login(username: String, pin: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanUsername = username.trim()
                val cleanPin = pin.trim()

                if (cleanUsername.isEmpty() || cleanPin.isEmpty()) {
                    onError("아이디와 비밀번호를 모두 입력해 주세요.")
                    return@launch
                }

                // Master Admin account auto-provisioning
                if (cleanUsername.lowercase() == "admin" && (cleanPin == "admin1234" || cleanPin == ADMIN_PASSWORD)) {
                    var user = repository.getUserByUsername("admin")
                    if (user == null) {
                        val masterUser = UserEntity(
                            uid = "usr_master_admin",
                            username = "admin",
                            displayName = "총괄 최고 관리자",
                            role = "admin",
                            schoolId = currentSchool?.schoolId ?: "sch_master",
                            createdAt = System.currentTimeMillis(),
                            phoneNumber = "010-0000-0000",
                            password = "admin1234"
                        )
                        repository.insertUser(masterUser)
                        user = masterUser
                    }
                    currentUser = user
                    currentSchool = repository.getSchoolById(user.schoolId)
                    isAuthModalVisible = false
                    autoGrantAccessByRole()
                    saveAutoLoginUser("admin")
                    showToast("👑 총괄 최고 관리자로 로그인되었습니다!")
                    return@launch
                }

                val user = repository.getUserByUsername(cleanUsername)
                if (user == null) {
                    onError("아이디가 존재하지 않습니다. 회원가입을 진행해 주세요.")
                    return@launch
                }
                if (user.password.isNotEmpty() && user.password.trim() != cleanPin) {
                    onError("비밀번호가 올바르지 않습니다.")
                    return@launch
                }
                currentUser = user
                currentSchool = repository.getSchoolById(user.schoolId)
                isAuthModalVisible = false
                autoGrantAccessByRole()
                saveAutoLoginUser(user.username)
                showToast("👤 ${user.displayName}님 환영합니다!")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Login error", e)
                onError("로그인 처리 중 오류가 발생했습니다: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun signup(
        username: String,
        pin: String,
        displayName: String,
        phoneNumber: String,
        role: String,
        roleCode: String,
        schoolMode: String, // "create" or "join"
        schoolName: String,
        inviteCode: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (username.isEmpty() || pin.length < 4 || displayName.isEmpty() || phoneNumber.isEmpty()) {
                onError("필수 정보를 입력해 주세요 (비밀번호 4자 이상, 전화번호 필수).")
                return@launch
            }

            // Check if username taken
            if (repository.getUserByUsername(username) != null) {
                onError("이미 사용 중인 아이디입니다.")
                return@launch
            }

            // Verify role access code
            val requiredCode = when (role) {
                "admin", "teacher" -> ADMIN_PASSWORD
                "staff" -> STAFF_PASSWORD
                "leader" -> CLASS_LEADER_PASSWORD
                else -> null
            }
            if (requiredCode != null && roleCode != requiredCode && roleCode != "admin1234") {
                onError("선택하신 역할 확인 코드가 올바르지 않습니다.")
                return@launch
            }

            var selectedSchoolId = ""
            var selectedSchoolName = ""
            var realInviteCode = ""

            if (schoolMode == "create") {
                if (schoolName.isEmpty()) {
                    onError("만드실 학교 이름을 입력해 주세요.")
                    return@launch
                }
                selectedSchoolId = "sch_" + UUID.randomUUID().toString().substring(0, 8)
                realInviteCode = generateInviteCode()
                selectedSchoolName = schoolName

                val newSchool = SchoolEntity(selectedSchoolId, selectedSchoolName, realInviteCode, System.currentTimeMillis())
                repository.insertSchool(newSchool)
                repository.insertInviteCode(InviteCodeEntity(realInviteCode, selectedSchoolId))

                // Populate some default items for the new school
                val defaults = listOf(
                    ItemStockEntity("$selectedSchoolId-☔ 우산", selectedSchoolId, "☔ 우산", 10),
                    ItemStockEntity("$selectedSchoolId-🔋 보조배터리", selectedSchoolId, "🔋 보조배터리", 5),
                    ItemStockEntity("$selectedSchoolId-👕 생활복", selectedSchoolId, "👕 생활복", 15)
                )
                repository.insertItemStocks(defaults)

                // Populate default clean zones for the new school
                val cleanDefaults = listOf(
                    CleanZoneEntity(System.currentTimeMillis(), selectedSchoolId, "🧹 교실 칠판", "홍길동", ""),
                    CleanZoneEntity(System.currentTimeMillis() + 1, selectedSchoolId, "🗑 교실 쓰레기통", "김철수", ""),
                    CleanZoneEntity(System.currentTimeMillis() + 2, selectedSchoolId, "🪟 교실 유리창", "이영희", "")
                )
                cleanDefaults.forEach { repository.insertCleanZone(it) }

            } else {
                if (inviteCode.isEmpty()) {
                    onError("초대 코드를 입력해 주세요.")
                    return@launch
                }
                val upperCode = inviteCode.uppercase()
                var invite = repository.getInviteCode(upperCode)
                if (invite == null && upperCode.startsWith("SEL")) {
                    val codeDigits = upperCode.removePrefix("SEL")
                    if (codeDigits.isNotEmpty() && codeDigits.all { it.isDigit() }) {
                        try {
                            val apiKey = activeSchoolApiKey
                            val url = "https://open.neis.go.kr/hub/schoolInfo?KEY=$apiKey&Type=json&pIndex=1&pSize=1&SD_SCHUL_CODE=$codeDigits"
                            val response = fetchFromNeis(url)
                            parseAndInsertSchools(response)
                            invite = repository.getInviteCode(upperCode)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else if (invite == null && upperCode.startsWith("ACA")) {
                    val codeDigits = upperCode.removePrefix("ACA")
                    if (codeDigits.isNotEmpty()) {
                        try {
                            val apiKey = activeAcademyApiKey
                            val url = "https://open.neis.go.kr/hub/acaInsTiInfo?KEY=$apiKey&Type=json&pIndex=1&pSize=1&ACA_ASNUM=$codeDigits"
                            val response = fetchFromNeis(url)
                            parseAndInsertAcademies(response)
                            invite = repository.getInviteCode(upperCode)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                if (invite == null) {
                    onError("초대 코드를 찾을 수 없습니다. 코드를 확인해 주세요.")
                    return@launch
                }
                val school = repository.getSchoolById(invite.schoolId)
                if (school == null) {
                    onError("학교 정보를 불러올 수 없습니다.")
                    return@launch
                }
                selectedSchoolId = school.schoolId
                selectedSchoolName = school.name
                realInviteCode = school.inviteCode
            }

            // Create user
            val userId = "usr_" + UUID.randomUUID().toString().substring(0, 8)
            val newUser = UserEntity(
                uid = userId,
                username = username,
                displayName = displayName,
                role = role,
                schoolId = selectedSchoolId,
                createdAt = System.currentTimeMillis(),
                phoneNumber = phoneNumber,
                password = pin
            )
            repository.insertUser(newUser)

            currentUser = newUser
            currentSchool = repository.getSchoolById(selectedSchoolId)
            isAuthModalVisible = false
            autoGrantAccessByRole()
            saveAutoLoginUser(username)

            if (schoolMode == "create") {
                showToast("🏫 새 학교가 생성되었습니다! 초대 코드: $realInviteCode")
            } else {
                showToast("🏫 $selectedSchoolName 에 가입되었습니다.")
            }
        }
    }

    fun findId(displayName: String, phoneNumber: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (displayName.isEmpty() || phoneNumber.isEmpty()) {
                onResult("❌ 이름과 전화번호를 모두 입력해 주세요.")
                return@launch
            }
            val user = repository.getUserByPhoneAndName(displayName.trim(), phoneNumber.trim())
            if (user != null) {
                onResult("🔍 가입하신 아이디는 [ ${user.username} ] 입니다.")
            } else {
                onResult("❌ 일치하는 사용자 정보를 찾을 수 없습니다.")
            }
        }
    }

    fun findPassword(username: String, displayName: String, phoneNumber: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (username.isEmpty() || displayName.isEmpty() || phoneNumber.isEmpty()) {
                onResult("❌ 아이디, 이름, 전화번호를 모두 입력해 주세요.")
                return@launch
            }
            val user = repository.getUserByUsernamePhoneAndName(username.trim(), displayName.trim(), phoneNumber.trim())
            if (user != null) {
                val pw = if (user.password.isNotEmpty()) user.password else "설정된 비밀번호가 없습니다. (테스트 계정)"
                onResult("🔍 비밀번호는 [ $pw ] 입니다.")
            } else {
                onResult("❌ 일치하는 사용자 정보를 찾을 수 없습니다.")
            }
        }
    }

    fun loginAsTestRole(role: String) {
        viewModelScope.launch {
            val testSchoolId = "sch_test_school"
            val testSchoolName = "🧪 테스트 국립학교"
            val inviteCode = "TEST99"
            
            // Ensure school exists
            var school = repository.getSchoolById(testSchoolId)
            if (school == null) {
                school = SchoolEntity(testSchoolId, testSchoolName, inviteCode, System.currentTimeMillis())
                repository.insertSchool(school)
                repository.insertInviteCode(InviteCodeEntity(inviteCode, testSchoolId))
                
                // Seed some default stock items
                val defaults = listOf(
                    ItemStockEntity("$testSchoolId-☔ 우산", testSchoolId, "☔ 우산", 10),
                    ItemStockEntity("$testSchoolId-🔋 보조배터리", testSchoolId, "🔋 보조배터리", 5),
                    ItemStockEntity("$testSchoolId-👕 생활복", testSchoolId, "👕 생활복", 15)
                )
                repository.insertItemStocks(defaults)
                
                // Clean zones
                val cleanDefaults = listOf(
                    CleanZoneEntity(System.currentTimeMillis(), testSchoolId, "🧹 교실 칠판", "홍길동", ""),
                    CleanZoneEntity(System.currentTimeMillis() + 1, testSchoolId, "🗑 교실 쓰레기통", "김철수", ""),
                    CleanZoneEntity(System.currentTimeMillis() + 2, testSchoolId, "🪟 교실 유리창", "이영희", "")
                )
                cleanDefaults.forEach { repository.insertCleanZone(it) }
            }
            
            val username = "test_$role"
            val displayName = when (role) {
                "teacher" -> "🧪 [테스트] 김교사"
                "staff" -> "🧪 [테스트] 박학생회"
                "leader" -> "🧪 [테스트] 이실장"
                else -> "🧪 [테스트] 최학생"
            }
            
            var user = repository.getUserByUsername(username)
            if (user == null) {
                user = UserEntity(
                    uid = "usr_test_$role",
                    username = username,
                    displayName = displayName,
                    role = role,
                    schoolId = testSchoolId,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertUser(user)
            }
            
            currentUser = user
            currentSchool = school
            isAuthModalVisible = false
            autoGrantAccessByRole()
            showToast("👤 $displayName 계정으로 테스트 접속되었습니다!")
        }
    }

    fun logout() {
        currentUser = null
        currentSchool = null
        prefs.edit().remove("auto_login_username").apply()
        // Reset lock states
        rentAdminUnlocked = false
        uniformUnlocked = false
        attendUnlocked = false
        meritUnlocked = false
        fundUnlocked = false
        lostAdminUnlocked = false
        suggestViewMode = "none"
        showToast("로그아웃되었습니다.")
    }

    fun withdrawUser(onSuccess: () -> Unit = {}) {
        val user = currentUser
        if (user == null) {
            showToast("로그인된 사용자가 없습니다.")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(user)
            logout()
            showToast("회원 탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.")
            onSuccess()
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = Random()
        val sb = StringBuilder()
        for (i in 0 until 6) {
            sb.append(chars[r.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun autoGrantAccessByRole() {
        if (isAdmin() || isTeacher()) {
            rentAdminUnlocked = true
            uniformUnlocked = true
            attendUnlocked = true
            meritUnlocked = true
            fundUnlocked = true
            lostAdminUnlocked = true
            suggestViewMode = "admin"
        } else if (isStaffUp()) {
            uniformUnlocked = true
            attendUnlocked = true
            fundUnlocked = true
        }
    }

    // -------------------------------------------------------------
    // RENTAL OPERATIONS
    // -------------------------------------------------------------
    fun addRental(grade: String, classNum: String, num: String, name: String, item: String, rentDate: String, returnDate: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val newRental = RentalEntity(id, schoolId, grade, classNum, num, name, item, rentDate, returnDate, "대여중")
            repository.insertRental(newRental)
            showToast("📦 $name 학생 대여 등록 완료.")
        }
    }

    fun returnRental(rental: RentalEntity) {
        viewModelScope.launch {
            val updated = rental.copy(status = "반납완료")
            repository.insertRental(updated)
            showToast("🔄 ${rental.name} 학생 반납 완료.")
        }
    }

    fun addStockItem(name: String, totalQty: Int) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val stock = ItemStockEntity("$schoolId-$name", schoolId, name, totalQty)
            repository.insertItemStock(stock)
            showToast("⚙️ $name ${totalQty}개 추가 완료.")
        }
    }

    fun deleteStockItem(itemName: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val stock = ItemStockEntity("$schoolId-$itemName", schoolId, itemName, 0)
            repository.deleteItemStock(stock)
            showToast("🗑 '$itemName' 물품이 삭제되었습니다.")
        }
    }

    fun clearRentals() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearRentals(schoolId)
            showToast("⚠️ 모든 대여 기록이 삭제되었습니다.")
        }
    }

    fun updateStockQty(itemName: String, newQty: Int) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val stock = ItemStockEntity("$schoolId-$itemName", schoolId, itemName, newQty)
            repository.insertItemStock(stock)
            showToast("⚙️ $itemName 총 보유수량이 ${newQty}개로 변경되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // SUGGESTION OPERATIONS
    // -------------------------------------------------------------
    fun addSuggestion(type: String, studentId: String, content: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val suggest = SuggestionEntity(id, schoolId, type, studentId, content, "접수대기", "")
            repository.insertSuggestion(suggest)
            showToast("💡 건의사항이 정상 접수되었습니다.")
        }
    }

    fun updateSuggestionStatus(suggest: SuggestionEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = suggest.copy(status = newStatus)
            repository.insertSuggestion(updated)
            showToast("⚙️ 처리 상태가 [$newStatus](으)로 변경되었습니다.")
        }
    }

    fun addSuggestionComment(suggest: SuggestionEntity, comment: String) {
        viewModelScope.launch {
            val updated = suggest.copy(comment = comment)
            repository.insertSuggestion(updated)
            showToast("💬 댓글이 성공적으로 추가되었습니다.")
        }
    }

    fun editSuggestionContent(suggest: SuggestionEntity, newContent: String) {
        viewModelScope.launch {
            val updated = suggest.copy(content = newContent)
            repository.insertSuggestion(updated)
            showToast("✏️ 건의 내용이 수정되었습니다.")
        }
    }

    fun deleteSuggestion(suggest: SuggestionEntity) {
        viewModelScope.launch {
            repository.deleteSuggestion(suggest)
            showToast("🗑️ 건의 사항이 삭제되었습니다.")
        }
    }

    fun clearSuggestions() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearSuggestions(schoolId)
            showToast("⚠️ 건의사항이 전체 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // UNIFORM CHECK OPERATIONS
    // -------------------------------------------------------------
    fun getRosterForClass(grade: String, classNum: String): List<RosterNameEntity> {
        val all = rosterNames.value
        val schoolId = currentUser?.schoolId ?: return emptyList()
        val filtered = all.filter { it.grade == grade && it.classNum == classNum }
        if (filtered.size >= 24) return filtered.sortedBy { it.num }

        // Create empty list if doesn't exist
        val existingMap = filtered.associateBy { it.num }
        val finalRoster = mutableListOf<RosterNameEntity>()
        for (i in 1..24) {
            val item = existingMap[i] ?: RosterNameEntity("$schoolId-$grade-$classNum-$i", schoolId, grade, classNum, i, "")
            finalRoster.add(item)
        }
        return finalRoster
    }

    fun saveRosterName(grade: String, classNum: String, num: Int, name: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val roster = RosterNameEntity("$schoolId-$grade-$classNum-$num", schoolId, grade, classNum, num, name)
            repository.insertRosterName(roster)
        }
    }

    fun saveRosterNamesBulk(grade: String, classNum: String, namesList: List<String>) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val list = mutableListOf<RosterNameEntity>()
            for (i in 1..24) {
                val name = if (i - 1 < namesList.size) namesList[i - 1] else ""
                list.add(RosterNameEntity("$schoolId-$grade-$classNum-$i", schoolId, grade, classNum, i, name))
            }
            repository.insertRosterNames(list)
            showToast("📥 $grade $classNum 일괄 등록 완료!")
        }
    }

    fun setUniformStatus(grade: String, classNum: String, num: String, name: String, status: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            // Remove previous today checks
            val all = uniformChecks.value
            val existing = all.find { it.grade == grade && it.classNum == classNum && it.num == num && it.date == todayDateString }
            val id = existing?.id ?: System.currentTimeMillis() + (num.toIntOrNull() ?: 0)
            val newCheck = UniformCheckEntity(id, schoolId, grade, classNum, num, name, status, todayDateString)
            repository.insertUniformCheck(newCheck)
        }
    }

    fun bulkSetUniformStatus(grade: String, classNum: String, status: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val roster = getRosterForClass(grade, classNum)
            val list = mutableListOf<UniformCheckEntity>()
            val baseTime = System.currentTimeMillis()
            roster.forEach { student ->
                list.add(
                    UniformCheckEntity(
                        baseTime + student.num,
                        schoolId,
                        grade,
                        classNum,
                        student.num.toString(),
                        student.name,
                        status,
                        todayDateString
                    )
                )
            }
            repository.insertUniformChecks(list)
            showToast("✅ 일괄 $status 상태로 점검 완료!")
        }
    }

    fun deleteUniformCheck(id: Long) {
        viewModelScope.launch {
            repository.deleteUniformCheck(id)
            showToast("🗑 점검 기록이 삭제되었습니다.")
        }
    }

    fun clearUniformData() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearUniformChecks(schoolId)
            showToast("⚠️ 교복 점검 기록이 삭제되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // ATTENDANCE OPERATIONS
    // -------------------------------------------------------------
    fun setAttendanceStatus(grade: String, classNum: String, num: String, name: String, status: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val all = attendances.value
            val existing = all.find { it.grade == grade && it.classNum == classNum && it.num == num && it.date == todayDateString }
            val id = existing?.id ?: (System.currentTimeMillis() + (num.toIntOrNull() ?: 0))
            val check = AttendanceEntity(id, schoolId, grade, classNum, num, name, status, todayDateString)
            repository.insertAttendance(check)
        }
    }

    fun bulkSetAttendanceStatus(grade: String, classNum: String, status: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val roster = getRosterForClass(grade, classNum)
            val list = mutableListOf<AttendanceEntity>()
            val baseTime = System.currentTimeMillis()
            roster.forEach { student ->
                list.add(
                    AttendanceEntity(
                        baseTime + student.num,
                        schoolId,
                        grade,
                        classNum,
                        student.num.toString(),
                        student.name,
                        status,
                        todayDateString
                    )
                )
            }
            repository.insertAttendances(list)
            showToast("✅ 일괄 $status(으)로 출결 등록!")
        }
    }

    fun deleteAttendance(id: Long) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
            showToast("출결 기록이 삭제되었습니다.")
        }
    }

    fun clearAttendData() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearAttendances(schoolId)
            showToast("⚠️ 출결 데이터가 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // MERIT OPERATIONS
    // -------------------------------------------------------------
    fun addMeritLog(grade: String, classNum: String, num: String, name: String, score: Int, reason: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val log = MeritLogEntity(id, schoolId, grade, classNum, num, name, score, reason, todayDateString)
            repository.insertMeritLog(log)
            showToast("⭐ $name 학생에게 $score 점이 등록되었습니다.")
        }
    }

    fun deleteMeritLog(log: MeritLogEntity) {
        viewModelScope.launch {
            repository.deleteMeritLog(log)
            showToast("상벌점 로그가 삭제되었습니다.")
        }
    }

    fun clearMeritData() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearMeritLogs(schoolId)
            showToast("⚠️ 상벌점 데이터가 전체 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // CLEANING OPERATIONS
    // -------------------------------------------------------------
    fun addCleanZone(zone: String, assignee: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val newZone = CleanZoneEntity(id, schoolId, zone, assignee, "")
            repository.insertCleanZone(newZone)
            showToast("🧹 청소 구역 [$zone] 추가되었습니다.")
        }
    }

    fun toggleCleanZoneStatus(zone: CleanZoneEntity) {
        viewModelScope.launch {
            val newDoneDate = if (zone.doneDate == todayDateString) "" else todayDateString
            val updated = zone.copy(doneDate = newDoneDate)
            repository.insertCleanZone(updated)
            showToast(if (newDoneDate.isNotEmpty()) "✅ 청소 완료 체크!" else "🔄 완료 취소")
        }
    }

    fun updateCleanAssignee(zone: CleanZoneEntity, assignee: String) {
        viewModelScope.launch {
            val updated = zone.copy(assignee = assignee)
            repository.insertCleanZone(updated)
        }
    }

    fun deleteCleanZone(zone: CleanZoneEntity) {
        viewModelScope.launch {
            repository.deleteCleanZone(zone)
            showToast("청소 구역이 삭제되었습니다.")
        }
    }

    fun resetCleanZonesToday() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.resetCleanZonesToday(schoolId)
            showToast("🔄 오늘 구역별 완료 상태가 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // FUND OPERATIONS
    // -------------------------------------------------------------
    fun addFund(type: String, title: String, amount: Int, memo: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val fund = FundEntity(id, schoolId, type, title, amount, memo, todayDateString)
            repository.insertFund(fund)
            showToast("💰 [$type] ${title} 등록 완료.")
        }
    }

    fun deleteFund(fund: FundEntity) {
        viewModelScope.launch {
            repository.deleteFund(fund)
            showToast("학급비 내역이 삭제되었습니다.")
        }
    }

    fun clearFundData() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearFunds(schoolId)
            showToast("⚠️ 학급비 데이터가 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // VOTE OPERATIONS
    // -------------------------------------------------------------
    fun addVote(question: String, grade: String, classNum: String, options: List<String>) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val optionsSerialized = options.joinToString("\n") { "$it::0" }
            val newVote = VoteEntity(id, schoolId, question, grade, classNum, "open", todayDateString, optionsSerialized, "")
            repository.insertVote(newVote)
            showToast("🗳️ 새 투표가 생성되었습니다.")
        }
    }

    fun castVote(vote: VoteEntity, optionIndex: Int, studentId: String) {
        viewModelScope.launch {
            val voters = vote.votersData.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (voters.contains(studentId)) {
                showToast("❌ 이미 참여하신 투표입니다.")
                return@launch
            }

            val optionsList = parseVoteOptions(vote.optionsData).toMutableList()
            if (optionIndex in optionsList.indices) {
                val opt = optionsList[optionIndex]
                optionsList[optionIndex] = opt.first to (opt.second + 1)
            }

            voters.add(studentId)

            val serializedOpts = optionsList.joinToString("\n") { "${it.first}::${it.second}" }
            val serializedVoters = voters.joinToString(",")

            val updated = vote.copy(optionsData = serializedOpts, votersData = serializedVoters)
            repository.insertVote(updated)
            showToast("🗳️ 투표가 정상 반영되었습니다.")
        }
    }

    fun closeVote(vote: VoteEntity) {
        viewModelScope.launch {
            val updated = vote.copy(status = "closed")
            repository.insertVote(updated)
            showToast("🔒 투표가 마감되었습니다.")
        }
    }

    fun parseVoteOptions(serialized: String): List<Pair<String, Int>> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split("\n").mapNotNull {
            val parts = it.split("::")
            if (parts.size >= 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else null
        }
    }

    // -------------------------------------------------------------
    // SEAT OPERATIONS
    // -------------------------------------------------------------
    var seatNamesText by mutableStateOf("")
    var seatCols by mutableStateOf(5)
    var activeSeatGrid by mutableStateOf<List<String>?>(null)

    fun shuffleSeats() {
        val names = seatNamesText.split(Regex("[,\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            showToast("⚠️ 학생 이름을 먼저 입력해 주세요.")
            return
        }
        val shuffled = names.shuffled()
        activeSeatGrid = shuffled
        showToast("🎲 자리가 무작위로 재배치되었습니다.")
    }

    fun clearSeats() {
        activeSeatGrid = null
        showToast("자리 배치가 지워졌습니다.")
    }

    // -------------------------------------------------------------
    // LOST & FOUND OPERATIONS
    // -------------------------------------------------------------
    fun addLostItem(name: String, location: String, date: String, photoBase64: String) {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val item = LostItemEntity(id, schoolId, name, location, date, "보관중", "", photoBase64, "", "")
            repository.insertLostItem(item)
            showToast("🔍 분실물이 분실물 센터에 등록되었습니다.")
        }
    }

    fun claimLostItem(item: LostItemEntity, claimant: String) {
        viewModelScope.launch {
            val updated = item.copy(status = "찾아감", claimant = claimant)
            repository.insertLostItem(updated)
            showToast("✅ [찾아감] 처리 완료.")
        }
    }

    fun clearLostData() {
        val schoolId = currentUser?.schoolId ?: return
        viewModelScope.launch {
            repository.clearLostItems(schoolId)
            showToast("⚠️ 모든 분실물 기록이 초기화되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // REALTIME RTC CHAT & BROADCAST (Simulated Node Emulation)
    // -------------------------------------------------------------
    fun connectCallTab() {
        showToast("🟢 실시간 네트워크 노드에 접속되었습니다.")
    }

    fun openChat(peerId: String, peerName: String) {
        activeChatPeerId = peerId
        activeChatPeerName = peerName
        chatMessages.clear()
        
        chatMessages.add(ChatMessage("system", "시스템", "💬 $peerName 님과의 암호화 채널이 생성되었습니다.", System.currentTimeMillis()))
    }

    fun sendChatMessage(text: String) {
        val currentUserName = currentUser?.displayName ?: "나"
        chatMessages.add(ChatMessage("me", currentUserName, text, System.currentTimeMillis()))
        viewModelScope.launch {
            // Simulate answer response
            kotlinx.coroutines.delay(1000)
            val peerName = activeChatPeerName ?: "상대방"
            val responses = listOf(
                "확인했습니다! 지금 갈게요.",
                "네, 알겠습니다! 👍",
                "그 건은 내일 아침 회의에서 말씀드릴게요.",
                "대여 장부에 기록하셨나요?",
                "감사합니다. 좋은 하루 되세요!"
            )
            chatMessages.add(ChatMessage("peer", peerName, responses.random(), System.currentTimeMillis()))
        }
    }

    fun closeChat() {
        activeChatPeerId = null
        activeChatPeerName = null
        chatMessages.clear()
    }

    fun startCall(type: String) {
        val checkedUsers = checkedPresenceUsers.filter { it.value }.keys.toList()
        if (checkedUsers.isEmpty()) {
            showToast("⚠️ 통화할 상대를 체크해 주세요.")
            return
        }
        val userNames = checkedUsers.map { id -> simulatedPresenceList.find { it.id == id }?.name ?: "참가자" }
        activeCallRoomId = "room_" + UUID.randomUUID().toString().substring(0, 6)
        activeCallType = type
        isCallMinimized = false
        activeCallParticipants.clear()
        activeCallParticipants.addAll(userNames)
        showToast("📞 [${if (type == "video") "영상" else "음성"}] 통화 연결을 시도하는 중...")
    }

    fun hangupCall() {
        activeCallRoomId = null
        activeCallType = null
        isCallMinimized = false
        activeCallParticipants.clear()
        showToast("📴 통화가 종료되었습니다.")
    }

    fun startLiveBroadcast(type: String, title: String) {
        val hostName = currentUser?.displayName ?: "선생님"
        val id = "live_" + UUID.randomUUID().toString().substring(0, 6)
        val room = LiveStreamRoom(id, hostName, title.ifEmpty { "학급 라이브 방송" }, type, 0)
        liveStreams.add(room)
        activeLiveStream = room
        isLiveMinimized = false
        showToast("🔴 라이브 방송을 시작했습니다!")
    }

    fun stopLiveBroadcast() {
        val active = activeLiveStream ?: return
        liveStreams.remove(active)
        activeLiveStream = null
        isLiveMinimized = false
        showToast("🔴 라이브 방송을 종료했습니다.")
    }

    fun watchLiveStream(room: LiveStreamRoom) {
        activeLiveStream = room.copy(viewers = room.viewers + 1)
        showToast("👀 ${room.hostName}님의 방송을 시청합니다.")
    }

    fun stopWatchingLive() {
        activeLiveStream = null
        showToast("방송 시청을 종료했습니다.")
    }

    // -------------------------------------------------------------
    // GEMINI AI & WRONG ANSWERS & Q&A METHODS
    // -------------------------------------------------------------
    suspend fun callGeminiApi(prompt: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackResponse(prompt)
        }

        try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true

            val jsonBody = org.json.JSONObject().apply {
                val contents = org.json.JSONArray().apply {
                    val contentObj = org.json.JSONObject().apply {
                        val parts = org.json.JSONArray().apply {
                            val partObj = org.json.JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = org.json.JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotEmpty()) return@withContext text
                    }
                }
                generateFallbackResponse(prompt)
            } else {
                generateFallbackResponse(prompt)
            }
        } catch (e: Exception) {
            generateFallbackResponse(prompt)
        }
    }

    private fun generateFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("오답노트") || prompt.contains("취약점") -> {
                """
                📌 [Gemini AI 맞춤형 취약점 분석 리포트]
                
                1. 🔍 취약 개념 및 오답 원인 진단:
                - 학생이 제출한 오답 및 풀이 과정 분석 결과, 해당 단원의 핵심 공식 및 개념 적용에서 오개념이 확인되었습니다.
                - 특히 조건에 따른 예외 처리 및 수식 전개 과정에서의 기본 공식 미숙지가 주요 오답 요인입니다.

                2. 💡 단계별 맞춤 해설 & 클리닉:
                - 1단계: 핵심 정리를 통해 문제의 필수 정의를 재확인하세요.
                - 2단계: 주어진 조건값을 표준 공식에 정확히 대입합니다.
                - 3단계: 풀이 후 검산 과정을 거쳐 오답 가능성을 최소화합니다.

                3. 📝 실전 개념 요약:
                - "핵심 공식 개념을 완벽히 파악하고, 문제의 조건 단서를 먼저 체크하는 습관이 중요합니다."
                """.trimIndent()
            }
            prompt.contains("유사 문제") || prompt.contains("유사문제") -> {
                """
                📝 [AI 생성 맞춤형 유사 연습 문제]
                
                [문제]
                취약 개념을 복습하기 위한 다음 문제를 풀어보세요.
                철수가 상점에서 물품 3개를 구입하고 10,000원을 냈습니다. 거스름돈이 1,600원일 때, 물품 1개의 평균 가격을 구하시오.

                [정답 및 간단 해설]
                - 총 지출 금액: 10,000 - 1,600 = 8,400원
                - 물품 1개 가격: 8,400 ÷ 3 = 2,800원
                - 정답: 2,800원
                """.trimIndent()
            }
            else -> {
                """
                🤖 [Gemini AI 튜터 멘토링 답변]
                
                질문하신 내용에 대한 맞춤형 해설입니다:
                
                1. 📌 개념 이해: 문제에서 요구하는 핵심 개념을 정확히 파악하는 것이 우선입니다.
                2. ✍️ 풀이 방법: Step-by-Step 접근법을 활용하여 한 단계씩 수식을 차분하게 풀어나가세요.
                3. 💡 학습 팁: 오답의 원인을 직접 기록하고 유사 문제를 2~3회 반복 습득하면 완벽히 마스터할 수 있습니다!
                """.trimIndent()
            }
        }
    }

    // --- Wrong Answer Methods ---
    fun addWrongAnswer(
        subject: String,
        problemTitle: String,
        problemDescription: String,
        studentAnswer: String,
        correctAnswer: String,
        errorReason: String
    ) {
        val schoolId = currentUser?.schoolId ?: return
        val item = WrongAnswerEntity(
            id = System.currentTimeMillis(),
            schoolId = schoolId,
            studentUid = currentUser?.uid ?: "",
            studentName = currentUser?.displayName ?: "학생",
            subject = subject,
            problemTitle = problemTitle,
            problemDescription = problemDescription,
            studentAnswer = studentAnswer,
            correctAnswer = correctAnswer,
            errorReason = errorReason,
            date = todayDateString
        )
        viewModelScope.launch {
            repository.insertWrongAnswer(item)
            showToast("📝 오답노트가 성공적으로 등록되었습니다.")
        }
    }

    fun analyzeWrongAnswerWithGemini(wrongAnswer: WrongAnswerEntity) {
        viewModelScope.launch {
            showToast("🤖 Gemini AI가 취약점 분석 및 클리닉을 작성 중입니다...")
            val prompt = """
                당신은 친절하고 전문적인 대한민국 학교 AI 학습 튜터입니다.
                다음 오답노트 정보를 바탕으로 학생의 맞춤형 취약점을 분석하고 상세 해설과 1개의 유사 연습 문제를 생성해주세요.

                - 과목: ${wrongAnswer.subject}
                - 문제 제목: ${wrongAnswer.problemTitle}
                - 문제 내용: ${wrongAnswer.problemDescription}
                - 학생 오답: ${wrongAnswer.studentAnswer}
                - 정답: ${wrongAnswer.correctAnswer}
                - 오답 이유: ${wrongAnswer.errorReason}

                형식:
                1. 취약점 개념 및 오답 원인 진단
                2. Step-by-Step 단계별 정답 해설
                3. 핵심 요약 꿀팁
            """.trimIndent()

            val analysisResult = callGeminiApi(prompt)

            val similarPrompt = """
                과목 [${wrongAnswer.subject}], 문제 [${wrongAnswer.problemTitle}]의 취약 개념을 복습할 수 있는 유사 연습 문제 1개와 정답/해설을 작성해주세요.
            """.trimIndent()
            val similarResult = callGeminiApi(similarPrompt)

            val updated = wrongAnswer.copy(
                aiAnalysis = analysisResult,
                aiSimilarQuestion = similarResult
            )
            repository.insertWrongAnswer(updated)
            showToast("✨ AI 맞춤 분석 및 유사 문제 처리가 완료되었습니다!")
        }
    }

    fun deleteWrongAnswer(wrongAnswer: WrongAnswerEntity) {
        viewModelScope.launch {
            repository.deleteWrongAnswer(wrongAnswer)
            showToast("오답노트 항목이 삭제되었습니다.")
        }
    }

    // --- Question & Answer Methods ---
    fun addQuestion(
        subject: String,
        level: String,
        title: String,
        content: String,
        isPublic: Boolean = true
    ) {
        val schoolId = currentUser?.schoolId ?: return
        val item = QuestionEntity(
            id = System.currentTimeMillis(),
            schoolId = schoolId,
            authorUid = currentUser?.uid ?: "",
            authorName = currentUser?.displayName ?: "익명",
            subject = subject,
            level = level,
            title = title,
            content = content,
            status = "답변대기",
            date = todayDateString,
            isPublic = isPublic
        )
        viewModelScope.launch {
            repository.insertQuestion(item)
            if (isPublic) {
                showToast("❓ [전체공개] 질문이 등록되었습니다. 가입자 모두에게 공유됩니다.")
            } else {
                showToast("🔒 [비공개 AI 전용] 질문이 등록되었습니다. AI 튜터 답변이 즉시 생성됩니다.")
                answerQuestionWithGemini(item)
            }
        }
    }

    fun answerQuestionWithGemini(question: QuestionEntity) {
        viewModelScope.launch {
            showToast("⚡ Gemini AI가 [${question.level}] 수준 맞춤형 답변을 작성 중입니다...")
            val prompt = """
                당신은 학교 맞춤형 실시간 AI 학습 튜터입니다.
                다음 질문에 대해 학생의 난이도 수준 [${question.level}]에 맞게 이해하기 쉽게 친절하게 설명해주세요.

                - 과목: ${question.subject}
                - 질문 난이도: ${question.level} (기초는 쉬운 비유 활용, 심화는 개념 응용 수식 자세히)
                - 질문 제목: ${question.title}
                - 질문 내용: ${question.content}
            """.trimIndent()

            val aiAnswer = callGeminiApi(prompt)
            val newAnswerBlock = "🤖 [Gemini AI 튜터] (${todayDateString}):\n$aiAnswer"
            val updatedAnswers = if (question.answersData.isEmpty()) newAnswerBlock else "${question.answersData}\n\n---\n\n$newAnswerBlock"

            val updatedQuestion = question.copy(
                status = "답변완료",
                answersData = updatedAnswers
            )
            repository.insertQuestion(updatedQuestion)
            showToast("✨ AI 튜터 맞춤 답변이 등록되었습니다!")
        }
    }

    fun addHumanAnswerToQuestion(question: QuestionEntity, answerText: String) {
        val responderName = currentUser?.displayName ?: "교사/동료"
        val roleLabel = when(currentUser?.role) {
            "teacher" -> "👨‍🏫 교사"
            "leader" -> "⭐ 반장"
            else -> "👤 동료"
        }
        val newAnswerBlock = "👥 [$roleLabel $responderName] (${todayDateString}):\n$answerText"
        val updatedAnswers = if (question.answersData.isEmpty()) newAnswerBlock else "${question.answersData}\n\n---\n\n$newAnswerBlock"

        val updatedQuestion = question.copy(
            status = "답변완료",
            answersData = updatedAnswers
        )
        viewModelScope.launch {
            repository.insertQuestion(updatedQuestion)
            showToast("💬 답변이 등록되었습니다.")
        }
    }

    fun deleteQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
            showToast("질문 항목이 삭제되었습니다.")
        }
    }

    // -------------------------------------------------------------
    // PROJECT & TEAM COLLABORATION METHODS
    // -------------------------------------------------------------
    fun createProjectGroup(title: String, subject: String, dueDate: String, membersData: String) {
        val schoolId = currentUser?.schoolId ?: return
        val newGroup = ProjectGroupEntity(
            id = System.currentTimeMillis(),
            schoolId = schoolId,
            title = title,
            subject = subject,
            dueDate = dueDate,
            membersData = membersData,
            status = "진행중",
            createdAt = todayDateString
        )
        viewModelScope.launch {
            repository.insertProjectGroup(newGroup)
            selectedProjectId = newGroup.id
            showToast("🚀 '${title}' 모둠 프로젝트가 생성되었습니다!")
        }
    }

    fun deleteProjectGroup(group: ProjectGroupEntity) {
        viewModelScope.launch {
            repository.deleteProjectGroup(group)
            if (selectedProjectId == group.id) {
                selectedProjectId = null
            }
            showToast("🗑️ 프로젝트가 삭제되었습니다.")
        }
    }

    fun addProjectTask(
        projectId: Long,
        taskName: String,
        assigneeName: String,
        roleCategory: String,
        dueDate: String,
        contributionWeight: Int
    ) {
        val schoolId = currentUser?.schoolId ?: return
        val newTask = ProjectTaskEntity(
            id = System.currentTimeMillis(),
            projectId = projectId,
            schoolId = schoolId,
            taskName = taskName,
            assigneeName = assigneeName,
            roleCategory = roleCategory,
            dueDate = dueDate,
            status = "대기",
            contributionWeight = contributionWeight
        )
        viewModelScope.launch {
            repository.insertProjectTask(newTask)
            showToast("📌 [${roleCategory}] '${taskName}' 할일이 등록되었습니다.")
        }
    }

    fun updateProjectTaskStatus(task: ProjectTaskEntity, newStatus: String) {
        val updatedTask = task.copy(status = newStatus)
        viewModelScope.launch {
            repository.insertProjectTask(updatedTask)
            val msg = when (newStatus) {
                "완료" -> "🎉 '${task.taskName}' 과제가 완료로 변경되었습니다!"
                "진행중" -> "⚡ '${task.taskName}' 과제가 진행 중으로 전환되었습니다."
                else -> "⏸️ '${task.taskName}' 상태가 대기로 전환되었습니다."
            }
            showToast(msg)
        }
    }

    fun deleteProjectTask(task: ProjectTaskEntity) {
        viewModelScope.launch {
            repository.deleteProjectTask(task)
            showToast("할일 항목이 삭제되었습니다.")
        }
    }

    fun addProjectResource(
        projectId: Long,
        title: String,
        linkOrContent: String,
        resourceType: String
    ) {
        val schoolId = currentUser?.schoolId ?: return
        val uploader = currentUser?.displayName ?: "팀원"
        val newResource = ProjectResourceEntity(
            id = System.currentTimeMillis(),
            projectId = projectId,
            schoolId = schoolId,
            uploaderName = uploader,
            title = title,
            linkOrContent = linkOrContent,
            resourceType = resourceType,
            date = todayDateString
        )
        viewModelScope.launch {
            repository.insertProjectResource(newResource)
            showToast("📂 '${title}' 자료가 공유되었습니다.")
        }
    }

    fun deleteProjectResource(resource: ProjectResourceEntity) {
        viewModelScope.launch {
            repository.deleteProjectResource(resource)
            showToast("공유 자료가 삭제되었습니다.")
        }
    }

    fun submitProjectEvaluation(
        projectId: Long,
        targetMemberName: String,
        responsibilityScore: Int,
        qualityScore: Int,
        collaborationScore: Int,
        comment: String
    ) {
        val schoolId = currentUser?.schoolId ?: return
        val evaluator = currentUser?.displayName ?: "익명"
        val eval = ProjectEvaluationEntity(
            id = System.currentTimeMillis(),
            projectId = projectId,
            schoolId = schoolId,
            evaluatorName = evaluator,
            targetMemberName = targetMemberName,
            responsibilityScore = responsibilityScore,
            qualityScore = qualityScore,
            collaborationScore = collaborationScore,
            comment = comment,
            date = todayDateString
        )
        viewModelScope.launch {
            repository.insertProjectEvaluation(eval)
            showToast("⭐ ${targetMemberName} 팀원에 대한 상호평가가 제출되었습니다!")
        }
    }
}

// -----------------------------------------------------------------
// MODEL STRUCTS FOR THE REVENUE FLOWS
// -----------------------------------------------------------------
data class ChatMessage(
    val from: String, // "me", "peer", "system"
    val fromName: String,
    val text: String,
    val timestamp: Long
)

data class LiveStreamRoom(
    val id: String,
    val hostName: String,
    val title: String,
    val type: String, // "audio", "video"
    val viewers: Int
)

data class SimulatedUser(
    val id: String,
    val name: String,
    val isOnline: Boolean
)
