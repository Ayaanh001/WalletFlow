package com.hussain.walletflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrunchDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
//import androidx.compose.material.icons.filled.Camping
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.EscalatorWarning
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.RunCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.walletflow.utils.AVAILABLE_COLORS
import com.hussain.walletflow.utils.AVAILABLE_ICONS
import com.hussain.walletflow.utils.colorToHex
import kotlin.math.*

// ─── Categorised icon map ─────────────────────────────────────────────────────

// All icons use only base Icons.Filled — no extended material icons dependency needed.
// Each key maps directly to a guaranteed-available icon vector.
private val ICON_CATEGORIES: LinkedHashMap<String, List<String>> = linkedMapOf(
    "Finance" to listOf(
        "AccountBalance", "AccountBalanceWallet", "Savings", "CreditCard", "Payment",
        "Payments", "AttachMoney", "MonetizationOn", "TrendingUp", "TrendingDown",
        "Receipt", "LocalAtm", "PaidIcon", "SwapHoriz", "CompareArrows"
    ),
    "Food & Drink" to listOf(
        "Restaurant", "LocalCafe", "LocalBar", "Fastfood", "LocalPizza",
        "LocalGroceryStore", "EmojiFoodBeverage2", "DinnerDining2", "SetMeal2", "KitchenIcon",
        "OutdoorGrill", "Blender", "WineBar", "BrunchDining", "LocalDrink"
    ),
    "Shopping & Home" to listOf(
        "ShoppingBag", "ShoppingCart", "Home", "House", "Apartment",
        "Chair", "Weekend2", "Checkroom2", "LocalMall2", "Storefront2",
        "Inventory2", "MoveToInbox", "Layers", "Dashboard", "GridView"
    ),
    "Transport" to listOf(
        "DirectionsCar", "DirectionsBus", "Train", "Flight", "TwoWheeler",
        "LocalTaxi", "Subway2", "Tram2", "LocalShipping2", "DirectionsBoat2",
        "Commute2", "NearMe", "Navigation", "AirplanemodeActive", "CarRental"
    ),
    "Health & Fitness" to listOf(
        "LocalHospital", "MedicalServices", "FitnessCenter", "Healing2", "Medication2",
        "Vaccines2", "HealthAndSafety2", "MonitorHeart2", "Spa2", "SelfImprovement2",
        "Psychology2", "Accessible2", "Elderly2", "VolunteerActivism2", "RunCircle"
    ),
    "Education" to listOf(
        "School", "MenuBook", "AutoStories2", "LibraryBooks2", "LocalLibrary2",
        "Science2", "Calculate2", "Straighten2", "HistoryEdu2", "Draw2",
        "Architecture2", "Lightbulb2", "Quiz2", "EmojiObjects2", "Biotech2"
    ),
    "Entertainment" to listOf(
        "Movie", "MusicNote", "SportsEsports", "SportsSoccer", "Celebration",
        "CardGiftcard", "Theaters2", "LiveTv2", "Headphones2", "Games2",
        "SportsBasketball2", "Casino2", "Attractions2", "Videocam", "CameraAlt"
    ),
    "Work & Business" to listOf(
        "Business", "Work", "Laptop", "Build", "Engineering2",
        "Construction2", "Group2", "Handshake2", "WorkHistory2", "ManageAccounts2",
        "AdminPanelSettings2", "AssignmentTurnedIn2", "Task2", "Analytics2", "BarChart2"
    ),
    "Utilities & Bills" to listOf(
        "Bolt", "WaterDrop", "Wifi", "PhoneAndroid", "Cable2",
        "Router2", "DeviceThermostat2", "AcUnit2", "PowerSettingsNew2", "Plumbing2",
        "Handyman2", "ElectricalServices2", "HomeRepairService2", "Settings", "Tune"
    ),
    "Travel & Places" to listOf(
        "BeachAccess2", "Landscape2", "Terrain2", "Park2", "Museum2",
        "Hotel2", "Camping2", "Luggage2", "Map2", "Explore2",
        "Public2", "LocationOn2", "TravelExplore2", "Language", "MyLocation"
    ),
    "Family & Social" to listOf(
        "Pets", "ChildCare", "FamilyRestroom2", "EscalatorWarning2", "Elderly3",
        "Groups2", "People2", "Favorite2", "ContactPhone2", "Chat2",
        "Forum", "Share", "ConnectWithoutContact", "HowToReg", "PersonAdd"
    ),
    "Misc" to listOf(
        "Category", "MoreHoriz", "Replay", "QrCode", "Star2",
        "Flag2", "Label2", "Bookmark2", "PushPin2", "Note2",
        "EmojiEvents2", "Whatshot2", "NewReleases2", "Verified2", "AutoAwesome2"
    )
)

