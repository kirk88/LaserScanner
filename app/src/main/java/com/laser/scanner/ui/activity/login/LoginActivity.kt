package com.laser.scanner.ui.activity.login

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.laser.scanner.R
import com.laser.scanner.data.datastore.getCurrentCode
import com.laser.scanner.data.datastore.setCurrentCode
import com.laser.scanner.databinding.ActivityLoginBinding
import com.laser.scanner.ui.activity.MainActivity
import com.laser.scanner.utils.c
import com.laser.scanner.utils.checkCode
import com.nice.common.app.NiceActivity
import com.nice.common.helper.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoginActivity : NiceActivity() {

    private val binding by viewBindings<ActivityLoginBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val password = runBlocking { getCurrentCode() }
        if (password.isNotBlank()) {
            startActivity<MainActivity>()
            finish()
            return
        }
        setContentView(binding)

        val editCode = binding.editCode
        binding.buttonLogin.doOnClick {
            login(editCode.string)
        }
    }

    private fun login(code: String?) {
        if (code.isNullOrBlank()) {
            showSnackBar(R.string.prompt_empty_code)
        } else if (!checkCode(code)) {
            showSnackBar(R.string.prompt_invalid_code)
        } else {
            lifecycleScope.launch {
                setCurrentCode(code)
                startActivity<MainActivity>()
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        c(this)
    }

}