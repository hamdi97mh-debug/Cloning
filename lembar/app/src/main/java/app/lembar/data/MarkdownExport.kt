package app.lembar.data

object MarkdownExport {
    fun export(title: String, blocks: List<Block>, titleOf: (String) -> String): String = buildString {
        if (title.isNotBlank()) append("# ").append(title.trim()).append("\n\n")
        var number = 0
        blocks.forEachIndexed { i, b ->
            number = if (b.type == BlockType.NUMBERED) number + 1 else 0
            val line = when (b.type) {
                BlockType.TEXT -> b.text
                BlockType.H1 -> "## ${b.text}"
                BlockType.H2 -> "### ${b.text}"
                BlockType.H3 -> "#### ${b.text}"
                BlockType.TODO -> "- [${if (b.checked) "x" else " "}] ${b.text}"
                BlockType.BULLET -> "- ${b.text}"
                BlockType.NUMBERED -> "$number. ${b.text}"
                BlockType.QUOTE -> b.text.lines().joinToString("\n") { "> $it" }
                BlockType.CALLOUT -> "> 💡 ${b.text}"
                BlockType.CODE -> "```\n${b.text}\n```"
                BlockType.DIVIDER -> "---"
                BlockType.PAGE -> "📄 ${b.childPageId?.let(titleOf).orEmpty()}"
            }
            append(line)
            val next = blocks.getOrNull(i + 1)
            if (next != null) append(if (b.type.isList && next.type.isList) "\n" else "\n\n")
        }
        append('\n')
    }
}
