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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aplikasitokosembakoarkhan.utils.ExcelHelper
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
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
    val productList by viewModel.allProducts.collectAsState(initial = emptyList())

    var selectedItem by remember { mutableStateOf("Dashboard") }
    var showMenu by remember { mutableStateOf(false) }
    var isPinSet by remember { mutableStateOf(SecurityHelper.isPinSet(context)) }
    var isAdminUnlocked by remember { mutableStateOf(!isPinSet) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf("") }
    var pendingItemName by remember { mutableStateOf("") }

    fun refreshSecurityState() {
        isPinSet = SecurityHelper.isPinSet(context)
        if (!isPinSet) isAdminUnlocked = true
    }

    fun navigateTo(route: String, itemName: String, restricted: Boolean) {
        scope.launch { drawerState.close() }
        refreshSecurityState()
        if (!isPinSet || !restricted || isAdminUnlocked) {
            selectedItem = itemName
            navController.navigate(route) { if (route == "dashboard") popUpTo("dashboard") { inclusive = true } }
        } else {
            pendingRoute = route; pendingItemName = itemName; showPinDialog = true
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
                NavigationDrawerItem(label = { Text("Beranda") }, selected = selectedItem == "Dashboard", icon = { Icon(Icons.Default.Home, null) }, onClick = { navigateTo("dashboard", "Dashboard", false) })
                NavigationDrawerItem(label = { Text("Kasir") }, selected = selectedItem == "Penjualan", icon = { Icon(Icons.Default.ShoppingCart, null) }, onClick = { navigateTo("sales", "Penjualan", false) })
                NavigationDrawerItem(label = { Text("Stok Barang") }, selected = selectedItem == "Stok Barang", icon = { Icon(Icons.Default.Inventory, null) }, onClick = { navigateTo("stock", "Stok Barang", true) })

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("MASTER DATA", modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)

                NavigationDrawerItem(label = { Text("Kategori") }, selected = selectedItem == "Kategori", icon = { Icon(Icons.Default.Category, null) }, onClick = { navigateTo("categories", "Kategori", true) })
                NavigationDrawerItem(label = { Text("Satuan") }, selected = selectedItem == "Satuan", icon = { Icon(Icons.Default.LinearScale, null) }, onClick = { navigateTo("units", "Satuan", true) })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(label = { Text("Kasbon") }, selected = selectedItem == "Kasbon", icon = { Icon(Icons.Default.Book, null) }, onClick = { navigateTo("debt", "Kasbon", true) })
                NavigationDrawerItem(label = { Text("Laporan") }, selected = selectedItem == "Laporan", icon = { Icon(Icons.Default.DateRange, null) }, onClick = { navigateTo("report", "Laporan", true) })
                NavigationDrawerItem(label = { Text("Biaya Ops.") }, selected = selectedItem == "Expenses", icon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, null) }, onClick = { navigateTo("expenses", "Expenses", true) })
                NavigationDrawerItem(label = { Text("Pengaturan") }, selected = selectedItem == "Settings", icon = { Icon(Icons.Default.Settings, null) }, onClick = { navigateTo("settings", "Settings", true) })
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Tampilkan judul dinamis berdasarkan halaman
                TopAppBar(
                    title = { Text(selectedItem) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } },
                    actions = {
                        refreshSecurityState()
                        if (isPinSet) {
                            IconButton(onClick = { if (isAdminUnlocked) { isAdminUnlocked = false; Toast.makeText(context, "Terkunci", Toast.LENGTH_SHORT).show(); navController.navigate("dashboard"); selectedItem="Dashboard" } else { pendingRoute=""; showPinDialog=true } }) {
                                Icon(if (isAdminUnlocked) Icons.Default.LockOpen else Icons.Default.Lock, null, tint = if (isAdminUnlocked) Color(0xFF2E7D32) else Color.Red)
                            }
                        }
                        if (selectedItem == "Stok Barang" && isAdminUnlocked) {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Opsi") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Export ke Excel") }, onClick = {
                                    showMenu = false
                                    scope.launch {
                                        if (productList.isNotEmpty()) {
                                            val file = ExcelHelper.exportProductsToExcel(context, productList)
                                            if (file.exists()) {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Kirim Excel"))
                                            }
                                        } else {
                                            Toast.makeText(context, "Data Kosong", Toast.LENGTH_SHORT).show()
                                        }
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
                    composable("dashboard") { DashboardScreen(viewModel, { navigateTo("sales", "Penjualan", false) }, { navigateTo("stock", "Stok Barang", true) }) }
                    composable("stock") { ProductScreen(viewModel) }
                    composable("categories") { CategoryScreen(viewModel) }
                    composable("units") { UnitScreen(viewModel) }
                    composable("sales") { SalesScreen(viewModel) }
                    composable("report") { ReportScreen(viewModel) }
                    composable("debt") { DebtScreen(viewModel) }
                    composable("expenses") { ExpenseScreen(viewModel) }

                    // --- ROUTE PENGATURAN ---
                    composable("settings") {
                        SettingsScreen(
                            viewModel,
                            onNavigateToReceipt = { navController.navigate("settings_receipt") },
                            onNavigateToBackup = { navController.navigate("settings_backup") },
                            onNavigateToSecurity = { navController.navigate("settings_security") }
                        )
                    }
                    composable("settings_receipt") { ReceiptSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable("settings_backup") { BackupSettingsScreen(viewModel, onBack = { navController.popBackStack() }) }
                    composable("settings_security") { SecuritySettingsScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
        if (showPinDialog) {
            Dialog(onDismissRequest = { showPinDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                PinLockScreen(onUnlock = { isAdminUnlocked = true; showPinDialog = false; if (pendingRoute.isNotEmpty()) { selectedItem = pendingItemName; navController.navigate(pendingRoute) } }, onCancel = { showPinDialog = false })
            }
        }
    }
}