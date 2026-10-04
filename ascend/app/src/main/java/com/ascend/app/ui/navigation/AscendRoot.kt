package com.ascend.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ascend.app.di.LocalAppContainer
import com.ascend.app.ui.celebration.CelebrationHost
import com.ascend.app.ui.celebration.CelebrationState
import com.ascend.app.ui.components.AscendBottomBar
import com.ascend.app.ui.components.AuroraBackground
import com.ascend.app.ui.components.MainTab
import com.ascend.app.ui.screens.achievements.AchievementsRoute
import com.ascend.app.ui.screens.hero.HeroRoute
import com.ascend.app.ui.screens.library.BookDetailRoute
import com.ascend.app.ui.screens.library.LibraryRoute
import com.ascend.app.ui.screens.onboarding.OnboardingScreen
import com.ascend.app.ui.screens.quests.QuestEditorRequest
import com.ascend.app.ui.screens.quests.QuestEditorSheet
import com.ascend.app.ui.screens.quests.QuestsRoute
import com.ascend.app.ui.screens.settings.SettingsRoute
import com.ascend.app.ui.screens.shop.ShopRoute
import com.ascend.app.ui.screens.today.TodayRoute
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Auras
import kotlinx.coroutines.launch

private object Routes {
    const val BOOK = "book/{bookId}"
    const val SHOP = "shop"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"

    fun book(id: Long) = "book/$id"
}

/** Корень приложения: пробуждение героя или главный экран, поверх — слой празднований. */
@Composable
fun AscendRoot() {
    val container = LocalAppContainer.current
    val hasHero by container.repository.hasHero.collectAsStateWithLifecycle(initialValue = null)
    val celebrations = remember { CelebrationState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(container) {
        container.celebrations.flow.collect { celebrations.enqueue(it) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AscendColors.Void),
    ) {
        AnimatedContent(
            targetState = hasHero,
            transitionSpec = {
                (fadeIn(tween(700)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f))) togetherWith
                    (fadeOut(tween(450)) + scaleOut(targetScale = 1.12f, animationSpec = tween(450)))
            },
            label = "root",
        ) { state ->
            when (state) {
                null -> Box(Modifier.fillMaxSize())
                false -> AuroraBackground { OnboardingScreen() }
                true -> MainScaffold()
            }
        }
        CelebrationHost(
            state = celebrations,
            onUndo = { logId -> scope.launch { container.repository.undo(logId) } },
        )
    }
}

@Composable
private fun MainScaffold() {
    val container = LocalAppContainer.current
    val hero by container.repository.heroState.collectAsStateWithLifecycle(initialValue = null)
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = MainTab.entries.firstOrNull { it.route == route }
    var editor by remember { mutableStateOf<QuestEditorRequest?>(null) }

    val aura = Auras.of(hero?.aura ?: 0)
    val accent = when (tab) {
        MainTab.TODAY -> AscendColors.Violet
        MainTab.QUESTS -> AscendColors.Indigo
        MainTab.LIBRARY -> AscendColors.Cyan
        MainTab.HERO -> aura.first()
        null -> AscendColors.Violet
    }
    val secondary = when (tab) {
        MainTab.LIBRARY -> AscendColors.Violet
        MainTab.HERO -> aura.last()
        else -> AscendColors.Cyan
    }

    AuroraBackground(accent = accent, secondary = secondary) {
        NavHost(
            navController = navController,
            startDestination = MainTab.TODAY.route,
            enterTransition = { fadeIn(tween(320)) + scaleIn(initialScale = 0.97f, animationSpec = tween(320)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(320)) + scaleIn(initialScale = 1.03f, animationSpec = tween(320)) },
            popExitTransition = { fadeOut(tween(220)) + scaleOut(targetScale = 0.96f, animationSpec = tween(220)) },
        ) {
            composable(MainTab.TODAY.route) {
                TodayRoute(
                    onOpenHero = { navController.navigateTab(MainTab.HERO) },
                    onOpenShop = { navController.navigate(Routes.SHOP) },
                    onOpenEditor = { editor = it },
                )
            }
            composable(MainTab.QUESTS.route) {
                QuestsRoute(onOpenEditor = { editor = it })
            }
            composable(MainTab.LIBRARY.route) {
                LibraryRoute(onOpenBook = { navController.navigate(Routes.book(it)) })
            }
            composable(MainTab.HERO.route) {
                HeroRoute(
                    onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                    onOpenShop = { navController.navigate(Routes.SHOP) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
                enterTransition = { slideIn() },
                popExitTransition = { slideOut() },
            ) { entry ->
                BookDetailRoute(
                    bookId = entry.arguments?.getLong("bookId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SHOP, enterTransition = { slideIn() }, popExitTransition = { slideOut() }) {
                ShopRoute(onBack = { navController.popBackStack() })
            }
            composable(Routes.ACHIEVEMENTS, enterTransition = { slideIn() }, popExitTransition = { slideOut() }) {
                AchievementsRoute(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS, enterTransition = { slideIn() }, popExitTransition = { slideOut() }) {
                SettingsRoute(onBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = tab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
        ) {
            AscendBottomBar(
                selected = tab ?: MainTab.TODAY,
                onSelect = { navController.navigateTab(it) },
                onAdd = { editor = QuestEditorRequest.New() },
            )
        }
    }

    editor?.let { request ->
        QuestEditorSheet(request = request, onDismiss = { editor = null })
    }
}

private fun AnimatedContentTransitionScope<*>.slideIn() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(380)) + fadeIn(tween(300))

private fun AnimatedContentTransitionScope<*>.slideOut() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(320)) + fadeOut(tween(260))

private fun NavHostController.navigateTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
