package app.lembar.data

import app.lembar.data.BlockType.CODE
import app.lembar.data.BlockType.DIVIDER
import app.lembar.data.BlockType.PAGE
import app.lembar.data.BlockType.TEXT

/**
 * Pure editing operations on a page's block list. Each returns the new list and
 * which block should get focus, with the cursor position inside it.
 */
object EditorOps {
    data class Result(val blocks: List<Block>, val focusId: String? = null, val cursor: Int = 0)

    private val prefixShortcuts = listOf(
        "### " to BlockType.H3,
        "## " to BlockType.H2,
        "# " to BlockType.H1,
        "[ ] " to BlockType.TODO,
        "[] " to BlockType.TODO,
        "- " to BlockType.BULLET,
        "* " to BlockType.BULLET,
        "> " to BlockType.QUOTE,
        "```" to CODE,
    )
    private val numberedPrefix = Regex("^\\d+\\. ")

    private fun List<Block>.indexOfId(id: String?) = if (id == null) -1 else indexOfFirst { it.id == id }
    private fun List<Block>.replaced(i: Int, b: Block) = toMutableList().also { it[i] = b }
    private fun List<Block>.inserted(i: Int, vararg b: Block) = toMutableList().also { it.addAll(i, b.toList()) }
    private fun List<Block>.removed(i: Int) = toMutableList().also { it.removeAt(i) }

    /** Markdown-style shortcuts typed at the start of a plain text block. */
    fun applyShortcut(blocks: List<Block>, id: String, text: String): Result? {
        val i = blocks.indexOfId(id)
        if (i < 0) return null
        val b = blocks[i]
        if (b.type != TEXT) return null
        if (text == "---") {
            val next = Block(newId(), TEXT)
            return Result(blocks.replaced(i, b.copy(type = DIVIDER, text = "")).inserted(i + 1, next), next.id, 0)
        }
        numberedPrefix.find(text)?.let { m ->
            return Result(blocks.replaced(i, b.copy(type = BlockType.NUMBERED, text = text.substring(m.value.length))), b.id, 0)
        }
        for ((prefix, type) in prefixShortcuts) {
            if (text.startsWith(prefix)) {
                return Result(blocks.replaced(i, b.copy(type = type, text = text.removePrefix(prefix))), b.id, 0)
            }
        }
        return null
    }

    /**
     * Enter pressed in block [id]: the text before the cursor stays, the rest moves into a new
     * block below. [after] may contain more line breaks (a paste); each line becomes a block.
     * Enter on an empty list item turns it back into plain text instead.
     */
    fun enter(blocks: List<Block>, id: String, before: String, after: String): Result {
        val i = blocks.indexOfId(id)
        if (i < 0) return Result(blocks)
        val b = blocks[i]
        if (b.type.continuesOnEnter && before.isEmpty() && after.isEmpty()) {
            return Result(blocks.replaced(i, b.copy(type = TEXT, checked = false)), b.id, 0)
        }
        val nextType = if (b.type.continuesOnEnter) b.type else TEXT
        val lines = after.split('\n')
        val created = lines.map { Block(newId(), nextType, it) }
        val out = blocks.replaced(i, b.copy(text = before)).inserted(i + 1, *created.toTypedArray())
        val last = created.last()
        return Result(out, last.id, if (lines.size > 1) last.text.length else 0)
    }

    /**
     * Backspace with the cursor at the start of block [id]. A styled block turns into plain text
     * first; a plain block merges into the one above. Returns null when there is nothing to do.
     */
    fun backspaceAtStart(blocks: List<Block>, id: String): Result? {
        val i = blocks.indexOfId(id)
        if (i < 0) return null
        val b = blocks[i]
        if (!b.type.isText) return null
        if (b.type != TEXT) return Result(blocks.replaced(i, b.copy(type = TEXT, checked = false)), b.id, 0)
        if (i == 0) return null
        val prev = blocks[i - 1]
        return when (prev.type) {
            DIVIDER -> Result(blocks.removed(i - 1), b.id, 0)
            PAGE -> if (b.text.isEmpty()) Result(blocks.removed(i)) else null
            else -> {
                val merged = prev.copy(text = prev.text + b.text)
                Result(blocks.replaced(i - 1, merged).removed(i), prev.id, prev.text.length)
            }
        }
    }

