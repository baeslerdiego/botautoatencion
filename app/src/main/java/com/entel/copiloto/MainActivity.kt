package com.entel.copiloto

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    // UI principales
    private lateinit var btnListen: ImageButton
    private lateinit var qrImage: ImageView
    private lateinit var celebrationImage: ImageView
    private lateinit var contenedorIntencion: FrameLayout
    private lateinit var tvInstrucciones: TextView

    // FAQs
    private lateinit var btnFaqMax: Button
    private lateinit var btnFaqDisney: Button
    private lateinit var btnFaqWifi: Button
    private lateinit var btnFaqCanales: Button
    private lateinit var btnFaq5G: Button

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
        btnListen = findViewById(R.id.btnListen)
        qrImage = findViewById(R.id.qrImage)
        celebrationImage = findViewById(R.id.bgCelebration)
        contenedorIntencion = findViewById(R.id.contenedorIntencion)
        tvInstrucciones = findViewById(R.id.tvInstrucciones)

        btnFaqMax = findViewById(R.id.btnFaqMax)
        btnFaqDisney = findViewById(R.id.btnFaqDisney)
        btnFaqWifi = findViewById(R.id.btnFaqWifi)
        btnFaqCanales = findViewById(R.id.btnFaqCanales)
        btnFaq5G = findViewById(R.id.btnFaq5G)

        btnListen.requestFocus()

        // Clicks FAQ
        btnListen.setOnClickListener { startSpeechRecognition() }
        btnFaqMax.setOnClickListener { showImage(R.drawable.max, "Instrucciones para activar Max") }
        btnFaqDisney.setOnClickListener { showImage(R.drawable.disney, "Instrucciones para activar Disney+") }
        btnFaqWifi.setOnClickListener { showWifiCard() }
        btnFaqCanales.setOnClickListener { showMisCanales() }
        btnFaq5G.setOnClickListener { show5GVideo() }   // <-- ahora abre el video

        // Estado inicial
        qrImage.visibility = View.GONE
        celebrationImage.visibility = View.GONE
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
            showText("Error: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeSpeech && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userSpeech = result?.get(0) ?: return
            interpretCommand(userSpeech)
        }
    }

    /** ---------- INTENCIONES POR VOZ ---------- **/
    private fun interpretCommand(transcript: String) {
        val lower = transcript.lowercase().trim()

        when {
            // --- MAX ---
            listOf("max", "hbo max").any { lower.contains(it) } -> {
                showImage(R.drawable.max, "Instrucciones para activar Max")
            }

            // --- Disney ---
            listOf("disney", "disney+").any { lower.contains(it) } -> {
                showImage(R.drawable.disney, "Instrucciones para activar Disney+")
            }

            // --- 5G / VoLTE / 4G -> video YouTube ---
            listOf("5g", "vo lte", "volte", "4g", "activar 5g").any { lower.contains(it) } -> {
                show5GVideo()
            }

            // --- Cambiar Wi-Fi (frases simples + variantes con guion y configuración) ---
            listOf(
                // Frases directas
                "contraseña wifi", "contrasena wifi", "contraseña wi-fi", "contrasena wi-fi",
                "cambiar clave de red",
                "cambiar clave wifi", "cambiar clave wi-fi",
                "cambiar contraseña wifi", "cambiar contrasena wifi",
                "cambiar contraseña wi-fi", "cambiar contrasena wi-fi",
                "cambiar password wifi", "cambiar password wi-fi",
                "cambiar nombre de red",
                "renombrar wifi", "renombrar wi-fi",
                // Configuración
                "configuracion wifi", "configuración wifi",
                "configuracion wi-fi", "configuración wi-fi",
                // Accesos rápidos
                "clave wifi", "wifi", "wi-fi"
            ).any { lower.contains(it) } -> {
                showWifiCard()
            }

            // --- Diagnóstico de internet ---
            listOf(
                "no tengo internet",
                "estoy sin conexion", "estoy sin conexión",
                "sin internet", "sin conexion", "sin conexión",
                "se cayo", "se cayó",
                "no hay internet",
                "problemas de internet", "problemas con internet",
                "no funciona internet", "no me funciona el internet",
                "internet lento",
                "tengo problemas de internet"
            ).any { lower.contains(it) } -> {
                showDiagnosticoCard()
            }

            // --- Mis Canales / Canales Premium ---
            listOf(
                "mis canales",
                "canales premium",
                "ver canales premium",
                "contratar premium",
                "contratar canales premium",
                "que canales premium tengo", "qué canales premium tengo",
                "cuales son mis canales", "cuáles son mis canales",
                "que canales tengo", "qué canales tengo"
            ).any { lower.contains(it) } -> {
                showMisCanales()
            }

            // --- Ejecutivo (QR) ---
            listOf("hablar con alguien", "necesito ayuda", "ejecutivo", "me contacten").any { lower.contains(it) } -> {
                showQrInContainer()
            }

            // --- Listas genéricas (texto) ---
            listOf("noticias", "canales de noticias", "quiero ver noticias", "informativo").any { lower.contains(it) } -> {
                showText("Noticias disponibles: CNN Chile HD (54), T13 HD (56), CHV HD (66), Canal 13 HD (67).")
            }
            listOf("deportes", "canales de deporte", "ver deportes", "partido").any { lower.contains(it) } -> {
                showText("Deportes: ESPN HD (212), ESPN 2 HD (214), ESPN 3 HD (216), ESPN 4 (213), ESPN 6 (211).")
            }

            // --- Canales específicos ---
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

            // --- Celebration ---
            (listOf("eduardo", "edu", "idea").any { lower.contains(it) } &&
                    (lower.contains("gustó") || lower.contains("gusto"))) -> {
                showCelebration()
                showText("¡Gracias por el apoyo! 🎊")
            }

            else -> showText("No entendí tu solicitud. ¿Puedes repetirlo de otra manera?")
        }
    }

    /** ---------- HELPERS DE CONTENIDO DINÁMICO ---------- **/

    private fun clearDynamicArea() {
        // Pausa y destruye cualquier WebView para evitar fugas y audio corriendo
        destroyWebViewsRecursively(contenedorIntencion)

        contenedorIntencion.removeAllViews()
        qrImage.visibility = View.GONE
        celebrationImage.visibility = View.GONE
        tvInstrucciones.visibility = View.GONE
    }

    private fun destroyWebViewsRecursively(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            when (child) {
                is WebView -> {
                    child.loadUrl("about:blank")
                    child.onPause()
                    child.removeAllViews()
                    child.destroy()
                }
                is ViewGroup -> destroyWebViewsRecursively(child)
            }
        }
    }

    private fun showText(text: String) {
        clearDynamicArea()
        val tv = TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        contenedorIntencion.addView(tv)
    }

    private fun showImage(drawableId: Int, altText: String) {
        clearDynamicArea()
        val iv = ImageView(this).apply {
            setImageResource(drawableId)
            adjustViewBounds = true
            contentDescription = altText
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        contenedorIntencion.addView(iv)
    }

    private fun showQrInContainer() {
        clearDynamicArea()

        // Card centrada con ancho del contenedor para que la leyenda no se corte
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = resources.getDrawable(R.drawable.bg_card, theme)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val qr = ImageView(this).apply {
            setImageResource(R.drawable.contactoentel)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(240)).apply {
                bottomMargin = dp(16)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            contentDescription = "QR para contacto con ejecutivo"
        }

        val caption = TextView(this).apply {
            text = "Escanea este QR para contactarte con un ejecutivo"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setLineSpacing(0f, 1.05f)
            includeFontPadding = false
        }

        card.addView(qr)
        card.addView(caption)

        contenedorIntencion.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        )
    }

    private fun showCelebration() {
        clearDynamicArea()
        celebrationImage.setImageResource(R.drawable.celebration)
        celebrationImage.visibility = View.VISIBLE
    }

    // --- Wi-Fi: Card Redes unificadas ---
    private fun showWifiCard() {
        clearDynamicArea()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = resources.getDrawable(R.drawable.bg_card, theme) // translúcido
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // Título con icono Wi-Fi
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val wifiIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_wifi)
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(8) }
            imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B0D9FF"))
            contentDescription = "Wi-Fi"
        }
        val tvTitle = TextView(this).apply {
            text = "Redes unificadas"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        titleRow.addView(wifiIcon)
        titleRow.addView(tvTitle)
        card.addView(titleRow)

        val tvSub = TextView(this).apply {
            text = "Tu red 2.4 GHz y 5 GHz está unida"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(10))
        }
        card.addView(tvSub)

        // Nombre de red
        val ssidRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        val ssidLabel = TextView(this).apply {
            text = "Nombre de red:  "
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        val ssidValue = TextView(this).apply {
            text = "ExperienciaTV"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        ssidRow.addView(ssidLabel)
        ssidRow.addView(ssidValue)
        card.addView(ssidRow)

        // Clave
        val passRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val passLabel = TextView(this).apply {
            text = "Clave:  "
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        val passValue = TextView(this).apply {
            text = "************"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        passRow.addView(passLabel)
        passRow.addView(passValue)
        card.addView(passRow)

        // Botones cápsula
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 0)
        }
        val btnCambiarNombre = Button(this).apply {
            text = "CAMBIAR NOMBRE DE RED"
            isFocusable = true
            isFocusableInTouchMode = true
            background = resources.getDrawable(R.drawable.btn_highlight, theme)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f).apply {
                rightMargin = dp(12)
            }
        }
        val btnCambiarClave = Button(this).apply {
            text = "CAMBIAR CLAVE"
            isFocusable = true
            isFocusableInTouchMode = true
            background = resources.getDrawable(R.drawable.btn_highlight, theme)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f)
        }
        btnRow.addView(btnCambiarNombre)
        btnRow.addView(btnCambiarClave)
        card.addView(btnRow)

        // Advertencia
        val warn = TextView(this).apply {
            text = "⚠ Al cambiar el nombre o la clave, todos los dispositivos se desconectarán y deberás volver a conectarlos."
            textSize = 13f
            setTextColor(Color.parseColor("#C8D8E6"))
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(warn)

        contenedorIntencion.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    // --- Diagnóstico: muestra card simple de conectividad ---
    private fun showDiagnosticoCard() {
        clearDynamicArea()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = resources.getDrawable(R.drawable.bg_card, theme)
        }

        val title = TextView(this).apply {
            text = "Diagnóstico de conectividad"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val body = TextView(this).apply {
            text = "Estamos revisando tu conexión…\n• Verificando estado del router\n• Probando señal y latencia\n• Comprobando tus servicios contratados"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(title)
        card.addView(body)

        contenedorIntencion.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    // --- Mis Canales (usa layout XML con botones uniformes) ---
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

    // --- 5G: video embebido en WebView ---
    private fun show5GVideo() {
        // Video: https://youtu.be/RYJ8XOOrFbk
        showYouTubeInContainer("https://youtu.be/RYJ8XOOrFbk")
    }

    private fun showYouTubeInContainer(youtubeUrlOrId: String) {
        clearDynamicArea()

        val VIDEO_MAX_HEIGHT_DP = 320     // ↓ sube/baja si quieres más/menos alto
        val SIDE_MARGIN_DP = 10
        val VERT_MARGIN_DP = 10

        val view = layoutInflater.inflate(R.layout.card_youtube_embed, contenedorIntencion, false)

        // Ocultamos por si acaso
        view.findViewById<TextView?>(R.id.tvTitle)?.visibility = View.GONE

        val web = view.findViewById<WebView>(R.id.webYouTube)

        // Márgenes para que el borde superior se vea completo
        (web.layoutParams as ViewGroup.MarginLayoutParams).apply {
            leftMargin = dp(SIDE_MARGIN_DP)
            rightMargin = dp(SIDE_MARGIN_DP)
            topMargin = dp(VERT_MARGIN_DP)       // ← importante para que no “coma” el trazo de arriba
            bottomMargin = dp(VERT_MARGIN_DP)
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = dp(VIDEO_MAX_HEIGHT_DP)     // límite de altura
            web.layoutParams = this
        }

        // Configuración WebView
        val id = extractYoutubeId(youtubeUrlOrId) ?: youtubeUrlOrId
        val html = buildYouTubeHtml(id)

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        web.setBackgroundColor(Color.TRANSPARENT)
        web.webChromeClient = WebChromeClient()
        web.isFocusable = true
        web.isFocusableInTouchMode = true
        web.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)

        contenedorIntencion.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                bottomMargin = dp(8)
            }
        )
    }





    private fun buildYouTubeHtml(videoId: String) = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            html,body { margin:0; background:transparent; }
            .container { position:relative; padding-bottom:56.25%; height:0; overflow:hidden; }
            .container iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:0; }
          </style>
        </head>
        <body>
          <div class="container">
            <iframe
              src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&controls=1&fs=0&modestbranding=1&rel=0&enablejsapi=1&playsinline=1"
              allow="autoplay; encrypted-media"
              allowfullscreen="false">
            </iframe>
          </div>
        </body>
        </html>
    """.trimIndent()

    private fun extractYoutubeId(urlOrId: String): String? {
        val u = urlOrId.trim()
        val regexes = listOf(
            ".*youtu\\.be/([A-Za-z0-9_-]{6,})".toRegex(),
            ".*youtube\\.com.*[?&]v=([A-Za-z0-9_-]{6,}).*".toRegex(),
            ".*youtube\\.com/embed/([A-Za-z0-9_-]{6,}).*".toRegex()
        )
        for (r in regexes) {
            val m = r.matchEntire(u)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    // --- Card simple para un canal específico ---
    private fun showChannelCard(nombre: String, numero: String, contratable: Boolean = false) {
        clearDynamicArea()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            background = resources.getDrawable(R.drawable.bg_card, theme)
        }

        val title = TextView(this).apply {
            text = nombre
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val info = TextView(this).apply {
            text = "Canal $numero"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 6, 0, 0)
        }

        card.addView(title)
        card.addView(info)

        if (contratable) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 12, 0, 0)
            }
            val btn = Button(this).apply {
                text = "CONTRATAR"
                setTextColor(Color.WHITE)
                background = resources.getDrawable(R.drawable.btn_translucent_slim, theme)
                isFocusable = true
                isFocusableInTouchMode = true
                minWidth = 0
                minHeight = 0
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(28))
                setPadding(0, 0, 0, 0)
                gravity = Gravity.CENTER
                textSize = 13f
                includeFontPadding = false
            }
            row.addView(btn)
            card.addView(row)
        }

        contenedorIntencion.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_VERTICAL }
        )
    }

    // helper dp -> px
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
