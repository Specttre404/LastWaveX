package com.lastwave.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val releaseNotes: String = "",
    val releaseUrl: String = "https://github.com/specttre404/LastWaveX/releases",
    val isDismissed: Boolean = false,
    val message: String? = null,
)

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _updateInfo = MutableStateFlow(
        UpdateInfo(
            currentVersion = getCurrentVersion(),
        )
    )
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    fun getCurrentVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "4.1.0"
    } catch (_: Exception) {
        "4.1.0"
    }

    fun checkForUpdate(isSilent: Boolean = false) {
        val currentVer = getCurrentVersion()
        if (!isSilent) {
            _updateInfo.update { it.copy(isChecking = true, message = "Checking for updates...") }
        }
        scope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    val url = URL("https://api.github.com/repos/specttre404/LastWaveX/releases/latest")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "LASTWAVEX-App")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val tag = json.optString("tag_name", "").removePrefix("v").trim()
                        val htmlUrl = json.optString("html_url", "https://github.com/specttre404/LastWaveX/releases")
                        val bodyText = json.optString("body", "")
                        val isNewer = isVersionNewer(tag, currentVer)
                        UpdateInfo(
                            isChecking = false,
                            isUpdateAvailable = isNewer,
                            latestVersion = tag,
                            currentVersion = currentVer,
                            releaseNotes = bodyText,
                            releaseUrl = htmlUrl,
                            message = if (isNewer) "Version v$tag available!" else "LASTWAVEX is up to date (v$currentVer)",
                        )
                    } else {
                        UpdateInfo(
                            isChecking = false,
                            currentVersion = currentVer,
                            message = if (!isSilent) "LASTWAVEX is up to date (v$currentVer)" else null,
                        )
                    }
                }
                _updateInfo.value = info
            } catch (_: Exception) {
                _updateInfo.update {
                    it.copy(
                        isChecking = false,
                        message = if (!isSilent) "Unable to check updates. Visit GitHub releases." else null,
                    )
                }
            }
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(lParts.size, cParts.size)) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun dismissUpdate(version: String) {
        _updateInfo.update { it.copy(isDismissed = true) }
    }

    fun openUpdate(context: Context) {
        val url = _updateInfo.value.releaseUrl.ifBlank { "https://github.com/specttre404/LastWaveX/releases" }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
