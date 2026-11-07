package app.lembar.ui.editor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.lembar.LembarApp
import app.lembar.R
import app.lembar.data.BlockType
import app.lembar.data.EditorOps
import app.lembar.data.PageTree
import app.lembar.ui.common.PageIcon
import app.lembar.ui.common.pageTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(pageId: String, onBack: () -> Unit, onOpenPage: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as LembarApp
    val vm: EditorViewModel = viewModel(factory = viewModelFactory { initializer { EditorViewModel(pageId, app.repository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val page by vm.page.collectAsStateWithLifecycle()
    val allPages by vm.allPages.collectAsStateWithLifecycle()
    val focusRequest by vm.focus.collectAsStateWithLifecycle()
    val pagesById = remember(allPages) { allPages.associateBy { it.id } }
    val readOnly = page?.inTrash == true
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val untitled = stringResource(R.string.untitled)

    var menuOpen by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var showIcons by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.load() }
    DisposableEffect(Unit) { onDispose { vm.flushDetached() } }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) vm.flushDetached() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is EditorViewModel.Event.OpenPage -> onOpenPage(event.id)
                EditorViewModel.Event.Close -> onBack()
                is EditorViewModel.Event.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
            }
        }
    }

    val callbacks = remember(vm) {
        BlockCallbacks(
            onText = vm::onTextChange,
            onEnter = vm::onEnter,
            onBackspace = vm::onBackspaceAtStart,
            onFocused = { vm.focusedId = it },
            onToggle = vm::toggleChecked,
            onHandle = { actionsFor = it },
            onOpenChild = onOpenPage,
            onFocusConsumed = vm::consumeFocus,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title.ifBlank { untitled },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (!readOnly && page != null) {
                        val fav = page?.isFavourite == true
                        IconButton(onClick = vm::toggleFavourite) {
                            Icon(
                                if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                stringResource(if (fav) R.string.favourite_remove else R.string.favourite_add),
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to)) },
                                    onClick = { menuOpen = false; showMove = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_markdown)) },
                                    onClick = { menuOpen = false; vm.exportMarkdown(untitled) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to_trash)) },
                                    onClick = { menuOpen = false; vm.trash() },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!readOnly) {
                EditorToolbar(
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onAdd = vm::openInsertPicker,
                    onTurnInto = { vm.openTurnIntoPicker() },
                    onTodo = vm::toggleFocusedTodo,
                    onUp = { vm.move(vm.focusedId, -1) },
                    onDown = { vm.move(vm.focusedId, 1) },
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onHide = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
                )
            }
        },
    ) { padding ->
        val numbers = remember(state.blocks) { EditorOps.listNumbers(state.blocks) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            if (readOnly) {
                item(key = "banner") {
                    TrashBanner(onRestore = vm::restore, onDelete = vm::deleteForever)
                }
            }
            item(key = "header") {
                PageHeader(
                    icon = page?.icon.orEmpty(),
                    title = state.title,
                    readOnly = readOnly,
                    focusRequest = focusRequest?.takeIf { it.blockId == EditorViewModel.TITLE_ID },
                    onIconClick = { if (!readOnly) showIcons = true },
                    onTitle = vm::setTitle,
                    onTitleEnter = vm::focusFirstBlock,
                    onFocusConsumed = vm::consumeFocus,
                )
            }
            if (state.loaded) {
                itemsIndexed(state.blocks, key = { _, b -> b.id }) { i, b ->
                    BlockRow(
                        block = b,
                        number = numbers.getOrElse(i) { 0 },
                        readOnly = readOnly,
                        focusRequest = focusRequest?.takeIf { it.blockId == b.id },
                        child = b.childPageId?.let { pagesById[it] },
                        cb = callbacks,
                    )
                }
            }
            if (!readOnly) {
                item(key = "below") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                vm.onTapBelow()
                            },
                    )
                }
            }
        }
    }

    state.picker?.let { target ->
        ModalBottomSheet(onDismissRequest = vm::dismissPicker) {
            val types = if (target.mode == EditorViewModel.PickerMode.TURN_INTO) {
                BlockType.entries.filter { it != BlockType.PAGE }
            } else {
                BlockType.entries.toList()
            }
            Text(
                stringResource(if (target.mode == EditorViewModel.PickerMode.TURN_INTO) R.string.turn_into else R.string.add_block),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.padding(bottom = 24.dp)) {
                items(types) { t ->
                    ListItem(
                        headlineContent = { Text(stringResource(labelOf(t))) },
                        leadingContent = { Icon(iconOf(t), null) },
                        modifier = Modifier.clickable { vm.pick(t) },
                    )
                }
            }
        }
    }

    actionsFor?.let { id ->
        val block = state.blocks.firstOrNull { it.id == id }
        if (block != null) {
            val close = { actionsFor = null }
            ModalBottomSheet(onDismissRequest = close) {
                Column(Modifier.padding(bottom = 24.dp)) {
                    if (block.type.isText || block.type == BlockType.DIVIDER) {
                        ActionItem(Icons.Filled.TextFields, stringResource(R.string.turn_into)) { close(); vm.openTurnIntoPicker(id) }
                    }
                    ActionItem(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) { vm.move(id, -1) }
                    ActionItem(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) { vm.move(id, 1) }
                    if (block.type != BlockType.PAGE) {
                        ActionItem(Icons.Filled.ContentCopy, stringResource(R.string.duplicate)) { close(); vm.duplicate(id) }
                    }
                    ActionItem(Icons.Filled.Delete, stringResource(R.string.delete)) { close(); vm.delete(id) }
                }
            }
        }
    }

    if (showMove) {
        val exclude = remember(allPages) { PageTree.subtreeIds(allPages, pageId) }
        val rows = remember(allPages) { PageTree.flatten(allPages, emptySet(), expandAll = true).filter { it.page.id !in exclude } }
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text(stringResource(R.string.move_to)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    item {
                        Text(
                            stringResource(R.string.move_top_level),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMove = false; vm.moveTo(null) }
                                .padding(vertical = 12.dp),
                        )
                    }
                    items(rows, key = { it.page.id }) { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showMove = false; vm.moveTo(row.page.id) }
                                .padding(start = (row.depth * 16).dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PageIcon(row.page.icon, 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(pageTitle(row.page), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showIcons) {
        AlertDialog(
            onDismissRequest = { showIcons = false },
            title = { Text(stringResource(R.string.choose_icon)) },
            text = {
                LazyVerticalGrid(GridCells.Adaptive(48.dp), Modifier.heightIn(max = 360.dp)) {
                    items(EMOJIS) { e ->
                        Box(
                            Modifier.size(48.dp).clickable { showIcons = false; vm.setIcon(e) },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 26.sp) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showIcons = false }) { Text(stringResource(R.string.cancel)) } },
            dismissButton = {
                if (page?.icon?.isNotBlank() == true) {
                    TextButton(onClick = { showIcons = false; vm.setIcon("") }) { Text(stringResource(R.string.remove_icon)) }
                }
            },
        )
    }
}

@Composable
private fun PageHeader(
    icon: String,
    title: String,
    readOnly: Boolean,
    focusRequest: EditorViewModel.FocusRequest?,
    onIconClick: () -> Unit,
    onTitle: (String) -> Unit,
    onTitleEnter: () -> Unit,
    onFocusConsumed: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(title, TextRange(title.length))) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(title) { if (value.text != title) value = TextFieldValue(title, TextRange(title.length)) }
    LaunchedEffect(focusRequest) {
        if (focusRequest != null) {
            runCatching { requester.requestFocus() }
            onFocusConsumed()
        }
    }
    val style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
        if (icon.isNotBlank()) {
            Text(icon, fontSize = 44.sp, modifier = Modifier.clickable(enabled = !readOnly, onClick = onIconClick))
        } else if (!readOnly) {
            TextButton(onClick = onIconClick, modifier = Modifier.padding(start = 0.dp)) {
                Text(stringResource(R.string.add_icon), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { v ->
                if (v.text.contains('\n')) {
                    onTitleEnter()
                } else {
                    value = v
                    if (v.text != title) onTitle(v.text)
                }
            },
            readOnly = readOnly,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().focusRequester(requester),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(stringResource(R.string.untitled), style = style.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun TrashBanner(onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.in_trash_banner),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRestore) { Text(stringResource(R.string.restore)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_forever)) }
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onAdd: () -> Unit,
    onTurnInto: () -> Unit,
    onTodo: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHide: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
        ) {
            ToolButton(Icons.Filled.Add, stringResource(R.string.add_block), onClick = onAdd)
            ToolButton(Icons.Filled.TextFields, stringResource(R.string.turn_into), onClick = onTurnInto)
            ToolButton(Icons.Filled.CheckBox, stringResource(R.string.block_todo), onClick = onTodo)
            ToolButton(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up), onClick = onUp)
            ToolButton(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down), onClick = onDown)
            ToolButton(Icons.Filled.Undo, stringResource(R.string.undo), enabled = canUndo, onClick = onUndo)
            ToolButton(Icons.Filled.Redo, stringResource(R.string.redo), enabled = canRedo, onClick = onRedo)
            ToolButton(Icons.Filled.KeyboardHide, stringResource(R.string.hide_keyboard), onClick = onHide)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, description) }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun labelOf(t: BlockType): Int = when (t) {
    BlockType.TEXT -> R.string.block_text
    BlockType.H1 -> R.string.block_h1
    BlockType.H2 -> R.string.block_h2
    BlockType.H3 -> R.string.block_h3
    BlockType.TODO -> R.string.block_todo
    BlockType.BULLET -> R.string.block_bullet
    BlockType.NUMBERED -> R.string.block_numbered
    BlockType.QUOTE -> R.string.block_quote
    BlockType.CALLOUT -> R.string.block_callout
    BlockType.CODE -> R.string.block_code
    BlockType.DIVIDER -> R.string.block_divider
    BlockType.PAGE -> R.string.block_page
}

private fun iconOf(t: BlockType): ImageVector = when (t) {
    BlockType.TEXT -> Icons.Filled.TextFields
    BlockType.H1, BlockType.H2, BlockType.H3 -> Icons.Filled.Title
    BlockType.TODO -> Icons.Filled.CheckBox
    BlockType.BULLET -> Icons.Filled.FormatListBulleted
    BlockType.NUMBERED -> Icons.Filled.FormatListNumbered
    BlockType.QUOTE -> Icons.Filled.FormatQuote
    BlockType.CALLOUT -> Icons.Outlined.Lightbulb
    BlockType.CODE -> Icons.Filled.Code
    BlockType.DIVIDER -> Icons.Filled.HorizontalRule
    BlockType.PAGE -> Icons.Outlined.Description
}

private val EMOJIS = listOf(
    "📄", "📝", "📌", "✅", "📚", "💡", "🎯", "📅",
    "📊", "💼", "🏠", "✈️", "🛒", "💰", "🎨", "🎵",
    "🍳", "🏃", "❤️", "⭐", "🔥", "🌱", "🧠", "🛠️",
    "📷", "🎮", "🧾", "🔒", "📦", "🌍", "☕", "🎓",
)
