package app.lembar.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lembar.data.Block
import app.lembar.data.BlockType
import app.lembar.data.EditorOps
import app.lembar.data.MarkdownExport
import app.lembar.data.PageEntity
import app.lembar.data.Repository
import app.lembar.data.newId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(private val pageId: String, private val repo: Repository) : ViewModel() {

    enum class PickerMode { INSERT_AFTER, SLASH, TURN_INTO }

    data class PickerTarget(val blockId: String?, val mode: PickerMode)

    /** Ask the block (or the title, with [TITLE_ID]) to take focus with the cursor at [cursor]. */
    data class FocusRequest(val blockId: String, val cursor: Int, val nonce: Long = System.nanoTime())

    data class UiState(
        val loaded: Boolean = false,
        val title: String = "",
        val blocks: List<Block> = emptyList(),
        val picker: PickerTarget? = null,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    )

    sealed interface Event {
        data class OpenPage(val id: String) : Event
        data object Close : Event
        data class Share(val title: String, val text: String) : Event
    }

    val page: StateFlow<PageEntity?> = repo.observePage(pageId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val allPages: StateFlow<List<PageEntity>> = repo.observeAllPages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _focus = MutableStateFlow<FocusRequest?>(null)
    val focus: StateFlow<FocusRequest?> = _focus.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events

    /** The block that last had focus, for the toolbar actions. */
    var focusedId: String? = null

    private val undoStack = ArrayDeque<List<Block>>()
    private val redoStack = ArrayDeque<List<Block>>()
    private var lastTypingSnapshot = 0L

    private var dirty = false
    private var deleted = false
    private var saveJob: Job? = null

    private val readOnly get() = page.value?.inTrash == true

    /** Called each time the screen is shown, so changes made elsewhere (a child moved away) are picked up. */
    fun load() {
        viewModelScope.launch {
            repo.awaitWrites()
            if (dirty) return@launch
            val p = repo.getPage(pageId)
            val blocks = repo.loadBlocks(pageId).ifEmpty { listOf(Block(newId(), BlockType.TEXT)) }
            val firstLoad = !_state.value.loaded
            undoStack.clear()
            redoStack.clear()
            _state.update { it.copy(loaded = true, title = p?.title.orEmpty(), blocks = blocks, canUndo = false, canRedo = false) }
            if (p != null && !p.inTrash) repo.touchOpened(pageId)
            if (firstLoad && p != null && p.title.isBlank() && blocks.size == 1 && blocks[0].text.isEmpty()) {
                _focus.value = FocusRequest(TITLE_ID, 0)
            }
        }
    }

    // ---- saving ------------------------------------------------------------------------------

    private fun scheduleSave() {
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            flushNow()
        }
    }

    private suspend fun flushNow() {
        if (!dirty || deleted) return
        dirty = false
        val s = _state.value
        repo.awaitWrites()
        repo.savePage(pageId, s.title, s.blocks)
    }

    /** Saves pending edits on a scope that survives this screen closing. */
    fun flushDetached() {
        saveJob?.cancel()
        if (!dirty || deleted) return
        dirty = false
        val s = _state.value
        repo.launchWrite { repo.savePage(pageId, s.title, s.blocks) }
    }

    override fun onCleared() {
        flushDetached()
    }

    // ---- state changes -----------------------------------------------------------------------

    private fun commit(blocks: List<Block>, snapshot: Boolean, focus: Pair<String, Int>? = null) {
        if (snapshot) {
            undoStack.addLast(_state.value.blocks)
            if (undoStack.size > 100) undoStack.removeFirst()
            redoStack.clear()
        }
        _state.update { it.copy(blocks = blocks, canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
        if (focus != null) _focus.value = FocusRequest(focus.first, focus.second)
        scheduleSave()
    }

    private fun commit(result: EditorOps.Result, snapshot: Boolean = true) {
        commit(result.blocks, snapshot, result.focusId?.let { it to result.cursor })
    }

    private val blocks get() = _state.value.blocks

    fun consumeFocus() {
        _focus.value = null
    }

    fun setTitle(title: String) {
        if (readOnly) return
        _state.update { it.copy(title = title) }
        scheduleSave()
    }

    fun focusFirstBlock() {
        val first = blocks.firstOrNull { it.type.isText }
        if (first != null) {
            _focus.value = FocusRequest(first.id, 0)
        } else {
            val b = Block(newId(), BlockType.TEXT)
            commit(listOf(b) + blocks, snapshot = true, focus = b.id to 0)
        }
    }

    fun onTextChange(id: String, text: String) {
        if (readOnly) return
        if (text == "/") {
            commit(blocks.map { if (it.id == id) it.copy(text = text) else it }, snapshot = true)
            _state.update { it.copy(picker = PickerTarget(id, PickerMode.SLASH)) }
            return
        }
        EditorOps.applyShortcut(blocks, id, text)?.let {
            commit(it)
            return
        }
        val now = System.currentTimeMillis()
        val snapshot = now - lastTypingSnapshot > 1_000
        if (snapshot) lastTypingSnapshot = now
        commit(blocks.map { if (it.id == id) it.copy(text = text) else it }, snapshot)
    }

    fun onEnter(id: String, before: String, after: String) {
        if (readOnly) return
        commit(EditorOps.enter(blocks, id, before, after))
    }

    /** Returns true when the key press was used. */
    fun onBackspaceAtStart(id: String): Boolean {
        if (readOnly) return false
        val r = EditorOps.backspaceAtStart(blocks, id) ?: return false
        commit(r)
        return true
    }

    fun toggleChecked(id: String) {
        if (readOnly) return
        commit(blocks.map { if (it.id == id) it.copy(checked = !it.checked) else it }, snapshot = true)
    }

    fun onTapBelow() {
        if (readOnly) return
        val last = blocks.lastOrNull()
        if (last != null && last.type == BlockType.TEXT && last.text.isEmpty()) {
            _focus.value = FocusRequest(last.id, 0)
        } else {
            commit(EditorOps.insertAfter(blocks, last?.id, BlockType.TEXT))
        }
    }

    // ---- block picker ------------------------------------------------------------------------

    fun openInsertPicker() {
        if (readOnly) return
        _state.update { it.copy(picker = PickerTarget(focusedId, PickerMode.INSERT_AFTER)) }
    }

    fun openTurnIntoPicker(id: String? = focusedId) {
        if (readOnly || id == null) return
        _state.update { it.copy(picker = PickerTarget(id, PickerMode.TURN_INTO)) }
    }

    fun dismissPicker() {
        _state.update { it.copy(picker = null) }
    }

    fun pick(type: BlockType) {
        val target = _state.value.picker ?: return
        _state.update { it.copy(picker = null) }
        if (type == BlockType.PAGE) {
            viewModelScope.launch {
                val childId = repo.createPage(parentId = pageId, addRefInParent = false)
                val r = if (target.mode == PickerMode.SLASH && target.blockId != null) {
                    EditorOps.convertToPage(blocks, target.blockId, childId)
                } else {
                    EditorOps.insertPageAfter(blocks, target.blockId, childId)
                }
                commit(r)
                flushNow()
                _events.emit(Event.OpenPage(childId))
            }
            return
        }
        val r = when (target.mode) {
            PickerMode.SLASH -> EditorOps.convert(blocks, target.blockId ?: return, type, clearText = true)
            PickerMode.TURN_INTO -> EditorOps.convert(blocks, target.blockId ?: return, type, clearText = false)
            PickerMode.INSERT_AFTER -> {
                val b = blocks.firstOrNull { it.id == target.blockId }
                if (b != null && b.type == BlockType.TEXT && b.text.isEmpty()) {
                    EditorOps.convert(blocks, b.id, type, clearText = false)
                } else {
                    EditorOps.insertAfter(blocks, target.blockId, type)
                }
            }
        }
        commit(r)
    }

    // ---- block actions -----------------------------------------------------------------------

    fun toggleFocusedTodo() {
        val id = focusedId ?: return
        val b = blocks.firstOrNull { it.id == id } ?: return
        if (!b.type.isText) return
        commit(EditorOps.convert(blocks, id, if (b.type == BlockType.TODO) BlockType.TEXT else BlockType.TODO, clearText = false))
    }

    fun move(id: String?, delta: Int) {
        if (readOnly || id == null) return
        val r = EditorOps.move(blocks, id, delta)
        val b = r.blocks.firstOrNull { it.id == id }
        commit(r.blocks, snapshot = true, focus = if (b != null && b.type.isText) id to b.text.length else null)
    }

    fun duplicate(id: String) {
        if (readOnly) return
        commit(EditorOps.duplicate(blocks, id))
    }

    fun delete(id: String) {
        if (readOnly) return
        val b = blocks.firstOrNull { it.id == id } ?: return
        commit(EditorOps.remove(blocks, id))
        val child = b.childPageId
        if (b.type == BlockType.PAGE && child != null) {
            // Removing the link moves the nested page to the trash, where it can be restored.
            viewModelScope.launch {
                flushNow()
                repo.trashPage(child)
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(blocks)
        val previous = undoStack.removeLast()
        lastTypingSnapshot = 0
        _state.update { it.copy(blocks = previous, canUndo = undoStack.isNotEmpty(), canRedo = true) }
        scheduleSave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(blocks)
        val next = redoStack.removeLast()
        lastTypingSnapshot = 0
        _state.update { it.copy(blocks = next, canUndo = true, canRedo = redoStack.isNotEmpty()) }
        scheduleSave()
    }

    // ---- page actions ------------------------------------------------------------------------

    fun toggleFavourite() {
        val p = page.value ?: return
        viewModelScope.launch { repo.setFavourite(pageId, !p.isFavourite) }
    }

    fun setIcon(icon: String) {
        viewModelScope.launch { repo.setIcon(pageId, icon) }
    }

    fun moveTo(newParentId: String?) {
        viewModelScope.launch {
            flushNow()
            repo.movePage(pageId, newParentId)
        }
    }

    fun trash() {
        viewModelScope.launch {
            flushNow()
            repo.trashPage(pageId)
            _events.emit(Event.Close)
        }
    }

    fun restore() {
        viewModelScope.launch { repo.restorePage(pageId) }
    }

    fun deleteForever() {
        deleted = true
        saveJob?.cancel()
        viewModelScope.launch {
            repo.deleteForever(pageId)
            _events.emit(Event.Close)
        }
    }

    fun exportMarkdown(untitled: String) {
        val titles = allPages.value.associate { it.id to it.title.ifBlank { untitled } }
        val s = _state.value
        val md = MarkdownExport.export(s.title, s.blocks) { titles[it] ?: untitled }
        _events.tryEmit(Event.Share(s.title.ifBlank { untitled }, md))
    }

    companion object {
        const val TITLE_ID = "__title__"
    }
}
