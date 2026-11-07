package app.lembar.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownExportTest {
    @Test fun exportsBlocks() {
        val blocks = listOf(
            Block("1", BlockType.H1, "Plan"),
            Block("2", BlockType.TODO, "buy milk", checked = true),
            Block("3", BlockType.TODO, "call mum"),
            Block("4", BlockType.NUMBERED, "one"),
            Block("5", BlockType.NUMBERED, "two"),
            Block("6", BlockType.DIVIDER),
            Block("7", BlockType.PAGE, childPageId = "c"),
        )
        val md = MarkdownExport.export("Week", blocks) { if (it == "c") "Child" else "?" }
        val expected = """
            # Week

            ## Plan

            - [x] buy milk
            - [ ] call mum
            1. one
            2. two

            ---

            📄 Child
        """.trimIndent() + "\n"
        assertEquals(expected, md)
    }

    @Test fun escapesLikePatterns() {
        assertEquals("50\\% off\\_now\\\\", escapeLike("50% off_now\\"))
    }
}
