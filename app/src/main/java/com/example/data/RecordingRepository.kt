package com.example.data

import kotlinx.coroutines.flow.Flow

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val commentDao: CommentDao
) {
    val allRecordings: Flow<List<Recording>> = recordingDao.getAllRecordings()

    suspend fun getRecordingById(id: Long): Recording? {
        return recordingDao.getRecordingById(id)
    }

    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun deleteRecording(recording: Recording) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteAllRecordings() {
        recordingDao.deleteAllRecordings()
    }

    // Comment operations
    fun getCommentsForRecording(recordingId: Long): Flow<List<Comment>> {
        return commentDao.getCommentsForRecording(recordingId)
    }

    suspend fun insertComment(comment: Comment): Long {
        return commentDao.insertComment(comment)
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteComment(comment)
    }

    suspend fun deleteCommentsForRecording(recordingId: Long) {
        commentDao.deleteCommentsForRecording(recordingId)
    }
}
