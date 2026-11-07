package app.lembar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages ORDER BY createdAt")
    fun observeAll(): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    fun observe(id: String): Flow<PageEntity?>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun get(id: String): PageEntity?

    @Query("SELECT * FROM pages")
    suspend fun all(): List<PageEntity>

    @Upsert
    suspend fun upsert(page: PageEntity)

    @Query("UPDATE pages SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun setTitle(id: String, title: String, now: Long)

    @Query("UPDATE pages SET icon = :icon, updatedAt = :now WHERE id = :id")
    suspend fun setIcon(id: String, icon: String, now: Long)

    @Query("UPDATE pages SET isFavourite = :favourite WHERE id = :id")
    suspend fun setFavourite(id: String, favourite: Boolean)

    @Query("UPDATE pages SET lastOpenedAt = :now WHERE id = :id")
    suspend fun touchOpened(id: String, now: Long)

    @Query("UPDATE pages SET updatedAt = :now WHERE id = :id")
    suspend fun touchUpdated(id: String, now: Long)

    @Query("UPDATE pages SET parentId = :parentId, updatedAt = :now WHERE id = :id")
    suspend fun setParent(id: String, parentId: String?, now: Long)

    @Query("UPDATE pages SET inTrash = :inTrash, trashedAt = :at WHERE id IN (:ids)")
    suspend fun setTrashed(ids: List<String>, inTrash: Boolean, at: Long?)

    @Query("DELETE FROM pages WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<String>)

    @Query(
        """
        SELECT * FROM pages
        WHERE inTrash = 0 AND (
            title LIKE :pattern ESCAPE '\'
            OR id IN (SELECT pageId FROM blocks WHERE text LIKE :pattern ESCAPE '\')
        )
        ORDER BY updatedAt DESC
        LIMIT 50
        """,
    )
    suspend fun search(pattern: String): List<PageEntity>
}

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY position")
    suspend fun forPage(pageId: String): List<BlockEntity>

    @Query("DELETE FROM blocks WHERE pageId = :pageId")
    suspend fun deleteForPage(pageId: String)

    @Insert
    suspend fun insertAll(blocks: List<BlockEntity>)

    @Insert
    suspend fun insert(block: BlockEntity)

    @Query("SELECT COALESCE(MAX(position), -1) FROM blocks WHERE pageId = :pageId")
    suspend fun maxPosition(pageId: String): Int

    @Query("DELETE FROM blocks WHERE childPageId = :childPageId")
    suspend fun deleteChildRefs(childPageId: String)

    @Query("SELECT COUNT(*) FROM blocks WHERE childPageId = :childPageId")
    suspend fun countChildRefs(childPageId: String): Int

    @Query("SELECT text FROM blocks WHERE pageId = :pageId AND text LIKE :pattern ESCAPE '\\' LIMIT 1")
    suspend fun firstMatch(pageId: String, pattern: String): String?
}
