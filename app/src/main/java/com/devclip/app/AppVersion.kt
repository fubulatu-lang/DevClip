package com.devclip.app

import android.content.Context
import android.os.Build

/**
 * The version this phone is running, and nothing else.
 *
 * Kept apart from [Changelog] on purpose. Settings shows the version on every
 * visit and the gate checks it on every launch; neither has any business
 * touching the release history to do it. Do not derive this from
 * `Changelog.entries.first().version` — the two are made to agree by the
 * build (see `checkVersion` in app/build.gradle.kts), not by one reading the
 * other.
 *
 * Read from the installed package rather than BuildConfig, so it is what the
 * phone believes it is running, not what the source said when it compiled.
 * `versionName` in app/build.gradle.kts is where the number is written.
 */
object AppVersion {

    /** "1.4.0", or null if the package manager could not say. */
    fun name(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }

    /** The number Android compares when deciding whether an install is an update. */
    fun code(context: Context): Long? = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    } catch (e: Exception) {
        null
    }
}
