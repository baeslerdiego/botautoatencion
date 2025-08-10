package com.entel.copiloto

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvTranscript: TextView
    private lateinit var qrImage: ImageView
    private lateinit var celebrationImage: ImageView
    private lateinit var fondoImage: ImageView
    private lateinit var logoFondo: ImageView
    private lateinit var btnListen: ImageButton

    private lateinit var btnFaqMax: Button
    private lateinit var btnFaqDisney: Button
    private lateinit var btnFaqWifi: Button
    private lateinit var btnFaqCanales: Button

    private val requestCodeSpeech = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            requestCodeSpeech
        )

        // Vistas
        tvTranscript = findViewById(R.id.tvTranscript)
        qrImage = findViewById(R.id.qrImage)
        celebrationImage = findViewById(R.id.bgCelebration)
        fondoImage = findViewById(R.id.fondoImage)
        logoFondo = findViewById(R.id.logoFondo)
        btnListen = findViewById(R.id.btnListen)

        btnFaqMax = findViewById(R.id.btnFaqMax)
        btnFaqDisney = findViewById(R.id.btnFaqDisney)
        btnFaqWifi = findViewById(R.id.btnFaqWifi)
        btnFaqCanales = findViewById(R.id.btnFaqCanales)

        btnListen.requestFocus()

        btnListen.setOnClickListener { startSpeechRecognition() }

        btnFaqMax.setOnClickListener {
            tvTranscript.text = "Prueba botón \"Activar Max\""
        }

        btnFaqDisney.setOnClickListener {
            tvTranscript.text = "Prueba botón \"Activar Disney+\""
        }

        btnFaqWifi.setOnClickListener {
            tvTranscript.text = "Prueba botón \"Cambiar contraseña Wi‑Fi\""
        }

        btnFaqCanales.setOnClickListener {
            tvTranscript.text = "Prueba botón \"Ver canales\""
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            btnListen.performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora…")
        }
        try {
            startActivityForResult(intent, requestCodeSpeech)
        } catch (e: Exception) {
            tvTranscript.text = "Error: ${e.message}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeSpeech && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userSpeech = result?.get(0) ?: "No se reconoció nada"
            val response = interpretCommand(userSpeech)
            tvTranscript.text = response
        }
    }

    private fun interpretCommand(transcript: String): String {
        val lower = transcript.lowercase()
        qrImage.visibility = View.GONE
        celebrationImage.visibility = View.GONE

        return when {
            listOf("wifi", "internet", "conexión", "se cayó", "sin red").any { lower.contains(it) } ->
                "Haremos un diagnóstico para resolver tu problema de conectividad."

            listOf("disney", "disney+", "netflix", "asociar cuenta", "login").any { lower.contains(it) } ->
                "Aquí están los pasos para asociar tu cuenta Disney:\n1. Abre la app Disney+.\n2. Ve a “Iniciar sesión”.\n3. Ingresa el código en disneyplus.com/begin."

            listOf("hablar con alguien", "necesito ayuda", "ejecutivo", "me contacten").any { lower.contains(it) } -> {
                qrImage.setImageResource(R.drawable.contactoentel)
                qrImage.visibility = View.VISIBLE
                "Te contactará un ejecutivo a la brevedad. Escanea el código QR en pantalla."
            }

            listOf("noticias", "canales de noticias", "quiero ver noticias", "informativo").any { lower.contains(it) } ->
                "Algunos canales de noticias disponibles son: CNN Chile HD (54), T13 HD (56), CHV HD (66), Canal 13 HD (67)."

            listOf("deportes", "canales de deporte", "ver deportes", "partido").any { lower.contains(it) } ->
                "Canales deportivos disponibles: ESPN HD (212), ESPN 2 HD (214), ESPN 3 HD (216), ESPN 4 (213), ESPN 6 (211)."

            lower.contains("chilevisión") || lower.contains("chv") ->
                "Chilevisión está disponible en el canal 66."

            lower.contains("mega") ->
                "Mega HD está en el canal 65."

            lower.contains("tnt sport") || lower.contains("tnt deportes") ->
                "TNT Sports está disponible en el canal 109."

            lower.contains("playboy") ->
                "El canal Playboy está en el 401 pero actualmente tu plan no lo tiene contratado. ¿Te gustaría contratarlo?"

            (listOf("eduardo", "edu", "idea").any { lower.contains(it) } && (lower.contains("gustó") || lower.contains("gusto"))) -> {
                celebrationImage.setImageResource(R.drawable.celebration)
                celebrationImage.visibility = View.VISIBLE
                "¡Gracias por el apoyo! 🎊"
            }

            else -> "No entendí tu solicitud. ¿Puedes repetirlo de otra manera?"
        }
    }
}
