package com.laser.scanner.ui.fragment.historydevices

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.iterator
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.laser.scanner.R
import com.laser.scanner.data.datastore.isPowerful
import com.laser.scanner.data.model.BleDevice
import com.laser.scanner.databinding.FragmentHistoryDevicesBinding
import com.laser.scanner.databinding.ItemDiviceBinding
import com.laser.scanner.event.EVENT_HAS_NEW_CONNECTED_DEVICE
import com.laser.scanner.ui.activity.detail.DeviceDetailActivity
import com.laser.scanner.utils.startConnectSelector
import com.nice.common.adapter.*
import com.nice.common.app.NiceViewModelFragment
import com.nice.common.event.collectStickEventWithLifecycle
import com.nice.common.helper.*
import java.util.*

class HistoryDevicesFragment : NiceViewModelFragment<HistoryDevicesViewModel>() {

    override val viewModel: HistoryDevicesViewModel by viewModels()

    private val bindings by viewBindings<FragmentHistoryDevicesBinding>()

    private val adapter by lazy {
        BleDevicesAdapter(requireContext()).also {
            bindings.recyclerView.adapter = it
        }
    }

    private val calendar = Calendar.getInstance()

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (item in menu) {
            if (item.itemId == R.id.action_generate_password) {
                item.isVisible = isPowerful()
            } else {
                item.isVisible = item.itemId != R.id.action_scan
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_date) {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    viewModel.onDateChanged(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.ok),
                    this
                )
                setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.cancel),
                    this
                )
            }.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bindings)

        bindings.statefulView.attachTo(adapter)

        adapter.setOnItemChildClickListener { adapter, holder, _ ->
            adapter.callOnItemClick(holder)
        }

        adapter.setOnItemClickListener { adapter, holder ->
            val item = adapter[holder.layoutPosition]
            requireContext().startConnectSelector(item)
        }

        viewModel.devices.observe(this) {
            adapter += it
        }

        collectStickEventWithLifecycle<BleDevice>(
            EVENT_HAS_NEW_CONNECTED_DEVICE,
            minActiveState = Lifecycle.State.RESUMED
        ) {
            it ?: return@collectStickEventWithLifecycle

            if (adapter.containsItem(it)) {
                adapter.removeItem(it)
            }
            adapter.addItem(0, it)
            bindings.recyclerView.let { rv ->
                rv.post { rv.smoothScrollToPosition(0) }
            }
        }
    }

    private class BleDevicesAdapter(context: Context) :
        ViewBindingRecyclerAdapter<BleDevice, ItemDiviceBinding>(context) {

        override fun onCreateItemViewBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemDiviceBinding = viewBinding(inflater, parent)

        override fun onItemViewHolderCreated(
            holder: ViewBindingHolder<ItemDiviceBinding>,
            viewType: Int
        ) = holder.bind {
            holder.addOnChildClickListener(button)
        }

        override fun onBindItemViewHolder(
            holder: ViewBindingHolder<ItemDiviceBinding>,
            item: BleDevice,
            payloads: List<Any>
        ) = holder.bind {
            text1.string = item.name
            text2.string = item.address
        }

    }


    init {
        setHasOptionsMenu(true)
    }

}