// Single source of truth: key → ImageVector, only using base Icons.Filled.
// Keys ending in a digit are aliases to avoid name collisions with AVAILABLE_ICONS keys.
val EXTENDED_ICONS: Map<String, androidx.compose.ui.graphics.vector.ImageVector> by lazy {
    linkedMapOf<String, androidx.compose.ui.graphics.vector.ImageVector>().apply {
        putAll(AVAILABLE_ICONS)
        // Finance
        put("LocalAtm",              Icons.Filled.LocalAtm)
        put("PaidIcon",              Icons.Filled.Paid)
        put("SwapHoriz",             Icons.Filled.SwapHoriz)
        put("CompareArrows",         Icons.Filled.CompareArrows)
        // Food
        put("EmojiFoodBeverage2",    Icons.Filled.LocalDining)
        put("DinnerDining2",         Icons.Filled.DinnerDining)
        put("SetMeal2",              Icons.Filled.SetMeal)
        put("KitchenIcon",           Icons.Filled.Kitchen)
        put("OutdoorGrill",          Icons.Filled.OutdoorGrill)
        put("Blender",               Icons.Filled.Blender)
        put("WineBar",               Icons.Filled.WineBar)
        put("BrunchDining",          Icons.Filled.BrunchDining)
        put("LocalDrink",            Icons.Filled.LocalDrink)
        // Shopping & Home
        put("Weekend2",              Icons.Filled.Weekend)
        put("Checkroom2",            Icons.Filled.Checkroom)
        put("LocalMall2",            Icons.Filled.LocalMall)
        put("Storefront2",           Icons.Filled.Storefront)
        put("Inventory2",            Icons.Filled.Inventory)
        put("MoveToInbox",           Icons.Filled.MoveToInbox)
        put("Layers",                Icons.Filled.Layers)
        put("Dashboard",             Icons.Filled.Dashboard)
        put("GridView",              Icons.Filled.GridView)
        // Transport
        put("Subway2",               Icons.Filled.Subway)
        put("Tram2",                 Icons.Filled.Tram)
        put("LocalShipping2",        Icons.Filled.LocalShipping)
        put("DirectionsBoat2",       Icons.Filled.DirectionsBoat)
        put("Commute2",              Icons.Filled.Commute)
        put("NearMe",                Icons.Filled.NearMe)
        put("Navigation",            Icons.Filled.Navigation)
        put("AirplanemodeActive",    Icons.Filled.AirplanemodeActive)
        put("CarRental",             Icons.Filled.CarRental)
        // Health
        put("Healing2",              Icons.Filled.Healing)
        put("Medication2",           Icons.Filled.Medication)
        put("Vaccines2",             Icons.Filled.Vaccines)
        put("HealthAndSafety2",      Icons.Filled.HealthAndSafety)
        put("MonitorHeart2",         Icons.Filled.MonitorHeart)
        put("Spa2",                  Icons.Filled.Spa)
        put("SelfImprovement2",      Icons.Filled.SelfImprovement)
        put("Psychology2",           Icons.Filled.Psychology)
        put("Accessible2",           Icons.Filled.Accessible)
        put("Elderly2",              Icons.Filled.Elderly)
        put("VolunteerActivism2",    Icons.Filled.VolunteerActivism)
        put("RunCircle",             Icons.Filled.RunCircle)
        // Education
        put("AutoStories2",          Icons.Filled.AutoStories)
        put("LibraryBooks2",         Icons.Filled.LibraryBooks)
        put("LocalLibrary2",         Icons.Filled.LocalLibrary)
        put("Science2",              Icons.Filled.Science)
        put("Calculate2",            Icons.Filled.Calculate)
        put("Straighten2",           Icons.Filled.Straighten)
        put("HistoryEdu2",           Icons.Filled.HistoryEdu)
        put("Draw2",                 Icons.Filled.Draw)
        put("Architecture2",         Icons.Filled.Architecture)
        put("Lightbulb2",            Icons.Filled.Lightbulb)
        put("Quiz2",                 Icons.Filled.Quiz)
        put("EmojiObjects2",         Icons.Filled.EmojiObjects)
        put("Biotech2",              Icons.Filled.Biotech)
        // Entertainment
        put("Theaters2",             Icons.Filled.Theaters)
        put("LiveTv2",               Icons.Filled.LiveTv)
        put("Headphones2",           Icons.Filled.Headphones)
        put("Games2",                Icons.Filled.Games)
        put("SportsBasketball2",     Icons.Filled.SportsBasketball)
        put("Casino2",               Icons.Filled.Casino)
//        put("Attractions2",          Icons.Filled.Attractions)
        put("Videocam",              Icons.Filled.Videocam)
        put("CameraAlt",             Icons.Filled.CameraAlt)
        // Work
        put("Engineering2",          Icons.Filled.Engineering)
        put("Construction2",         Icons.Filled.Construction)
        put("Group2",                Icons.Filled.Group)
        put("Handshake2",            Icons.Filled.Handshake)
        put("WorkHistory2",          Icons.Filled.WorkHistory)
        put("ManageAccounts2",       Icons.Filled.ManageAccounts)
        put("AdminPanelSettings2",   Icons.Filled.AdminPanelSettings)
        put("AssignmentTurnedIn2",   Icons.Filled.AssignmentTurnedIn)
        put("Task2",                 Icons.Filled.Task)
        put("Analytics2",            Icons.Filled.Analytics)
        put("BarChart2",             Icons.Filled.BarChart)
        // Utilities
        put("Cable2",                Icons.Filled.Cable)
        put("Router2",               Icons.Filled.Router)
        put("DeviceThermostat2",     Icons.Filled.DeviceThermostat)
        put("AcUnit2",               Icons.Filled.AcUnit)
        put("PowerSettingsNew2",     Icons.Filled.PowerSettingsNew)
        put("Plumbing2",             Icons.Filled.Plumbing)
        put("Handyman2",             Icons.Filled.Handyman)
        put("ElectricalServices2",   Icons.Filled.ElectricalServices)
        put("HomeRepairService2",    Icons.Filled.HomeRepairService)
        put("Settings",              Icons.Filled.Settings)
        put("Tune",                  Icons.Filled.Tune)
        // Travel
        put("BeachAccess2",          Icons.Filled.BeachAccess)
        put("Landscape2",            Icons.Filled.Landscape)
        put("Terrain2",              Icons.Filled.Terrain)
        put("Park2",                 Icons.Filled.Park)
        put("Museum2",               Icons.Filled.Museum)
        put("Hotel2",                Icons.Filled.Hotel)
//        put("Camping2",              Icons.Filled.Camping)
        put("Luggage2",              Icons.Filled.Luggage)
        put("Map2",                  Icons.Filled.Map)
        put("Explore2",              Icons.Filled.Explore)
        put("Public2",               Icons.Filled.Public)
        put("LocationOn2",           Icons.Filled.LocationOn)
        put("TravelExplore2",        Icons.Filled.TravelExplore)
        put("Language",              Icons.Filled.Language)
        put("MyLocation",            Icons.Filled.MyLocation)
        // Family & Social
        put("FamilyRestroom2",       Icons.Filled.FamilyRestroom)
        put("EscalatorWarning2",     Icons.Filled.EscalatorWarning)
        put("Elderly3",              Icons.Filled.Elderly)
        put("Groups2",               Icons.Filled.Groups)
        put("People2",               Icons.Filled.People)
        put("Favorite2",             Icons.Filled.Favorite)
        put("ContactPhone2",         Icons.Filled.ContactPhone)
        put("Chat2",                 Icons.Filled.Chat)
        put("Forum",                 Icons.Filled.Forum)
        put("Share",                 Icons.Filled.Share)
        put("ConnectWithoutContact", Icons.Filled.ConnectWithoutContact)
        put("HowToReg",              Icons.Filled.HowToReg)
        put("PersonAdd",             Icons.Filled.PersonAdd)
        // Misc
        put("Star2",                 Icons.Filled.Star)
        put("Flag2",                 Icons.Filled.Flag)
        put("Label2",                Icons.Filled.Label)
        put("Bookmark2",             Icons.Filled.Bookmark)
        put("PushPin2",              Icons.Filled.PushPin)
        put("Note2",                 Icons.Filled.Note)
        put("EmojiEvents2",          Icons.Filled.EmojiEvents)
        put("Whatshot2",             Icons.Filled.Whatshot)
        put("NewReleases2",          Icons.Filled.NewReleases)
        put("Verified2",             Icons.Filled.Verified)
        put("AutoAwesome2",          Icons.Filled.AutoAwesome)
    }
}

