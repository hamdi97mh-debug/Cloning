package app.lembar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lembar.LembarApp
import app.lembar.data.PageEntity
import app.lembar.data.Repository
import app.lembar.ui.editor.EditorScreen
import app.lembar.ui.home.HomeScreen
import app.lembar.ui.home.Sidebar
import app.lembar.ui.search.SearchScreen
import app.lembar.ui.settings.SettingsScreen
import app.lembar.ui.trash.TrashScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PagesViewModel(repository: Repository) : ViewModel() {
    val pages: StateFlow<List<PageEntity>> = repository.observeAllPages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

object Routes {
    const val HOME = "home"
    const val PAGE = "page/{id}"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    fun page(id: String) = "page/$id"
}

@Composable
fun LembarRoot(app: LembarApp) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagesVm: PagesViewModel = viewModel(factory = viewModelFactory { initializer { PagesViewModel(app.repository) } })
    val pages by pagesVm.pages.collectAsStateWithLifecycle()
    val backEntry by nav.currentBackStackEntryAsState()
    val onHome = backEntry?.destination?.route == Routes.HOME

    LaunchedEffect(Unit) {
        app.openRequests.collect { id -> nav.navigate(Routes.page(id)) }
    }

    fun openPage(id: String) {
        scope.launch { drawerState.close() }
        nav.navigate(Routes.page(id))
    }

    fun newPage(parentId: String?) {
        scope.launch {
            val id = app.repository.createPage(parentId)
            drawerState.close()
            nav.navigate(Routes.page(id))
        }
    }

    fun go(route: String) {
        scope.launch { drawerState.close() }
        nav.navigate(route) { launchSingleTop = true }
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = onHome || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Sidebar(
                    pages = pages,
                    onOpen = ::openPage,
                    onNewPage = ::newPage,
                    onSearch = { go(Routes.SEARCH) },
                    onTrash = { go(Routes.TRASH) },
                    onSettings = { go(Routes.SETTINGS) },
                )
            }
        },
    ) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    pages = pages,
                    onMenu = { scope.launch { drawerState.open() } },
                    onOpen = ::openPage,
                    onNewPage = { newPage(null) },
                    onSearch = { go(Routes.SEARCH) },
                    onSettings = { go(Routes.SETTINGS) },
                )
            }
            composable(Routes.PAGE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                EditorScreen(
                    pageId = id,
                    onBack = { nav.popBackStack() },
                    onOpenPage = { nav.navigate(Routes.page(it)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    repository = app.repository,
                    pages = pages,
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Routes.page(id)) { popUpTo(Routes.SEARCH) { inclusive = true } } },
                )
            }
            composable(Routes.TRASH) {
                TrashScreen(
                    repository = app.repository,
                    pages = pages,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(Routes.page(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(prefs = app.prefs, onBack = { nav.popBackStack() })
            }
        }
    }
}
