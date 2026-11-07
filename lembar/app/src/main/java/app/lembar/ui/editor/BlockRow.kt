package app.lembar.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lembar.R
import app.lembar.data.Block
import app.lembar.data.BlockType
import app.lembar.data.PageEntity
import app.lembar.ui.common.PageIcon
import app.lembar.ui.common.pageTitle

class BlockCallbacks(
    val onText: (String, String) -> Unit,
    val onEnter: (String, String, String) -> Unit,
    val onBackspace: (String) -> Boolean,
    val onFocused: (String) -> Unit,
    val onToggle: (String) -> Unit,
    val onHandle: (String) -> Unit,
    val onOpenChild: (String) -> Unit,
    val onFocusConsumed: () -> Unit,
)

@Composable
fun BlockRow(
    block: Block,
    number: Int,
    readOnly: Boolean,
    focusRequest: EditorViewModel.FocusRequest?,
    child: PageEntity?,
    cb: BlockCallbacks,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.Top) {
        // Handle: opens the block actions sheet.
        Box(
            Modifier
                .padding(top = if (block.type == BlockType.H1) 10.dp else 4.dp)
                .size(width = 32.dp, height = 28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = !readOnly) { cb.onHandle(block.id) },
            contentAlignment = Alignment.Center,
        ) {
            if (!readOnly) {
                Icon(
                    Icons.Filled.DragIndicator,
                    stringResource(R.string.block_actions),
                    Modifier.size(18.dp),
                    tint = colors.onSurface.copy(alpha = 0.3f),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            val field: @Composable (TextStyle, String, Modifier) -> Unit = { style, placeholder, mod ->
                BlockTextField(block, style, placeholder, readOnly, focusRequest, cb, mod)
            }
            when (block.type) {
                BlockType.DIVIDER -> HorizontalDivider(Modifier.padding(vertical = 17.dp))
                BlockType.PAGE -> ChildPageLink(child) { block.childPageId?.let(cb.onOpenChild) }
                BlockType.H1 -> field(
                    type.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    stringResource(R.string.block_h1),
                    Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                BlockType.H2 -> field(
                    type.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    stringResource(R.string.block_h2),
                    Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                BlockType.H3 -> field(
                    type.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    stringResource(R.string.block_h3),
                    Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                BlockType.TODO -> Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !readOnly) { cb.onToggle(block.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (block.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            stringResource(R.string.to_do_done),
                            Modifier.size(22.dp),
                            tint = if (block.checked) colors.primary else colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    val style = if (block.checked) {
                        type.bodyLarge.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = colors.onSurface.copy(alpha = 0.5f),
                        )
                    } else type.bodyLarge
                    field(style, stringResource(R.string.placeholder_todo), Modifier.padding(vertical = 4.dp))
                }
                BlockType.BULLET -> Row(verticalAlignment = Alignment.Top) {
                    Text("•", style = type.bodyLarge, modifier = Modifier.width(22.dp).padding(start = 6.dp, top = 4.dp))
                    field(type.bodyLarge, stringResource(R.string.placeholder_list), Modifier.padding(vertical = 4.dp))
                }
                BlockType.NUMBERED -> Row(verticalAlignment = Alignment.Top) {
                    Text("$number.", style = type.bodyLarge, modifier = Modifier.width(28.dp).padding(start = 2.dp, top = 4.dp))
                    field(type.bodyLarge, stringResource(R.string.placeholder_list), Modifier.padding(vertical = 4.dp))
                }
                BlockType.QUOTE -> {
                    val bar = colors.onSurface
                    field(
                        type.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        stringResource(R.string.placeholder_quote),
                        Modifier
                            .padding(vertical = 4.dp)
                            .drawBehind { drawRect(bar, size = Size(3.dp.toPx(), size.height)) }
                            .padding(start = 14.dp),
                    )
                }
                BlockType.CALLOUT -> Row(
                    Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.secondaryContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("💡", style = type.bodyLarge)
                    Spacer(Modifier.width(10.dp))
                    field(
                        type.bodyLarge.copy(color = colors.onSecondaryContainer),
                        stringResource(R.string.placeholder_callout),
                        Modifier,
                    )
                }
                BlockType.CODE -> field(
                    type.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    stringResource(R.string.placeholder_code),
                    Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant)
                        .padding(12.dp),
                )
                BlockType.TEXT -> field(type.bodyLarge, stringResource(R.string.placeholder_text), Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ChildPageLink(child: PageEntity?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageIcon(child?.icon.orEmpty())
        Spacer(Modifier.width(8.dp))
        Text(
            if (child == null) stringResource(R.string.page_missing) else pageTitle(child),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BlockTextField(
    block: Block,
    style: TextStyle,
    placeholder: String,
    readOnly: Boolean,
    focusRequest: EditorViewModel.FocusRequest?,
    cb: BlockCallbacks,
    modifier: Modifier,
) {
    var value by remember(block.id) { mutableStateOf(TextFieldValue(block.text, TextRange(block.text.length))) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }

    // The view model changed the text (merge, shortcut, undo): adopt it.
    LaunchedEffect(block.text) {
        if (value.text != block.text) value = TextFieldValue(block.text, TextRange(block.text.length))
    }
    LaunchedEffect(focusRequest) {
        if (focusRequest != null && focusRequest.blockId == block.id) {
            val cursor = focusRequest.cursor.coerceIn(0, block.text.length)
            value = TextFieldValue(block.text, TextRange(cursor))
            runCatching { requester.requestFocus() }
            cb.onFocusConsumed()
        }
    }

    val textColor = if (style.color.isSpecified()) style.color else MaterialTheme.colorScheme.onSurface
    val multiline = block.type == BlockType.CODE
    BasicTextField(
        value = value,
        onValueChange = { v ->
            if (!multiline && v.text.contains('\n')) {
                val i = v.text.indexOf('\n')
                cb.onEnter(block.id, v.text.substring(0, i), v.text.substring(i + 1))
            } else {
                value = v
                if (v.text != block.text) cb.onText(block.id, v.text)
            }
        },
        readOnly = readOnly,
        textStyle = style.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) cb.onFocused(block.id)
            }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace &&
                    value.selection.collapsed && value.selection.start == 0
                ) {
                    cb.onBackspace(block.id)
                } else {
                    false
                }
            },
        decorationBox = { inner ->
            Box {
                val showHint = value.text.isEmpty() && !readOnly && (focused || block.type != BlockType.TEXT)
                if (showHint) {
                    Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)))
                }
                inner()
            }
        },
    )
}

private fun androidx.compose.ui.graphics.Color.isSpecified() = this != androidx.compose.ui.graphics.Color.Unspecified