fun resolveIcon(key: String) = EXTENDED_ICONS[key] ?: AVAILABLE_ICONS[key] ?: Icons.Filled.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomItemScreen(
    isCategory: Boolean,
    initialTypeIndex: Int = 0,
    existingNames: List<String> = emptyList(),
    onConfirm: (name: String, iconKey: String, colorHex: String, type: String) -> Unit,
    onBack: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    var name          by remember { mutableStateOf("") }
    var selectedKey   by remember { mutableStateOf(AVAILABLE_ICONS.keys.first()) }
    var selectedColor by remember { mutableStateOf(AVAILABLE_COLORS[0]) }
    var nameError     by remember { mutableStateOf<String?>(null) }
    var typeTabIndex  by remember { mutableIntStateOf(initialTypeIndex) }
    var colorTabIndex by remember { mutableIntStateOf(0) }
    var showIconPicker by remember { mutableStateOf(false) }

    val itemType = when {
        !isCategory       -> "payment"
        typeTabIndex == 0 -> "expense"
        else              -> "income"
    }

    fun validate(): Boolean {
        val t = name.trim()
        return when {
            t.isEmpty() -> { nameError = "Name cannot be empty"; false }
            existingNames.any { it.equals(t, ignoreCase = true) } ->
            { nameError = "\"$t\" already exists"; false }
            else -> { nameError = null; true }
        }
    }

    // Intercept back button when icon picker is visible
    BackHandler(enabled = showIconPicker) {
        showIconPicker = false
    }

    // ── Icon Picker full-screen overlay ──────────────────────────────────────
    if (showIconPicker) {
        IconPickerScreen(
            selectedKey   = selectedKey,
            selectedColor = selectedColor,
            onSelect      = { key -> selectedKey = key; showIconPicker = false },
            onBack        = { showIconPicker = false }
        )
        return
    }

    // ── Main create screen ────────────────────────────────────────────────────
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isCategory) "New Category" else "New Payment Method",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {

                // ── Income / Expense switcher ─────────────────────────────────
                if (isCategory) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Expense" to 0, "Income" to 1).forEach { (label, idx) ->
                                val isSelected = typeTabIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable { typeTabIndex = idx }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Black,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Live preview chip ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(selectedColor.copy(alpha = 0.12f))
                        .border(1.5.dp, selectedColor.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        resolveIcon(selectedKey),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = selectedColor
                    )
                    Text(
                        text = name.ifBlank { "Preview" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = selectedColor
                    )
                }

                // ── Name field with icon button ───────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.padding(top = if (nameError != null) 0.dp else 7.dp)) {
                        Surface(
                            onClick = { keyboard?.hide(); showIconPicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = selectedColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    resolveIcon(selectedKey),
                                    contentDescription = "Choose icon",
                                    modifier = Modifier.size(26.dp),
                                    tint = selectedColor
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = { Text("Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // ── Color section ─────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        listOf("Swatches" to 0, "Wheel" to 1).forEachIndexed { index, (label, idx) ->
                            val isSelected = colorTabIndex == idx

                            val shapes = when (index) {
                                0    -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            }

                            ToggleButton(
                                checked         = isSelected,
                                onCheckedChange = { if (it) colorTabIndex = idx },
                                shapes          = shapes,
                                modifier        = Modifier.weight(1f),
                                colors          = ToggleButtonDefaults.toggleButtonColors(
                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedContentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor        = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor          = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Text(
                                    text       = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 13.sp
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = colorTabIndex,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "color_tab"
                    ) { tab ->
                        if (tab == 0) {
                            val cols = 6
                            val rows = (AVAILABLE_COLORS.size + cols - 1) / cols
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(rows) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        repeat(cols) { col ->
                                            val idx = row * cols + col
                                            if (idx < AVAILABLE_COLORS.size) {
                                                val c       = AVAILABLE_COLORS[idx]
                                                val selected = c == selectedColor
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(c)
                                                        .then(
                                                            if (selected)
                                                                Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                            else Modifier
                                                        )
                                                        .clickable { selectedColor = c },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selected) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = Color.White)
                                                }
                                            } else {
                                                Spacer(Modifier.size(44.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                ColorWheelPicker(
                                    modifier = Modifier.fillMaxWidth(0.75f),
                                    color = selectedColor,
                                    onColorChanged = { selectedColor = it }
                                )
                            }
                        }
                    }
                }

                // ── Create button ─────────────────────────────────────────────
                Button(
                    onClick = {
                        if (validate()) {
                            keyboard?.hide()
                            onConfirm(name.trim(), selectedKey, colorToHex(selectedColor), itemType)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Create",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ─── Icon Picker Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerScreen(
    selectedKey: String,
    selectedColor: Color,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val categoryNames = remember { ICON_CATEGORIES.keys.toList() }

    // Filtered flat list when searching
    val filteredKeys = remember(searchQuery) {
        if (searchQuery.isBlank()) null
        else ICON_CATEGORIES.values.flatten()
            .filter { it.contains(searchQuery, ignoreCase = true) }
            .distinct()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Choose Icon", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search icons…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50.dp)
                )

                if (filteredKeys != null) {
                    // Search results flat grid
                    Text(
                        "${filteredKeys.size} results",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredKeys) { key ->
                            IconGridCell(key, key == selectedKey, selectedColor) { onSelect(it) }
                        }
                    }
                } else {
                    // Categorised LazyColumn
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        categoryNames.forEach { catName ->
                            val keys = ICON_CATEGORIES[catName] ?: return@forEach
                            item(key = "header_$catName") {
                                Text(
                                    catName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            item(key = "grid_$catName") {
                                // Non-lazy grid inside the lazy column
                                val cols = 6
                                val rows = (keys.size + cols - 1) / cols
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    repeat(rows) { row ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            repeat(cols) { col ->
                                                val idx = row * cols + col
                                                if (idx < keys.size) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        IconGridCell(
                                                            keys[idx],
                                                            keys[idx] == selectedKey,
                                                            selectedColor
                                                        ) { onSelect(it) }
                                                    }
                                                } else {
                                                    Spacer(Modifier.weight(1f))
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
    }
}

@Composable
private fun IconGridCell(
    key: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: (String) -> Unit
) {
    val icon = resolveIcon(key)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) selectedColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .then(
                if (isSelected)
                    Modifier.border(1.5.dp, selectedColor, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick(key) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = key,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Color Wheel ─────────────────────────────────────────────────────────────

@Composable
private fun ColorWheelPicker(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    val initHsv    = remember(color) { colorToHsv(color) }
    var hue        by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initHsv[2]) }

    LaunchedEffect(hue, saturation, brightness) { onColorChanged(Color.hsv(hue, saturation, brightness)) }

    val density     = LocalDensity.current
    val ringThickDp = 26.dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val sizePx = constraints.maxWidth.toFloat()
            val cx = sizePx / 2f; val cy = sizePx / 2f
            val ringPx = with(density) { ringThickDp.toPx() }
            val outerR = sizePx / 2f; val innerR = outerR - ringPx
            val sqPx = innerR * sqrt(2f) * 0.85f; val sqDp = with(density) { sqPx.toDp() }

            Box(modifier = Modifier.fillMaxSize().drawWithCache {
                val shader = android.graphics.SweepGradient(cx, cy,
                    intArrayOf(0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
                        0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()),
                    floatArrayOf(0f, 1f/6f, 2f/6f, 3f/6f, 4f/6f, 5f/6f, 1f))
                val paint = android.graphics.Paint().apply { this.shader = shader; style = android.graphics.Paint.Style.STROKE; strokeWidth = ringPx; isAntiAlias = true }
                onDrawBehind { drawContext.canvas.nativeCanvas.drawCircle(cx, cy, outerR - ringPx / 2f, paint) }
            }.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    applyHue(down.position, cx, cy, innerR) { hue = it }
                    do { val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                        if (ch.pressed) { ch.consume(); applyHue(ch.position, cx, cy, innerR) { hue = it } }
                    } while (ev.changes.any { it.pressed })
                }
            })

            val hRad = Math.toRadians(hue.toDouble()); val knobR = outerR - ringPx / 2f; val knobSz = 22.dp
            Box(modifier = Modifier
                .offset(with(density){(cx + knobR * cos(hRad).toFloat()).toDp()} - knobSz/2, with(density){(cy + knobR * sin(hRad).toFloat()).toDp()} - knobSz/2)
                .size(knobSz).clip(CircleShape).background(Color.hsv(hue, 1f, 1f)).border(3.dp, Color.White, CircleShape))

            Box(modifier = Modifier.align(Alignment.Center).size(sqDp).clip(RoundedCornerShape(6.dp))
                .drawWithCache {
                    val satBrush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f)))
                    val valBrush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                    onDrawBehind { drawRect(satBrush); drawRect(valBrush) }
                }
                .pointerInput(hue) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        applySV(down.position, size.width, size.height) { s, v -> saturation = s; brightness = v }
                        do { val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                            if (ch.pressed) { ch.consume(); applySV(ch.position, size.width, size.height) { s, v -> saturation = s; brightness = v } }
                        } while (ev.changes.any { it.pressed })
                    }
                }) {
                val svKnob = 20.dp
                Box(modifier = Modifier.offset(sqDp * saturation - svKnob/2, sqDp * (1f - brightness) - svKnob/2)
                    .size(svKnob).clip(CircleShape).border(3.dp, Color.White, CircleShape))
            }
        }

        val currentColor = Color.hsv(hue, saturation, brightness)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(currentColor).border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape))
            Text("#${colorToHex(currentColor).uppercase()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun applyHue(pos: Offset, cx: Float, cy: Float, innerR: Float, set: (Float) -> Unit) {
    val dx = pos.x - cx; val dy = pos.y - cy
    if (sqrt(dx * dx + dy * dy) >= innerR - 12f) { var a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat(); if (a < 0f) a += 360f; set(a) }
}
private fun applySV(pos: Offset, w: Int, h: Int, set: (Float, Float) -> Unit) { set((pos.x / w).coerceIn(0f, 1f), 1f - (pos.y / h).coerceIn(0f, 1f)) }
private fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()), hsv)
    return hsv
}