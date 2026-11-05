package se.majid.archlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView

/**
 * Home-screen activity whose only job is to (re)launch the target app.
 * Pressing Home, rebooting, or the target app closing all bring us here,
 * and we immediately start the target again.
 */
class LauncherActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 18f
            text = "Starting…"
        }
        setContentView(status)
    }

    override fun onResume() {
        super.onResume()
        if (ensureLandscapePermission()) {
            OrientationService.start(this)
            launchTarget(this)
        }
    }

    /**
     * Asks once for "Display over other apps", which the landscape lock needs.
     * Returns false while the settings screen is being shown.
     */
    private fun ensureLandscapePermission(): Boolean {
        if (OrientationService.canDrawOverlays(this)) return true
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        if (prefs.getBoolean("overlay_asked", false)) return true
        prefs.edit().putBoolean("overlay_asked", true).apply()
        status.text = "Allow \"Display over other apps\"\nto keep the screen in landscape."
        return try {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            false
        } catch (_: Exception) { true }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (OrientationService.canDrawOverlays(this)) OrientationService.start(this)
        launchTarget(this)
    }

    private fun launchTarget(ctx: Context) {
        handler.removeCallbacksAndMessages(null)
        if (!startTarget(ctx)) {
            status.text = "RetroArch is not installed.\n\nRetrying…"
            // Retry until the target app is installed/available.
            handler.postDelayed({ launchTarget(ctx) }, 3000)
        }
    }

    // Disable Back on the launcher screen.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* do nothing */ }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        fun startTarget(ctx: Context): Boolean {
            val pm = ctx.packageManager
            val intent = BuildConfig.TARGET_PACKAGES
                .firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) } ?: return false
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            return try { ctx.startActivity(intent); true } catch (e: Exception) { false }
        }
    }
}
