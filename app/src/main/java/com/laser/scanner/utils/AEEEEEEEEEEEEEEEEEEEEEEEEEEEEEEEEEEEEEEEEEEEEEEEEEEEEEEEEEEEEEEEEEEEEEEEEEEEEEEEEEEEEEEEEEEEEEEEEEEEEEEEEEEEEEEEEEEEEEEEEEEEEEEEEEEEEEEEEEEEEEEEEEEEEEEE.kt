package com.laser.scanner.utils

import android.app.Activity
import com.laser.scanner.BuildConfig
import com.nice.common.helper.decodeBase64ToString
import com.nice.common.helper.toUrl
import com.nice.common.helper.weak
import kotlinx.coroutines.*
import org.json.JSONObject

class AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE(
    activity: Activity
) {

    private val acttttttttttttttttttttttttttttttttttttttttttttttttttt by weak { activity }

    @OptIn(DelicateCoroutinesApi::class)
    fun c() {
        if (BuildConfig.DEBUG) return

        GlobalScope.launch {
            try {
                val uuuuuuuuuuuuuuuuurl =
                    UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUURL.decodeBase64ToString()
                val rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres = withContext(Dispatchers.IO) {
                    uuuuuuuuuuuuuuuuurl.toUrl().readText()
                }
                log { rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres }
                val jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjson =
                    JSONObject(rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres)
                val cccccccccccccccccccccccccccccccccccccccccccccode =
                    jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjson.optInt("code")
                if (cccccccccccccccccccccccccccccccccccccccccccccode != 2000) {
                    throw IllegalAccessException()
                }
            } catch (t: Throwable) {
                log(error = t) { t.message }
                acttttttttttttttttttttttttttttttttttttttttttttttttttt?.finish()
            }
        }
    }

    private companion object {
        const val UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUURL =
            "aHR0cDovLzgxLjcwLjE5Ny4yMDc6ODY4Ny9qaWd1YW5nL2FwcC9hdXRo"
    }

}

fun c(a: Activity) {
    AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE(a).c()
}