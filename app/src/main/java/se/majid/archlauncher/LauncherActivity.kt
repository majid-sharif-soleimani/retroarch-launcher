package se.majid.archlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.os.Build
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
        if (ensureLandscapePermissions()) {
            OrientationService.start(this)
            OrientationService.lockSystemRotation(this)
            launchTarget(this)
        }
    }

    /**
     * The landscape lock needs "Display over other apps" (main method) and
     * "Modify system settings" (backup method). Each missing one is asked
     * for once per boot, so a missed prompt is shown again after a restart.
     * Returns false while a settings screen is being shown.
     */
    private fun ensureLandscapePermissions(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        if (!Settings.canDrawOverlays(this) && !askedOverlay) {
            askedOverlay = true
            if (openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "Turn ON \"Display over other apps\"\nfor RetroArch Launcher, then press Back.")) return false
        }
        if (!Settings.System.canWrite(this) && !askedWriteSettings) {
            askedWriteSettings = true
            if (openSettings(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    "Turn ON \"Modify system settings\"\nfor RetroArch Launcher, then press Back.")) return false
        }
        return true
    }

    private fun openSettings(action: String, message: String): Boolean {
        status.text = message
        return try {
            startActivity(Intent(action, Uri.parse("package:$packageName")))
            true
        } catch (_: Exception) {
            try { startActivity(Intent(action)); true } catch (_: Exception) { false }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        OrientationService.start(this)
        OrientationService.lockSystemRotation(this)
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
        // Per-process flags: reset on reboot, so missed prompts come back.
        private var askedOverlay = false
        private var askedWriteSettings = false

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
