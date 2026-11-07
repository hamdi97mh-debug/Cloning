package app.lembar.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lembar.R
import app.lembar.data.PageEntity
import app.lembar.data.PageTree
import app.lembar.ui.common.PageIcon
import app.lembar.ui.common.SectionHeader
import app.lembar.ui.common.pageTitle

@Composable
fun Sidebar(
    pages: List<PageEntity>,
    onOpen: (String) -> Unit,
    onNewPage: (String?) -> Unit,
    onSearch: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(setOf<String>()) }
    val rows = remember(pages, expanded) { PageTree.flatten(pages, expanded) }
    val favourites = remember(pages) { pages.filter { it.isFavourite && !it.inTrash } }

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
            )
        }
        item {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.search)) },
                icon = { Icon(Icons.Filled.Search, null) },
                selected = false,
                onClick = onSearch,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.new_page)) },
                icon = { Icon(Icons.Filled.Add, null) },
                selected = false,
                onClick = { onNewPage(null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (favourites.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_favourites)) }
            items(favourites, key = { "fav-" + it.id }) { page ->
                TreeRow(PageTree.Row(page, 0, false), expanded = false, onToggle = {}, onClick = { onOpen(page.id) }, onAdd = null)
            }
        }
        item { SectionHeader(stringResource(R.string.home_pages)) }
        items(rows, key = { "tree-" + it.page.id }) { row ->
            val id = row.page.id
            TreeRow(
                row = row,
                expanded = id in expanded,
                onToggle = { expanded = if (id in expanded) expanded - id else expanded + id },
                onClick = { onOpen(id) },
                onAdd = {
                    expanded = expanded + id
                    onNewPage(id)
                },
            )
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.trash)) },
                icon = { Icon(Icons.Filled.Delete, null) },
                selected = false,
                onClick = onTrash,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings)) },
                icon = { Icon(Icons.Filled.Settings, null) },
                selected = false,
                onClick = onSettings,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TreeRow(
    row: PageTree.Row,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onAdd: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick)
            .padding(start = (12 + row.depth * 16).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.hasChildren) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                    stringResource(if (expanded) R.string.collapse else R.string.expand),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
        PageIcon(row.page.icon, 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            pageTitle(row.page),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onAdd != null) {
            IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_subpage), modifier = Modifier.size(18.dp))
            }
        }
    }
}
