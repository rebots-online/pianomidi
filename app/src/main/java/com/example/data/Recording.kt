package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class NoteEvent(
    val pitch: Int,          // MIDI note number (e.g. 60 = C4)
    val startMs: Long,       // Time offset in ms from start of recording
    val durationMs: Long,    // Hold duration in ms
    val velocity: Float      // Velocity/Touch intensity (0.0f - 1.0f)
)

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long,
    val bpm: Int,
    val noteEventsJson: String, // Serialized list of NoteEvents
    val durationMs: Long,
    val averageVelocity: Float,
    val rubatoScore: Float,     // Measure of micro-timing flexibility (0.0 = perfect grid, 1.0 = highly expressive)
    val voicingDensity: Float   // Average active notes played simultaneously
)

class RecordingTypeConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
    private val adapter = moshi.adapter<List<NoteEvent>>(listType)

    @TypeConverter
    fun fromNoteEventsList(value: List<NoteEvent>?): String {
        return adapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toNoteEventsList(value: String?): List<NoteEvent> {
        if (value.isNullOrEmpty()) return emptyList()
        return adapter.fromJson(value) ?: emptyList()
    }
}
