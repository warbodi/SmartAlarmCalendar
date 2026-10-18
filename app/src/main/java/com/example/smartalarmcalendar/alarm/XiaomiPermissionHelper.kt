package com.example.smartalarmcalendar.alarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object XiaomiPermissionHelper {

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return manufacturer.equals("Xiaomi", ignoreCase = true) ||
                manufacturer.equals("Redmi", ignoreCase = true) ||
                manufacturer.equals("POCO", ignoreCase = true)
    }

    fun openXiaomiPermissionSettings(context: Context) {
        val packageName = context.packageName

        // Try Xiaomi Security Center permission editor
        try {
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(miuiIntent)
            return
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try Xiaomi App Info page
        try {
            val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appInfoIntent)
            return
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to Overlay permission
        try {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(overlayIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
