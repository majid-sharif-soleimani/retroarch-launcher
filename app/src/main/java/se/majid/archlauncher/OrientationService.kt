package se.majid.archlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager

/**
 * Forces the whole screen into landscape, for every app (RetroArch included).
 *
 * How: an invisible 0x0 overlay window that requests landscape orientation.
 * The system uses the overlay's orientation for the display, so the phone
 * stays landscape no matter what the app in front asks for.
 * Needs the "Display over other apps" permission.
 *
 * Fallback: if "Modify system settings" is allowed, auto-rotate is turned
 * off and the rotation is locked to landscape as well.
 */
class OrientationService : Service() {

    private var overlay: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        goForeground()
        lockSystemRotation(this)
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlay == null) addOverlay()
        lockSystemRotation(this)
        return START_STICKY
    }

    override fun onDestroy() {
        overlay?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
        super.onDestroy()
    }

    private fun addOverlay() {
        if (!canDrawOverlays(this)) return
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            0, 0, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        val view = View(this)
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(view, params)
            overlay = view
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun goForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Landscape lock", NotificationManager.IMPORTANCE_MIN)
            )
            Notification.Builder(this, CHANNEL)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("Landscape lock is on")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL = "landscape_lock"
        private const val NOTIF_ID = 1

        fun canDrawOverlays(ctx: Context): Boolean =
            Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

        fun start(ctx: Context) {
            val i = Intent(ctx, OrientationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
            } catch (_: Exception) {
                lockSystemRotation(ctx)
            }
        }

        /** Locks rotation via system settings when that permission is granted. */
        fun lockSystemRotation(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(ctx)) return
            try {
                val cr = ctx.contentResolver
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, Surface.ROTATION_90)
            } catch (_: Exception) {}
        }
    }
}
