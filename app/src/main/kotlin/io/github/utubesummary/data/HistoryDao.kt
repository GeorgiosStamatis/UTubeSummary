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
         AND (:query = '' OR title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR lengthResults LIKE '%' || :query || '%')
         ORDER BY createdOn DESC
    """
    )
    fun getSummaries(query: String, type: SummaryType?): PagingSource<Int, HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(summaries: List<HistoryEntity>)

    @Query("SELECT sourceLink FROM history WHERE sourceLink IS NOT NULL")
    suspend fun getAllSourceLinks(): List<String>

    @Query("SELECT * FROM history WHERE sourceLink = :link ORDER BY createdOn DESC")
    suspend fun getAllBySourceLink(link: String): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE sourceText = :text ORDER BY createdOn DESC")
    suspend fun getAllBySourceText(text: String): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY createdOn DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}