package com.entel.copiloto

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    // Fijos / overlays
    private lateinit var tvTituloFijo: TextView
    private lateinit var fondoImage: ImageView
    private lateinit var logoFondo: ImageView
    private lateinit var btnListen: ImageButton
    private lateinit var celebrationImage: ImageView
    private lateinit var qrImageOverlayLegacy: ImageView // legacy (no se usa)

    // FAQ
    private lateinit var btnFaqMax: Button
    private lateinit var btnFaqDisney: Button
    private lateinit var btnFaqWifi: Button
    private lateinit var btnFaqCanales: Button

    // Contenedor dinámico
    private lateinit var contenedorIntencion: FrameLayout
    private lateinit var tvInstrucciones: TextView

    private val requestCodeSpeech = 100
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Permiso de micrófono (STT)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            requestCodeSpeech
        )

        // Views
        tvTituloFijo = findViewById(R.id.tvTituloFijo)
        fondoImage = findViewById(R.id.fondoImage)
        logoFondo = findViewById(R.id.logoFondo)
        btnListen = findViewById(R.id.btnListen)
        celebrationImage = findViewById(R.id.bgCelebration)
        qrImageOverlayLegacy = findViewById(R.id.qrImage)
        contenedorIntencion = findViewById(R.id.contenedorIntencion)
        tvInstrucciones = findViewById(R.id.tvInstrucciones)

        btnFaqMax = findViewById(R.id.btnFaqMax)
        btnFaqDisney = findViewById(R.id.btnFaqDisney)
        btnFaqWifi = findViewById(R.id.btnFaqWifi)
        btnFaqCanales = findViewById(R.id.btnFaqCanales)

        // Título fijo nunca se toca
        // Estado inicial: mostrar instrucciones
        showInstructions()

        // Botón OK del control remoto → micrófono
        btnListen.requestFocus()
        btnListen.setOnClickListener { startSpeechRecognition() }

        // FAQ → actualizan el contenedor
        btnFaqMax.setOnClickListener { showText("Activación de MAX: pronto disponible aquí.") }
        btnFaqDisney.setOnClickListener { showText("Activación de Disney+: pronto disponible aquí.") }
        btnFaqWifi.setOnClickListener { showText("Cambiar Wi‑Fi: en breve mostraremos pasos guiados.") }
        btnFaqCanales.setOnClickListener { showText("Explorar canales: pronto mostraremos parrilla y buscador.") }

        // Back: si hay contenido distinto a instrucciones → volver a instrucciones
        onBackPressedDispatcher.addCallback(this) {
            val showingInstructions = tvInstrucciones.parent != null && tvInstrucciones.visibility == View.VISIBLE
            if (!showingInstructions && contenedorIntencion.childCount > 0) {
                showInstructions()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            btnListen.performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Reconocimiento de voz
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora…")
        }
        try {
            startActivityForResult(intent, requestCodeSpeech)
        } catch (e: Exception) {
            showText("No se pudo iniciar el reconocimiento de voz: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeSpeech && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userSpeech = result?.get(0) ?: "No se reconoció nada"
            interpretCommand(userSpeech)
        }
    }

    // Helpers de render
    private fun showInstructions() {
        contenedorIntencion.removeAllViews()
        tvInstrucciones.visibility = View.VISIBLE
        contenedorIntencion.addView(tvInstrucciones)
        hideCelebration()
        hideLegacyQrOverlay()
    }

    private fun clearDynamicArea() {
        contenedorIntencion.removeAllViews()
        tvInstrucciones.visibility = View.GONE
    }

    private fun showText(text: String) {
        clearDynamicArea()
        val tv = TextView(this).apply {
            setText(text)
            textSize = 20f
            setLineSpacing(0f, 1.15f)
            setPadding(16, 16, 16, 16)
            setTextColor(0xFFFFFFFF.toInt())
        }
        contenedorIntencion.addView(tv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun showImage(resId: Int, contentDescription: String? = null) {
        clearDynamicArea()
        val iv = ImageView(this).apply {
            setImageResource(resId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            this.contentDescription = contentDescription
            isFocusable = true
            isFocusableInTouchMode = true
        }
        contenedorIntencion.addView(iv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun showChannelCard(nombre: String, numero: String, contratable: Boolean = false) {
        clearDynamicArea()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val title = TextView(this).apply {
            text = nombre
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "Canal $numero"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }
        container.addView(title)
        container.addView(subtitle)
        if (contratable) {
            val btn = Button(this).apply {
                text = "Contratar canal"
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener {
                    Toast.makeText(this@MainActivity, "¡Felicidades! Has contratado el canal", Toast.LENGTH_LONG).show()
                }
            }
            container.addView(btn)
        }
        contenedorIntencion.addView(container, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun showQrInContainer() {
        clearDynamicArea()
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.contactoentel)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Código QR para hablar con un ejecutivo"
        }
        val tv = TextView(this).apply {
            text = "Escanea el código para que te contactemos"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 12, 0, 0)
        }
        wrapper.addView(iv, LinearLayout.LayoutParams(200, 200))
        wrapper.addView(tv)
        contenedorIntencion.addView(wrapper, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    // Overlays
    private fun showCelebration(timeoutMs: Long = 5000L) {
        celebrationImage.setImageResource(R.drawable.celebration)
        celebrationImage.visibility = View.VISIBLE
        uiHandler.removeCallbacksAndMessages(null)
        uiHandler.postDelayed({ hideCelebration() }, timeoutMs)
    }

    private fun hideCelebration() {
        celebrationImage.visibility = View.GONE
    }

    private fun hideLegacyQrOverlay() {
        qrImageOverlayLegacy.visibility = View.GONE
    }

    // Intenciones
    private fun interpretCommand(transcript: String) {
        val lower = transcript.lowercase()
        when {
            listOf("wifi", "internet", "conexión", "se cayó", "sin red").any { lower.contains(it) } -> {
                showText("Haremos un diagnóstico para resolver tu problema de conectividad.")
            }
            listOf("disney", "disney+", "netflix", "asociar cuenta", "login").any { lower.contains(it) } -> {
                showText(
                    "Pasos para asociar tu cuenta Disney:\n" +
                            "1) Abre la app Disney+.\n" +
                            "2) Ve a “Iniciar sesión”.\n" +
                            "3) Ingresa el código en disneyplus.com/begin."
                )
            }
            listOf("hablar con alguien", "necesito ayuda", "ejecutivo", "me contacten").any { lower.contains(it) } -> {
                showQrInContainer()
            }
            listOf("noticias", "canales de noticias", "quiero ver noticias", "informativo").any { lower.contains(it) } -> {
                showText("Noticias disponibles: CNN Chile HD (54), T13 HD (56), CHV HD (66), Canal 13 HD (67).")
            }
            listOf("deportes", "canales de deporte", "ver deportes", "partido").any { lower.contains(it) } -> {
                showText("Deportes: ESPN HD (212), ESPN 2 HD (214), ESPN 3 HD (216), ESPN 4 (213), ESPN 6 (211).")
            }
            lower.contains("chilevisión") || lower.contains("chv") -> {
                showChannelCard("Chilevisión", "66")
            }
            lower.contains("mega") -> {
                showChannelCard("Mega HD", "65")
            }
            lower.contains("tnt sport") || lower.contains("tnt deportes") -> {
                showChannelCard("TNT Sports", "109")
            }
            lower.contains("playboy") -> {
                showChannelCard("Playboy", "401", contratable = true)
            }
            (listOf("eduardo", "edu", "idea").any { lower.contains(it) } &&
                    (lower.contains("gustó") || lower.contains("gusto"))) -> {
                showCelebration()
                showText("¡Gracias por el apoyo! 🎊")
            }
            else -> {
                showText("No entendí tu solicitud. ¿Puedes repetirlo de otra manera?")
            }
        }
    }
}