package app.lembar.data

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class Repository(private val db: AppDatabase) {
    private val pageDao = db.pageDao()
    private val blockDao = db.blockDao()

    /** Outlives screens, so a save started as a screen closes still finishes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastWrite: Job? = null

    /** Runs writes one after another, in the order they were queued. Call from the main thread. */
    fun launchWrite(block: suspend () -> Unit) {
        val previous = lastWrite
        lastWrite = appScope.launch {
            previous?.join()
            try {
                block()
            } catch (e: Exception) {
                Log.e("Lembar", "Write failed", e)
            }
        }
    }

    suspend fun awaitWrites() {
        lastWrite?.join()
    }

    fun observeAllPages(): Flow<List<PageEntity>> = pageDao.observeAll()

    fun observePage(id: String): Flow<PageEntity?> = pageDao.observe(id)

    suspend fun getPage(id: String): PageEntity? = pageDao.get(id)

    suspend fun loadBlocks(pageId: String): List<Block> = blockDao.forPage(pageId).map { it.toBlock() }

    /**
     * Creates a page. When [addRefInParent] is true, a link block to it is appended to the parent,
     * so it shows up inside the parent page as well as in the sidebar.
     */
    suspend fun createPage(
        parentId: String?,
        title: String = "",
        initialText: String? = null,
        addRefInParent: Boolean = true,
    ): String {
        val now = System.currentTimeMillis()
        val id = newId()
        db.withTransaction {
            pageDao.upsert(
                PageEntity(
                    id = id, parentId = parentId, title = title, icon = "",
                    isFavourite = false, inTrash = false, trashedAt = null,
                    createdAt = now, updatedAt = now, lastOpenedAt = now,
                ),
            )
            if (initialText != null) {
                blockDao.insertAll(
                    initialText.split('\n').mapIndexed { i, line ->
                        BlockEntity(newId(), id, i, BlockType.TEXT.name, line, false, null)
                    },
                )
            }
            if (parentId != null && addRefInParent) appendRef(parentId, id)
        }
        return id
    }

    private suspend fun appendRef(parentId: String, childId: String) {
        val position = blockDao.maxPosition(parentId) + 1
        blockDao.insert(BlockEntity(newId(), parentId, position, BlockType.PAGE.name, "", false, childId))
    }

    /** Replaces the page's blocks and title. Does nothing if the page was deleted meanwhile. */
    suspend fun savePage(pageId: String, title: String, blocks: List<Block>) {
        db.withTransaction {
            val page = pageDao.get(pageId) ?: return@withTransaction
            blockDao.deleteForPage(pageId)
            blockDao.insertAll(blocks.mapIndexed { i, b -> b.toEntity(pageId, i) })
            val now = System.currentTimeMillis()
            if (page.title != title) pageDao.setTitle(pageId, title, now) else pageDao.touchUpdated(pageId, now)
        }
    }

    suspend fun setIcon(id: String, icon: String) = pageDao.setIcon(id, icon, System.currentTimeMillis())

    suspend fun setFavourite(id: String, favourite: Boolean) = pageDao.setFavourite(id, favourite)

    suspend fun touchOpened(id: String) = pageDao.touchOpened(id, System.currentTimeMillis())

    /** Moves a page under a new parent (null = top level). Refuses to move a page into itself. */
    suspend fun movePage(id: String, newParentId: String?): Boolean {
        val all = pageDao.all()
        if (newParentId != null && newParentId in PageTree.subtreeIds(all, id)) return false
        db.withTransaction {
            blockDao.deleteChildRefs(id)
            pageDao.setParent(id, newParentId, System.currentTimeMillis())
            if (newParentId != null) appendRef(newParentId, id)
        }
        return true
    }

    /** Moves a page and everything nested in it to the trash. */
    suspend fun trashPage(id: String) {
        val ids = PageTree.subtreeIds(pageDao.all(), id).toList()
        db.withTransaction {
            pageDao.setTrashed(ids, true, System.currentTimeMillis())
            blockDao.deleteChildRefs(id)
        }
    }

    /** Restores a page and its subtree. If its parent is gone or trashed, it comes back at the top level. */
    suspend fun restorePage(id: String) {
        val all = pageDao.all()
        val page = all.firstOrNull { it.id == id } ?: return
        val parent = page.parentId?.let { pid -> all.firstOrNull { it.id == pid } }
        val ids = PageTree.subtreeIds(all, id).toList()
        db.withTransaction {
            pageDao.setTrashed(ids, false, null)
            if (parent == null || parent.inTrash) {
                blockDao.deleteChildRefs(id)
                pageDao.setParent(id, null, System.currentTimeMillis())
            } else if (blockDao.countChildRefs(id) == 0) {
                appendRef(parent.id, id)
            }
        }
    }

    suspend fun deleteForever(id: String) {
        deleteIds(PageTree.subtreeIds(pageDao.all(), id).toList())
    }

    suspend fun emptyTrash() {
        deleteIds(pageDao.all().filter { it.inTrash }.map { it.id })
    }

    private suspend fun deleteIds(ids: List<String>) {
        if (ids.isEmpty()) return
        db.withTransaction {
            ids.forEach { blockDao.deleteChildRefs(it) }
            pageDao.deleteIds(ids) // blocks of these pages go with them (foreign key cascade)
        }
    }

    suspend fun search(query: String): List<SearchHit> {
        val pattern = "%" + escapeLike(query) + "%"
        return pageDao.search(pattern).map { page ->
            val snippet = if (page.title.contains(query, ignoreCase = true)) null
            else blockDao.firstMatch(page.id, pattern)
            SearchHit(page, snippet)
        }
    }

}
