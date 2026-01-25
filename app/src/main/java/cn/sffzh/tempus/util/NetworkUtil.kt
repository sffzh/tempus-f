package cn.sffzh.tempus.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cappielloantonio.tempo.App

object NetworkUtil {
    fun isOffline(): Boolean {
        val connectivityManager =
            App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork

            if (network != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (capabilities != null) {
                    return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || !capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                }
            }
        }
        return true
    }
}

fun Context.getNetworkType(): String {
    val cm = getSystemService(ConnectivityManager::class.java)
    val network = cm.activeNetwork ?: return "NONE"
    val caps = cm.getNetworkCapabilities(network) ?: return "NONE"

    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        else -> "OTHER"
    }
}