package com.Azelmods.App.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RootDetection - Detects whether the device is rooted using multiple heuristics.
 *
 * None of these checks are 100% foolproof in isolation; combining them raises
 * the detection confidence significantly. Results should be used to inform
 * security decisions (e.g. warn the user, restrict sensitive features) rather
 * than as a hard enforcement gate on their own.
 */
@Singleton
class RootDetection @Inject constructor(
    private val context: Context
) {

    // ── Public data contract ───────────────────────────────────────────────────

    /**
     * Summarises the root-detection outcome.
     *
     * @param isRooted   true when at least one indicator was found.
     * @param indicators Human-readable list of the triggered checks.
     */
    data class RootStatus(
        val isRooted: Boolean,
        val indicators: List<String>
    )

    // ── High-level API ─────────────────────────────────────────────────────────

    /**
     * Quick check – returns true if any root indicator is detected.
     * Prefer [getDetailedStatus] when you need to know *which* checks fired.
     */
    fun isRooted(): Boolean = getDetailedStatus().isRooted

    /**
     * Runs all heuristics and aggregates results into a [RootStatus].
     */
    fun getDetailedStatus(): RootStatus {
        val indicators = mutableListOf<String>()

        if (checkSuperuserApk())    indicators.add("Superuser / Magisk app installed")
        if (checkSuBinary())        indicators.add("'su' binary found in system path")
        if (checkBusybox())         indicators.add("'busybox' binary found in system path")
        if (checkBuildTags())       indicators.add("Build signed with test-keys")
        if (checkSystemWritable())  indicators.add("System partition appears writable")

        return RootStatus(
            isRooted = indicators.isNotEmpty(),
            indicators = indicators.toList()
        )
    }

    // ── Individual checks ──────────────────────────────────────────────────────

    /**
     * Checks for well-known superuser / root-management packages.
     * Magisk, SuperSU, KingRoot, etc. all leave a package footprint.
     */
    fun checkSuperuserApk(): Boolean {
        val rootPackages = listOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.topjohnwu.magisk",
            "me.phh.superuser",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "com.yellowes.su",
            "com.ryja.kc.framaroot"
        )
        return rootPackages.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Searches well-known filesystem paths for a `su` executable.
     * Root access typically requires placing `su` in one of these locations.
     */
    fun checkSuBinary(): Boolean {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/app/Superuser.apk"
        )
        return suPaths.any { path -> File(path).exists() }
    }

    /**
     * Searches for `busybox`, a multi-call Unix utility commonly installed
     * alongside root access to provide shell utilities not present on stock ROM.
     */
    fun checkBusybox(): Boolean {
        val busyboxPaths = listOf(
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/data/local/xbin/busybox",
            "/data/local/bin/busybox",
            "/data/local/busybox",
            "/sbin/busybox"
        )
        return busyboxPaths.any { path -> File(path).exists() }
    }

    /**
     * Inspects [Build.TAGS] for `"test-keys"`.
     *
     * Official production builds are signed with release keys and carry the tag
     * `"release-keys"`. A `"test-keys"` tag is a strong signal that the ROM has
     * been custom-built or modified.
     */
    fun checkBuildTags(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    /**
     * Attempts to create a file under `/system/`.
     *
     * On a non-rooted device this will always throw a [SecurityException] or
     * [java.io.IOException] because the system partition is mounted read-only.
     * If the write succeeds the device is rooted (or has a very unusual ROM).
     *
     * The test file is deleted immediately if creation somehow succeeds, so
     * there are no lasting side-effects.
     */
    fun checkSystemWritable(): Boolean {
        return try {
            val probe = File("/system/.azelgram_probe_${System.currentTimeMillis()}")
            val created = probe.createNewFile()
            if (created) probe.delete()
            created
        } catch (_: Exception) {
            // Expected on non-rooted devices: Permission denied / read-only fs
            false
        }
    }
}
