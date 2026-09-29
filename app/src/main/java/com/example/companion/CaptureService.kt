package com.example.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CaptureService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var modeButton: Button
    private lateinit var pttButton: Button
    private lateinit var closeButton: Button

    private var serverIp = "http://192.168.0.102:1234"
    private val client = OkHttpClient()

    private var currentModeIndex = 0
    private val modes = arrayOf("🖥 Экран", "📷 Задняя", "🤳 Фронталка")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("SERVER_IP")?.let { serverIp = it }
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "COMPANION_CHANNEL")
            .setContentTitle("AI Companion активен")
            .setContentText("Режим: ${modes[currentModeIndex]}")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
        showOverlay()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#80000000")) // Полупрозрачный фон
            setPadding(16, 16, 16, 16)
        }

        // Кнопка смены режима
        modeButton = Button(this).apply {
            text = modes[currentModeIndex]
            setOnClickListener {
                currentModeIndex = (currentModeIndex + 1) % modes.size
                text = modes[currentModeIndex]
            }
        }

        // Кнопка Push-to-Talk
        pttButton = Button(this).apply {
            text = "🎤 Удерживать"
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        text = "Слушаю..."
                        setBackgroundColor(Color.RED)
                        // TODO: Старт записи звука и захвата кадра
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        text = "🎤 Удерживать"
                        setBackgroundColor(Color.LTGRAY)
                        sendDataToLMStudio() // Отправка на ПК
                        true
                    }
                    else -> false
                }
            }
        }

        // Кнопка закрытия
        closeButton = Button(this).apply {
            text = "✖ Закрыть"
            setOnClickListener {
                stopSelf()
            }
        }

        overlayView.addView(modeButton)
        overlayView.addView(pttButton)
        overlayView.addView(closeButton)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(overlayView, params)
    }

    private fun sendDataToLMStudio() {
        // Формируем JSON в формате OpenAI Vision для LM Studio
        val json = JSONObject()
        val messages = JSONArray()
        val message = JSONObject()
        message.put("role", "user")

        val contentArray = JSONArray()
        
        val textContent = JSONObject()
        textContent.put("type", "text")
        textContent.put("text", "Что ты видишь на этом кадре? [Тест связи]")
        contentArray.put(textContent)

        message.put("content", contentArray)
        messages.put(message)
        json.put("messages", messages)
        json.put("temperature", 0.7)

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("$serverIp/v1/chat/completions")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "COMPANION_CHANNEL",
                "Companion Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized && ::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
