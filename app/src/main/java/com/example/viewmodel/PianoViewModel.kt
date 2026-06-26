package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PianoSynth
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.math.abs
import kotlin.math.roundToLong

class PianoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository
    val allRecordings: StateFlow<List<Recording>>

    val synth = PianoSynth()

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Metronome & Tempo
    private val _bpm = MutableStateFlow(100)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _isMetronomeOn = MutableStateFlow(false)
    val isMetronomeOn: StateFlow<Boolean> = _isMetronomeOn.asStateFlow()

    private val _currentMetronomeBeat = MutableStateFlow(0) // 0 to 3
    val currentMetronomeBeat: StateFlow<Int> = _currentMetronomeBeat.asStateFlow()

    private val _beatProgress = MutableStateFlow(0f) // 0.0f to 1.0f progress of current beat
    val beatProgress: StateFlow<Float> = _beatProgress.asStateFlow()

    private val _beatTriggerFlow = MutableSharedFlow<Long>()
    val beatTriggerFlow = _beatTriggerFlow.asSharedFlow()

    // Keyboard configuration
    private val _keyboardOctave = MutableStateFlow(4) // octave starting offset (e.g. 4 = C4 is middle C)
    val keyboardOctave: StateFlow<Int> = _keyboardOctave.asStateFlow()

    // Real-time state of keys currently active (either on-screen touch or playing back)
    private val _activeKeys = MutableStateFlow<Set<Int>>(emptySet())
    val activeKeys: StateFlow<Set<Int>> = _activeKeys.asStateFlow()

    // Synthesis settings
    private val _decayFactor = MutableStateFlow(0.5f)
    val decayFactor: StateFlow<Float> = _decayFactor.asStateFlow()

    private val _overtoneMix = MutableStateFlow(0.6f)
    val overtoneMix: StateFlow<Float> = _overtoneMix.asStateFlow()

    // Live Visualization Feeds
    private val _liveAmplitude = MutableStateFlow(0.0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val _liveActivePitches = MutableStateFlow<List<Int>>(emptyList())
    val liveActivePitches: StateFlow<List<Int>> = _liveActivePitches.asStateFlow()

    private val _liveRubatoDeviation = MutableStateFlow(0.0f) // -1.0 to 1.0 (early to late)
    val liveRubatoDeviation: StateFlow<Float> = _liveRubatoDeviation.asStateFlow()

    // Recording session temporary store
    private var recordingStartMs: Long = 0
    private val recordedNotes = Collections.synchronizedList(mutableListOf<NoteEvent>())
    // Keep track of which pitches are currently pressed during recording to calculate duration later
    private val activeRecordedNotes = mutableMapOf<Int, TempNote>()

    private data class TempNote(
        val pitch: Int,
        val startMs: Long,
        val velocity: Float
    )

    // Current playing back recording
    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedRecordingComments: StateFlow<List<Comment>> = _selectedRecording
        .flatMapLatest { recording ->
            if (recording != null) {
                repository.getCommentsForRecording(recording.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _playbackProgressMs = MutableStateFlow(0L)
    val playbackProgressMs: StateFlow<Long> = _playbackProgressMs.asStateFlow()

    // Comment MIDI snippet recording
    private val _isSnippetRecording = MutableStateFlow(false)
    val isSnippetRecording: StateFlow<Boolean> = _isSnippetRecording.asStateFlow()

    private val tempSnippetNotes = Collections.synchronizedList(mutableListOf<NoteEvent>())
    private val activeSnippetNotes = mutableMapOf<Int, TempNote>()
    private var snippetStartMs: Long = 0L

    // Comment MIDI snippet playback
    private var snippetPlaybackJob: Job? = null
    private val _isSnippetPlaying = MutableStateFlow<Long?>(null)
    val isSnippetPlaying: StateFlow<Long?> = _isSnippetPlaying.asStateFlow()

    private var metronomeJob: Job? = null
    private var beatTimerJob: Job? = null
    private var playbackJob: Job? = null

    val midiDeviceManager = com.example.midi.MidiDeviceManager(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecordingRepository(database.recordingDao(), database.commentDao())
        allRecordings = repository.allRecordings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Bind synth's audio frame output to live visualization flows
        synth.onAudioFrameCalculated = { amp, pitches ->
            _liveAmplitude.value = amp
            _liveActivePitches.value = pitches
        }

        // Bind MIDI Device Manager key events to synthesizer
        midiDeviceManager.onNoteOn = { pitch, velocity ->
            pianoKeyPress(pitch, velocity)
        }
        midiDeviceManager.onNoteOff = { pitch ->
            pianoKeyRelease(pitch)
        }
    }

    // Setters for Synthesis Parameters
    fun setDecayFactor(value: Float) {
        _decayFactor.value = value
        synth.decayFactor = value
    }

    fun setOvertoneMix(value: Float) {
        _overtoneMix.value = value
        synth.overtoneMix = value
    }

    fun setBpm(value: Int) {
        _bpm.value = value.coerceIn(40, 240)
        if (_isMetronomeOn.value || _isRecording.value) {
            restartMetronome()
        }
    }

    fun setKeyboardOctave(value: Int) {
        _keyboardOctave.value = value.coerceIn(1, 7)
    }

    fun setMetronomeEnabled(enabled: Boolean) {
        _isMetronomeOn.value = enabled
        synth.isMetronomeEnabled = enabled
        if (enabled) {
            restartMetronome()
        } else {
            stopMetronome()
        }
    }

    fun selectRecording(recording: Recording?) {
        _selectedRecording.value = recording
    }

    // Retriggers or resets metronome loop
    private fun restartMetronome() {
        stopMetronome()
        val intervalMs = (60000.0 / _bpm.value).toLong()

        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            var nextBeatTime = System.currentTimeMillis()
            while (isActive) {
                val isDownbeat = _currentMetronomeBeat.value == 0
                
                // Trigger actual sample click in piano synth
                synth.triggerMetronomeTick(isDownbeat)
                
                // Trigger sub-coroutine to interpolate 0.0 -> 1.0 beat progress visually
                triggerBeatProgressAnimation(intervalMs)

                _beatTriggerFlow.emit(System.currentTimeMillis())

                // Advance beat counter
                _currentMetronomeBeat.value = (_currentMetronomeBeat.value + 1) % 4

                nextBeatTime += intervalMs
                val sleepTime = nextBeatTime - System.currentTimeMillis()
                if (sleepTime > 0) {
                    delay(sleepTime)
                } else {
                    nextBeatTime = System.currentTimeMillis()
                    delay(10)
                }
            }
        }
    }

    private fun triggerBeatProgressAnimation(durationMs: Long) {
        beatTimerJob?.cancel()
        beatTimerJob = viewModelScope.launch(Dispatchers.Default) {
            val steps = 25
            val stepDelay = durationMs / steps
            for (step in 0..steps) {
                _beatProgress.value = step.toFloat() / steps.toFloat()
                delay(stepDelay)
            }
        }
    }

    private fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
        beatTimerJob?.cancel()
        beatTimerJob = null
        _beatProgress.value = 0f
        _currentMetronomeBeat.value = 0
    }

    // Keyboard Interactions
    fun pianoKeyPress(pitch: Int, velocity: Float) {
        // Play audio note
        synth.noteOn(pitch, velocity)
        _activeKeys.value = _activeKeys.value + pitch

        // If recording, log the start of the note
        if (_isRecording.value) {
            val relativeTime = System.currentTimeMillis() - recordingStartMs
            activeRecordedNotes[pitch] = TempNote(pitch, relativeTime, velocity)

            // Calculate live deviation from closest theoretical metronome beat
            val beatLengthMs = 60000.0 / _bpm.value
            val nearestBeatIndex = (relativeTime.toDouble() / beatLengthMs).roundToLong()
            val theoreticalBeatTime = nearestBeatIndex * beatLengthMs
            val deviationMs = relativeTime - theoreticalBeatTime
            // Normalize deviation range from -1.0 to 1.0 relative to half a beat window
            val normalizedDeviation = (deviationMs / (beatLengthMs / 2.0)).toFloat().coerceIn(-1.0f, 1.0f)
            _liveRubatoDeviation.value = normalizedDeviation
        }

        // If recording an illustrative snippet for a comment
        if (_isSnippetRecording.value) {
            val relativeTime = System.currentTimeMillis() - snippetStartMs
            activeSnippetNotes[pitch] = TempNote(pitch, relativeTime, velocity)
        }
    }

    fun pianoKeyRelease(pitch: Int) {
        synth.noteOff(pitch)
        _activeKeys.value = _activeKeys.value - pitch

        // If recording, finalize note duration
        if (_isRecording.value) {
            val relativeReleaseTime = System.currentTimeMillis() - recordingStartMs
            val tempNote = activeRecordedNotes.remove(pitch)
            if (tempNote != null) {
                val duration = relativeReleaseTime - tempNote.startMs
                recordedNotes.add(
                    NoteEvent(
                        pitch = tempNote.pitch,
                        startMs = tempNote.startMs,
                        durationMs = duration.coerceAtLeast(50L),
                        velocity = tempNote.velocity
                    )
                )
            }
        }

        // If recording snippet, finalize note duration
        if (_isSnippetRecording.value) {
            val relativeReleaseTime = System.currentTimeMillis() - snippetStartMs
            val tempNote = activeSnippetNotes.remove(pitch)
            if (tempNote != null) {
                val duration = relativeReleaseTime - tempNote.startMs
                tempSnippetNotes.add(
                    NoteEvent(
                        pitch = tempNote.pitch,
                        startMs = tempNote.startMs,
                        durationMs = duration.coerceAtLeast(50L),
                        velocity = tempNote.velocity
                    )
                )
            }
        }
    }

    // Recording Controls
    fun startRecording() {
        if (_isRecording.value || _isPlaying.value) return
        
        // Stop playback if any
        stopPlayback()

        recordedNotes.clear()
        activeRecordedNotes.clear()
        _liveRubatoDeviation.value = 0f

        // Ensure metronome is running when recording
        setMetronomeEnabled(true)

        _isRecording.value = true
        recordingStartMs = System.currentTimeMillis()
    }

    fun stopAndSaveRecording(title: String) {
        if (!_isRecording.value) return

        val recordingDurationMs = System.currentTimeMillis() - recordingStartMs
        _isRecording.value = false

        // Stop metronome if it was only turned on automatically
        setMetronomeEnabled(false)

        // Release any notes currently still held down
        val relativeReleaseTime = System.currentTimeMillis() - recordingStartMs
        activeRecordedNotes.forEach { (pitch, tempNote) ->
            val duration = relativeReleaseTime - tempNote.startMs
            recordedNotes.add(
                NoteEvent(
                    pitch = pitch,
                    startMs = tempNote.startMs,
                    durationMs = duration.coerceAtLeast(50L),
                    velocity = tempNote.velocity
                )
            )
        }
        activeRecordedNotes.clear()

        if (recordedNotes.isEmpty()) {
            // Nothing to save
            return
        }

        val notesList = recordedNotes.toList().sortedBy { it.startMs }

        // Analyze Rubato, Voicing, and Touch
        val avgVelocity = notesList.map { it.velocity }.average().toFloat()
        
        // Rubato Standard Deviation
        val bpmValue = _bpm.value
        val beatLengthMs = 60000.0 / bpmValue
        val deviations = notesList.map { note ->
            val nearestBeatIndex = (note.startMs.toDouble() / beatLengthMs).roundToLong()
            val theoreticalBeatTime = nearestBeatIndex * beatLengthMs
            abs(note.startMs - theoreticalBeatTime)
        }
        val avgDeviation = deviations.average()
        val variance = deviations.map { Math.pow(it - avgDeviation, 2.0) }.average()
        val standardDeviation = Math.sqrt(variance)

        // Normalize Standard Deviation to a 0.0 - 1.0 Rubato score
        // An expressively flexible but structurally disciplined pianist typically has a std dev between 30ms and 150ms.
        // Extremely robotic (quantized/perfect) is near 0. Complete chaotic timing is > 200ms.
        // Let's scale std dev such that 100ms is a full 0.8 rubato score
        val rubatoScore = (standardDeviation / 180.0).toFloat().coerceIn(0.0f, 1.0f)

        // Calculate voicing density (average active polyphonic notes at note starts)
        var voicingSum = 0
        for (note in notesList) {
            val activeAtStart = notesList.count { 
                it.startMs <= note.startMs && (it.startMs + it.durationMs) > note.startMs 
            }
            voicingSum += activeAtStart
        }
        val voicingDensity = if (notesList.isNotEmpty()) voicingSum.toFloat() / notesList.size else 1.0f

        viewModelScope.launch(Dispatchers.IO) {
            val moshi = Moshi.Builder().build()
            val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
            val adapter: JsonAdapter<List<NoteEvent>> = moshi.adapter(listType)
            val json = adapter.toJson(notesList)

            val session = Recording(
                title = title.ifBlank { "Performance #${System.currentTimeMillis() / 1000 % 10000}" },
                timestamp = System.currentTimeMillis(),
                bpm = bpmValue,
                noteEventsJson = json,
                durationMs = recordingDurationMs,
                averageVelocity = avgVelocity,
                rubatoScore = rubatoScore,
                voicingDensity = voicingDensity
            )

            val newId = repository.insertRecording(session)
            // Auto select new recording
            val savedRecording = session.copy(id = newId)
            _selectedRecording.value = savedRecording
        }
    }

    fun cancelRecording() {
        _isRecording.value = false
        recordedNotes.clear()
        activeRecordedNotes.clear()
        setMetronomeEnabled(false)
    }

    // Playback Controls
    fun startPlayback() {
        val recording = _selectedRecording.value ?: return
        if (_isPlaying.value || _isRecording.value) return

        _isPlaying.value = true
        _playbackProgressMs.value = 0L
        synth.releaseAll()

        // Sync BPM to saved recording BPM to play back in original meter
        setBpm(recording.bpm)
        setMetronomeEnabled(true)

        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
        val adapter: JsonAdapter<List<NoteEvent>> = moshi.adapter(listType)
        val notes = adapter.fromJson(recording.noteEventsJson) ?: emptyList()

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            val playbackStartTime = System.currentTimeMillis()
            var eventIndex = 0

            // Keep track of scheduled note-offs
            val activeNotesToTurnOff = mutableListOf<Pair<Long, Int>>()

            while (eventIndex < notes.size || activeNotesToTurnOff.isNotEmpty()) {
                if (!isActive) break

                val elapsed = System.currentTimeMillis() - playbackStartTime
                _playbackProgressMs.value = elapsed

                // 1. Check if any currently sounding notes should be turned off
                val noteOffsToExecute = activeNotesToTurnOff.filter { it.first <= elapsed }
                noteOffsToExecute.forEach {
                    synth.noteOff(it.second)
                    _activeKeys.value = _activeKeys.value - it.second
                    activeNotesToTurnOff.remove(it)
                }

                // 2. Play any note onsets that should start now
                while (eventIndex < notes.size && notes[eventIndex].startMs <= elapsed) {
                    val note = notes[eventIndex]
                    synth.noteOn(note.pitch, note.velocity)
                    _activeKeys.value = _activeKeys.value + note.pitch

                    // Schedule note-off
                    val offTime = note.startMs + note.durationMs
                    activeNotesToTurnOff.add(Pair(offTime, note.pitch))
                    
                    eventIndex++
                }

                delay(5) // High-precision polling loop
            }

            // Cleanup
            synth.releaseAll()
            _activeKeys.value = emptySet()
            _isPlaying.value = false
            _playbackProgressMs.value = 0L
            setMetronomeEnabled(false)
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        synth.releaseAll()
        _activeKeys.value = emptySet()
        _isPlaying.value = false
        _playbackProgressMs.value = 0L
        setMetronomeEnabled(false)
    }

    fun seekTo(positionMs: Long) {
        _playbackProgressMs.value = positionMs
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCommentsForRecording(recording.id)
            repository.deleteRecording(recording)
            if (_selectedRecording.value?.id == recording.id) {
                _selectedRecording.value = null
            }
        }
    }

    // Comment and Snippet interactions
    fun startSnippetRecording() {
        tempSnippetNotes.clear()
        activeSnippetNotes.clear()
        _isSnippetRecording.value = true
        snippetStartMs = System.currentTimeMillis()
    }

    fun stopSnippetRecording(): String? {
        if (!_isSnippetRecording.value) return null
        _isSnippetRecording.value = false

        val relativeReleaseTime = System.currentTimeMillis() - snippetStartMs
        activeSnippetNotes.forEach { (pitch, tempNote) ->
            val duration = relativeReleaseTime - tempNote.startMs
            tempSnippetNotes.add(
                NoteEvent(
                    pitch = tempNote.pitch,
                    startMs = tempNote.startMs,
                    durationMs = duration.coerceAtLeast(50L),
                    velocity = tempNote.velocity
                )
            )
        }
        activeSnippetNotes.clear()

        if (tempSnippetNotes.isEmpty()) return null

        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
        val adapter: JsonAdapter<List<NoteEvent>> = moshi.adapter(listType)
        val json = adapter.toJson(tempSnippetNotes.toList())
        tempSnippetNotes.clear()
        return json
    }

    fun cancelSnippetRecording() {
        _isSnippetRecording.value = false
        tempSnippetNotes.clear()
        activeSnippetNotes.clear()
    }

    fun addComment(sender: String, text: String, playbackPositionMs: Long, attachedMidiJson: String? = null) {
        val recordingId = _selectedRecording.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val comment = Comment(
                recordingId = recordingId,
                sender = sender,
                text = text,
                timestamp = System.currentTimeMillis(),
                playbackPositionMs = playbackPositionMs,
                attachedMidiJson = attachedMidiJson
            )
            repository.insertComment(comment)
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(comment)
        }
    }

    fun startSnippetPlayback(commentId: Long, snippetJson: String) {
        if (snippetPlaybackJob != null) {
            stopSnippetPlayback()
        }

        synth.releaseAll()
        _activeKeys.value = emptySet()
        _isSnippetPlaying.value = commentId

        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
        val adapter: JsonAdapter<List<NoteEvent>> = moshi.adapter(listType)
        val notes = adapter.fromJson(snippetJson) ?: return

        snippetPlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var eventIndex = 0
            val activeNotesToTurnOff = mutableListOf<Pair<Long, Int>>()

            while (eventIndex < notes.size || activeNotesToTurnOff.isNotEmpty()) {
                if (!isActive) break

                val elapsed = System.currentTimeMillis() - startTime

                val noteOffsToExecute = activeNotesToTurnOff.filter { it.first <= elapsed }
                noteOffsToExecute.forEach {
                    synth.noteOff(it.second)
                    _activeKeys.value = _activeKeys.value - it.second
                    activeNotesToTurnOff.remove(it)
                }

                while (eventIndex < notes.size && notes[eventIndex].startMs <= elapsed) {
                    val note = notes[eventIndex]
                    synth.noteOn(note.pitch, note.velocity)
                    _activeKeys.value = _activeKeys.value + note.pitch

                    val offTime = note.startMs + note.durationMs
                    activeNotesToTurnOff.add(Pair(offTime, note.pitch))
                    eventIndex++
                }

                delay(5)
            }

            synth.releaseAll()
            _activeKeys.value = emptySet()
            _isSnippetPlaying.value = null
            snippetPlaybackJob = null
        }
    }

    fun stopSnippetPlayback() {
        snippetPlaybackJob?.cancel()
        snippetPlaybackJob = null
        synth.releaseAll()
        _activeKeys.value = emptySet()
        _isSnippetPlaying.value = null
    }

    override fun onCleared() {
        super.onCleared()
        synth.stop()
        stopMetronome()
        stopSnippetPlayback()
    }
}

// Inline helper to import Moshi Types cleanly
object Types {
    fun newParameterizedType(rawType: java.lang.reflect.Type, vararg typeArguments: java.lang.reflect.Type): java.lang.reflect.Type {
        return com.squareup.moshi.Types.newParameterizedType(rawType, *typeArguments)
    }
}
