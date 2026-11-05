package se.majid.archlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Backup for boot. When this app is the default launcher, Android opens
 * LauncherActivity on boot anyway; this receiver just helps on devices
 * where it is not (Android 10+ may block starting activities from here).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        OrientationService.start(context)
        val home = Intent(context, LauncherActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(home) } catch (_: Exception) {
            LauncherActivity.startTarget(context)
        }
    }
}
