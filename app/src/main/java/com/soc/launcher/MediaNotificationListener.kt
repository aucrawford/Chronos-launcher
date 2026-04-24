package com.soc.launcher

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaInfo(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val packageName: String = ""
)

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        private val _mediaInfo = MutableStateFlow(MediaInfo())
        val mediaInfo = _mediaInfo.asStateFlow()
        
        private var activeController: MediaController? = null

        fun sendCommand(command: String) {
            when (command) {
                "play" -> activeController?.transportControls?.play()
                "pause" -> activeController?.transportControls?.pause()
                "stop" -> activeController?.transportControls?.stop()
                "next" -> activeController?.transportControls?.skipToNext()
                "previous" -> activeController?.transportControls?.skipToPrevious()
            }
        }
    }

    private lateinit var sessionManager: MediaSessionManager

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val componentName = android.content.ComponentName(this, MediaNotificationListener::class.java)
            val controllers = try {
                sessionManager.getActiveSessions(componentName)
            } catch (e: SecurityException) {
                Log.e("MediaNotification", "SecurityException in getActiveSessions", e)
                null
            }
            updateActiveController(controllers)
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            } catch (e: SecurityException) {
                Log.e("MediaNotification", "SecurityException in addOnActiveSessionsChangedListener", e)
            }
        } catch (e: Exception) {
            Log.e("MediaNotification", "Unexpected error in onListenerConnected", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.e("MediaNotification", "Failed to remove session listener", e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        // Prefer Spotify or the first active one
        val controller = controllers?.find { it.packageName == "com.spotify.music" } ?: controllers?.firstOrNull()
        
        activeController?.unregisterCallback(callback)
        activeController = controller
        activeController?.registerCallback(callback)
        
        updateMediaInfo()
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
        }
    }

    private fun updateMediaInfo() {
        val metadata = activeController?.metadata
        val state = activeController?.playbackState
        
        _mediaInfo.value = MediaInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            packageName = activeController?.packageName ?: ""
        )
    }
}
