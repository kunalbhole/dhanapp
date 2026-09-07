package com.dhan.app.backup

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DriveBackupFile(val id: String, val modifiedTimeMillis: Long, val sizeBytes: Long)

/**
 * Minimal hand-rolled REST client for the Drive v3 "appDataFolder" (list/upload/download) —
 * deliberately not the official google-api-services-drive client library, which drags in
 * google-http-client + Guava on top of an already Google-Maven-only dependency chain for
 * three calls we can make directly. Every request/response here is the hidden per-app
 * folder (`spaces=appDataFolder`), never the user's visible Drive.
 */
class DriveBackupClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    class DriveException(val code: Int, message: String) : IOException(message)

    /** Null if this account has no backup yet. */
    fun findBackupFile(accessToken: String): DriveBackupFile? {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME'")
            .addQueryParameter("fields", "files(id,modifiedTime,size)")
            .build()
        val request = Request.Builder().url(url).get().authorized(accessToken).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw DriveException(resp.code, "Drive list failed: HTTP ${resp.code}")
            val files = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            val f = files.getJSONObject(0)
            return DriveBackupFile(
                id = f.getString("id"),
                modifiedTimeMillis = parseRfc3339(f.optString("modifiedTime")),
                sizeBytes = f.optString("size", "0").toLongOrNull() ?: 0L,
            )
        }
    }

    /** Creates the backup file on first upload, or overwrites it in place on every
     *  subsequent upload — one backup per account, no version history (per spec). */
    fun upload(accessToken: String, existingFileId: String?, bytes: ByteArray) {
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            if (existingFileId == null) put("parents", JSONArray().put("appDataFolder"))
        }
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(MultipartBody.Part.create(metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())))
            .addPart(MultipartBody.Part.create(bytes.toRequestBody("application/octet-stream".toMediaType())))
            .build()

        val url = if (existingFileId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
        }
        val request = Request.Builder()
            .url(url)
            .method(if (existingFileId == null) "POST" else "PATCH", body)
            .authorized(accessToken)
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw DriveException(resp.code, "Drive upload failed: HTTP ${resp.code}")
        }
    }

    fun download(accessToken: String, fileId: String): ByteArray {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder().url(url).get().authorized(accessToken).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw DriveException(resp.code, "Drive download failed: HTTP ${resp.code}")
            return resp.body?.bytes() ?: throw DriveException(0, "Drive download returned an empty body")
        }
    }

    private fun Request.Builder.authorized(accessToken: String): Request.Builder =
        header("Authorization", "Bearer $accessToken")

    /** Second-precision is plenty for a "last backed up" display; avoids java.time, which
     *  needs API 26+ (this app's minSdk is 24). */
    private fun parseRfc3339(value: String): Long {
        val m = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})""").find(value) ?: return 0L
        val (y, mo, d, h, mi, s) = m.destructured
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(y.toInt(), mo.toInt() - 1, d.toInt(), h.toInt(), mi.toInt(), s.toInt())
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val BACKUP_FILE_NAME = "dhan_backup.enc"
    }
}
