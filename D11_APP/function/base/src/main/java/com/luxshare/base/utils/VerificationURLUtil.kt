package com.luxshare.base.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2025/1/15 9:51
 */
object VerificationURLUtil {
    private const val TAG = "OtaVerificationUtil"
    fun isValidUrl(urlString: String?): Boolean {
        Log.i(TAG, "url=$urlString")
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("HEAD")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
            false
        }
    }
}