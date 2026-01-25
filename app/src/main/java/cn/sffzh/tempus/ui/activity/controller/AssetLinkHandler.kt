package cn.sffzh.tempus.ui.activity.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.util.AssetLinkNavigator
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences

@UnstableApi
class AssetLinkHandler(
    private val activity: MainActivity,
    private val bottomSheetController: BottomSheetController
) {

    companion object {
        private const val TAG = "AssetLinkHandler"
    }

    private val assetLinkNavigator = AssetLinkNavigator(activity)
    private var pendingAssetLink: AssetLink? = null
    private var pendingDownloadPlaybackIntent: Intent? = null

    fun handleIntent(intent: Intent?) {
        if (intent == null) return

        maybeSchedulePlaybackIntent(intent)
        handleAssetLinkIntent(intent)
        consumePendingPlaybackIntent()
    }

    fun consumePendingAssetLink() {
        val link = pendingAssetLink ?: return
        assetLinkNavigator.open(link)
        pendingAssetLink = null
    }

    fun openAssetLink(assetLink: AssetLink, collapsePlayer: Boolean = true) {
        if (!Preferences.isLogged()) {
            pendingAssetLink = assetLink
            return
        }
        if (collapsePlayer) {
            bottomSheetController.setInPeek(true)
        }
        assetLinkNavigator.open(assetLink)
    }

    private fun maybeSchedulePlaybackIntent(intent: Intent) {
        if (Constants.ACTION_PLAY_EXTERNAL_DOWNLOAD == intent.action ||
            intent.hasExtra(Constants.EXTRA_DOWNLOAD_URI)
        ) {
            pendingDownloadPlaybackIntent = Intent(intent)
        }
    }

    private fun consumePendingPlaybackIntent() {
        Log.d(TAG, "consumePendingPlaybackIntent, pending=$pendingDownloadPlaybackIntent")
        val pending = pendingDownloadPlaybackIntent ?: return
        pendingDownloadPlaybackIntent = null
        playDownloadedMedia(pending)
        Log.d(TAG, "consumePendingPlaybackIntent.END")
    }

    private fun handleAssetLinkIntent(intent: Intent) {
        val assetLink = AssetLinkUtil.parse(intent) ?: return
        if (!Preferences.isLogged()) {
            pendingAssetLink = assetLink
            intent.data = null
            return
        }
        assetLinkNavigator.open(assetLink)
        intent.data = null
    }

    private fun playDownloadedMedia(intent: Intent) {
        val uriString = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_URI)?:return

        val uri = uriString.toUri()
        var mediaId = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_MEDIA_ID)
        if (mediaId.isNullOrEmpty()) {
            mediaId = uri.toString()
        }

        val title = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_TITLE)
        val artist = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ARTIST)
        val album = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ALBUM)
        val duration = intent.getIntExtra(Constants.EXTRA_DOWNLOAD_DURATION, 0)

        val extras = Bundle()
        extras.putString("id", mediaId)
        extras.putString("title", title)
        extras.putString("artist", artist)
        extras.putString("album", album)
        extras.putString("uri", uri.toString())
        extras.putString("type", Constants.MEDIA_TYPE_MUSIC)
        extras.putInt("duration", duration)

        val metadataBuilder = MediaMetadata.Builder()
            .setExtras(extras)
            .setIsBrowsable(false)
            .setIsPlayable(true)

        if (!title.isNullOrEmpty()) metadataBuilder.setTitle(title)
        if (!artist.isNullOrEmpty()) metadataBuilder.setArtist(artist)
        if (!album.isNullOrEmpty()) metadataBuilder.setAlbumTitle(album)

        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadataBuilder.build())
            .setUri(uri)
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .build()

        MediaManager.playDownloadedMediaItem(activity.getMediaBrowserListenableFuture(), mediaItem)
    }
}
