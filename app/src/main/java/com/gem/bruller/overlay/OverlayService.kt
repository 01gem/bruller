package com.gem.bruller.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gem.bruller.MainActivity
import com.gem.bruller.R
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayService : Service() {

    companion object {
        private const val ACTION_START = "com.gem.bruller.action.START"
        private const val ACTION_SHOW = "com.gem.bruller.action.SHOW"
        private const val ACTION_HIDE = "com.gem.bruller.action.HIDE"
        private const val ACTION_TOGGLE_LOCK = "com.gem.bruller.action.TOGGLE_LOCK"
        private const val ACTION_STOP = "com.gem.bruller.action.STOP"

        private const val CHANNEL_ID = "ruler_overlay"
        private const val NOTIFICATION_ID = 8

        private val BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val isRunning = MutableStateFlow(false)
        val isVisible = MutableStateFlow(false)
        val isLocked = MutableStateFlow(false)

        fun show(context: Context) = sendCommand(context, ACTION_SHOW)

        fun hide(context: Context) = sendCommand(context, ACTION_HIDE)

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }

        private fun sendCommand(context: Context, action: String) {
            val intent = Intent(context, OverlayService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: RulerOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var viewAttached = false
    private var visible = false
    private var locked = false
    private var foregroundStarted = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        overlayView?.applySettings(OverlaySettings.load(applicationContext))
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        OverlaySettings.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdown()
                return START_NOT_STICKY
            }

            ACTION_HIDE -> hideOverlay()
            ACTION_TOGGLE_LOCK -> setLocked(!locked)
            ACTION_SHOW, ACTION_START, null -> showOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            updateForegroundNotification()
            Toast.makeText(this, "Allow \"Display over other apps\" first.", Toast.LENGTH_LONG).show()
            shutdown()
            return
        }

        if (overlayView == null) {
            val view = RulerOverlayView(this).apply {
                onClose = { shutdown() }
                onLock = { setLocked(true) }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                BASE_FLAGS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                title = "Ruler for 8 Ball"
            }
            overlayView = view
            layoutParams = params
        }

        val view = overlayView ?: return
        val params = layoutParams ?: return

        if (!viewAttached) {
            val added = runCatching { windowManager.addView(view, params) }.isSuccess
            if (!added) {
                Toast.makeText(this, "Could not show the overlay.", Toast.LENGTH_LONG).show()
                shutdown()
                return
            }
            viewAttached = true
        }

        visible = true
        isVisible.value = true
        applyWindowFlags()
        updateForegroundNotification()
    }

    private fun hideOverlay() {
        if (viewAttached) {
            overlayView?.let { runCatching { windowManager.removeView(it) } }
            viewAttached = false
        }
        visible = false
        isVisible.value = false
        updateForegroundNotification()
    }

    private fun setLocked(value: Boolean) {
        locked = value
        isLocked.value = value
        overlayView?.isLocked = value
        if (viewAttached) {
            applyWindowFlags()
        }
        if (value) {
            Toast.makeText(
                this,
                "Ruler locked — unlock from the notification.",
                Toast.LENGTH_SHORT
            ).show()
        }
        updateForegroundNotification()
    }

    private fun applyWindowFlags() {
        if (!viewAttached) return
        val view = overlayView ?: return
        val params = layoutParams ?: return
        params.flags = BASE_FLAGS or if (locked) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            0
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun updateForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun buildNotification(): Notification {
        val status = when {
            !visible -> getString(R.string.notification_hidden)
            locked -> getString(R.string.notification_locked)
            else -> getString(R.string.notification_active)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ruler_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(status)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                0,
                getString(if (locked) R.string.action_unlock else R.string.action_lock),
                servicePendingIntent(ACTION_TOGGLE_LOCK, 1)
            )
            .addAction(
                0,
                getString(if (visible) R.string.action_hide else R.string.action_show),
                servicePendingIntent(if (visible) ACTION_HIDE else ACTION_SHOW, 2)
            )
            .addAction(
                0,
                getString(R.string.action_stop),
                servicePendingIntent(ACTION_STOP, 3)
            )
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, OverlayService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun shutdown() {
        if (viewAttached) {
            overlayView?.let { runCatching { windowManager.removeView(it) } }
            viewAttached = false
        }
        overlayView = null
        layoutParams = null
        visible = false
        locked = false
        isVisible.value = false
        isLocked.value = false
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        if (viewAttached) {
            overlayView?.let { runCatching { windowManager.removeView(it) } }
            viewAttached = false
        }
        runCatching {
            OverlaySettings.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
        isRunning.value = false
        isVisible.value = false
        isLocked.value = false
        super.onDestroy()
    }
}
