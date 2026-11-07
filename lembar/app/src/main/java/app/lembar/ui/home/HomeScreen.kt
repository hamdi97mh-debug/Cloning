package app.lembar.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lembar.R
import app.lembar.data.PageEntity
import app.lembar.data.PageTree
import app.lembar.ui.common.EmptyState
import app.lembar.ui.common.PageRow
import app.lembar.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pages: List<PageEntity>,
    onMenu: () -> Unit,
    onOpen: (String) -> Unit,
    onNewPage: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val active = remember(pages) { pages.filter { !it.inTrash } }
    val favourites = remember(active) { active.filter { it.isFavourite } }
    val recent = remember(active) { active.sortedByDescending { it.lastOpenedAt }.take(6) }
    val roots = remember(active) { PageTree.roots(active) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.open_menu)) }
                },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, stringResource(R.string.search)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewPage) { Icon(Icons.Filled.Add, stringResource(R.string.new_page)) }
        },
    ) { padding ->
        if (active.isEmpty()) {
            EmptyState(
                stringResource(R.string.home_empty_title),
                stringResource(R.string.home_empty_body),
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                if (favourites.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.home_favourites)) }
                    items(favourites, key = { "fav-" + it.id }) { PageRow(it, onClick = { onOpen(it.id) }) }
                }
                item { SectionHeader(stringResource(R.string.home_recent)) }
                items(recent, key = { "recent-" + it.id }) { PageRow(it, onClick = { onOpen(it.id) }) }
                item { SectionHeader(stringResource(R.string.home_pages)) }
                items(roots, key = { "root-" + it.id }) { PageRow(it, onClick = { onOpen(it.id) }) }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}
