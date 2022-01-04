package com.laser.scanner.ui.fragment.discoverdevices

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.iterator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.laser.scanner.R
import com.laser.scanner.data.datastore.isPowerful
import com.laser.scanner.databinding.FragmentDiscoverDevicesBinding
import com.laser.scanner.databinding.ItemDiviceBinding
import com.laser.scanner.ui.activity.detail.DeviceDetailActivity
import com.laser.scanner.ui.fragment.discoverdevices.DiscoverDevicesViewModel.Companion.KEY_IS_SCANNING
import com.laser.scanner.ui.fragment.discoverdevices.DiscoverDevicesViewModel.Companion.MSG_CHANGE_SCAN_STATE
import com.laser.scanner.ui.fragment.discoverdevices.DiscoverDevicesViewModel.Companion.MSG_PERMISSION_DENIED
import com.laser.scanner.utils.showPermissionDeniedDialog
import com.laser.scanner.utils.startConnectSelector
import com.nice.bluetooth.common.Advertisement
import com.nice.common.adapter.ViewBindingHolder
import com.nice.common.adapter.ViewBindingRecyclerAdapter
import com.nice.common.adapter.bind
import com.nice.common.adapter.get
import com.nice.common.app.NiceViewModelFragment
import com.nice.common.helper.*
import com.nice.common.viewmodel.Message
import com.nice.common.widget.TipView
import com.nice.common.widget.tipViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DiscoverDevicesFragment : NiceViewModelFragment<DiscoverDevicesViewModel>() {

    override val viewModel: DiscoverDevicesViewModel by viewModels()

    override val tipView: TipView by tipViews()

    private val bindings by viewBindings<FragmentDiscoverDevicesBinding>()

    private val adapter: DevicesAdapter by lazy {
        DevicesAdapter(requireContext()).also {
            bindings.recyclerView.adapter = it
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (item in menu) {
            if (item.itemId == R.id.action_date) {
                item.isVisible = false
            } else {
                if (item.itemId == R.id.action_generate_password) {
                    item.isVisible = isPowerful()
                } else {
                    item.isVisible = true
                }
                if (item.itemId == R.id.action_scan) {
                    item.setIcon(if (viewModel.isScanning) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_scan_24)
                }
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_scan) {
            viewModel.toggleScan()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bindings)

        viewModel.register(this)

        bindings.statefulView.attachTo(adapter)

        bindings.statefulView.setOnEmptyActionListener {
            if (!viewModel.isScanning) viewModel.toggleScan()
        }

        adapter.setOnItemChildClickListener { adapter, holder, _ ->
            adapter.callOnItemClick(holder)
        }

        adapter.setOnItemClickListener { adapter, holder ->
            val item = adapter[holder.layoutPosition]
            requireContext().startConnectSelector(item)
        }

        viewModel.advertisements
            .onEach {
                if (!adapter.containsItem(it)) {
                    adapter.addItem(it)
                }
            }
            .flowOn(Dispatchers.Main)
            .launchIn(lifecycleScope)
    }

    override fun onViewModelMessage(message: Message): Boolean {
        if (message.what == MSG_CHANGE_SCAN_STATE) {
            val isScanning: Boolean = message[KEY_IS_SCANNING]
            if (isScanning) {
                bindings.loadingView.visible()
                bindings.statefulView.showContent()
            } else {
                bindings.loadingView.gone()
                if (adapter.isEmpty()) {
                    bindings.statefulView.showEmpty()
                }
            }

            activity?.invalidateOptionsMenu()
            return true
        } else if (message.what == MSG_PERMISSION_DENIED) {
            context?.showPermissionDeniedDialog { finishActivity() }
            return true
        }
        return super.onViewModelMessage(message)
    }


    private class DevicesAdapter(context: Context) :
        ViewBindingRecyclerAdapter<Advertisement, ItemDiviceBinding>(context) {

        override fun onCreateItemViewBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemDiviceBinding {
            return viewBinding(inflater, parent)
        }

        override fun onItemViewHolderCreated(
            holder: ViewBindingHolder<ItemDiviceBinding>,
            viewType: Int
        ) = holder.bind {
            holder.addOnChildClickListener(button)
        }

        override fun onBindItemViewHolder(
            holder: ViewBindingHolder<ItemDiviceBinding>,
            item: Advertisement,
            payloads: List<Any>
        ) = holder.bind {
            text1.string = item.name.ifNullOrEmpty { "Unknown" }
            text2.string = item.address
        }

    }

    init {
        setHasOptionsMenu(true)
    }

}