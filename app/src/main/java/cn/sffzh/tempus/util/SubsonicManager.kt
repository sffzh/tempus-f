package cn.sffzh.tempus.util

import android.util.Log
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.SubsonicPreferences
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Preferences.getInUseServerAddress
import com.cappielloantonio.tempo.util.Preferences.getUser
import com.cappielloantonio.tempo.util.Preferences.isLowScurity
import com.cappielloantonio.tempo.util.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


object SubsonicManager {
    private const val TAG = "SubsonicManager"

    private val mutex = Mutex()
    private var subsonic: Subsonic? = null

    // Flow 监听 token/salt/server/user 的变化
    private val prefsFlow = combine(
        SecurePrefs.tokenFlow,
        SecurePrefs.saltFlow,
        SecurePrefs.passwordFlow
    ) { token, salt, password ->
        if (password == null) {
            Log.e(TAG, "初始化 subsonic Client失败，password为null. 这属于意外逻辑，可能引起崩溃。")
            null
        } else {
            val server = getInUseServerAddress()
            val username = getUser()
            val isLowSecurity = isLowScurity()

            val preferences = SubsonicPreferences()
            preferences.serverUrl = server
            preferences.username = username
            preferences.setAuthentication(password, token, salt, isLowSecurity)

            if (token == null || salt == null) {
                //更新了token 所以当需要手动更新token时，可以直接将其设置为null.
                Preferences.setToken(
                    preferences.authentication.token,
                    preferences.authentication.salt
                )
            }
            preferences
        }
    }.distinctUntilChanged()

    init {
        // 当任意参数变化时，自动重建 Subsonic
        CoroutineScope(Dispatchers.IO).launch {
            prefsFlow.collect { prefs ->
                mutex.withLock {
                    subsonic = Subsonic(prefs)
                }
            }
        }
    }

    suspend fun getSubsonicAsync(): Subsonic {
        subsonic?.let { return it }

        return mutex.withLock {
            subsonic?.let { return it }

            val prefs = prefsFlow.first()
            val client = Subsonic(prefs)
            subsonic = client
            client
        }
    }

    fun getSubsonicBlocking(): Subsonic =
        runBlocking { getSubsonicAsync() }
}