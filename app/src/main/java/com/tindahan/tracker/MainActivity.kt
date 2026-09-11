package com.tindahan.tracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tindahan.tracker.ui.navigation.Routes
import com.tindahan.tracker.ui.screens.CalculatorScreen
import com.tindahan.tracker.ui.screens.DashboardScreen
import com.tindahan.tracker.ui.screens.MoreScreen
import com.tindahan.tracker.ui.screens.OnboardingScreen
import com.tindahan.tracker.ui.screens.ProductDetailsScreen
import com.tindahan.tracker.ui.screens.SalesHistoryScreen
import com.tindahan.tracker.ui.screens.StockScreen
import com.tindahan.tracker.ui.screens.TrackerScreen
import com.tindahan.tracker.ui.theme.TindahanTheme
import com.tindahan.tracker.viewmodel.DashboardViewModel
import com.tindahan.tracker.viewmodel.ExpenseViewModel
import com.tindahan.tracker.viewmodel.ProductDetailsViewModel
import com.tindahan.tracker.viewmodel.SettingsViewModel
import com.tindahan.tracker.viewmodel.StockViewModel
import com.tindahan.tracker.viewmodel.UtangViewModel
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language (en/tl) very early. Read DataStore synchronously via prefs file fallback:
        // We store the same value in a tiny SharedPreferences mirror for attach-time access.
        // SettingsRepository is DataStore; for simplicity read SharedPreferences mirror first.
        val prefs = newBase.getSharedPreferences("locale_mirror", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", null)
        if (lang == "tl" || lang == "fil") {
            super.attachBaseContext(wrap(newBase, "tl"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TindahanApp
        setContent {
            TindahanAppContent(app)
        }
    }

    companion object {
        fun wrap(context: Context, language: String): Context {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
}

@Composable
private fun TindahanAppContent(app: TindahanApp) {
    val repo = remember { app.repository }
    val settings = remember { app.settingsRepository }
    val scope = rememberCoroutineScope()

    val theme by settings.theme.collectAsState(initial = "dark")
    val language by settings.language.collectAsState(initial = "en")
    val currency by settings.currency.collectAsState(initial = "₱")
    val businessName by settings.businessName.collectAsState(initial = "")
    val defaultLow by settings.defaultLowStock.collectAsState(initial = 5)
    val onboardingDone by settings.onboardingDone.collectAsState(initial = false)

    // Mirror language for next process start (attachBaseContext)
    androidx.compose.runtime.LaunchedEffect(language) {
        app.getSharedPreferences("locale_mirror", Context.MODE_PRIVATE)
            .edit().putString("language", language).apply()
    }

    TindahanTheme(themeMode = theme) {
        val nav = rememberNavController()
        val stockVm: StockViewModel = viewModel(factory = StockViewModel.Factory(repo))
        val utangVm: UtangViewModel = viewModel(factory = UtangViewModel.Factory(repo))
        val expenseVm: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repo))
        val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repo, settings))
        val dashboardVm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(repo))

        // Keep onboarding as start destination when not done
        val start = if (onboardingDone) Routes.STOCK else Routes.ONBOARDING
        // Recompose NavHost when onboardingDone flips: use key
        Keyed(start) {
            Scaffold(
                bottomBar = {
                    val entry by nav.currentBackStackEntryAsState()
                    val route = entry?.destination?.route
                    val showBar = route in listOf(Routes.STOCK, Routes.TRACKER, Routes.MORE)
                    if (showBar) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = route == Routes.STOCK,
                                onClick = { nav.navigate(Routes.STOCK) { popUpTo(Routes.STOCK); launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(stringResource(com.tindahan.tracker.R.string.nav_stock)) }
                            )
                            NavigationBarItem(
                                selected = route == Routes.TRACKER,
                                onClick = { nav.navigate(Routes.TRACKER) { popUpTo(Routes.STOCK); launchSingleTop = true } },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text(stringResource(com.tindahan.tracker.R.string.nav_tracker)) }
                            )
                            NavigationBarItem(
                                selected = route == Routes.MORE,
                                onClick = { nav.navigate(Routes.MORE) { popUpTo(Routes.STOCK); launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(stringResource(com.tindahan.tracker.R.string.nav_more)) }
                            )
                        }
                    }
                }
            ) { pad ->
                NavHost(nav, startDestination = start, modifier = Modifier.padding(pad)) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(onFinish = { name ->
                            scope.launch {
                                if (name != null) settings.setBusinessName(name)
                                settings.setOnboardingDone(true)
                                nav.navigate(Routes.STOCK) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                            }
                        })
                    }
                    composable(Routes.STOCK) {
                        StockScreen(
                            vm = stockVm,
                            currency = currency,
                            defaultThreshold = defaultLow,
                            onOpenProduct = { nav.navigate(Routes.productDetails(it)) },
                            onOpenSalesHistory = { nav.navigate(Routes.SALES_HISTORY) }
                        )
                    }
                    composable(Routes.TRACKER) {
                        TrackerScreen(utangVm, expenseVm, currency, businessName)
                    }
                    composable(Routes.MORE) {
                        MoreScreen(
                            settingsVm,
                            onOpenDashboard = { nav.navigate(Routes.DASHBOARD) },
                            onOpenCalculator = { nav.navigate(Routes.CALCULATOR) }
                        )
                    }
                    composable(
                        Routes.DASHBOARD,
                        enterTransition = { slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)) }
                    ) {
                        DashboardScreen(dashboardVm, currency, onBack = { nav.popBackStack() })
                    }
                    composable(
                        Routes.CALCULATOR,
                        enterTransition = { slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)) }
                    ) {
                        CalculatorScreen(onBack = { nav.popBackStack() })
                    }
                    composable(
                        Routes.SALES_HISTORY,
                        enterTransition = { slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)) }
                    ) {
                        SalesHistoryScreen(repo, currency, onBack = { nav.popBackStack() })
                    }
                    composable(
                        Routes.PRODUCT_DETAILS,
                        arguments = listOf(navArgument("productId") { type = NavType.LongType }),
                        enterTransition = { slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)) }
                    ) { backStack ->
                        val id = backStack.arguments?.getLong("productId") ?: 0L
                        val detailsVm: ProductDetailsViewModel =
                            viewModel(key = "pd_$id", factory = ProductDetailsViewModel.Factory(repo, id))
                        ProductDetailsScreen(detailsVm, currency, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

// Helper to force recomposition key
@Composable
private fun Keyed(key: String, content: @Composable () -> Unit) {
    androidx.compose.runtime.key(key) { content() }
}
