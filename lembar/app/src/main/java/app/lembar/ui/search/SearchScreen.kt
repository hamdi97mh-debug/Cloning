package app.lembar.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.lembar.R
import app.lembar.data.PageEntity
import app.lembar.data.Repository
import app.lembar.data.SearchHit
import app.lembar.ui.common.EmptyState
import app.lembar.ui.common.PageRow
import app.lembar.ui.common.SectionHeader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: Repository) : ViewModel() {
    val query = MutableStateFlow("")
    val results: StateFlow<List<SearchHit>?> = query
        .debounce(200)
        .mapLatest { q -> if (q.isBlank()) null else repository.search(q.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: Repository,
    pages: List<PageEntity>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(repository) } })
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val recent = remember(pages) { pages.filter { !it.inTrash }.sortedByDescending { it.lastOpenedAt }.take(10) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focus),
            )
            val hits = results
            when {
                hits == null -> LazyColumn {
                    item { SectionHeader(stringResource(R.string.home_recent)) }
                    items(recent, key = { it.id }) { PageRow(it, onClick = { onOpen(it.id) }) }
                }
                hits.isEmpty() -> EmptyState(stringResource(R.string.search_no_results), "")
                else -> LazyColumn {
                    items(hits, key = { it.page.id }) { hit ->
                        PageRow(hit.page, onClick = { onOpen(hit.page.id) }, subtitle = hit.snippet)
                    }
                }
            }
        }
    }
}
