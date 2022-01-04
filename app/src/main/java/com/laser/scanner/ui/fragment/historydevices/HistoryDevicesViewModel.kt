package com.laser.scanner.ui.fragment.historydevices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.laser.scanner.data.dao.BleDeviceDao
import com.laser.scanner.data.model.BleDevice
import com.nice.common.helper.toDateString
import com.nice.common.viewmodel.NiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class HistoryDevicesViewModel : NiceViewModel() {


    private val _devices = MutableLiveData<List<BleDevice>>()
    val devices: LiveData<List<BleDevice>> = _devices

    init {
        onDateChanged()
    }

    fun onDateChanged(date: Date? = null){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
               if(date == null){
                   BleDeviceDao.queryAll()
               }else{
                   BleDeviceDao.queryByDate(date.toDateString("yyyy-MM-dd"))
               }
            }

            _devices.value = result
        }
    }

}