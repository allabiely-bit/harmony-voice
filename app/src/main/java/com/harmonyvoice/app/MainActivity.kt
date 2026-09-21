package com.harmonyvoice.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class MainActivity : Activity() {

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var outputFile = ""
    private var outputPfd: android.os.ParcelFileDescriptor? = null
    private var isRecording = false
    private var isPlaying = false

    private var seconds = 0

    private lateinit var timerText: TextView
    private lateinit var recordButton: TextView
    private lateinit var listenButton: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                seconds++

                val minutes = seconds / 60
                val secs = seconds % 60

                timerText.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    minutes,
                    secs
                )

                handler.postDelayed(this, 1000)
            }
        }
    }

    // ---------------------------------------------------------
    // OUTILS D'AFFICHAGE
    // ---------------------------------------------------------

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun roundedBackground(
        color: Int,
        radius: Int = 16
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun createTextButton(
        text: String,
        backgroundColor: Int,
        textColor: Int,
        textSize: Float,
        minHeight: Int = 54,
        bold: Boolean = true
    ): TextView {

        val button = TextView(this)

        button.text = text
        button.setTextColor(textColor)
        button.setTextSize(textSize)

        button.gravity = Gravity.CENTER
        button.textAlignment = View.TEXT_ALIGNMENT_CENTER

        button.includeFontPadding = true

        if (bold) {
            button.setTypeface(null, Typeface.BOLD)
        }

        button.background = roundedBackground(
            backgroundColor,
            14
        )

        button.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        button.minimumHeight = dp(minHeight)

        button.isClickable = true
        button.isFocusable = true

        return button
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        val space = View(this)

        parent.addView(
            space,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(height)
            )
        )
    }

    // ---------------------------------------------------------
    // CREATION DE L'INTERFACE
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(10, 10, 10)
        window.navigationBarColor = Color.rgb(10, 10, 10)

        createInterface()
    }

    private fun createInterface() {

        // Fond général
        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(10, 10, 10))

        // =====================================================
        // ZONE PRINCIPALE SCROLLABLE
        // =====================================================

        val scrollView = ScrollView(this)

        scrollView.isFillViewport = true

        val content = LinearLayout(this)

        content.orientation = LinearLayout.VERTICAL

        content.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(24)
        )

        // -----------------------------------------------------
        // TITRE
        // -----------------------------------------------------

        val title = TextView(this)

        title.text = "HARMONY VOICE"
        title.setTextColor(Color.WHITE)
        title.setTextSize(23f)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER
        title.includeFontPadding = true

        content.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 4)

        // Sous-titre
        val subtitle = TextView(this)

        subtitle.text = "Chante une voix • Crée ton harmonie"
        subtitle.setTextColor(Color.rgb(190, 190, 190))
        subtitle.setTextSize(13f)
        subtitle.gravity = Gravity.CENTER
        subtitle.includeFontPadding = true

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 20)

        // =====================================================
        // BLOC TA VOIX
        // =====================================================

        val voiceBox = LinearLayout(this)

        voiceBox.orientation = LinearLayout.VERTICAL
        voiceBox.gravity = Gravity.CENTER

        voiceBox.setPadding(
            dp(16),
            dp(18),
            dp(16),
            dp(18)
        )

        voiceBox.background = roundedBackground(
            Color.rgb(35, 35, 42),
            20
        )

        // TA VOIX
        val voiceTitle = TextView(this)

        voiceTitle.text = "TA VOIX"
        voiceTitle.setTextColor(Color.WHITE)
        voiceTitle.setTextSize(18f)
        voiceTitle.setTypeface(null, Typeface.BOLD)
        voiceTitle.gravity = Gravity.CENTER
        voiceTitle.includeFontPadding = true

        voiceBox.addView(
            voiceTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(voiceBox, 10)

        // Micro
        val microphone = TextView(this)

        microphone.text = "🎤"
        microphone.setTextSize(34f)
        microphone.gravity = Gravity.CENTER

        voiceBox.addView(
            microphone,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        addSpace(voiceBox, 5)

        // Compteur
        timerText = TextView(this)

        timerText.text = "00:00"
        timerText.setTextColor(Color.WHITE)
        timerText.setTextSize(19f)
        timerText.setTypeface(null, Typeface.BOLD)
        timerText.gravity = Gravity.CENTER
        timerText.includeFontPadding = true

        voiceBox.addView(
            timerText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            voiceBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 16)

        // =====================================================
        // BOUTON ENREGISTRER
        // =====================================================

        recordButton = createTextButton(
            "●  ENREGISTRER MA VOIX",
            Color.rgb(124, 0, 255),
            Color.WHITE,
            14f,
            56
        )

        recordButton.setOnClickListener {

            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        content.addView(
            recordButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 10)

        // =====================================================
        // BOUTON ÉCOUTER
        // =====================================================

        listenButton = createTextButton(
            "▶  ÉCOUTER MA VOIX",
            Color.rgb(124, 0, 255),
            Color.WHITE,
            14f,
            56
        )

        listenButton.isEnabled = true
        listenButton.alpha = 0.75f

        listenButton.setOnClickListener {
            playRecording()
        }

        content.addView(
            listenButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 24)

        // =====================================================
        // CHOISIS UNE PARTIE
        // =====================================================

        val chooseTitle = TextView(this)

        chooseTitle.text = "CHOISIS UNE PARTIE"
        chooseTitle.setTextColor(Color.WHITE)
        chooseTitle.setTextSize(16f)
        chooseTitle.setTypeface(null, Typeface.BOLD)
        chooseTitle.gravity = Gravity.CENTER
        chooseTitle.includeFontPadding = true

        content.addView(
            chooseTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 12)

        // =====================================================
        // SOPRANO
        // =====================================================

        val sopranoButton = createTextButton(
            "SOPRANO",
            Color.rgb(55, 55, 62),
            Color.WHITE,
            14f,
            50
        )

        sopranoButton.setOnClickListener {
            Toast.makeText(
                this,
                "Partie Soprano sélectionnée",
                Toast.LENGTH_SHORT
            ).show()
        }

        content.addView(
            sopranoButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 8)

        // =====================================================
        // ALTO
        // =====================================================

        val altoButton = createTextButton(
            "ALTO",
            Color.rgb(55, 55, 62),
            Color.WHITE,
            14f,
            50
        )

        altoButton.setOnClickListener {
            Toast.makeText(
                this,
                "Partie Alto sélectionnée",
                Toast.LENGTH_SHORT
            ).show()
        }

        content.addView(
            altoButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 8)

        // =====================================================
        // TÉNOR
        // =====================================================

        val tenorButton = createTextButton(
            "TÉNOR",
            Color.rgb(55, 55, 62),
            Color.WHITE,
            14f,
            50
        )

        tenorButton.setOnClickListener {
            Toast.makeText(
                this,
                "Partie Ténor sélectionnée",
                Toast.LENGTH_SHORT
            ).show()
        }

        content.addView(
            tenorButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 18)

        // =====================================================
        // ÉCOUTER L'HARMONIE
        // =====================================================

        val harmonyButton = createTextButton(
            "▶  ÉCOUTER L'HARMONIE",
            Color.rgb(124, 0, 255),
            Color.WHITE,
            14f,
            56
        )

        harmonyButton.setOnClickListener {

            Toast.makeText(
                this,
                "La création de l'harmonie sera disponible prochainement.",
                Toast.LENGTH_LONG
            ).show()
        }

        content.addView(
            harmonyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 20)

        scrollView.addView(content)

        // =====================================================
        // BARRE DU BAS
        // MES VOIX / HARMONIE / PROFIL
        // =====================================================

        val bottomBar = LinearLayout(this)

        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.gravity = Gravity.CENTER

        bottomBar.setBackgroundColor(
            Color.rgb(25, 25, 30)
        )

        bottomBar.setPadding(
            dp(4),
            dp(6),
            dp(4),
            dp(6)
        )

        // MES VOIX
        val myVoices = createBottomItem(
            "🎤",
            "Mes voix"
        )

        myVoices.setOnClickListener {
            Toast.makeText(
                this,
                "Mes voix",
                Toast.LENGTH_SHORT
            ).show()
        }

        // HARMONIE
        val harmony = createBottomItem(
            "🎵",
            "Harmonie"
        )

        harmony.setOnClickListener {
            Toast.makeText(
                this,
                "Harmonie",
                Toast.LENGTH_SHORT
            ).show()
        }

        // PROFIL
        val profile = createBottomItem(
            "👤",
            "Profil"
        )

        profile.setOnClickListener {
            Toast.makeText(
                this,
                "Profil",
                Toast.LENGTH_SHORT
            ).show()
        }

        bottomBar.addView(
            myVoices,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        bottomBar.addView(
            harmony,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        bottomBar.addView(
            profile,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        // =====================================================
        // ASSEMBLAGE FINAL
        // =====================================================

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            bottomBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72)
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // ÉLÉMENT DE NAVIGATION DU BAS
    // ---------------------------------------------------------

    private fun createBottomItem(
        icon: String,
        label: String
    ): LinearLayout {

        val item = LinearLayout(this)

        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER

        item.setPadding(
            dp(4),
            dp(2),
            dp(4),
            dp(2)
        )

        val iconText = TextView(this)

        iconText.text = icon
        iconText.setTextSize(20f)
        iconText.gravity = Gravity.CENTER

        item.addView(
            iconText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        val labelText = TextView(this)

        labelText.text = label
        labelText.setTextColor(Color.WHITE)
        labelText.setTextSize(12f)
        labelText.gravity = Gravity.CENTER
        labelText.includeFontPadding = true
        labelText.setTypeface(null, Typeface.BOLD)

        item.addView(
            labelText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return item
    }

    // ---------------------------------------------------------
    // ENREGISTREMENT
    // ---------------------------------------------------------

    private fun startRecording() {
        
        if (android.os.Build.VERSION.SDK_INT >= 23) {

    val microphonePermission =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO)

    val storagePermission =
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    if (
        microphonePermission != PackageManager.PERMISSION_GRANTED ||
        storagePermission != PackageManager.PERMISSION_GRANTED
    ) {

        Toast.makeText(
            this,
            "Autorise le microphone et le stockage.",
            Toast.LENGTH_LONG
        ).show()

        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            100
        )

        return
    }
    }

    try {

        val musicDirectory = android.os.Environment.getExternalStoragePublicDirectory(
    android.os.Environment.DIRECTORY_MUSIC
)

val directory = File(
    musicDirectory,
    "HARMONY VOICE"
)

if (!directory.exists()) {
    directory.mkdirs()
}

        if (directory == null) {
            Toast.makeText(
                this,
                "Impossible d'utiliser le stockage.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        outputFile = File(
            directory,
            "ma_voix_${System.currentTimeMillis()}.3gp"
        ).absolutePath

        recorder = MediaRecorder()

        recorder?.setAudioSource(
            MediaRecorder.AudioSource.MIC
        )

        recorder?.setOutputFormat(
            MediaRecorder.OutputFormat.THREE_GPP
        )

        recorder?.setAudioEncoder(
            MediaRecorder.AudioEncoder.AMR_NB
        )

        recorder?.setOutputFile(outputFile)

        recorder?.prepare()
        recorder?.start()

        isRecording = true
        seconds = 0

        timerText.text = "00:00"

        recordButton.text =
            "⏹  ARRÊTER L'ENREGISTREMENT"

        listenButton.isEnabled = false
        listenButton.alpha = 0.45f

        handler.post(timerRunnable)

        Toast.makeText(
            this,
            "Enregistrement en cours...",
            Toast.LENGTH_SHORT
        ).show()

    } catch (e: Exception) {

        recorder?.release()
        recorder = null

        isRecording = false

        Toast.makeText(
            this,
            "Impossible de démarrer l'enregistrement.",
            Toast.LENGTH_LONG
        ).show()
    }
    }


    // ---------------------------------------------------------
    // ARRÊTER L'ENREGISTREMENT
    // ---------------------------------------------------------

    private fun stopRecording() {

    try {
        recorder?.stop()
    } catch (e: Exception) {
    }

    recorder?.release()
    recorder = null

    isRecording = false

    handler.removeCallbacks(timerRunnable)

    recordButton.text = "●  ENREGISTRER MA VOIX"

    listenButton.isEnabled = true
    listenButton.alpha = 1.0f

    Toast.makeText(
        this,
        "Enregistrement terminé",
        Toast.LENGTH_SHORT
    ).show()
}

// ---------------------------------------------------------
// ÉCOUTER L'ENREGISTREMENT
// ---------------------------------------------------------

private fun playRecording() {

    if (outputFile.isEmpty()) {

        Toast.makeText(
            this,
            "Aucun enregistrement disponible.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    if (isPlaying) {

        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
        }

        mediaPlayer?.release()
        mediaPlayer = null

        isPlaying = false

        listenButton.text = "▶  ÉCOUTER MA VOIX"

        return
    }

    try {

        mediaPlayer = MediaPlayer()

        mediaPlayer?.setDataSource(
    this,
    android.net.Uri.parse(outputFile)
)

        mediaPlayer?.setOnCompletionListener {

            isPlaying = false

            listenButton.text = "▶  ÉCOUTER MA VOIX"

            mediaPlayer?.release()
            mediaPlayer = null
        }

        mediaPlayer?.prepare()
        mediaPlayer?.start()

        isPlaying = true

        listenButton.text = "⏹  ARRÊTER L'ÉCOUTE"

    } catch (e: Exception) {

        mediaPlayer?.release()
        mediaPlayer = null

        isPlaying = false

        Toast.makeText(
            this,
            "Impossible de lire l'enregistrement.",
            Toast.LENGTH_LONG
        ).show()
    }
}

// ---------------------------------------------------------
// NETTOYAGE
// ---------------------------------------------------------

override fun onDestroy() {

    handler.removeCallbacks(timerRunnable)

    try {
        recorder?.release()
    } catch (e: Exception) {
    }

    recorder = null
    try {
    outputPfd?.close()
} catch (e: Exception) {
}

outputPfd = null

    try {
        mediaPlayer?.release()
    } catch (e: Exception) {
    }

    mediaPlayer = null

        super.onDestroy()
}
}
