package com.laser.scanner.ui.activity.detail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laser.scanner.R
import com.laser.scanner.databinding.ActivityDeviceDetailBinding
import com.laser.scanner.databinding.DialogDegreeSettingBinding
import com.laser.scanner.ui.activity.detail.DeviceDetailViewModel.Companion.MSG_BLUETOOTH_CONNECT
import com.laser.scanner.ui.activity.detail.DeviceDetailViewModel.Companion.MSG_BLUETOOTH_CONNECTED
import com.laser.scanner.ui.activity.detail.DeviceDetailViewModel.Companion.MSG_BLUETOOTH_DISCONNECTED
import com.laser.scanner.ui.activity.detail.DeviceDetailViewModel.Companion.MSG_LIGHT_OFF
import com.laser.scanner.ui.activity.detail.DeviceDetailViewModel.Companion.MSG_LIGHT_ON
import com.laser.scanner.utils.checkDivide
import com.laser.scanner.utils.log
import com.nice.common.app.NiceViewModelActivity
import com.nice.common.app.subtitle
import com.nice.common.helper.*
import com.nice.common.viewmodel.Message
import com.nice.common.widget.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class DeviceDetailActivity : NiceViewModelActivity<DeviceDetailViewModel>() {

    override val viewModel: DeviceDetailViewModel by viewModels()
    override val progressView: ProgressView by progressViews()
    override val tipView: TipView by tipViews { defaultSnackTipViewFactory }

    private val binding by viewBindings<ActivityDeviceDetailBinding>()

    private var hideButtonJob: Job? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_device_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_more) {
            if (binding.sideActionBar.isShown) {
                delayHideSideActionBar(0)
            } else {
                showSideActionBar()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding)

        title = viewModel.deviceName
        subtitle = viewModel.deviceAddress

        binding.canvasView.setDrawDistance(viewModel.isRangeFinding)

        binding.btnActionConnect.doOnClick {
            delayHideSideActionBar(200)
            viewModel.togglePeripheralState()
        }

        binding.btnActionDegreeSetting.apply {
            isGone = viewModel.isRangeFinding

            doOnClick {
                delayHideSideActionBar(200)
                showDegreeSpanSelectorDialog()
            }
        }

        binding.btnActionReset.apply {
            isGone = viewModel.isRangeFinding

            doOnClick {
                delayHideSideActionBar(200)
                showResetTipDialog()
            }
        }

        binding.btnActionSave.doOnClick {
            delayHideSideActionBar(200)
            if (viewModel.isDataSaved) {
                tipView.show(R.string.tip_no_data_to_save)
                return@doOnClick
            }
            showSaveTipDialog(false)
        }

        binding.btnActionLight.doOnClick {
            delayHideSideActionBar(200)
            viewModel.togglePeripheralLightState()
        }

        viewModel.showInfo
            .onEach {
                binding.canvasView.setDrawInfo(it?.drawInfo)

                binding.tvDegree.string = if (it == null) getString(R.string.degree_placeholder)
                else getString(R.string.degree, it.degree)
                binding.tvDistance.string = if (it == null) getString(R.string.distance_placeholder)
                else getString(R.string.distance, it.distance)
                binding.tvBattery.string = if (it == null) getString(R.string.battery_placeholder)
                else getString(R.string.battery, it.battery)
            }
            .catch { log(error = it) { it.message } }
            .launchIn(lifecycleScope)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (viewModel.isRangeFinding) {
            viewModel.startConnect(4000)
        } else {
            showDegreeSpanInputDialog()
        }
    }

    override fun onViewModelMessage(message: Message): Boolean {
        when (message.what) {
            MSG_BLUETOOTH_CONNECTED, MSG_BLUETOOTH_DISCONNECTED -> {
                binding.btnActionConnect.imageResource =
                    if (viewModel.isStarted) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_start_24
                return true
            }
            MSG_LIGHT_ON, MSG_LIGHT_OFF -> {
                val state: Boolean = message["state"]
                binding.btnActionLight.apply {
                    imageTintList = ColorStateList.valueOf(if (state) Color.WHITE else Color.GRAY)
                }
                return true
            }
            MSG_BLUETOOTH_CONNECT -> showDegreeSpanInputDialog()
        }
        return super.onViewModelMessage(message)
    }

    override fun onBackPressed() {
        if (!viewModel.isRangeFinding && !viewModel.isDataSaved) {
            showSaveTipDialog(true)
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopConnect()
    }

    private fun delayHideSideActionBar(delayMillis: Long = 5000) {
        hideButtonJob?.cancel()

        val bar = binding.sideActionBar
        if (!bar.isShown) return

        hideButtonJob = lifecycleScope.launch {
            delay(delayMillis)

            val alphaHide = ObjectAnimator.ofFloat(bar, "alpha", 1f, 0f)
            val slideOut = ObjectAnimator.ofFloat(bar, "translationX", 0f, bar.width.toFloat())
            AnimatorSet().apply {
                playTogether(alphaHide, slideOut)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        bar.isVisible = false
                    }
                })
                start()
            }
        }
    }

    private fun showSideActionBar() {
        val bar = binding.sideActionBar
        if (bar.isShown) return

        val alphaShow = ObjectAnimator.ofFloat(bar, "alpha", 0f, 1f)
        val slideIn = ObjectAnimator.ofFloat(bar, "translationX", bar.width.toFloat(), 0f)
        AnimatorSet().apply {
            playTogether(alphaShow, slideIn)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    bar.isVisible = true
                }
            })
            start()
        }
    }

    private fun showDegreeSpanSelectorDialog() {
        val isStartedBefore = viewModel.isStarted
        viewModel.stopConnect()

        var onDismiss: Runnable? = Runnable {
            if (isStartedBefore) {
                viewModel.startConnect()
            }
        }

        lifecycleScope.launch {
            delay(1000)

            var degreeSpan = 25

            MaterialAlertDialogBuilder(this@DeviceDetailActivity)
                .setTitle(R.string.degree_setting_selector)
                .setSingleChoiceItems(R.array.degrees_to_select, 0) { _, which ->
                    degreeSpan = when (which) {
                        0 -> 25
                        1 -> 100
                        2 -> 200
                        else -> 300
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    onDismiss = null
                    if (isStartedBefore) {
                        viewModel.startConnect()
                    }
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    onDismiss = null
                    viewModel.startConnect(degreeSpan)
                }
                .setOnDismissListener {
                    onDismiss?.run()
                }.show()
        }
    }

    private fun showDegreeSpanInputDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .create()
        dialog.setView(viewOfBinding<DialogDegreeSettingBinding>(layoutInflater) {
            edittext.string = "${viewModel.degreeSpan}"
            edittext.setSelection(edittext.length())

            edittext.doAfterTextChanged {
                val input = it?.toString()?.toIntOrNull()
                if (input != null && input > 8000) {
                    it.replace(0, it.length, "8000")
                }
            }

            connect.doOnClick {
                val degree = edittext.string?.toIntOrNull()
                if (degree == null) {
                    showToast(R.string.tip_degree_input_empty)
                } else if ((degree < 100 && !checkDivide(100, degree))
                    || (degree >= 100 && !checkDivide(degree, 100))
                ) {
                    showToast(R.string.tip_degree_input_invalid)
                } else {
                    viewModel.startConnect(degree)
                    dialog.dismiss()
                }
            }

            cancel.doOnClick {
                dialog.dismiss()
                if (!viewModel.hasTryConnect) {
                    finish()
                }
            }

            lifecycleScope.launch {
                delay(200)
                edittext.showIme()
            }
        })
        dialog.show()
    }

    private fun showResetTipDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_alert_title)
            .setMessage(R.string.tip_reset_canvas)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.resetData()
            }
            .show()
    }

    private fun showSaveTipDialog(exit: Boolean) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_alert_title)
            .setMessage(if (exit) R.string.tip_data_not_save else R.string.tip_data_save)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                if (exit) R.string.dialog_button_save_and_exit else R.string.dialog_button_save
            ) { _, _ ->
                viewModel.saveData()
                if (exit) finish()
            }
        if (exit) {
            builder.setNeutralButton(R.string.dialog_button_exit) { _, _ ->
                finish()
            }
        }
        builder.show()
    }

}