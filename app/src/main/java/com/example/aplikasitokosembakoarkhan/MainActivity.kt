package com.example.aplikasitokosembakoarkhan

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aplikasitokosembakoarkhan.ui.theme.AplikasiTokoSembakoArkhanTheme
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AplikasiTokoSembakoArkhanTheme {
                // Surface dasar untuk aplikasi
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }

                    // Box Tumpukan: MainApp di bawah, Splash di atas
                    Box(modifier = Modifier.fillMaxSize()) {

                        // 1. DASHBOARD / MAIN APP (Di Bawah)
                        MainApp()

                        // 2. SPLASH SCREEN (Di Atas)
                        AnimatedVisibility(
                            visible = showSplashScreen,
                            exit = fadeOut(animationSpec = tween(700)),
                            modifier = Modifier
                                .fillMaxSize()
                                // --- PERBAIKAN PENTING DI SINI ---
                                // Kita paksa background menjadi solid (sesuai tema/putih)
                                // agar tidak tembus pandang ke Dashboard di belakangnya.
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            SplashScreen {
                                showSplashScreen = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()

    // Gunakan DrawerState standar (Closed)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "dashboard"

    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val context = LocalContext.current

    var isSessionUnlocked by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var targetRoute by remember { mutableStateOf("") }

    val isPinSet = SecurityHelper.isPinSet(context)
    val lockedMenus by settingsViewModel.lockedMenus.collectAsState()

    // --- FIX SIDEBAR (TETAP DIPERTAHANKAN) ---
    // Memaksa sidebar tertutup dengan delay kecil saat aplikasi baru dimuat.
    LaunchedEffect(Unit) {
        delay(50)
        if (drawerState.isOpen) {
            drawerState.snapTo(DrawerValue.Closed)
        }
    }

    // Auto-Close Sidebar saat pindah halaman
    LaunchedEffect(currentRoute) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    // Tombol Back menutup Sidebar
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val menuGroups = listOf(
        MenuGroup("Utama", listOf(
            MenuItem("Dashboard", "dashboard", Icons.Default.Dashboard)
        )),
        MenuGroup("Transaksi", listOf(
            MenuItem("Kasir", "sales", Icons.Default.ShoppingCart),
            MenuItem("Restok Barang", "restock", Icons.Default.AddBox),
            MenuItem("Hutang / Kasbon", "debt", Icons.Default.Book),
            MenuItem("Pengeluaran", "expense", Icons.Default.MoneyOff)
        )),
        MenuGroup("Master Data", listOf(
            MenuItem("Data Barang", "products", Icons.Default.Inventory),
            MenuItem("Data Pelanggan", "customers", Icons.Default.People),
            MenuItem("Kategori", "categories", Icons.Default.Category),
            MenuItem("Satuan", "units", Icons.Default.Straighten)
        )),
        MenuGroup("Laporan", listOf(
            MenuItem("Laporan Keuangan", "report", Icons.Default.Assessment)
        )),
        MenuGroup("Admin", listOf(
            MenuItem("Pengaturan", "settings", Icons.Default.Settings)
        ))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Toko Arkhan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Versi 1.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                HorizontalDivider()

                LazyColumn {
                    items(menuGroups) { group ->
                        Text(
                            text = group.title,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        group.items.forEach { item ->
                            val isMenuLocked = lockedMenus.contains(item.route)

                            NavigationDrawerItem(
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title)
                                        if (isPinSet && isMenuLocked) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(12.dp), tint = Color.Red)
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, null) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    scope.launch { drawerState.close() }

                                    if (isPinSet && isMenuLocked && !isSessionUnlocked) {
                                        targetRoute = item.route
                                        showPinDialog = true
                                    } else {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo("dashboard") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = menuGroups.flatMap { it.items }.find { it.route == currentRoute }?.title ?: "Toko Arkhan"
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    actions = {
                        if (isPinSet) {
                            IconButton(onClick = {
                                if (isSessionUnlocked) {
                                    isSessionUnlocked = false
                                    Toast.makeText(context, "Sesi Terkunci", Toast.LENGTH_SHORT).show()
                                    if (lockedMenus.contains(currentRoute)) {
                                        navController.navigate("dashboard")
                                    }
                                } else {
                                    targetRoute = currentRoute
                                    showPinDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSessionUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Status Keamanan",
                                    tint = if (isSessionUnlocked) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            onNavigateToSales = { navController.navigate("sales") },
                            onNavigateToProduct = { navController.navigate("products") },
                            onNavigateToReport = { navController.navigate("report") }
                        )
                    }
                    composable("sales") { SalesScreen() }
                    composable("products") { ProductScreen() }
                    composable("customers") { CustomerScreen() }
                    composable("categories") { CategoryScreen() }
                    composable("units") { UnitScreen() }
                    composable("restock") { RestockScreen() }
                    composable("report") { ReportScreen() }
                    composable("expense") { ExpenseScreen() }
                    composable("debt") { DebtScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSuccess = {
                showPinDialog = false
                isSessionUnlocked = true
                Toast.makeText(context, "Akses Dibuka", Toast.LENGTH_SHORT).show()
                if (targetRoute.isNotEmpty() && targetRoute != currentRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}

data class MenuGroup(val title: String, val items: List<MenuItem>)
data class MenuItem(val title: String, val route: String, val icon: ImageVector)