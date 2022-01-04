package com.laser.scanner.ui.activity.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.laser.scanner.R
import com.laser.scanner.contract.FILE_TYPE_LOG
import com.laser.scanner.contract.generateExtractFile
import com.laser.scanner.data.dao.BleDeviceDao
import com.laser.scanner.data.dao.BleRecordDao
import com.laser.scanner.data.model.*
import com.laser.scanner.event.EVENT_HAS_NEW_CONNECTED_DEVICE
import com.laser.scanner.event.EVENT_HAS_NEW_HISTORY_DATA
import com.laser.scanner.utils.decodeToHexString
import com.laser.scanner.utils.log
import com.laser.scanner.utils.transformToString
import com.nice.bluetooth.Bluetooth
import com.nice.bluetooth.common.*
import com.nice.bluetooth.peripheral
import com.nice.common.applicationContext
import com.nice.common.event.emitStickyEvent
import com.nice.common.helper.getOrDefault
import com.nice.common.helper.getValue
import com.nice.common.helper.toDateString
import com.nice.common.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

class DeviceDetailViewModel(state: SavedStateHandle) : SavedStateViewModel(state) {

    val deviceName: String = state.getValue("name")
    val deviceAddress: String = state.getValue("address")
    val isRangeFinding: Boolean = state.getOrDefault("range_finding", false)

    @Volatile
    private var isRunning: Boolean = false

    @Volatile
    private var isConnected: Boolean = false

    @Volatile
    private var isConnecting: Boolean = false

    private var connectJob: Job? = null

    private val peripheralState = MutableStateFlow(false)
    private val peripheralRemoteState = MutableStateFlow(false)

    private val peripheralLightState = MutableStateFlow(false)

    val isStarted: Boolean get() = peripheralRemoteState.value

    private val _showInfo = MutableSharedFlow<ShowInfo?>()
    val showInfo: SharedFlow<ShowInfo?> = _showInfo.asSharedFlow()

    private val dataCache = ShowInfo()

    var degreeSpan: Int = 200
        private set

    @Volatile
    var isDataSaved: Boolean = true
        private set

    var hasTryConnect: Boolean = false

    init {
        emitStickyEvent(EVENT_HAS_NEW_CONNECTED_DEVICE) {
            withContext(Dispatchers.IO) {
                val device = BleDevice(name = deviceName, address = deviceAddress)
                val id = BleDeviceDao.save(device)
                if (id >= 0) {
                    device.copy(id = id)
                } else null
            }
        }

//        viewModelScope.launch {
//
//            val drawInfo = withContext(Dispatchers.IO) {
//                applicationContext.getTestDrawInfo()
//            }
//
//           _showInfo.emit(ShowInfo(drawInfo = drawInfo).also {
//               dataCache = it
//               isDataSaved = false
//           })
//        }
    }

    fun togglePeripheralState() {
        if (!isRunning) {
            message = Message(MSG_BLUETOOTH_CONNECT)
            return
        }

        val isStarted = peripheralState.value
        peripheralState.value = !isStarted

        if (isRangeFinding && !isStarted) {
            resetData()
        }
    }

    fun togglePeripheralLightState() {
        if (!isRunning) {
            message = Message(MSG_BLUETOOTH_CONNECT)
            return
        }

        val isOn = peripheralLightState.value
        peripheralLightState.value = !isOn
    }

    /**
     * @param degreeSpan 1 - 8000 (100 -> 4.5°, 200 -> 9°)
     */
    fun startConnect(degreeSpan: Int = this.degreeSpan) {
        this.degreeSpan = degreeSpan
        Bluetooth.state.onEach {
            if (it == BluetoothState.Opened) {
                connect()
            }
        }.launchIn(viewModelScope)

        if (!Bluetooth.isEnabled) {
            Bluetooth.isEnabled = true
        }
    }

