package io.github.utubesummary.data

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import io.github.utubesummary.data.local.database.entity.HistoryEntity
import io.github.utubesummary.model.SummaryType

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface HistoryDao {
    @Query(
        """
         SELECT * FROM history 
         WHERE (:type IS NULL OR type = :type) 
         AND (:profileKey IS NULL OR variantKeys LIKE '%' || :profileKey || '%' ESCAPE '\')
         AND (:pinnedOnly = 0 OR pinned = 1)
         AND (:query = '' OR title LIKE '%' || :query || '%' ESCAPE '\' OR author LIKE '%' || :query || '%' ESCAPE '\' OR summary LIKE '%' || :query || '%' ESCAPE '\' OR lengthResults LIKE '%' || :query || '%' ESCAPE '\' OR alternatives LIKE '%' || :query || '%' ESCAPE '\')
         ORDER BY pinned DESC,
                  (CASE WHEN :sort = 1 THEN title END) COLLATE NOCASE ASC,
                  (CASE WHEN :sort = 2 THEN createdOn END) ASC,
                  createdOn DESC
    """
    )
    fun getSummaries(
        query: String,
        type: SummaryType?,
        profileKey: String?,
        pinnedOnly: Boolean,
        sort: Int,
    ): PagingSource<Int, HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(summaries: List<HistoryEntity>)

    @Query("SELECT * FROM history WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<HistoryEntity>

    @Query("SELECT sourceLink FROM history WHERE sourceLink IS NOT NULL")
    suspend fun getAllSourceLinks(): List<String>

    @Query("SELECT * FROM history WHERE sourceLink = :link ORDER BY createdOn DESC")
    suspend fun getAllBySourceLink(link: String): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE sourceText = :text ORDER BY createdOn DESC")
    suspend fun getAllBySourceText(text: String): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY createdOn DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("UPDATE history SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}