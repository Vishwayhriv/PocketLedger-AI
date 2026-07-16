package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.service.NotificationEventBus
import java.util.Locale

@Composable
fun LocaleProvider(localeCode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val locale = remember(localeCode) { Locale(localeCode) }
    
    LaunchedEffect(locale) {
        Locale.setDefault(locale)
    }
    
    val resources = context.resources
    val configuration = resources.configuration
    
    val localeContext = remember(localeCode) {
        val config = android.content.res.Configuration(configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
    
    val activityResultRegistryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
        ?: run {
            var curr: android.content.Context? = context
            var res: androidx.activity.result.ActivityResultRegistryOwner? = null
            while (curr is android.content.ContextWrapper) {
                if (curr is androidx.activity.result.ActivityResultRegistryOwner) {
                    res = curr
                    break
                }
                curr = curr.baseContext
            }
            if (res == null && curr is androidx.activity.result.ActivityResultRegistryOwner) {
                res = curr
            }
            res
        }
        ?: error("No ActivityResultRegistryOwner found")

    val onBackPressedDispatcherOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
        ?: run {
            var curr: android.content.Context? = context
            var res: androidx.activity.OnBackPressedDispatcherOwner? = null
            while (curr is android.content.ContextWrapper) {
                if (curr is androidx.activity.OnBackPressedDispatcherOwner) {
                    res = curr
                    break
                }
                curr = curr.baseContext
            }
            if (res == null && curr is androidx.activity.OnBackPressedDispatcherOwner) {
                res = curr
            }
            res
        }
        ?: error("No OnBackPressedDispatcherOwner found")

    CompositionLocalProvider(
        LocalContext provides localeContext,
        androidx.activity.compose.LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides onBackPressedDispatcherOwner,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.monetization.AdMobManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
            
            LocaleProvider(localeCode = appLanguage) {
                MyApplicationTheme(darkTheme = darkModeEnabled) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        NotificationEventBus.events.collect { event ->
                            val result = snackbarHostState.showSnackbar(
                                message = event.message,
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.deleteTransaction(event.transactionId.toInt())
                            }
                        }
                    }

                    // Decide start destination based on onboarding flag
                    val startDest = remember(onboardingCompleted) {
                        if (onboardingCompleted) "home" else "welcome"
                    }

                    LaunchedEffect(onboardingCompleted, currentRoute) {
                        if (currentRoute == "splash") return@LaunchedEffect

                        if (onboardingCompleted && currentRoute == "welcome") {
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        } else if (!onboardingCompleted && currentRoute != "welcome" && currentRoute != null && currentRoute != "salary_setup") {
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            // Display bottom bar only on major root dashboards
                            if (currentRoute in listOf("home", "transactions", "reports", "profile", "settings")) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.navigationBarsPadding() // Respect device navigation bar padding
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "home",
                                        onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                        icon = { Icon(if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                                        label = { Text(stringResource(R.string.nav_desc_home)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "transactions",
                                        onClick = { navController.navigate("transactions") },
                                        icon = { Icon(if (currentRoute == "transactions") Icons.Filled.List else Icons.Outlined.List, contentDescription = "Ledger") },
                                        label = { Text(stringResource(R.string.nav_desc_ledger)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "reports",
                                        onClick = { navController.navigate("reports") },
                                        icon = { Icon(if (currentRoute == "reports") Icons.Filled.Analytics else Icons.Outlined.Analytics, contentDescription = "Reports") },
                                        label = { Text(stringResource(R.string.nav_desc_reports)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "profile",
                                        onClick = { navController.navigate("profile") },
                                        icon = { Icon(if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                                        label = { Text(stringResource(R.string.nav_desc_profile)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "settings",
                                        onClick = { navController.navigate("settings") },
                                        icon = { Icon(if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                                        label = { Text(stringResource(R.string.nav_desc_settings)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onSplashFinished = {
                                        navController.navigate(startDest) {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            
                            composable("welcome") {
                                WelcomeScreen(
                                    viewModel = viewModel,
                                    onOnboardingFinished = {
                                        navController.navigate("salary_setup") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("salary_setup") {
                                SalarySetupScreen(
                                    viewModel = viewModel,
                                    onFinished = {
                                        navController.navigate("home") {
                                            popUpTo("salary_setup") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToCashEntry = { navController.navigate("cash_entry") },
                                    onNavigateToTransactions = { navController.navigate("transactions") },
                                    onNavigateToSubscriptions = { navController.navigate("subscriptions") },
                                    onNavigateToLeaks = { navController.navigate("leaks") },
                                    onNavigateToReports = { navController.navigate("reports") },
                                    onNavigateToSalarySetup = { navController.navigate("salary_setup") },
                                    onNavigateToJourney = { navController.navigate("journey") },
                                    onNavigateToCoach = { navController.navigate("coach") },
                                    onNavigateToStatementImport = { navController.navigate("statement_import") }
                                )
                            }

                            composable("statement_import") {
                                StatementImportScreen(
                                    viewModel = viewModel,
                                    onNavigateToPremium = { navController.navigate("premium") },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("coach") {
                                FinancialCoachScreen(
                                    viewModel = viewModel,
                                    onNavigateToPremium = { navController.navigate("premium") },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "cash_entry?type={type}",
                                arguments = listOf(navArgument("type") { defaultValue = "Expense" })
                            ) { backStackEntry ->
                                val type = backStackEntry.arguments?.getString("type") ?: "Expense"
                                CashEntryScreen(
                                    viewModel = viewModel,
                                    initialType = type,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("transactions") {
                                TransactionTimelineScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                    onNavigateToCashEntry = { type ->
                                        navController.navigate("cash_entry?type=$type")
                                    }
                                )
                            }

                            composable("subscriptions") {
                                GhostSubscriptionsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("leaks") {
                                MicroLeaksScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("reports") {
                                ReportsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToPrivacy = { navController.navigate("privacy") },
                                    onNavigateToSalarySetup = { navController.navigate("salary_setup") }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToPrivacy = { navController.navigate("privacy") },
                                    onNavigateToSalarySetup = { navController.navigate("salary_setup") },
                                    onNavigate = { route -> navController.navigate(route) },
                                    onBack = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
                                )
                            }

                            composable("premium") {
                                PremiumScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // --- HELP & COMPLIANCE SCREENS ---
                            composable("privacy_policy") {
                                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                            }
                            composable("terms") {
                                TermsScreen(onBack = { navController.popBackStack() })
                            }
                            composable("about") {
                                AboutScreen(onBack = { navController.popBackStack() })
                            }
                            composable("contact_support") {
                                ContactSupportScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { route -> navController.navigate(route) }
                                )
                            }
                            composable("help_center") {
                                HelpCenterScreen(onBack = { navController.popBackStack() })
                            }
                            composable("feedback") {
                                FeedbackScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("bug_report") {
                                BugReportScreen(onBack = { navController.popBackStack() })
                            }

                            composable("privacy") {
                                PrivacyDashboardScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("journey") {
                                MyFinancialJourneyScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
