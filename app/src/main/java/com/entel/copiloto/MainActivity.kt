package com.entel.copiloto

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.Gravity
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
    private lateinit var qrImageOverlayLegacy: ImageView // compat. antigua

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

        // Permisos
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

        // Estado inicial
        showInstructions()

        // Mic
        btnListen.requestFocus()
        btnListen.setOnClickListener { startSpeechRecognition() }

        // FAQs
        btnFaqMax.setOnClickListener { showImage(R.drawable.max, "Instrucciones para activar Max") }
        btnFaqDisney.setOnClickListener { showImage(R.drawable.disney, "Instrucciones para activar Disney+") }
        btnFaqWifi.setOnClickListener { showWifiCard() }
        btnFaqCanales.setOnClickListener { showMisCanales() }

        // Back → limpia contenedor (vuelve a instrucciones)
        onBackPressedDispatcher.addCallback(this) {
            val showingInstructions =
                tvInstrucciones.parent != null && tvInstrucciones.visibility == View.VISIBLE
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

    // ---------------- Reconocimiento de voz ----------------
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
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

    // ---------------- Utilidades de render ----------------
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
        contenedorIntencion.addView(
            tv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    private fun showImage(resId: Int, contentDescription: String? = null) {
        clearDynamicArea()
        val iv = ImageView(this).apply {
            setImageResource(resId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            this.contentDescription = contentDescription
            isFocusable = true
            isFocusableInTouchMode = true
        }
        contenedorIntencion.addView(
            iv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    // ---------------- Cards ----------------

    /** Cambiar Wi‑Fi – mismo estilo (bg_card) */
    private fun showWifiCard() {
        clearDynamicArea()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            background = resources.getDrawable(R.drawable.bg_card, theme)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // Header: icono + título
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val wifiIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_wifi)
            contentDescription = "Icono Wi‑Fi"
        }
        val title = TextView(this).apply {
            text = "Redes unificadas"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 0, 0, 0)
        }
        header.addView(wifiIcon, LinearLayout.LayoutParams(24, 24))
        header.addView(title)
        card.addView(header)

        // Subtítulo
        val subtitle = TextView(this).apply {
            text = "Tu red 2.4 GHz y 5 GHz está unida"
            textSize = 16f
            setTextColor(0xB3FFFFFF.toInt())
            setPadding(0, 6, 0, 12)
        }
        card.addView(subtitle)

        // Nombre de red
        val ssidRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val ssidLabel = TextView(this).apply {
            text = "Nombre de red:"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val ssidValue = TextView(this).apply {
            text = "  ExperienciaTV"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        ssidRow.addView(ssidLabel)
        ssidRow.addView(ssidValue)
        card.addView(ssidRow)

        // Clave
        val passRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 0)
        }
        val passLabel = TextView(this).apply {
            text = "Clave:"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val passValue = TextView(this).apply {
            text = "  ************"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        passRow.addView(passLabel)
        passRow.addView(passValue)
        card.addView(passRow)

        // Botones (mismo estilo de FAQs)
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val btnCambiarNombre = Button(this).apply {
            text = "Cambiar nombre de red"
            isFocusable = true
            isFocusableInTouchMode = true
            background = resources.getDrawable(R.drawable.btn_highlight, theme)
            setTextColor(Color.WHITE)
        }
        val btnCambiarClave = Button(this).apply {
            text = "Cambiar clave"
            isFocusable = true
            isFocusableInTouchMode = true
            background = resources.getDrawable(R.drawable.btn_highlight, theme)
            setTextColor(Color.WHITE)
        }
        val lpWeight =
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 12, 0)
            }
        val lpWeightRight =
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        btnRow.addView(btnCambiarNombre, lpWeight)
        btnRow.addView(btnCambiarClave, lpWeightRight)
        card.addView(btnRow)

        // Advertencia
        val warnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
            gravity = Gravity.START
        }
        val warnIcon = TextView(this).apply {
            text = "⚠"
            textSize = 18f
            setTextColor(0xFFFFD54F.toInt())
            setPadding(0, 0, 6, 0)
        }
        val warnText = TextView(this).apply {
            text =
                "Al cambiar el nombre o la clave, todos los dispositivos se desconectarán y deberás volver a conectarlos."
            textSize = 14f
            setTextColor(0xFFB0BEC5.toInt())
        }
        warnRow.addView(warnIcon)
        warnRow.addView(warnText)
        card.addView(warnRow)

        contenedorIntencion.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )

        // Acciones mock
        btnCambiarNombre.setOnClickListener {
            Toast.makeText(this, "Próximamente: cambio de nombre de red", Toast.LENGTH_SHORT).show()
        }
        btnCambiarClave.setOnClickListener {
            Toast.makeText(this, "Próximamente: cambio de clave", Toast.LENGTH_SHORT).show()
        }
    }

    /** Mis Canales – SOLO vertical más compacto; horizontal se mantiene */
    private fun showMisCanales() {
        clearDynamicArea()
        val view = layoutInflater.inflate(R.layout.card_mis_canales, contenedorIntencion, false)
        contenedorIntencion.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }


    // ---------------- Overlays ----------------
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

    // ---------------- Intenciones (voz) ----------------
    private fun interpretCommand(transcript: String) {
        val lower = transcript.lowercase()
        when {
            // Max
            listOf("hbo max", "activar max", "quiero hbo", "quiero max", "max", "hbo")
                .any { lower.contains(it) } ->
                showImage(R.drawable.max, "Instrucciones para activar Max")

            // Disney
            listOf("disney+", "disney plus", "activar disney", "quiero disney", "disney")
                .any { lower.contains(it) } ->
                showImage(R.drawable.disney, "Instrucciones para activar Disney+")

            // Cambiar Wi‑Fi
            listOf(
                "cambiar clave wifi", "contraseña wifi", "contrasena wifi",
                "cambiar clave", "cambiar wifi", "clave de wifi",
                "wifi", "wi-fi", "internet", "conexión", "conexion", "se cayó", "se cayo", "sin red"
            ).any { lower.contains(it) } -> showWifiCard()

            // Mis Canales / Ver canales / Canales premium
            listOf("mis canales", "ver canales", "canales premium")
                .any { lower.contains(it) } -> showMisCanales()

            // Hablar con ejecutivo -> QR en contenedor
            listOf(
                "hablar con ejecutivo", "quiero hablar con un ejecutivo",
                "ejecutivo", "atención", "atencion", "contactar",
                "necesito ayuda", "me contacten"
            ).any { lower.contains(it) } -> showQrInContainer()

            // Info general
            listOf("canales de noticias", "quiero ver noticias", "informativo", "noticias")
                .any { lower.contains(it) } ->
                showText("Noticias disponibles: CNN Chile HD (54), T13 HD (56), CHV HD (66), Canal 13 HD (67).")

            listOf("canales de deporte", "ver deportes", "partido", "deportes")
                .any { lower.contains(it) } ->
                showText("Deportes: ESPN HD (212), ESPN 2 HD (214), ESPN 3 HD (216), ESPN 4 (213), ESPN 6 (211).")

            // Canales específicos
            lower.contains("chilevisión") || lower.contains("chilevision") || lower.contains("chv") ->
                showChannelCard("Chilevisión", "66")
            lower.contains("mega") ->
                showChannelCard("Mega HD", "65")
            lower.contains("tnt sports") || lower.contains("tnt sport") || lower.contains("tnt deportes") ->
                showChannelCard("TNT Sports", "109")
            lower.contains("playboy") || lower.contains("canal 401") || lower.contains("quiero playboy") ->
                showChannelCard("Playboy", "401", contratable = true)

            // Easter egg
            (listOf("eduardo", "edu", "idea").any { lower.contains(it) } &&
                    (lower.contains("gustó") || lower.contains("gusto"))) -> {
                showCelebration()
                showText("¡Gracias por el apoyo! 🎊")
            }

            else -> showText("No entendí tu solicitud. ¿Puedes repetirlo de otra manera?")
        }
    }

    // Tarjeta simple para canales específicos (la de siempre)
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
                    Toast.makeText(
                        this@MainActivity,
                        "¡Felicidades! Has contratado el canal",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            container.addView(btn)
        }
        contenedorIntencion.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    private fun showQrInContainer() {
        clearDynamicArea()
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
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
        contenedorIntencion.addView(
            wrapper,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }
}
