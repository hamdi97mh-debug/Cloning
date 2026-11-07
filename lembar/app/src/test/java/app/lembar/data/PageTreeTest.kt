package app.lembar.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTreeTest {
    private var clock = 0L
    private fun p(id: String, parent: String? = null, trash: Boolean = false) = PageEntity(
        id = id, parentId = parent, title = id, icon = "", isFavourite = false,
        inTrash = trash, trashedAt = if (trash) clock else null,
        createdAt = clock++, updatedAt = 0, lastOpenedAt = 0,
    )

    @Test fun subtreeIncludesAllDescendants() {
        val pages = listOf(p("a"), p("b", "a"), p("c", "b"), p("d"))
        assertEquals(setOf("a", "b", "c"), PageTree.subtreeIds(pages, "a"))
    }

    @Test fun subtreeSurvivesACycle() {
        val pages = listOf(p("a", "b"), p("b", "a"))
        assertEquals(setOf("a", "b"), PageTree.subtreeIds(pages, "a"))
    }

    @Test fun flattenRespectsExpandedState() {
        val pages = listOf(p("a"), p("b", "a"), p("c", "b"), p("d"))
        assertEquals(listOf("a", "d"), PageTree.flatten(pages, emptySet()).map { it.page.id })
        assertEquals(listOf("a", "b", "d"), PageTree.flatten(pages, setOf("a")).map { it.page.id })
        val all = PageTree.flatten(pages, emptySet(), expandAll = true)
        assertEquals(listOf("a", "b", "c", "d"), all.map { it.page.id })
        assertEquals(listOf(0, 1, 2, 0), all.map { it.depth })
        assertEquals(listOf(true, true, false, false), all.map { it.hasChildren })
    }

    @Test fun trashedPagesAreHiddenFromTree() {
        val pages = listOf(p("a"), p("b", "a", trash = true))
        assertEquals(listOf("a"), PageTree.flatten(pages, emptySet(), expandAll = true).map { it.page.id })
    }

    @Test fun trashListsOnlyTopLevelTrashedPages() {
        val pages = listOf(p("a", trash = true), p("b", "a", trash = true), p("c"), p("d", "c", trash = true))
        assertEquals(setOf("a", "d"), PageTree.topLevelTrash(pages).map { it.id }.toSet())
    }
}
