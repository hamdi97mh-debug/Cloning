package app.lembar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    indices = [Index("parentId"), Index("inTrash"), Index("lastOpenedAt")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    /** Parent page id, or null for a top-level page. */
    val parentId: String?,
    val title: String,
    /** An emoji, or empty for the default icon. */
    val icon: String,
    val isFavourite: Boolean,
    val inTrash: Boolean,
    val trashedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long,
)

@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pageId"), Index("childPageId")],
)
data class BlockEntity(
    @PrimaryKey val id: String,
    val pageId: String,
    val position: Int,
    val type: String,
    val text: String,
    val checked: Boolean,
    /** For PAGE blocks: the nested page this block links to. */
    val childPageId: String?,
)

fun BlockEntity.toBlock() = Block(id, BlockType.of(type), text, checked, childPageId)

fun Block.toEntity(pageId: String, position: Int) =
    BlockEntity(id, pageId, position, type.name, text, checked, childPageId)
