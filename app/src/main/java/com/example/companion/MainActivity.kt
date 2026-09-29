package com.example.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var ipInput: EditText

    // Обработчик запроса прав на камеру и микрофон
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            checkOverlayPermissionAndStart()
        } else {
            Toast.makeText(this, "Без камеры и микрофона ассистент не сможет работать", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем интерфейс прямо в коде, чтобы не плодить лишние файлы
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            gravity = Gravity.CENTER
        }

        ipInput = EditText(this).apply {
            hint = "IP сервера (IP:PORT)"
            setText("http://192.168.0.102:1234") // Твой адрес уже вписан по умолчанию
            textSize = 18f
        }

        val startButton = Button(this).apply {
            text = "Запустить ассистента"
            setOnClickListener {
                requestBasicPermissions()
            }
        }

        layout.addView(ipInput)
        layout.addView(startButton)
        setContentView(layout)
    }

    private fun requestBasicPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val needRequest = permissions.any { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (needRequest) {
            requestPermissionsLauncher.launch(permissions)
        } else {
            checkOverlayPermissionAndStart()
        }
    }

    private fun checkOverlayPermissionAndStart() {
        // Проверяем, может ли приложение рисовать плавающую кнопку поверх игр
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Дай разрешение на отображение поверх других окон и нажми кнопку снова", Toast.LENGTH_LONG).show()
        } else {
            startCompanionService()
        }
    }

    private fun startCompanionService() {
        val ip = ipInput.text.toString()
        val intent = Intent(this, CaptureService::class.java).apply {
            putExtra("SERVER_IP", ip)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // Закрываем главное окно, оставляем только плавающий оверлей
        finish()
    }
}
