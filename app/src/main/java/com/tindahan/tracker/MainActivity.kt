package com.tindahan.tracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.tindahan.tracker.ui.screens.NotesScreen
import com.tindahan.tracker.ui.screens.OnboardingScreen
import com.tindahan.tracker.ui.screens.ProductDetailsScreen
import com.tindahan.tracker.ui.screens.SalesHistoryScreen
import com.tindahan.tracker.ui.screens.StockScreen
import com.tindahan.tracker.ui.screens.TrackerScreen
import com.tindahan.tracker.ui.theme.TindahanTheme
import com.tindahan.tracker.viewmodel.DashboardViewModel
import com.tindahan.tracker.viewmodel.ExpenseViewModel
import com.tindahan.tracker.viewmodel.NotesViewModel
import com.tindahan.tracker.viewmodel.ProductDetailsViewModel
import com.tindahan.tracker.viewmodel.SettingsViewModel
import com.tindahan.tracker.viewmodel.StockViewModel
import com.tindahan.tracker.viewmodel.UtangViewModel
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language (en/tl) very early. DataStore can't be read
        // synchronously here, so a tiny SharedPreferences mirror is kept
        // in sync (see the LaunchedEffect below).
        val prefs = newBase.getSharedPreferences("locale_mirror", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", null)
        super.attachBaseContext(wrap(newBase, if (lang == "tl" || lang == "fil") "tl" else "en"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TindahanApp
        setContent {
            TindahanAppContent(app)
        }
        // Load the first ad after the first frame so cold start stays smooth.
        // Returning from background shows the already-cached ad instantly.
        Handler(Looper.getMainLooper()).postDelayed({
            app.adsManager.load()
        }, 1500)
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
    val onboardingDone by settings.onboardingDone.collectAsState(initial = null)

    // Language switching restarts the Activity (the only reliable way to
    // re-resolve every resource). Hardened against glitches:
    // - preference is commit()ed synchronously first, so the restarted
    //   Activity can never read a stale value (that race caused a restart storm)
    // - a short delay lets open popups/dialogs fully dismiss first
    // - a process-level guard makes a repeat loop impossible
    // - the loading gate below means the UI never flashes the wrong start screen
    val realActivity = LocalContext.current as? Activity
    androidx.compose.runtime.LaunchedEffect(language) {
        app.getSharedPreferences("locale_mirror", Context.MODE_PRIVATE)
            .edit().putString("language", language).commit()
        Locale.setDefault(if (language == "tl") Locale("tl") else Locale.ENGLISH)
        kotlinx.coroutines.delay(300)
        val target = if (language == "tl") "tl" else "en"
        val current = realActivity?.resources?.configuration?.locales?.get(0)?.language
        if (realActivity != null && current != null && current != target && lastRecreateLang != language) {
            lastRecreateLang = language
            realActivity.recreate()
        }
    }

    if (onboardingDone == null) {
        // DataStore hasn't emitted yet: don't guess a start destination
        // (guessing wrong flashes onboarding and destabilizes navigation).
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    TindahanTheme(themeMode = theme) {
        val nav = rememberNavController()
        val stockVm: StockViewModel = viewModel(factory = StockViewModel.Factory(repo))
        val utangVm: UtangViewModel = viewModel(factory = UtangViewModel.Factory(repo))
        val expenseVm: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repo))
        val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repo, settings))
        val dashboardVm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(repo))
        val notesVm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(repo))

        // Keep onboarding as start destination when not done
        val start = if (onboardingDone == true) Routes.STOCK else Routes.ONBOARDING
        // Recompose NavHost when onboardingDone flips: use key
        Keyed(start) {
            Scaffold(
                bottomBar = {
                    val entry by nav.currentBackStackEntryAsState()
                    val route = entry?.destination?.route
                    val showBar = route in listOf(Routes.STOCK, Routes.TRACKER, Routes.CALCULATOR, Routes.NOTES, Routes.MORE)
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
                                selected = route == Routes.CALCULATOR,
                                onClick = { nav.navigate(Routes.CALCULATOR) { popUpTo(Routes.STOCK); launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                                label = { Text(stringResource(com.tindahan.tracker.R.string.nav_calculator)) }
                            )
                            NavigationBarItem(
                                selected = route == Routes.NOTES,
                                onClick = { nav.navigate(Routes.NOTES) { popUpTo(Routes.STOCK); launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                                label = { Text(stringResource(com.tindahan.tracker.R.string.nav_notes)) }
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
                                // Replaying the intro from More just goes back;
                                // first run replaces it with Stock.
                                if (nav.previousBackStackEntry != null) {
                                    nav.popBackStack()
                                } else {
                                    nav.navigate(Routes.STOCK) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                                }
                            }
                        })
                    }
                    composable(Routes.STOCK) {
                        StockScreen(
                            vm = stockVm,
                            currency = currency,
                            defaultThreshold = defaultLow,
                            businessName = businessName,
                            onOpenProduct = { nav.navigate(Routes.productDetails(it)) },
                            onOpenSalesHistory = { nav.navigate(Routes.SALES_HISTORY) }
                        )
                    }
                    composable(Routes.TRACKER) {
                        TrackerScreen(utangVm, expenseVm, currency, businessName)
                    }
                    composable(Routes.NOTES) {
                        NotesScreen(notesVm)
                    }
                    composable(Routes.MORE) {
                        MoreScreen(
                            settingsVm,
                            onOpenDashboard = { nav.navigate(Routes.DASHBOARD) },
                            onReplayIntro = { nav.navigate(Routes.ONBOARDING) }
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

// Process-level guard: survives Activity recreation, breaks any recreate loop.
private var lastRecreateLang: String? = null
