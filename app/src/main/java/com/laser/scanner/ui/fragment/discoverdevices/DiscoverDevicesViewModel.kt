package com.laser.scanner.ui.fragment.discoverdevices

import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import com.laser.scanner.R
import com.laser.scanner.ui.activity.MainActivity
import com.nice.bluetooth.Bluetooth
import com.nice.bluetooth.Scanner
import com.nice.bluetooth.common.Advertisement
import com.nice.bluetooth.common.BluetoothState
import com.nice.common.app.PocketActivityResultLauncher
import com.nice.common.viewmodel.Message
import com.nice.common.viewmodel.NiceViewModel
import com.nice.common.viewmodel.Tip
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DiscoverDevicesViewModel : NiceViewModel() {

    private val permissionRequestLauncher =
        PocketActivityResultLauncher(ActivityResultContracts.RequestMultiplePermissions())

    private var scanJob: Job? = null

    @Volatile
    var isScanning: Boolean = false
        private set

    private val _advertisements =
        MutableSharedFlow<Advertisement>(extraBufferCapacity = Int.MAX_VALUE)
    val advertisements: SharedFlow<Advertisement> = _advertisements.asSharedFlow()

    fun register(fragment: DiscoverDevicesFragment) {
        permissionRequestLauncher.register(fragment)
    }

    fun toggleScan() {
        if (isScanning) {
            scanJob?.cancel()
        } else {
            checkOrScanBluetooth()
        }
    }

    private fun checkOrScanBluetooth() {
        permissionRequestLauncher.launch(Bluetooth.permissions) {
            if (it.all { entry -> entry.value }) {
                if (Bluetooth.isEnabled) {
                    scanBluetooth()
                } else {
                    Bluetooth.isEnabled = true

                    Bluetooth.state.onEach { state ->
                        if (state == BluetoothState.Opened) {
                            scanBluetooth()
                        }
                    }.launchIn(viewModelScope)
                }
            } else {
                message = Message(MSG_PERMISSION_DENIED)
            }
        }
    }

    private fun scanBluetooth() {
        if (isScanning) return

        changeScanningState(true)
        viewModelScope.launch(Dispatchers.Main.immediate + CoroutineExceptionHandler { _, _ ->
            message = Tip(R.string.tip_scan_bluetooth_failed)
        }) {
            withTimeout(30 * 1000L) {
                Scanner(MainActivity.scannerType).advertisements.collect {
                    _advertisements.emit(it)
                }
            }
        }.also { scanJob = it }.invokeOnCompletion {
            changeScanningState(false)
        }
    }

    private fun changeScanningState(on: Boolean) {
        isScanning = on
        message = Message(MSG_CHANGE_SCAN_STATE, KEY_IS_SCANNING to on)
    }

    companion object {
        const val KEY_IS_SCANNING = "is_scanning"

        const val MSG_CHANGE_SCAN_STATE = 0x001
        const val MSG_PERMISSION_DENIED = 0x002
    }

}