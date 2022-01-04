package com.laser.scanner.ui.activity.code

import android.os.Bundle
import com.laser.scanner.R
import com.laser.scanner.databinding.ActivityGenerateCodeBinding
import com.laser.scanner.utils.createCodeString
import com.laser.scanner.utils.isPassword
import com.laser.scanner.utils.shareText
import com.nice.common.app.NiceActivity
import com.nice.common.helper.*

class GenerateCodeActivity : NiceActivity() {

    private val binding by viewBindings<ActivityGenerateCodeBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding)

        val tvPwd = binding.tvPassword
        val tvCode = binding.tvCode

        binding.buttonGenerate.doOnClick {
            val pwd = tvPwd.string
            if (pwd.isNullOrEmpty()) {
                showSnackBar(R.string.prompt_empty_password)
            } else if (!isPassword(pwd)) {
                showSnackBar(R.string.prompt_error_password)
            } else {
                tvCode.text = createCodeString(pwd)
            }
        }

        binding.buttonShare.doOnClick {
            val code = tvCode.string
            if (code.isNullOrEmpty()) {
                showSnackBar(R.string.prompt_share_code_empty)
            } else {
                shareText(code)
            }
        }
    }

}