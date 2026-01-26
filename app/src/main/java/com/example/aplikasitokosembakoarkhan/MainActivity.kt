package com.example.aplikasitokosembakoarkhan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aplikasitokosembakoarkhan.utils.ExcelHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: ProductViewModel by viewModels { ProductViewModel.Factory }
        setContent { MaterialTheme { MainApp(viewModel) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: ProductViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val productList by viewModel.allProducts.collectAsState()

    var selectedItem by remember { mutableStateOf("Dashboard") }
    var showMenu by remember { mutableStateOf(false) }

    // --- LOGIKA KEAMANAN ADMIN ---
    // isAdmin: False = Mode Kasir (Terbatas), True = Mode Admin (Bebas)
    var isAdminUnlocked by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf("") } // Rute tujuan setelah PIN benar
    var pendingItemName by remember { mutableStateOf("") } // Nama menu tujuan

    // Fungsi Navigasi Pintar
    fun navigateTo(route: String, itemName: String, restricted: Boolean) {
        scope.launch { drawerState.close() }

        if (!restricted || isAdminUnlocked) {
            // Jika menu bebas ATAU admin sudah login -> Langsung Buka
            selectedItem = itemName
            navController.navigate(route) {
                if (route == "dashboard") popUpTo("dashboard") { inclusive = true }
            }
        } else {
            // Jika menu terbatas & belum admin -> Minta PIN
            pendingRoute = route
            pendingItemName = itemName
            showPinDialog = true
        }
    }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importExcel(context, it) { c -> scope.launch { Toast.makeText(context, "Impor $c berhasil!", Toast.LENGTH_SHORT).show() } } } }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Toko Arkhan", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()

                // MENU 1: DASHBOARD (BEBAS AKSES)
                NavigationDrawerItem(
                    label = { Text("Beranda / Dashboard") },
                    selected = selectedItem == "Dashboard",
                    icon = { Icon(Icons.Default.Home, null) },
                    onClick = { navigateTo("dashboard", "Dashboard", restricted = false) }
                )

                // MENU 2: KASIR (BEBAS AKSES - BIAR KARYAWAN BISA JUALAN)
                NavigationDrawerItem(
                    label = { Text("Penjualan (Kasir)") },
                    selected = selectedItem == "Penjualan",
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    onClick = { navigateTo("sales", "Penjualan", restricted = false) }
                )

                // MENU 3: KASBON (DIBATASI PIN)
                NavigationDrawerItem(
                    label = { Text("Buku Kasbon") },
                    selected = selectedItem == "Kasbon",
                    icon = { Icon(Icons.Default.Book, null) },
                    onClick = { navigateTo("debt", "Kasbon", restricted = true) }
                )

                // MENU 4: STOK (DIBATASI PIN)
                NavigationDrawerItem(
                    label = { Text("Stok / Input Barang") },
                    selected = selectedItem == "Stok Barang",
                    icon = { Icon(Icons.Default.Inventory, null) },
                    onClick = { navigateTo("stock", "Stok Barang", restricted = true) }
                )

                // MENU 5: LAPORAN (DIBATASI PIN)
                NavigationDrawerItem(
                    label = { Text("Laporan Keuangan") },
                    selected = selectedItem == "Laporan",
                    icon = { Icon(Icons.Default.DateRange, null) },
                    onClick = { navigateTo("report", "Laporan", restricted = true) }
                )

                // MENU 6: BIAYA OPERASIONAL (DIBATASI PIN)
                NavigationDrawerItem(
                    label = { Text("Biaya Operasional") },
                    selected = selectedItem == "Expenses",
                    icon = { Icon(Icons.Default.TrendingDown, null) },
                    onClick = { navigateTo("expenses", "Expenses", restricted = true) }
                )

                HorizontalDivider()

                // MENU 7: PENGATURAN (DIBATASI PIN)
                NavigationDrawerItem(
                    label = { Text("Pengaturan & Printer") },
                    selected = selectedItem == "Settings",
                    icon = { Icon(Icons.Default.Settings, null) },
                    onClick = { navigateTo("settings", "Settings", restricted = true) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedItem) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } },
                    actions = {
                        if (selectedItem == "Stok Barang") {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Opsi") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Export ke Excel") }, onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val file = ExcelHelper.exportProductsToExcel(context, productList)
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Kirim Excel"))
                                    }
                                })
                                DropdownMenuItem(text = { Text("Import dari Excel") }, onClick = { showMenu = false; launcherImport.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSales = { navigateTo("sales", "Penjualan", restricted = false) },
                            onNavigateToStock = { navigateTo("stock", "Stok Barang", restricted = true) }
                        )
                    }
                    composable("stock") { ProductScreen(viewModel) }
                    composable("sales") { SalesScreen(viewModel) }
                    composable("report") { ReportScreen(viewModel) }
                    composable("debt") { DebtScreen(viewModel) }
                    composable("expenses") { ExpenseScreen(viewModel) }
                    composable("settings") { SettingsScreen(viewModel = viewModel) }
                }
            }
        }

        // --- POP-UP PIN DIALOG ---
        if (showPinDialog) {
            Dialog(
                onDismissRequest = { showPinDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false) // Full Screen Overlay
            ) {
                PinLockScreen(
                    onUnlock = {
                        isAdminUnlocked = true // BERHASIL JADI ADMIN
                        showPinDialog = false
                        Toast.makeText(context, "Mode Admin Terbuka", Toast.LENGTH_SHORT).show()

                        // Lanjut ke menu yang tadi dipencet
                        if (pendingRoute.isNotEmpty()) {
                            selectedItem = pendingItemName
                            navController.navigate(pendingRoute)
                        }
                    },
                    onCancel = {
                        showPinDialog = false
                        pendingRoute = ""
                    }
                )
            }
        }
    }
}