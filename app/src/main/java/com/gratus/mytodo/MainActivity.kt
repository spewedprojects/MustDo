package com.gratus.mytodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.Screen
import com.gratus.mytodo.ui.SortOption
import com.gratus.mytodo.ui.components.FaintBackground
import com.gratus.mytodo.ui.screens.HistoryScreen
import com.gratus.mytodo.ui.screens.HomeScreen
import com.gratus.mytodo.ui.screens.SettingsScreen
import com.gratus.mytodo.ui.screens.StatsScreen
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity is the host core of the Soft To-Do application.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Activity launcher for runtime permissions (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Reminders won't show notifications without permissions", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt notification permission at boot on Android 13+
        checkNotificationPermissions()

        setContent {
            val activeTheme by viewModel.settingsTheme.collectAsState()
            val colorSchemeType by viewModel.settingsColorScheme.collectAsState()

            SoftTodoTheme(
                themeMode = activeTheme,
                colorSchemeType = colorSchemeType
            ) {
                MainLayout(viewModel, colorSchemeType)
            }
        }
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * MainLayout containing Drawer, Persistent Header, and the Dynamic body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeScreen by viewModel.activeScreen.collectAsState()
    val focusDate by viewModel.currentDate.collectAsState()
    val sortOption by viewModel.sortingOption.collectAsState()

    val dateLabelFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(280.dp)
                    .testTag("nav_drawer_sheet"),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 4.dp
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                // Drawer logo / branding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "\uD83D\uDCC5 My To-Do",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Time-locked task tracker",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                val navItems = listOf(
                    Triple(Screen.HOME, "Home", Icons.Default.Home),
                    Triple(Screen.HISTORY, "History Records", Icons.Default.History),
                    Triple(Screen.STATS, "Statistics Analyzer", Icons.Default.Analytics),
                    Triple(Screen.SETTINGS, "Aesthetic Settings", Icons.Default.Settings)
                )

                navItems.forEach { (screenKey, name, icon) ->
                    val isSelected = activeScreen == screenKey
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = icon, contentDescription = name) },
                        label = { Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = isSelected,
                        onClick = {
                            viewModel.setActiveScreen(screenKey)
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_${screenKey.name.lowercase()}"),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // Fine signature branding in drawer footer
                Text(
                    text = "v1.0.0 • Personal Edition",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) {
        FaintBackground(colorSchemeType = colorSchemeType) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars), // Safely pad camera notch / cutout
                containerColor = Color.Transparent, // Let the custom backgrounds flow through
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            if (activeScreen == Screen.HOME) {
                                // Center consumed by Date and arrows to navigate dates
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Faint Left Date Increment
                                    IconButton(
                                        onClick = { viewModel.navigateDate(-1) },
                                        modifier = Modifier.alpha(0.6f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Previous Day",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = dateLabelFormatter.format(focusDate.time),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Faint Right Date Increment
                                    IconButton(
                                        onClick = { viewModel.navigateDate(1) },
                                        modifier = Modifier.alpha(0.6f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Next Day",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                // Center displays modern static names for persistent alignment
                                Text(
                                    text = when (activeScreen) {
                                        Screen.HISTORY -> "Historical Timelines"
                                        Screen.STATS -> "Completion Statistics"
                                        Screen.SETTINGS -> "Settings Profile"
                                        else -> "Soft To-Do"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        navigationIcon = {
                            // Left hamburger menu icon to reveal drawer
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("hamburger_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            // Custom Action based on active screen
                            if (activeScreen == Screen.HOME) {
                                // Sorting trigger button (Priority order vs Chronological Addition Sequence)
                                IconButton(
                                    onClick = { 
                                        viewModel.toggleSorting()
                                        val label = if (sortOption == SortOption.PRIORITY) "Sequence chronological" else "Priority list levels"
                                        Toast.makeText(viewModel.getApplication(), "Sorted by $label", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("sort_tasks_button")
                                ) {
                                    Icon(
                                        imageVector = if (sortOption == SortOption.PRIORITY) Icons.Default.SortByAlpha else Icons.Default.Sort,
                                        contentDescription = "Toggle Sort Mode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                // Keep slot padding consistent for visual balance
                                Box(modifier = Modifier.size(48.dp))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent, // Keep transparent so backdrops show clean
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (activeScreen) {
                        Screen.HOME -> HomeScreen(viewModel, { coroutineScope.launch { drawerState.open() } }, colorSchemeType)
                        Screen.HISTORY -> HistoryScreen(viewModel, colorSchemeType)
                        Screen.STATS -> StatsScreen(viewModel, colorSchemeType)
                        Screen.SETTINGS -> SettingsScreen(viewModel, colorSchemeType)
                    }
                }
            }
        }
    }
}
