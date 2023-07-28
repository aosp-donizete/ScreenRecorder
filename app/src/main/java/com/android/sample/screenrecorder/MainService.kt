package com.android.sample.screenrecorder

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodecInfo
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File

class MainService : Service() {
    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }
    private val mediaProjectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }
    private val windowManager by lazy {
        getSystemService(WindowManager::class.java)
    }
    private val displayManager by lazy {
        getSystemService(DisplayManager::class.java)
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var file = File("")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannelCompat.Builder(
            "my_channel",
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).apply{
            setName("my_channel")
        }.build().run(notificationManager::createNotificationChannel)

        NotificationCompat.Builder(
            this,
            "my_channel"
        ).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
        }.build().also {
            startForeground(1234, it)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent != null) {
            when(intent.action) {
                START -> startMediaProjection(intent)
                STOP -> stopMediaProjection()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startMediaProjection(intent: Intent) {
        file = File.createTempFile(
            "video",
            ".mp4",
            getExternalFilesDir(null)
        )
        Log.d("SCREENRECORDER", "$file")

        val refreshRate = displayManager.displays.first().refreshRate.toInt()
        val dpi = resources.configuration.densityDpi
        val (width, height) = windowManager.maximumWindowMetrics.bounds.run {
            Pair(width(), height())
        }
        val vidBitRate = (width * height * refreshRate /
                SystemUI.VIDEO_FRAME_RATE * SystemUI.VIDEO_FRAME_RATE_TO_RESOLUTION_RATIO)
        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingProfileLevel(
                MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                MediaCodecInfo.CodecProfileLevel.AVCLevel42
            )
            setVideoSize(width, height)
            setVideoFrameRate(refreshRate)
            setVideoEncodingBitRate(vidBitRate)
            setMaxDuration(SystemUI.MAX_DURATION_MS)
            setMaxFileSize(SystemUI.MAX_FILESIZE_BYTES)
            setOutputFile(file)
            prepare()
            start()
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(-1, intent)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "my_virtual_display",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null,
            null
        )
    }

    private fun stopMediaProjection() {
        mediaRecorder?.stop()
        mediaProjection?.stop()

        mediaRecorder?.release()
        virtualDisplay?.release()
    }

    companion object {
        const val START = "com.android.sample.screenrecorder.START"
        const val STOP = "com.android.sample.screenrecorder.STOP"
    }

    //ANDROID/11.0.0_r68/frameworks/base/packages/SystemUI/src/com/android/systemui/screenrecord/ScreenMediaRecorder.java
    private object SystemUI {
        const val TOTAL_NUM_TRACKS = 1
        const val VIDEO_FRAME_RATE = 30
        const val VIDEO_FRAME_RATE_TO_RESOLUTION_RATIO = 6
        const val AUDIO_BIT_RATE = 196000
        const val AUDIO_SAMPLE_RATE = 44100
        const val MAX_DURATION_MS = 60 * 60 * 1000
        const val MAX_FILESIZE_BYTES = 5000000000L
    }
}