package app.lembar.data

/** Pure helpers for the page hierarchy. */
object PageTree {
    data class Row(val page: PageEntity, val depth: Int, val hasChildren: Boolean)

    /** The page and every page nested under it, at any depth. */
    fun subtreeIds(pages: List<PageEntity>, rootId: String): Set<String> {
        val byParent = pages.groupBy { it.parentId }
        val out = LinkedHashSet<String>()
        val stack = ArrayDeque(listOf(rootId))
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (out.add(id)) byParent[id]?.forEach { stack.addLast(it.id) }
        }
        return out
    }

    /** Active pages with no active parent. */
    fun roots(pages: List<PageEntity>): List<PageEntity> {
        val active = pages.filter { !it.inTrash }
        val ids = active.mapTo(HashSet()) { it.id }
        return active.filter { it.parentId == null || it.parentId !in ids }.sortedBy { it.createdAt }
    }

    /** Flattens active pages into rows for a tree view. Children of collapsed pages are skipped. */
    fun flatten(pages: List<PageEntity>, expanded: Set<String>, expandAll: Boolean = false): List<Row> {
        val active = pages.filter { !it.inTrash }
        val children = active.groupBy { it.parentId }.mapValues { (_, v) -> v.sortedBy { it.createdAt } }
        val out = ArrayList<Row>()
        val seen = HashSet<String>()
        fun visit(page: PageEntity, depth: Int) {
            if (!seen.add(page.id)) return
            val kids = children[page.id].orEmpty()
            out += Row(page, depth, kids.isNotEmpty())
            if (expandAll || page.id in expanded) kids.forEach { visit(it, depth + 1) }
        }
        roots(active).forEach { visit(it, 0) }
        return out
    }

    /** Trashed pages whose parent is not also in the trash: what the Trash screen lists. */
    fun topLevelTrash(pages: List<PageEntity>): List<PageEntity> {
        val trashed = pages.filter { it.inTrash }
        val trashedIds = trashed.mapTo(HashSet()) { it.id }
        return trashed
            .filter { it.parentId == null || it.parentId !in trashedIds }
            .sortedByDescending { it.trashedAt ?: 0L }
    }
}
