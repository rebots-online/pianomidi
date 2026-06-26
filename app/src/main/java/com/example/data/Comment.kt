package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val sender: String, // "Teacher" or "Student"
    val text: String,
    val timestamp: Long, // Real-world datestamp
    val playbackPositionMs: Long, // Position inside the audio/MIDI piece
    val attachedMidiJson: String? = null // Optional serialized NoteEvents to illustrate a point
)
