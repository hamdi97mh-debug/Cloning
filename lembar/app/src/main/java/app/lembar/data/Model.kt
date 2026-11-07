package app.lembar.data

import java.util.UUID

enum class BlockType {
    TEXT, H1, H2, H3, TODO, BULLET, NUMBERED, QUOTE, CALLOUT, CODE, DIVIDER, PAGE;

    /** Blocks that hold editable text. */
    val isText: Boolean get() = this != DIVIDER && this != PAGE

    /** List-like blocks: pressing Enter creates another block of the same type. */
    val continuesOnEnter: Boolean get() = this == TODO || this == BULLET || this == NUMBERED

    val isList: Boolean get() = continuesOnEnter

    companion object {
        fun of(name: String): BlockType = entries.firstOrNull { it.name == name } ?: TEXT
    }
}

/** One block as the editor sees it. Position is its index in the page's list. */
data class Block(
    val id: String,
    val type: BlockType,
    val text: String = "",
    val checked: Boolean = false,
    val childPageId: String? = null,
)

fun newId(): String = UUID.randomUUID().toString()

data class SearchHit(val page: PageEntity, val snippet: String?)

/** Escapes % and _ (and the escape character) for a LIKE pattern used with ESCAPE '\'. */
fun escapeLike(s: String): String =
    s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
