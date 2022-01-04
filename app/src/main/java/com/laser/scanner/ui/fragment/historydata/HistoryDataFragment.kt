package com.laser.scanner.ui.fragment.historydata

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laser.scanner.R
import com.laser.scanner.contract.FILE_TYPE_PNG
import com.laser.scanner.contract.FILE_TYPE_SVG
import com.laser.scanner.contract.FILE_TYPE_TXT
import com.laser.scanner.contract.mimeType
import com.laser.scanner.data.datastore.isPowerful
import com.laser.scanner.data.model.BleRecord
import com.laser.scanner.databinding.*
import com.laser.scanner.event.EVENT_HAS_NEW_HISTORY_DATA
import com.laser.scanner.utils.shareFile
import com.nice.common.adapter.*
import com.nice.common.app.NiceViewModelFragment
import com.nice.common.event.collectStickEventWithLifecycle
import com.nice.common.helper.*
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.delay
import java.io.File
import java.util.*
import kotlin.collections.set

class HistoryDataFragment : NiceViewModelFragment<HistoryDataViewModel>() {

    override val viewModel: HistoryDataViewModel by viewModels()

    private val bindings by viewBindings<FragmentHistoryDataBinding>()

    private val calendar = Calendar.getInstance()

    private val adapter by lazy {
        BleRecordsAdapter(requireContext()).also {
            bindings.recyclerView.adapter = it
        }
    }

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
            showExtractDialog(item)
        }

        viewModel.records.observe(this) {
            adapter += it
        }

        collectStickEventWithLifecycle<BleRecord>(
            EVENT_HAS_NEW_HISTORY_DATA,
            minActiveState = Lifecycle.State.RESUMED
        ) {
            it ?: return@collectStickEventWithLifecycle

            adapter.addItem(0, it)

            bindings.recyclerView.let { rv ->
                if (!rv.isInLayout) {
                    rv.post { rv.smoothScrollToPosition(0) }
                }
            }

            delay(200)
        }
    }

    private fun showExtractDialog(record: BleRecord) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_extract_dialog)
            .setNegativeButton(R.string.cancel, null)
            .create()
        var disposable: DisposableHandle? = null
        dialog.setView(viewOfBinding<DialogExtractHistoryDataBinding>(layoutInflater) {
            recyclerView.adapter = ExtractFilesAdapter(
                requireContext(),
                listOf(
                    FILE_TYPE_PNG to record.pngPath,
                    FILE_TYPE_SVG to record.svgPath,
                    FILE_TYPE_TXT to record.txtPath
                )
            ).apply {
                setOnItemChildClickListener { adapter, holder, view ->
                    val item = adapter[holder.layoutPosition]
                    val path = item.second
                    val file = if (path.isNullOrBlank()) null else File(path)
                    if (view.id == R.id.btn_share) {
                        if (file != null) adapter.context.shareFile(file, mimeType(item.first))
                    } else {
                        adapter as ExtractFilesAdapter
                        if (file == null || !file.exists()) {
                            adapter.setLoading(holder.layoutPosition, true)
                            disposable = viewModel.extract(record, item.first) {
                                showToast(if (it != null) R.string.tip_extract_succeed else R.string.tip_extract_failed)
                                adapter.setLoading(holder.layoutPosition, false)

                                if (it != null) {
                                    adapter.updateItemAt(
                                        holder.layoutPosition,
                                        item.copy(second = it)
                                    )
                                }
                            }
                        } else {
                            showLookDialog(file, item.first)
                            dialog.dismiss()
                        }
                    }
                }
            }
        })
        dialog.doOnDismiss { disposable?.dispose() }
        dialog.show()
    }

    private fun showLookDialog(file: File, type: String) {
        val dialog = MaterialAlertDialogBuilder(requireContext()).create()
        dialog.window?.setLayout(-2, -1)
        dialog.setView(viewOfBinding<DialogLookDataBinding>(layoutInflater) {
            GestureDelegate(imageContainer, consume = true).doOnSingleTapUp { dialog.dismiss() }
            GestureDelegate(textContainer, consume = false).doOnSingleTapUp { dialog.dismiss() }
            root.doOnLayout {
                if (type == FILE_TYPE_TXT) {
                    textContainer.isVisible = true
                    imageContainer.isVisible = false
                    file.bufferedReader().useLines {
                        text.text = it.joinToString("\n")
                    }
                } else {
                    textContainer.isVisible = false
                    imageContainer.isVisible = true
                    image.imageFile = file
                }
            }
        })
        dialog.show()
    }

    private class BleRecordsAdapter(context: Context) :
        ViewBindingRecyclerAdapter<BleRecord, ItemHistoryDataBinding>(context) {

        override fun onCreateItemViewBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemHistoryDataBinding = viewBinding(inflater, parent)

        override fun onItemViewHolderCreated(
            holder: ViewBindingHolder<ItemHistoryDataBinding>,
            viewType: Int
        ) = holder.bind {
            holder.addOnChildClickListener(R.id.button)
        }

        override fun onBindItemViewHolder(
            holder: ViewBindingHolder<ItemHistoryDataBinding>,
            item: BleRecord,
            payloads: List<Any>
        ) = holder.bind {
            text1.string = item.name
            text2.string =
                context.getString(R.string.data_from, item.deviceName, item.deviceAddress)
        }
    }

    private class ExtractFilesAdapter(
        context: Context,
        items: List<Pair<String, String?>>
    ) : ViewBindingRecyclerAdapter<Pair<String, String?>, ItemExtractFileBinding>(
        context,
        items = items
    ) {

        private val loadings = mutableMapOf<Int, Boolean>()

        fun setLoading(position: Int, loading: Boolean) {
            loadings[position] = loading
            notifyItemChanged(position, 0)
        }

        override fun onCreateItemViewBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ItemExtractFileBinding = viewBinding(inflater, parent)

        override fun onItemViewHolderCreated(
            holder: ViewBindingHolder<ItemExtractFileBinding>,
            viewType: Int
        ) = holder.bind {
            holder.addOnChildClickListener(btnShare, btnLook)
        }

        override fun onBindItemViewHolder(
            holder: ViewBindingHolder<ItemExtractFileBinding>,
            item: Pair<String, String?>,
            payloads: List<Any>
        ) = holder.bind {
            tvType.text = item.first
            val path = item.second
            val file = if (path.isNullOrBlank()) null else File(path)
            btnShare.isVisible = file != null && file.exists()
            btnLook.textResource = if (btnShare.isVisible) R.string.look else R.string.extract
            val isLoading = loadings.getOrElse(holder.layoutPosition) { false }
            btnLook.isVisible = !isLoading
            progressBar.isVisible = isLoading
        }

    }

    init {
        setHasOptionsMenu(true)
    }

}