    /** Inserts an empty block of [type] after [afterId] (or at the end). A divider gets a text block after it. */
    fun insertAfter(blocks: List<Block>, afterId: String?, type: BlockType): Result {
        val i = blocks.indexOfId(afterId)
        val pos = if (i < 0) blocks.size else i + 1
        val created = Block(newId(), type)
        if (type == DIVIDER) {
            val textBlock = Block(newId(), TEXT)
            return Result(blocks.inserted(pos, created, textBlock), textBlock.id, 0)
        }
        return Result(blocks.inserted(pos, created), created.id, 0)
    }

    /** Changes a block's type. [clearText] is used when the change came from typing "/". */
    fun convert(blocks: List<Block>, id: String, type: BlockType, clearText: Boolean): Result {
        val i = blocks.indexOfId(id)
        if (i < 0) return Result(blocks)
        val b = blocks[i]
        val text = if (clearText || !type.isText) "" else b.text
        var out = blocks.replaced(i, b.copy(type = type, text = text, checked = if (type == b.type) b.checked else false))
        if (type == DIVIDER) {
            val next = out.getOrNull(i + 1)
            return if (next != null && next.type.isText) {
                Result(out, next.id, 0)
            } else {
                val textBlock = Block(newId(), TEXT)
                out = out.inserted(i + 1, textBlock)
                Result(out, textBlock.id, 0)
            }
        }
        return Result(out, b.id, text.length)
    }

    /** Turns block [id] into a link to a new child page, keeping a text block after it to continue typing. */
    fun convertToPage(blocks: List<Block>, id: String, childPageId: String): Result {
        val i = blocks.indexOfId(id)
        if (i < 0) return insertPageAfter(blocks, null, childPageId)
        var out = blocks.replaced(i, Block(blocks[i].id, PAGE, childPageId = childPageId))
        if (out.getOrNull(i + 1)?.type?.isText != true) out = out.inserted(i + 1, Block(newId(), TEXT))
        return Result(out)
    }

    fun insertPageAfter(blocks: List<Block>, afterId: String?, childPageId: String): Result {
        val i = blocks.indexOfId(afterId)
        val pos = if (i < 0) blocks.size else i + 1
        // An empty text block where the link goes is replaced rather than left dangling.
        if (i >= 0 && blocks[i].type == TEXT && blocks[i].text.isEmpty()) {
            return convertToPage(blocks, blocks[i].id, childPageId)
        }
        return Result(blocks.inserted(pos, Block(newId(), PAGE, childPageId = childPageId)))
    }

    fun move(blocks: List<Block>, id: String, delta: Int): Result {
        val i = blocks.indexOfId(id)
        val j = i + delta
        if (i < 0 || j !in blocks.indices) return Result(blocks)
        val out = blocks.toMutableList()
        out[i] = blocks[j]
        out[j] = blocks[i]
        return Result(out)
    }

    fun duplicate(blocks: List<Block>, id: String): Result {
        val i = blocks.indexOfId(id)
        if (i < 0 || blocks[i].type == PAGE) return Result(blocks)
        val copy = blocks[i].copy(id = newId())
        return Result(blocks.inserted(i + 1, copy), if (copy.type.isText) copy.id else null, copy.text.length)
    }

    /** Removes a block. A page always keeps at least one text block. */
    fun remove(blocks: List<Block>, id: String): Result {
        val i = blocks.indexOfId(id)
        if (i < 0) return Result(blocks)
        val out = blocks.removed(i)
        if (out.isEmpty()) {
            val b = Block(newId(), TEXT)
            return Result(listOf(b), b.id, 0)
        }
        val focus = (i - 1 downTo 0).firstOrNull { out[it].type.isText }?.let { out[it] }
        return Result(out, focus?.id, focus?.text?.length ?: 0)
    }

    /** The number shown in front of each numbered-list block (0 for other blocks). */
    fun listNumbers(blocks: List<Block>): IntArray {
        val out = IntArray(blocks.size)
        for (i in blocks.indices) {
            if (blocks[i].type == BlockType.NUMBERED) {
                out[i] = if (i > 0 && blocks[i - 1].type == BlockType.NUMBERED) out[i - 1] + 1 else 1
            }
        }
        return out
    }
}
