package com.example.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class MyMidiDevice {
    abstract val id: String
    abstract val name: String
    abstract val type: String // "USB" or "BLE" or "VIRTUAL"

    data class UsbDevice(val info: MidiDeviceInfo) : MyMidiDevice() {
        override val id: String = "usb_${info.id}"
        override val name: String = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB MIDI Device"
        override val type: String = "USB"
    }

    data class BleDevice(val device: BluetoothDevice, val displayName: String) : MyMidiDevice() {
        override val id: String = "ble_${device.address}"
        override val name: String = displayName
        override val type: String = "BLE"
    }

    object VirtualDevice : MyMidiDevice() {
        override val id: String = "virtual_loopback"
        override val name: String = "Virtual Loopback Piano (Simulator)"
        override val type: String = "VIRTUAL"
    }
}

class MidiDeviceManager(private val context: Context) {

    private val tag = "MidiDeviceManager"

    private val midiManager: MidiManager? by lazy {
        try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
                context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get MidiManager", e)
            null
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } catch (e: Exception) {
            Log.e(tag, "Failed to get BluetoothAdapter", e)
            null
        }
    }

    private val _discoveredDevices = MutableStateFlow<List<MyMidiDevice>>(listOf(MyMidiDevice.VirtualDevice))
    val discoveredDevices: StateFlow<List<MyMidiDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<MyMidiDevice?>(null)
    val connectedDevice: StateFlow<MyMidiDevice?> = _connectedDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow("DISCONNECTED") // "DISCONNECTED", "CONNECTING", "CONNECTED"
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var openDeviceInstance: MidiDevice? = null
    private var openOutputPort: android.media.midi.MidiOutputPort? = null
    private var bleScanCallback: ScanCallback? = null
    private var isScanning = false

    private val mainHandler = Handler(Looper.getMainLooper())

    // Callback invoked when a MIDI key press or release is received
    var onNoteOn: ((pitch: Int, velocity: Float) -> Unit)? = null
    var onNoteOff: ((pitch: Int) -> Unit)? = null

    init {
        registerUsbCallbacks()
        refreshUsbDevices()
    }

    private fun registerUsbCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                midiManager?.registerDeviceCallback(object : MidiManager.DeviceCallback() {
                    override fun onDeviceAdded(device: MidiDeviceInfo) {
                        refreshUsbDevices()
                    }

                    override fun onDeviceRemoved(device: MidiDeviceInfo) {
                        refreshUsbDevices()
                        val current = _connectedDevice.value
                        if (current is MyMidiDevice.UsbDevice && current.info.id == device.id) {
                            disconnect()
                        }
                    }
                }, mainHandler)
            } catch (e: Exception) {
                Log.e(tag, "Error registering device callbacks", e)
            }
        }
    }

    fun refreshUsbDevices() {
        try {
            val usbDevicesList = midiManager?.devices?.map { MyMidiDevice.UsbDevice(it) } ?: emptyList()
            val currentList = _discoveredDevices.value.filter { it !is MyMidiDevice.UsbDevice }
            _discoveredDevices.value = listOf(MyMidiDevice.VirtualDevice) + usbDevicesList + currentList.filter { it != MyMidiDevice.VirtualDevice }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing USB devices", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScanning() {
        if (isScanning) return
        val adapter = bluetoothAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return

        _connectionStatus.value = "SCANNING BLE..."

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = device.name ?: "Unknown MIDI Instrument"
                
                // Add BLE device if not already present
                val currentList = _discoveredDevices.value
                val exists = currentList.any { it is MyMidiDevice.BleDevice && it.device.address == device.address }
                if (!exists) {
                    val bleDev = MyMidiDevice.BleDevice(device, name)
                    _discoveredDevices.value = currentList + bleDev
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "BLE Scan failed: $errorCode")
                isScanning = false
            }
        }

        bleScanCallback = callback
        isScanning = true

        try {
            scanner.startScan(callback)
            // Auto stop scanning after 15 seconds to save power
            mainHandler.postDelayed({
                stopBleScanning()
            }, 15000)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start BLE scan", e)
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScanning() {
        if (!isScanning) return
        val adapter = bluetoothAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        val callback = bleScanCallback ?: return

        try {
            scanner.stopScan(callback)
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop BLE scan", e)
        }
        bleScanCallback = null
        isScanning = false
        if (_connectionStatus.value == "SCANNING BLE...") {
            _connectionStatus.value = "DISCONNECTED"
        }
    }

    fun connectToDevice(device: MyMidiDevice) {
        disconnect()
        _connectionStatus.value = "CONNECTING..."
        _connectedDevice.value = device

        when (device) {
            is MyMidiDevice.VirtualDevice -> {
                _connectionStatus.value = "CONNECTED"
            }
            is MyMidiDevice.UsbDevice -> {
                try {
                    midiManager?.openDevice(device.info, { midiDev ->
                        if (midiDev != null) {
                            openDeviceInstance = midiDev
                            // Open first input port / output port to receive events
                            val portInfo = device.info.ports
                            val outputPortInfo = portInfo.find { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
                            if (outputPortInfo != null) {
                                val outPort = midiDev.openOutputPort(outputPortInfo.portNumber)
                                if (outPort != null) {
                                    openOutputPort = outPort
                                    outPort.connect(MidiInputReceiver())
                                    _connectionStatus.value = "CONNECTED"
                                    Log.d(tag, "Successfully connected to USB MIDI output port: ${outputPortInfo.portNumber}")
                                } else {
                                    _connectionStatus.value = "PORT_OPEN_FAILED"
                                }
                            } else {
                                _connectionStatus.value = "NO_OUTPUT_PORT"
                            }
                        } else {
                            _connectionStatus.value = "FAILED"
                        }
                    }, mainHandler)
                } catch (e: Exception) {
                    Log.e(tag, "Error opening USB MIDI device", e)
                    _connectionStatus.value = "ERROR"
                }
            }
            is MyMidiDevice.BleDevice -> {
                try {
                    midiManager?.openBluetoothDevice(device.device, { midiDev ->
                        if (midiDev != null) {
                            openDeviceInstance = midiDev
                            val portInfo = midiDev.info.ports
                            val outputPortInfo = portInfo.find { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
                            if (outputPortInfo != null) {
                                val outPort = midiDev.openOutputPort(outputPortInfo.portNumber)
                                if (outPort != null) {
                                    openOutputPort = outPort
                                    outPort.connect(MidiInputReceiver())
                                    _connectionStatus.value = "CONNECTED"
                                    Log.d(tag, "Successfully connected to BLE MIDI output port")
                                } else {
                                    _connectionStatus.value = "PORT_OPEN_FAILED"
                                }
                            } else {
                                _connectionStatus.value = "NO_OUTPUT_PORT"
                            }
                        } else {
                            _connectionStatus.value = "FAILED"
                        }
                    }, mainHandler)
                } catch (e: Exception) {
                    Log.e(tag, "Error opening BLE MIDI device", e)
                    _connectionStatus.value = "ERROR"
                }
            }
        }
    }

    fun disconnect() {
        try {
            openOutputPort?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing port", e)
        }
        try {
            openDeviceInstance?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing device", e)
        }
        openOutputPort = null
        openDeviceInstance = null
        _connectedDevice.value = null
        _connectionStatus.value = "DISCONNECTED"
    }

    // Direct loopback trigger for simulated events
    fun triggerVirtualNoteOn(pitch: Int, velocity: Float) {
        if (_connectedDevice.value is MyMidiDevice.VirtualDevice) {
            onNoteOn?.invoke(pitch, velocity)
        }
    }

    fun triggerVirtualNoteOff(pitch: Int) {
        if (_connectedDevice.value is MyMidiDevice.VirtualDevice) {
            onNoteOff?.invoke(pitch)
        }
    }

    // MIDI Receiver that decodes standard Note On and Note Off status bytes
    private inner class MidiInputReceiver : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            val end = offset + count
            while (i < end) {
                val status = msg[i].toInt() and 0xFF
                if (status >= 0x80) {
                    val command = status and 0xF0
                    // Note On message has status 0x90 to 0x9F (depending on channel)
                    if (command == 0x90 && i + 2 < end) {
                        val pitch = msg[i + 1].toInt() and 0x7F
                        val velocity = msg[i + 2].toInt() and 0x7F
                        if (velocity > 0) {
                            val normalizedVelocity = velocity.toFloat() / 127f
                            mainHandler.post {
                                onNoteOn?.invoke(pitch, normalizedVelocity)
                            }
                        } else {
                            mainHandler.post {
                                onNoteOff?.invoke(pitch)
                            }
                        }
                        i += 3
                    }
                    // Note Off message has status 0x80 to 0x8F (depending on channel)
                    else if (command == 0x80 && i + 2 < end) {
                        val pitch = msg[i + 1].toInt() and 0x7F
                        mainHandler.post {
                            onNoteOff?.invoke(pitch)
                        }
                        i += 3
                    } else {
                        // Advance to process subsequent bytes
                        i++
                    }
                } else {
                    i++
                }
            }
        }
    }
}
