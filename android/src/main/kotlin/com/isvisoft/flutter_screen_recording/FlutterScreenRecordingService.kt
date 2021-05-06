package com.isvisoft.flutter_screen_recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin.Companion.mMediaProjection
import com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin.Companion.mProjectionManager
import io.flutter.plugin.common.PluginRegistry
import java.io.IOException

class FlutterScreenRecordingService :Service() {

    private val CHANNEL_ID = "ForegroundService Kotlin"
    companion object {
        fun startService(resultCode: Int,context: Context, message: String) {
            val startIntent = Intent(context, FlutterScreenRecordingService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, FlutterScreenRecordingService::class.java)
            context.stopService(stopIntent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //do heavy work on a background thread
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service Kotlin Example")
                .setContentText(input)
                .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(startId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            var i=0;
        }
        //stopSelf();
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}