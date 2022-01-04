package com.laser.scanner.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laser.scanner.R
import com.laser.scanner.data.datastore.deviceRadius
import com.laser.scanner.databinding.ActivityMainBinding
import com.laser.scanner.databinding.DialogDeviceRadiusSettingBinding
import com.laser.scanner.ui.activity.code.GenerateCodeActivity
import com.laser.scanner.ui.fragment.discoverdevices.DiscoverDevicesFragment
import com.laser.scanner.ui.fragment.historydata.HistoryDataFragment
import com.laser.scanner.ui.fragment.historydevices.HistoryDevicesFragment
import com.laser.scanner.utils.c
import com.nice.bluetooth.ScannerType
import com.nice.common.helper.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val bindings: ActivityMainBinding by viewBindings()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_change_new -> scannerType = ScannerType.New
            R.id.action_change_old -> scannerType = ScannerType.Old
            R.id.action_change_system -> scannerType = ScannerType.System
            R.id.action_generate_password -> startActivity<GenerateCodeActivity>()
            R.id.action_device_radius -> showDeviceRadiusSettingDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (scannerType) {
            ScannerType.New -> menu.findItem(R.id.action_change_new).isChecked = true
            ScannerType.Old -> menu.findItem(R.id.action_change_old).isChecked = true
            ScannerType.System -> menu.findItem(R.id.action_change_system).isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bindings)

        val tabLayout = bindings.tabLayout
        val viewPager = bindings.viewPager

        viewPager.offscreenPageLimit = 2

        val graph = FragmentGraph()
        graph += FragmentDestination(
            R.id.fragment_first,
            DiscoverDevicesFragment::class.java.name,
            label = getString(R.string.tab_discover_devices)
        )
        graph += FragmentDestination(
            R.id.fragment_second,
            HistoryDevicesFragment::class.java.name,
            label = getString(R.string.tab_history_devices)
        )
        graph += FragmentDestination(
            R.id.fragment_third,
            HistoryDataFragment::class.java.name,
            label = getString(R.string.tab_history_data)
        )

        setupTabLayoutWithFragmentController(graph, tabLayout, viewPager)
    }

    private fun showDeviceRadiusSettingDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .create()
        dialog.setView(viewOfBinding<DialogDeviceRadiusSettingBinding>(layoutInflater) {
            edittext.string = deviceRadius.toString()
            edittext.setSelection(edittext.length())

            ok.doOnClick {
                val radius = edittext.string?.toIntOrNull() ?: 0
                deviceRadius = radius
                dialog.dismiss()
            }

            cancel.doOnClick {
                dialog.dismiss()
            }

            lifecycleScope.launch {
                delay(200)
                edittext.showIme()
            }
        })
        dialog.show()
    }

    companion object {

        var scannerType = ScannerType.New
            private set

    }

}