package com.laser.scanner.ui.fragment.historydata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.laser.scanner.contract.FILE_TYPE_PNG
import com.laser.scanner.contract.FILE_TYPE_SVG
import com.laser.scanner.contract.FILE_TYPE_TXT
import com.laser.scanner.contract.generateExtractFile
import com.laser.scanner.data.dao.BleRecordDao
import com.laser.scanner.data.model.BleRecord
import com.laser.scanner.utils.extractByFileType
import com.laser.scanner.utils.log
import com.nice.common.helper.toDateString
import com.nice.common.viewmodel.NiceViewModel
import kotlinx.coroutines.*
import java.util.*

class HistoryDataViewModel : NiceViewModel() {

    private val _records = MutableLiveData<List<BleRecord>>()
    val records: LiveData<List<BleRecord>> = _records

    init {
        onDateChanged()
    }

    fun onDateChanged(date: Date? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (date == null) {
                    BleRecordDao.queryAll()
                } else {
                    BleRecordDao.queryByDate(date.toDateString("yyyy-MM-dd"))
                }
            }

            _records.value = result
        }
    }

    fun extract(
        record: BleRecord,
        type: String,
        onCompletion: (String?) -> Unit
    ): DisposableHandle {
        val file = generateExtractFile(record.name, type)
        return viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            log(error = e) { e.message }
        }) {
            val result = withContext(Dispatchers.IO) { extractByFileType(record.content, file) }
            if (result) {
                when (type) {
                    FILE_TYPE_PNG -> record.pngPath = file.absolutePath
                    FILE_TYPE_SVG -> record.svgPath = file.absolutePath
                    FILE_TYPE_TXT -> record.txtPath = file.absolutePath
                }
                BleRecordDao.update(record)
            } else {
                throw IllegalAccessException("Extract failed")
            }
        }.invokeOnCompletion {
            onCompletion.invoke(if (it == null) file.absolutePath else null)
        }
    }

}