    fun stopConnect() {
        fun stop() {
            isConnecting = false
            isConnected = false
            connectJob?.cancel()
            connectJob = null
        }

        if (peripheralRemoteState.value) {
            viewModelScope.launch {
                peripheralState.value = false

                peripheralRemoteState.first { !it }

                stop()
            }
        } else {
            stop()
        }
    }

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            log(error = throwable) { throwable.message }
        }) {
            val name = Date().toDateString()
            val record = BleRecord(
                name = name,
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                content = transformToString(dataCache.drawInfo)
            )
            val id = BleRecordDao.save(record)
            if (id >= 0) emitStickyEvent(EVENT_HAS_NEW_HISTORY_DATA, record.copy(id = id))
            log { "save data: $id" }

            val file = generateExtractFile(name, FILE_TYPE_LOG)
            file.bufferedWriter().use {
                val list = dataCache.rawDataList
                for ((index, data) in list.withIndex()) {
                    val time = if (index == 0) {
                        Date(0).toDateString("[sss:SSS]")
                    } else {
                        Date(data.time - list[index - 1].time).toDateString("[sss:SSS]")
                    }
                    val info = data.toString()
                    it.appendLine("$time  $info")
                }
                it.flush()
            }

            isDataSaved = true
        }
    }

    fun resetData(stop: Boolean = false) = viewModelScope.launch {
        dataCache.clear()
        _showInfo.emit(null)

        if (stop) {
            peripheralState.value = false
        }

        isDataSaved = true
    }

    private fun connect() {
        if (isConnecting) return
        isConnecting = true

        connectJob?.cancel()
        connectJob =
            viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
                log(error = throwable) { throwable.message }
            }) {
                isRunning = true

                log { "start connecting........" }

                val peripheral = peripheral(deviceAddress)
                observeConnectState(peripheral)
                peripheral.connect()

                val service = peripheral.findService { it.serviceUuid.toString().contains("ffe0") }
                val characteristic = requireNotNull(service?.findCharacteristic {
                    it.characteristicUuid.toString().contains("ffe1")
                })

                setDegreeSpan(peripheral, characteristic, degreeSpan)

                peripheralState.value = true

                observePeripheralState(peripheral, characteristic)
                observePeripheralLightState(peripheral, characteristic)

                log { "observe data........" }
                peripheral.observe(characteristic).collect {
                    try {
                        log { "receive data: ${it.decodeToHexString()}" }
                        val data = BleReceiveData(it, degreeSpan)
                        when (data.type) {
                            1 -> {
                                log { data.toString() }

                                val success = dataCache.addData(data, !isRangeFinding)

                                if (isRangeFinding) {
                                    if (data.isValid()) {
                                        if (dataCache.size == 2) {
                                            _showInfo.emit(dataCache)
                                        }
                                    } else {
                                        notifyDistanceError(data.distanceErrorInfo!!)
                                        peripheralState.value = false
                                    }
                                } else {
                                    isDataSaved = false

                                    _showInfo.emit(dataCache)
                                }

                                if (!success) {
                                    peripheralState.value = false
                                }
                            }
                            2 -> {
                                log { "degreeSpan: $degreeSpan" }
                            }
                            3 -> {
                                peripheralRemoteState.value = true
                                peripheralState.value = true
                                notifyPeripheralState(true)
                            }
                            4 -> {
                                peripheralRemoteState.value = false
                                peripheralState.value = false
                                notifyPeripheralState(false)
                            }

                            5 -> {
                                peripheralLightState.value = true
                                notifyPeripheralLightState(true)
                            }

                            6 -> {
                                peripheralLightState.value = false
                                notifyPeripheralLightState(false)
                            }
                        }
                    } catch (e: Throwable) {
                        log(error = e) { e.message }
                    }
                }
            }.apply {
                invokeOnCompletion {
                    if (it != null && it !is CancellationException) notifyConnectFailed()
                    isConnecting = false
                    isConnected = false
                    isRunning = false
                }
            }
    }

    private suspend fun setDegreeSpan(
        peripheral: Peripheral,
        characteristic: Characteristic,
        degreeSpan: Int
    ) {
        peripheral.write(
            characteristic,
            BleSendData.DegreeSpan(degreeSpan).toByteArray().also {
                log { "set degree gap: ${it.decodeToHexString()}" }
            })
    }

    private fun CoroutineScope.observeConnectState(peripheral: Peripheral) = launch {
        peripheral.state.drop(1).collect {
            when (it) {
                is ConnectionState.Connecting -> {
                    notifyConnecting()
                    isConnecting = true

                    hasTryConnect = true
                }
                ConnectionState.Connected -> {
                    notifyConnected()
                    isConnecting = false
                    isConnected = true
                }
                is ConnectionState.Disconnecting -> isConnecting = false
                is ConnectionState.Disconnected -> {
                    notifyDisconnected()
                    isConnecting = false
                    isConnected = false
                }
            }
        }
    }

    private fun CoroutineScope.observePeripheralLightState(
        peripheral: Peripheral,
        characteristic: Characteristic
    ) = launch {
        log { "observePeripheralLightState........" }
        peripheralLightState.drop(1).collect { on ->
            peripheral.write(
                characteristic,
                (if (on) BleSendData.LightOn else BleSendData.LightOff).toByteArray().also {
                    log { "change light state: ${it.decodeToHexString()}" }
                }
            )
        }
    }

    private fun CoroutineScope.observePeripheralState(
        peripheral: Peripheral,
        characteristic: Characteristic
    ) = launch {
        log { "observePeripheralState........" }

        peripheralState.collect { on ->
            peripheral.write(
                characteristic,
                (if (on) BleSendData.Start else BleSendData.Stop).toByteArray().also {
                    log { "change state: ${it.decodeToHexString()}" }
                }
            )
        }
    }

    private fun notifyPeripheralState(on: Boolean) {
        message = messageBatchOf(
            if (on) Message(MSG_BLUETOOTH_CONNECTED)
            else Message(MSG_BLUETOOTH_DISCONNECTED),
            Tip(
                if (on) R.string.tip_peripheral_started
                else R.string.tip_peripheral_stopped
            )
        )
    }

    private fun notifyPeripheralLightState(on: Boolean) {
        val toggleEvent = if (on) Message(MSG_LIGHT_ON)
        else Message(MSG_LIGHT_OFF)
        toggleEvent.apply {
            set("state", on)
        }
        message = messageBatchOf(
            toggleEvent,
            Tip(
                if (on) R.string.tip_peripheral_light_on
                else R.string.tip_peripheral_light_off
            )
        )
    }

    private fun notifyDistanceError(errorInfo: String) {
        message = Tip(applicationContext.getString(R.string.tip_distance_error, errorInfo))
    }

    private fun notifyConnecting() {
        message = ShowProgress(R.string.device_opening)
    }

    private fun notifyConnected() {
        message = messageBatchOf(
            DismissProgress(),
            Message(MSG_BLUETOOTH_CONNECTED),
            Tip(R.string.device_opened)
        )
    }

    private fun notifyDisconnected() {
        peripheralState.value = false
        message = messageBatchOf(
            DismissProgress(),
            Message(MSG_BLUETOOTH_DISCONNECTED),
            Tip(
                if (isConnected) R.string.tip_bluetooth_disconnected
                else R.string.tip_bluetooth_connect_failed
            )
        )
    }

    private fun notifyConnectFailed() {
        peripheralState.value = false
        message = messageBatchOf(
            DismissProgress(),
            Message(MSG_BLUETOOTH_DISCONNECTED)
        )
    }

    companion object {
        const val MSG_BLUETOOTH_CONNECTED = 0x001
        const val MSG_BLUETOOTH_DISCONNECTED = 0x002

        const val MSG_BLUETOOTH_CONNECT = 0x003

        const val MSG_LIGHT_ON = 0x004
        const val MSG_LIGHT_OFF = 0x005
    }

}