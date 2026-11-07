package app.lembar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorOpsTest {
    private fun b(id: String, type: BlockType = BlockType.TEXT, text: String = "", childPageId: String? = null) =
        Block(id, type, text, childPageId = childPageId)

    @Test fun enterSplitsTextAtCursor() {
        val r = EditorOps.enter(listOf(b("a", text = "helloworld")), "a", "hello", "world")
        assertEquals(listOf("hello", "world"), r.blocks.map { it.text })
        assertEquals(r.blocks[1].id, r.focusId)
        assertEquals(0, r.cursor)
    }

    @Test fun enterContinuesListsButNotHeadings() {
        val todo = EditorOps.enter(listOf(b("a", BlockType.TODO, "x")), "a", "x", "")
        assertEquals(BlockType.TODO, todo.blocks[1].type)
        val heading = EditorOps.enter(listOf(b("a", BlockType.H1, "x")), "a", "x", "")
        assertEquals(BlockType.TEXT, heading.blocks[1].type)
    }

    @Test fun enterOnEmptyListItemEndsTheList() {
        val r = EditorOps.enter(listOf(b("a", BlockType.BULLET)), "a", "", "")
        assertEquals(1, r.blocks.size)
        assertEquals(BlockType.TEXT, r.blocks[0].type)
    }

    @Test fun pastedLinesBecomeBlocks() {
        val r = EditorOps.enter(listOf(b("a", text = "one")), "a", "one", "two\nthree")
        assertEquals(listOf("one", "two", "three"), r.blocks.map { it.text })
        assertEquals(r.blocks[2].id, r.focusId)
        assertEquals(5, r.cursor)
    }

    @Test fun backspaceMergesIntoPrevious() {
        val r = EditorOps.backspaceAtStart(listOf(b("a", text = "foo"), b("b", text = "bar")), "b")!!
        assertEquals(listOf("foobar"), r.blocks.map { it.text })
        assertEquals("a", r.focusId)
        assertEquals(3, r.cursor)
    }

    @Test fun backspaceOnStyledBlockTurnsItIntoText() {
        val r = EditorOps.backspaceAtStart(listOf(b("a", BlockType.H2, "x")), "a")!!
        assertEquals(BlockType.TEXT, r.blocks[0].type)
        assertEquals("x", r.blocks[0].text)
    }

    @Test fun backspaceOnFirstPlainBlockDoesNothing() {
        assertNull(EditorOps.backspaceAtStart(listOf(b("a", text = "x")), "a"))
    }

    @Test fun backspaceAfterDividerRemovesDivider() {
        val r = EditorOps.backspaceAtStart(listOf(b("d", BlockType.DIVIDER), b("a", text = "x")), "a")!!
        assertEquals(listOf("a"), r.blocks.map { it.id })
    }

    @Test fun backspaceNeverMergesIntoAPageLink() {
        val blocks = listOf(b("p", BlockType.PAGE, childPageId = "c"), b("a", text = "x"))
        assertNull(EditorOps.backspaceAtStart(blocks, "a"))
    }

    @Test fun markdownShortcuts() {
        fun shortcut(text: String) = EditorOps.applyShortcut(listOf(b("a")), "a", text)?.blocks?.get(0)
        assertEquals(BlockType.H1, shortcut("# ")?.type)
        assertEquals(BlockType.H2, shortcut("## ")?.type)
        assertEquals(BlockType.H3, shortcut("### ")?.type)
        assertEquals(BlockType.BULLET, shortcut("- ")?.type)
        assertEquals(BlockType.TODO, shortcut("[] ")?.type)
        assertEquals(BlockType.NUMBERED, shortcut("12. ")?.type)
        assertEquals(BlockType.QUOTE, shortcut("> hi")?.type)
        assertEquals("hi", shortcut("> hi")?.text)
        assertNull(shortcut("#hashtag"))
        assertNull(shortcut("hello"))
    }

    @Test fun shortcutOnlyAppliesToPlainText() {
        assertNull(EditorOps.applyShortcut(listOf(b("a", BlockType.CODE)), "a", "# "))
    }

    @Test fun threeDashesMakeADividerAndNewLine() {
        val r = EditorOps.applyShortcut(listOf(b("a")), "a", "---")!!
        assertEquals(listOf(BlockType.DIVIDER, BlockType.TEXT), r.blocks.map { it.type })
        assertEquals(r.blocks[1].id, r.focusId)
    }

    @Test fun convertKeepsTextUnlessSlash() {
        val kept = EditorOps.convert(listOf(b("a", text = "x")), "a", BlockType.QUOTE, clearText = false)
        assertEquals("x", kept.blocks[0].text)
        val cleared = EditorOps.convert(listOf(b("a", text = "/")), "a", BlockType.QUOTE, clearText = true)
        assertEquals("", cleared.blocks[0].text)
    }

    @Test fun convertToPageKeepsATextBlockAfter() {
        val r = EditorOps.convertToPage(listOf(b("a", text = "/")), "a", "child")
        assertEquals(listOf(BlockType.PAGE, BlockType.TEXT), r.blocks.map { it.type })
        assertEquals("child", r.blocks[0].childPageId)
    }

    @Test fun moveSwapsAndStopsAtEdges() {
        val blocks = listOf(b("a"), b("b"), b("c"))
        assertEquals(listOf("b", "a", "c"), EditorOps.move(blocks, "a", 1).blocks.map { it.id })
        assertEquals(listOf("a", "b", "c"), EditorOps.move(blocks, "a", -1).blocks.map { it.id })
    }

    @Test fun removingLastBlockLeavesAnEmptyOne() {
        val r = EditorOps.remove(listOf(b("a", text = "x")), "a")
        assertEquals(1, r.blocks.size)
        assertTrue(r.blocks[0].text.isEmpty())
    }

    @Test fun duplicateGetsNewId() {
        val r = EditorOps.duplicate(listOf(b("a", text = "x")), "a")
        assertEquals(2, r.blocks.size)
        assertTrue(r.blocks[0].id != r.blocks[1].id)
        assertEquals("x", r.blocks[1].text)
    }

    @Test fun numberingRestartsAfterOtherBlocks() {
        val blocks = listOf(
            b("1", BlockType.NUMBERED), b("2", BlockType.NUMBERED), b("t"),
            b("3", BlockType.NUMBERED),
        )
        assertEquals(listOf(1, 2, 0, 1), EditorOps.listNumbers(blocks).toList())
    }
}
