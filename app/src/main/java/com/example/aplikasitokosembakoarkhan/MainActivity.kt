package com.example.aplikasitokosembakoarkhan

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.* // Import penting untuk Column, Row, fillMaxWidth, Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aplikasitokosembakoarkhan.ui.theme.AplikasiTokoSembakoArkhanTheme
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper // Import penting untuk Cek PIN
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AplikasiTokoSembakoArkhanTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "dashboard"

    // Helper Dialog PIN
    var showPinDialog by remember { mutableStateOf(false) }
    var targetRoute by remember { mutableStateOf("") }

    // Inventory ViewModel untuk Import Excel
    val inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val context = androidx.compose.ui.platform.LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            inventoryViewModel.importExcel(context, uri) { count ->
                Toast.makeText(context, "Berhasil import $count barang", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- STRUKTUR MENU SIDEBAR ---
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
        drawerContent = {
            ModalDrawerSheet {
                // Header Drawer
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Toko Arkhan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Versi 1.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                HorizontalDivider()

                LazyColumn {
                    items(menuGroups) { group ->
                        // Judul Grup
                        Text(
                            text = group.title,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Item Menu
                        group.items.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                icon = { Icon(item.icon, null) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    scope.launch { drawerState.close() }

                                    // --- LOGIKA KUNCI PIN YANG BENAR ---
                                    val isSensitive = item.route in listOf("report", "expense", "settings")
                                    val isPinSet = SecurityHelper.isPinSet(context)

                                    // Hanya kunci JIKA menu sensitif DAN PIN sudah diatur
                                    if (isSensitive && isPinSet) {
                                        targetRoute = item.route
                                        showPinDialog = true
                                    } else {
                                        // Buka langsung (Default tanpa PIN)
                                        navController.navigate(item.route) {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    // Menu Tambahan (Import Excel)
                    item {
                        NavigationDrawerItem(
                            label = { Text("Import Excel") },
                            icon = { Icon(Icons.Default.UploadFile, null) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"))
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
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
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(navController, startDestination = "dashboard") {
                    composable("dashboard") { DashboardScreen() }
                    composable("sales") { SalesScreen() }
                    composable("products") { ProductScreen() }
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

    // Dialog PIN (Hanya muncul jika dipicu)
    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSuccess = {
                showPinDialog = false
                navController.navigate(targetRoute) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

// Data Class untuk Struktur Menu
data class MenuGroup(val title: String, val items: List<MenuItem>)
data class MenuItem(val title: String, val route: String, val icon: ImageVector)