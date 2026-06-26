package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE recordingId = :recordingId ORDER BY playbackPositionMs ASC, timestamp ASC")
    fun getCommentsForRecording(recordingId: Long): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment): Long

    @Delete
    suspend fun deleteComment(comment: Comment)

    @Query("DELETE FROM comments WHERE recordingId = :recordingId")
    suspend fun deleteCommentsForRecording(recordingId: Long)
}
