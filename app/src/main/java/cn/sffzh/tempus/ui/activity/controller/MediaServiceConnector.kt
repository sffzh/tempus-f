package cn.sffzh.tempus.ui.activity.controller

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors

class MediaServiceConnector(
    private val activity: MainActivity,
    private val mainViewModel: MainViewModel,
    private val bottomSheetController: BottomSheetController
) {

    companion object {
        private const val TAG = "MediaServiceConnector"
    }

    fun onStart() {
        pingServer()
        initService()
    }

    @OptIn(UnstableApi::class)
    fun resetMusicSession() {
        MediaManager.reset(activity.getMediaBrowserListenableFuture())
    }

    @OptIn(UnstableApi::class)
    fun hideMusicSession() {
        MediaManager.hide(activity.getMediaBrowserListenableFuture())
    }

    @OptIn(UnstableApi::class)
    private fun initService() {
        MediaManager.checkAsync(activity.getMediaBrowserListenableFuture())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(
                activity.getMediaBrowserListenableFuture(),
                object : FutureCallback<MediaBrowser?> {
                    override fun onSuccess(browser: MediaBrowser?) {
                        activity.runOnUiThread {
                            Log.d(TAG, "init_service: init BottomSheetListener")
                            if (browser != null && browser.isConnected) {
                                autoPlay(browser)
                                browser.addListener(object : Player.Listener {
                                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                                        Log.d(TAG, "init_service: onIsPlayingChanged")
                                        if (isPlaying &&
                                            bottomSheetController.behavior.state ==
                                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                                        ) {
                                            bottomSheetController.setInPeek(true)
                                        }
                                    }
                                })
                            }
                            Log.d(TAG, "init_service: init BottomSheetListener. END")
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        Log.e(TAG, "initService", t)
                    }
                },
                MoreExecutors.directExecutor()
            )
        }
    }

    private fun autoPlay(browser: MediaBrowser?) {
        if (Preferences.isPaused()) return
        if (browser == null || !browser.isConnected) return

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (activity.isFinishing || activity.isDestroyed || !browser.isConnected) return@postDelayed
                Log.d(TAG, "MediaBrowser about to auto-start")
                if (browser.mediaItemCount > 0) {
                    browser.play()
                }
                Log.d(TAG, "MediaBrowser started")
            } catch (e: Throwable) {
                Log.e(TAG, "Auto-play failed", e)
            }
        }, 2000)
    }

    private fun pingServer() {
        if (!Preferences.isLogged()) return

        if (Preferences.isInUseServerAddressLocal()) {
            mainViewModel.ping().observe(activity) { response ->
                if (response == null) {
                    Preferences.setServerSwitchableTimer()
                    Preferences.switchInUseServerAddress()
                    Log.w(TAG, "pingServer - server response is null")
                    Preferences.resetTokenSalt()
                    pingServer()
                    activity.resetView()
                } else {
                    Preferences.setOpenSubsonic(response.isOpenseSubsonic())
                }
            }
        } else {
            if (Preferences.isServerSwitchable()) {
                Preferences.setServerSwitchableTimer()
                Preferences.switchInUseServerAddress()
                Preferences.resetTokenSalt()
                Log.w(TAG, "refreshSubsonicClient")
                pingServer()
                activity.resetView()
            } else {
                mainViewModel.ping().observe(activity) { response ->
                    if (response == null) {
                        if (Preferences.showServerUnreachableDialog()) {
                            com.cappielloantonio.tempo.ui.dialog.ServerUnreachableDialog()
                                .show(activity.supportFragmentManager, null)
                        }
                    } else {
                        Preferences.setOpenSubsonic(response.isOpenseSubsonic())
                    }
                }
            }
        }
    }
}
