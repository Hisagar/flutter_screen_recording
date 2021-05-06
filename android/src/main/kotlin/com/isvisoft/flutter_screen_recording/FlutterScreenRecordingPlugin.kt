package com.isvisoft.flutter_screen_recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.IOException


class FlutterScreenRecordingPlugin(
        private val registrar: Registrar
) : MethodCallHandler,
        PluginRegistry.ActivityResultListener {

    var mMediaProjectionCallback: MediaProjectionCallback? = null
    var mVirtualDisplay: VirtualDisplay? = null
    var mScreenDensity: Int = 0
    var mMediaRecorder: MediaRecorder? = null
    var mDisplayWidth: Int = 1280
    var mDisplayHeight: Int = 720
    var storePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + File.separator
    var videoName: String? = ""
    var recordAudio: Boolean? = false;
    private val SCREEN_RECORD_REQUEST_CODE = 333
    private val SCREEN_STOP_RECORD_REQUEST_CODE = 334
    private lateinit var _result: MethodChannel.Result
    private val CHANNEL_ID = "ForegroundService Kotlin"

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_screen_recording")
            val plugin = FlutterScreenRecordingPlugin(registrar)
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
        }
        var mProjectionManager: MediaProjectionManager? = null
        var mMediaProjection: MediaProjection? = null
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //initMediaRecorder();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FlutterScreenRecordingService.startService(resultCode,registrar.activeContext(),"Foreground Service");
                }
                Handler().postDelayed({
                    mMediaProjectionCallback = MediaProjectionCallback()
                    mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data!!)
                    mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
                    mVirtualDisplay = createVirtualDisplay()
                    _result.success(true)
                }, 2000)
                return true
            } else {
                _result.success(false)
            }
        }

        return false
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "startRecordScreen") {
            try {
                _result = result
                mMediaRecorder = MediaRecorder()

                mProjectionManager = registrar.context().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?

                videoName = call.argument<String?>("name")
                recordAudio = call.argument<Boolean?>("audio")
                initMediaRecorder();
                startRecordScreen()
                //result.success(true)
            } catch (e: Exception) {
                println("Error onMethodCall startRecordScreen")
                println(e.message)
                result.success(false)
            }

        } else if (call.method == "stopRecordScreen") {
            try {
                if (mMediaRecorder != null) {
                    stopRecordScreen()
                    result.success("${storePath}${videoName}.mp4")
                } else {
                    result.success("")
                }
            } catch (e: Exception) {
                result.success("")
            }

        } else {
            result.notImplemented()
        }
    }

    fun calculeResolution(screenSize: Point) {

        //   val screenRatio: Double = (screenSize.x.toDouble() / screenSize.y.toDouble())
        //  println(screenSize.x.toString() + " --- " + screenSize.y.toString())
        // var height: Double = mDisplayWidth / screenRatio;
        //  println("height - " + height)
        //mDisplayHeight = height.toInt()

        mDisplayWidth=screenSize.x;
        mDisplayHeight=screenSize.y;
/*        mDisplayWidth = 2560;
        mDisplayHeight = 1440;*/

        //println("Scaled Density")
        //println(metrics.scaledDensity)
        // println("Original Resolution ")
        //println(metrics.widthPixels.toString() + " x " + metrics.heightPixels)
        println("Calcule Resolution ")
        println("$mDisplayWidth x $mDisplayHeight")
    }

    fun initMediaRecorder() {
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)

        if (recordAudio!!) {
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }
        //mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        if (recordAudio!!) {

            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//AAC //HE_AAC
            mMediaRecorder?.setAudioEncodingBitRate(16 * 44100);
            mMediaRecorder?.setAudioSamplingRate(44100);
        }

        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        val windowManager = registrar.context().applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()
        windowManager.defaultDisplay.getRealSize(screenSize);
        calculeResolution(screenSize);

        println(mDisplayWidth.toString() + " " + mDisplayHeight);
        mMediaRecorder?.setVideoSize(mDisplayWidth, mDisplayHeight)
        mMediaRecorder?.setVideoFrameRate(30)

        mMediaRecorder?.setOutputFile("${storePath}${videoName}.mp4")

        println("file --- " + "${storePath}${videoName}.mp4")

        mMediaRecorder?.setVideoEncodingBitRate(5 * mDisplayWidth * mDisplayHeight)
        mMediaRecorder?.prepare()
    }

    fun startRecordScreen() {
        try {
            //mMediaRecorder?.prepare()

            mMediaRecorder?.start()

        } catch (e: IOException) {
            println("ERR");
            Log.d("--INIT-RECORDER", e.message!!)
            println("Error startRecordScreen")
            println(e.message)
        }

        val permissionIntent = mProjectionManager?.createScreenCaptureIntent()
        ActivityCompat.startActivityForResult(registrar.activity(), permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)

    }

    fun stopRecordScreen() {
        try {

            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            println("stopRecordScreen success")

        } catch (e: Exception) {
            Log.d("--INIT-RECORDER", e.message!!)
            println("stopRecordScreen error")
            println(e.message)

        } finally {
            stopScreenSharing()
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        val windowManager = registrar.context().applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics: DisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenSize = Point()
        windowManager.defaultDisplay.getRealSize(screenSize);
        calculeResolution(screenSize)
        mScreenDensity = metrics.densityDpi
        println("density " + mScreenDensity.toString())
        println("msurface " + mMediaRecorder?.getSurface())
        println("aaa" + mDisplayWidth.toString() + " " + mDisplayHeight);

        return mMediaProjection?.createVirtualDisplay("MainActivity", mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder?.getSurface(), null, null)
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            if (mMediaProjection != null) {
                mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
                mMediaProjection?.stop()
                mMediaProjection = null
            }
            //FlutterScreenRecordingService.stopService(registrar.activeContext())
            Log.d("TAG", "MediaProjection Stopped")
        }
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()

            mMediaProjection = null
            stopScreenSharing()
        }
    }
}