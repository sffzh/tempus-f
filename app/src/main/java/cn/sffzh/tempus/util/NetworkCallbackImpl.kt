package cn.sffzh.tempus.util

import android.net.ConnectivityManager
import android.net.Network

class NetworkCallbackImpl(
    private val onAvailable: () -> Unit,
    private val onLost: () -> Unit
) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) = onAvailable()
    override fun onLost(network: Network) = onLost()
}
