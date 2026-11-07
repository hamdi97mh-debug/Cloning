package app.lembar.ui.trash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.lembar.R
import app.lembar.data.PageEntity
import app.lembar.data.PageTree
import app.lembar.data.Repository
import app.lembar.ui.common.EmptyState
import app.lembar.ui.common.PageRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    repository: Repository,
    pages: List<PageEntity>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val trashed = remember(pages) { PageTree.topLevelTrash(pages) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<PageEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (trashed.isNotEmpty()) {
                        TextButton(onClick = { confirmEmpty = true }) { Text(stringResource(R.string.trash_empty_all)) }
                    }
                },
            )
        },
    ) { padding ->
        if (trashed.isEmpty()) {
            EmptyState(stringResource(R.string.trash_empty), "", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                items(trashed, key = { it.id }) { page ->
                    PageRow(page, onClick = { onOpen(page.id) }, trailing = {
                        IconButton(onClick = { scope.launch { repository.restorePage(page.id) } }) {
                            Icon(Icons.Filled.Restore, stringResource(R.string.restore))
                        }
                        IconButton(onClick = { confirmDelete = page }) {
                            Icon(Icons.Filled.DeleteForever, stringResource(R.string.delete_forever))
                        }
                    })
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            text = { Text(stringResource(R.string.trash_empty_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmpty = false
                    scope.launch { repository.emptyTrash() }
                }) { Text(stringResource(R.string.delete_forever)) }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    confirmDelete?.let { page ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_forever)) },
            text = { Text(page.title.ifBlank { stringResource(R.string.untitled) }) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch { repository.deleteForever(page.id) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
