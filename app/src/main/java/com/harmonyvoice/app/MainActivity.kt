package com.harmonyvoice.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : Activity() {

    // =========================================================
    // AUDIO
    // =========================================================

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private var pcmOutputFile = ""
    private var outputFile = ""

    private var isPcmRecording = false
    private var isRecording = false
    private var isPlaying = false

    private var selectedAudioUri: Uri? = null
    private var selectedVoicePart = "SOPRANO"

    private val audioSampleRate = 44100
    private val audioChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

    private var mediaPlayer: MediaPlayer? = null

    // =========================================================
    // INTERFACE
    // =========================================================

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

                handler.postDelayed(
                    this,
                    1000
                )
            }
        }
    }

    // =========================================================
    // OUTILS D'AFFICHAGE
    // =========================================================

    private fun dp(value: Int): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun roundedBackground(
        color: Int,
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()
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

        button.gravity =
            Gravity.CENTER

        button.textAlignment =
            View.TEXT_ALIGNMENT_CENTER

        button.includeFontPadding = true

        if (bold) {

            button.setTypeface(
                null,
                Typeface.BOLD
            )
        }

        button.background =
            roundedBackground(
                backgroundColor,
                14
            )

        button.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        button.minimumHeight =
            dp(minHeight)

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

    // =========================================================
    // CRÉATION DE L'INTERFACE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        window.statusBarColor =
            Color.rgb(
                10,
                10,
                10
            )

        window.navigationBarColor =
            Color.rgb(
                10,
                10,
                10
            )

        createInterface()
    }

    private fun createInterface() {

        // =====================================================
        // FOND GÉNÉRAL
        // =====================================================

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(
            Color.rgb(
                10,
                10,
                10
            )
        )

        // =====================================================
        // ZONE SCROLLABLE
        // =====================================================

        val scrollView =
            ScrollView(this)

        scrollView.isFillViewport =
            true

        val content =
            LinearLayout(this)

        content.orientation =
            LinearLayout.VERTICAL

        content.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(24)
        )

        // =====================================================
        // TITRE
        // =====================================================

        val title =
            TextView(this)

        title.text =
            "HARMONY VOICE"

        title.setTextColor(
            Color.WHITE
        )

        title.setTextSize(
            23f
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        title.gravity =
            Gravity.CENTER

        title.includeFontPadding =
            true

        content.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            content,
            4
        )

        // =====================================================
        // SOUS-TITRE
        // =====================================================

        val subtitle =
            TextView(this)

        subtitle.text =
            "Chante une voix • Crée ton harmonie"

        subtitle.setTextColor(
            Color.rgb(
                190,
                190,
                190
            )
        )

        subtitle.setTextSize(
            13f
        )

        subtitle.gravity =
            Gravity.CENTER

        subtitle.includeFontPadding =
            true

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            content,
            20
        )

        // =====================================================
        // BLOC TA VOIX
        // =====================================================

        val voiceBox =
            LinearLayout(this)

        voiceBox.orientation =
            LinearLayout.VERTICAL

        voiceBox.gravity =
            Gravity.CENTER

        voiceBox.setPadding(
            dp(16),
            dp(18),
            dp(16),
            dp(18)
        )

        voiceBox.background =
            roundedBackground(
                Color.rgb(
                    35,
                    35,
                    42
                ),
                20
            )

        val voiceTitle =
            TextView(this)

        voiceTitle.text =
            "TA VOIX"

        voiceTitle.setTextColor(
            Color.WHITE
        )

        voiceTitle.setTextSize(
            18f
        )

        voiceTitle.setTypeface(
            null,
            Typeface.BOLD
        )

        voiceTitle.gravity =
            Gravity.CENTER

        voiceTitle.includeFontPadding =
            true

        voiceBox.addView(
            voiceTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            voiceBox,
            10
        )

        // =====================================================
        // MICROPHONE
        // =====================================================

        val microphone =
            TextView(this)

        microphone.text =
            "🎤"

        microphone.setTextSize(
            34f
        )

        microphone.gravity =
            Gravity.CENTER

        voiceBox.addView(
            microphone,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        addSpace(
            voiceBox,
            5
        )

        // =====================================================
        // COMPTEUR
        // =====================================================

        timerText =
            TextView(this)

        timerText.text =
            "00:00"

        timerText.setTextColor(
            Color.WHITE
        )

        timerText.setTextSize(
            19f
        )

        timerText.setTypeface(
            null,
            Typeface.BOLD
        )

        timerText.gravity =
            Gravity.CENTER

        timerText.includeFontPadding =
            true

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

        addSpace(
            content,
            16
        )

        // =====================================================
        // ENREGISTRER
        // =====================================================

        recordButton =
            createTextButton(
                "●  ENREGISTRER MA VOIX",
                Color.rgb(
                    124,
                    0,
                    255
                ),
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

        addSpace(
            content,
            10
        )

        // =====================================================
        // UTILISER UNE VOIX EXISTANTE
        // =====================================================

        val existingVoiceButton =
            createTextButton(
                "📂  UTILISER UNE VOIX EXISTANTE",
                Color.rgb(
                    45,
                    45,
                    55
                ),
                Color.WHITE,
                14f,
                56
            )

        existingVoiceButton.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                )

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            intent.type =
                "audio/*"

            startActivityForResult(
                intent,
                200
            )
        }

        content.addView(
            existingVoiceButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            content,
            10
        )

        // =====================================================
        // ÉCOUTER
        // =====================================================

        listenButton =
            createTextButton(
                "▶  ÉCOUTER MA VOIX",
                Color.rgb(
                    124,
                    0,
                    255
                ),
                Color.WHITE,
                14f,
                56
            )

        listenButton.isEnabled =
            true

        listenButton.alpha =
            0.75f

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

        addSpace(
            content,
            24
        )

        // =====================================================
        // CHOISIS UNE PARTIE
        // =====================================================

        val chooseTitle =
            TextView(this)

        chooseTitle.text =
            "CHOISIS UNE PARTIE"

        chooseTitle.setTextColor(
            Color.WHITE
        )

        chooseTitle.setTextSize(
            16f
        )

        chooseTitle.setTypeface(
            null,
            Typeface.BOLD
        )

        chooseTitle.gravity =
            Gravity.CENTER

        chooseTitle.includeFontPadding =
            true

        content.addView(
            chooseTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            content,
            12
        )

        // =====================================================
        // SOPRANO
        // =====================================================

        val sopranoButton =
            createTextButton(
                "SOPRANO",
                Color.rgb(
                    55,
                    55,
                    62
                ),
                Color.WHITE,
                14f,
                50
            )

        sopranoButton.setOnClickListener {

            selectedVoicePart =
                "SOPRANO"

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

        addSpace(
            content,
            8
        )

        // =====================================================
        // ALTO
        // =====================================================

        val altoButton =
            createTextButton(
                "ALTO",
                Color.rgb(
                    55,
                    55,
                    62
                ),
                Color.WHITE,
                14f,
                50
            )

        altoButton.setOnClickListener {

            selectedVoicePart =
                "ALTO"

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

        addSpace(
            content,
            8
        )

        // =====================================================
        // TÉNOR
        // =====================================================

        val tenorButton =
            createTextButton(
                "TÉNOR",
                Color.rgb(
                    55,
                    55,
                    62
                ),
                Color.WHITE,
                14f,
                50
            )

        tenorButton.setOnClickListener {

            selectedVoicePart =
                "TÉNOR"

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

        addSpace(
            content,
            18
        )

        // =====================================================
        // ÉCOUTER L'HARMONIE
        // =====================================================

        val harmonyButton =
            createTextButton(
                "▶  ÉCOUTER L'HARMONIE",
                Color.rgb(
                    124,
                    0,
                    255
                ),
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

        addSpace(
            content,
            20
        )

        scrollView.addView(
            content
        )

        // =====================================================
        // BARRE DU BAS
        // =====================================================

        val bottomBar =
            LinearLayout(this)

        bottomBar.orientation =
            LinearLayout.HORIZONTAL

        bottomBar.gravity =
            Gravity.CENTER

        bottomBar.setBackgroundColor(
            Color.rgb(
                25,
                25,
                30
            )
        )

        bottomBar.setPadding(
            dp(4),
            dp(6),
            dp(4),
            dp(6)
        )

        // MES VOIX
        val myVoices =
            createBottomItem(
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
        val harmony =
            createBottomItem(
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
        val profile =
            createBottomItem(
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
        // ASSEMBLAGE
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

        setContentView(
            root
        )
    }

    // =========================================================
    // ÉLÉMENT NAVIGATION BAS
    // =========================================================

    private fun createBottomItem(
        icon: String,
        label: String
    ): LinearLayout {

        val item =
            LinearLayout(this)

        item.orientation =
            LinearLayout.VERTICAL

        item.gravity =
            Gravity.CENTER

        item.setPadding(
            dp(4),
            dp(2),
            dp(4),
            dp(2)
        )
        val iconText =
            TextView(this)

        iconText.text =
            icon

        iconText.setTextSize(
            20f
        )

        iconText.gravity =
            Gravity.CENTER

        item.addView(
            iconText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        val labelText =
            TextView(this)

        labelText.text =
            label

        labelText.setTextColor(
            Color.WHITE
        )

        labelText.setTextSize(
            12f
        )

        labelText.gravity =
            Gravity.CENTER

        labelText.includeFontPadding =
            true

        labelText.setTypeface(
            null,
            Typeface.BOLD
        )

        item.addView(
            labelText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return item
    }

    // =========================================================
    // DÉMARRER L'ENREGISTREMENT
    // =========================================================

    private fun startRecording() {

        if (Build.VERSION.SDK_INT >= 23) {

            val microphonePermission =
                checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
                )

            val storagePermission =
                checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            if (
                microphonePermission !=
                    PackageManager.PERMISSION_GRANTED ||
                (
                    Build.VERSION.SDK_INT <= 28 &&
                    storagePermission !=
                        PackageManager.PERMISSION_GRANTED
                )
            ) {

                Toast.makeText(
                    this,
                    "Autorise le microphone et le stockage.",
                    Toast.LENGTH_LONG
                ).show()

                if (Build.VERSION.SDK_INT <= 28) {

                    requestPermissions(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        100
                    )

                } else {

                    requestPermissions(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO
                        ),
                        100
                    )
                }

                return
            }
        }

        selectedAudioUri = null

        val success =
            startPcmRecording()

        if (!success) {
            return
        }

        isRecording = true

        seconds = 0

        timerText.text =
            "00:00"

        recordButton.text =
            "⏹  ARRÊTER L'ENREGISTREMENT"

        listenButton.isEnabled =
            false

        listenButton.alpha =
            0.45f

        handler.post(
            timerRunnable
        )
    }

    // =========================================================
    // ENREGISTREMENT PCM
    // =========================================================

    private fun startPcmRecording(): Boolean {

        try {

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    audioSampleRate,
                    audioChannelConfig,
                    audioEncoding
                )

            if (minBufferSize <= 0) {

                Toast.makeText(
                    this,
                    "Impossible de préparer l'enregistrement audio.",
                    Toast.LENGTH_LONG
                ).show()

                return false
            }

            val bufferSize =
                minBufferSize * 2

            pcmOutputFile =
                File(
                    cacheDir,
                    "ma_voix_${System.currentTimeMillis()}.pcm"
                ).absolutePath

            outputFile = ""

            val record =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    audioSampleRate,
                    audioChannelConfig,
                    audioEncoding,
                    bufferSize
                )

            audioRecord =
                record

            if (
                record.state !=
                    AudioRecord.STATE_INITIALIZED
            ) {

                record.release()

                audioRecord =
                    null

                Toast.makeText(
                    this,
                    "Le microphone ne peut pas être initialisé.",
                    Toast.LENGTH_LONG
                ).show()

                return false
            }

            record.startRecording()

            if (
                record.recordingState !=
                    AudioRecord.RECORDSTATE_RECORDING
            ) {

                record.release()

                audioRecord =
                    null

                Toast.makeText(
                    this,
                    "L'enregistrement n'a pas pu démarrer.",
                    Toast.LENGTH_LONG
                ).show()

                return false
            }

            isPcmRecording =
                true

            recordingThread =
                Thread {

                    val buffer =
                        ShortArray(
                            bufferSize / 2
                        )

                    try {

                        FileOutputStream(
                            pcmOutputFile
                        ).use { output ->

                            while (
                                isPcmRecording
                            ) {

                                val read =
                                    record.read(
                                        buffer,
                                        0,
                                        buffer.size
                                    )

                                if (read > 0) {

                                    for (
                                        i in 0 until read
                                    ) {

                                        val sample =
                                            buffer[i].toInt()

                                        output.write(
                                            sample and 0xFF
                                        )

                                        output.write(
                                            (sample shr 8) and 0xFF
                                        )
                                    }
                                }
                            }
                        }

                    } catch (
                        e: Exception
                    ) {

                        runOnUiThread {

                            Toast.makeText(
                                this,
                                "Erreur pendant l'enregistrement audio.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            recordingThread?.start()

            return true

        } catch (
            e: Exception
        ) {

            try {
                audioRecord?.release()
            } catch (_: Exception) {
            }

            audioRecord =
                null

            isPcmRecording =
                false

 Toast.makeText(
                this,
                "Impossible de démarrer le microphone.",
                Toast.LENGTH_LONG
            ).show()

            return false
        }
    }

    // =========================================================
    // ARRÊTER L'ENREGISTREMENT
    // =========================================================

    private fun stopRecording() {

        isRecording =
            false

        handler.removeCallbacks(
            timerRunnable
        )

        stopPcmRecording()

        recordButton.text =
            "●  ENREGISTRER MA VOIX"

        val pcmFile =
            File(
                pcmOutputFile
            )

        if (
            pcmOutputFile.isEmpty() ||
            !pcmFile.exists() ||
            pcmFile.length() == 0L
        ) {

            listenButton.isEnabled =
                false

            listenButton.alpha =
                0.45f

            Toast.makeText(
                this,
                "ERREUR : l'enregistrement n'a pas été créé.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val wavPath =
            File(
                cacheDir,
                "ma_voix_${System.currentTimeMillis()}.wav"
            ).absolutePath

        val wavCreated =
            convertPcmToWav(
                pcmOutputFile,
                wavPath
            )

        if (!wavCreated) {

            listenButton.isEnabled =
                false

            listenButton.alpha =
                0.45f

            Toast.makeText(
                this,
                "Impossible de préparer la lecture de la voix.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        outputFile =
            wavPath

        listenButton.isEnabled =
            true

        listenButton.alpha =
            1.0f

        showVoiceReadyDialog()
    }

    // =========================================================
    // ARRÊTER LE PCM
    // =========================================================
private fun stopPcmRecording() {

        isPcmRecording =
            false

        try {

            audioRecord?.stop()

        } catch (_: Exception) {
        }

        try {

            recordingThread?.join(
                1500
            )

        } catch (_: Exception) {
        }

        recordingThread =
            null

        try {

            audioRecord?.release()

        } catch (_: Exception) {
        }

        audioRecord =
            null
    }

    // =========================================================
    // CONVERSION PCM -> WAV
    // =========================================================

    private fun convertPcmToWav(
        pcmPath: String,
        wavPath: String
    ): Boolean {

        try {

            val pcmFile =
                File(pcmPath)

            if (
                !pcmFile.exists() ||
                pcmFile.length() == 0L
            ) {
                return false
            }

            val pcmDataLength =
                pcmFile.length()

            val byteRate =
                audioSampleRate *
                    1 *
                    16 /
                    8

            FileInputStream(
                pcmFile
            ).use { input ->

                FileOutputStream(
                    wavPath
                ).use { output ->

                    // RIFF
                    output.write(
                        byteArrayOf(
                            'R'.code.toByte(),
                            'I'.code.toByte(),
                            'F'.code.toByte(),
                            'F'.code.toByte()
                        )
                    )

                    writeIntLE(
                        output,
                        (36 + pcmDataLength).toInt()
                    )

                    // WAVE
                    output.write(
                        byteArrayOf(
                            'W'.code.toByte(),
                            'A'.code.toByte(),
                            'V'.code.toByte(),
                            'E'.code.toByte()
                        )
                    )

                    // fmt
                    output.write(

                        byteArrayOf(
                            'f'.code.toByte(),
                            'm'.code.toByte(),
                            't'.code.toByte(),
                            ' '.code.toByte()
                        )
                    )

                    writeIntLE(
                        output,
                        16
                    )

                    // PCM format
                    writeShortLE(
                        output,
                        1
                    )

                    // Mono
                    writeShortLE(
                        output,
                        1
                    )

                    // Sample rate
                    writeIntLE(
                        output,
                        audioSampleRate
                    )

                    // Byte rate
                    writeIntLE(
                        output,
                        byteRate
                    )

                    // Block align
                    writeShortLE(
                        output,
                        2
                    )

                    // Bits per sample
                    writeShortLE(
                        output,
                        16
                    )

                    // data
                    output.write(
                        byteArrayOf(
                            'd'.code.toByte(),
                            'a'.code.toByte(),
                            't'.code.toByte(),
                            'a'.code.toByte()
                        )
                    )

                    writeIntLE(
                        output,
                        pcmDataLength.toInt()
                    )

                    val buffer =
                        ByteArray(
                            8192
                        )

                    var read: Int

                    while (
                        input.read(
                            buffer
                        ).also {
                            read = it
                        } != -1
                    ) {

                        output.write(
                            buffer,
                            0,
                            read
                        )
                    }
                }
            }

            return File(
                wavPath
            ).exists()

        } catch (
            e: Exception
        ) {

            return false
        }
    }

    // =========================================================
    // ÉCRITURE LITTLE-ENDIAN
    // =========================================================

    private fun writeIntLE(
        output: FileOutputStream,
        value: Int
    ) {

        output.write(
            value and 0xFF
        )

        output.write(
            (value shr 8) and 0xFF
        )

        output.write(
            (value shr 16) and 0xFF
        )

        output.write(
            (value shr 24) and 0xFF
        )
    }

    private fun writeShortLE(
        output: FileOutputStream,
        value: Int
    ) {

        output.write(
            value and 0xFF
        )

        output.write(
            (value shr 8) and 0xFF
        )
    }

    // =========================================================
    // PANNEAU : TA VOIX EST PRÊTE
    // =========================================================

    private fun showVoiceReadyDialog() {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            dp(24),
            dp(24),
            dp(24),
            dp(16)
        )

        // =====================================================
        // TITRE
        // =====================================================

        val title =
            TextView(this)

        title.text =
            "🎵  Ta voix est prête"

        title.textSize =
            22f

        title.setTextColor(
            Color.BLACK
        )

        title.gravity =
            Gravity.CENTER

        layout.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // MESSAGE
        // =====================================================

        val message =
            TextView(this)

        message.text =
            "Choisis ce que tu veux faire avec ton enregistrement."

        message.textSize =
            15f

        message.setTextColor(
            Color.DKGRAY
        )

        message.gravity =
            Gravity.CENTER

        message.setPadding(
            0,
            dp(15),
            0,
            dp(25)
        )

        layout.addView(
            message,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // ÉCOUTER
        // =====================================================

        listenChoice.setOnClickListener {

            dialog.dismiss()

            playRecording()
        }

        // =====================================================
        // UTILISER POUR LES HARMONIES
        // =====================================================

        harmonyChoice.setOnClickListener {

            dialog.dismiss()

            if (
                outputFile.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Aucune voix disponible.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "🎶 Ta voix est prête pour les harmonies.",
                Toast.LENGTH_LONG
            ).show()
        }

        // =====================================================
        // SAUVEGARDER
        // =====================================================

        saveChoice.setOnClickListener {

            saveRecordingToPhone()
        }

        // =====================================================
        // RECOMMENCER
        // =====================================================

        restartChoice.setOnClickListener {

            dialog.dismiss()

            restartRecording()
        }

        dialog.show()
    }

    // =========================================================
    // RECOMMENCER
    // =========================================================

    private fun restartRecording() {

        stopPlayback()

        try {

            if (
                pcmOutputFile.isNotEmpty()
            ) {

                val pcm =
                    File(
                        pcmOutputFile
                    )

                if (pcm.exists()) {
                    pcm.delete()
                }
            }

            if (
                outputFile.isNotEmpty()
            ) {

                val wav =
                    File(
                        outputFile
                    )

                if (wav.exists()) {
                    wav.delete()
                }
            }

        } catch (_: Exception) {
        }

        pcmOutputFile =
            ""

        outputFile =
            ""

        selectedAudioUri =
            null

        seconds =
            0

        timerText.text =
            "00:00"

        listenButton.isEnabled =
            false

        listenButton.alpha =
            0.45f

        recordButton.text =
            "●  ENREGISTRER MA VOIX"

        Toast.makeText(
            this,
            "Prêt pour un nouvel enregistrement.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // LECTURE
    // =========================================================

    private fun playRecording() {

        if (
            selectedAudioUri == null &&
            outputFile.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Aucune voix disponible.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (isPlaying) {

            stopPlayback()

            return
        }

        try {

            mediaPlayer?.release()

            mediaPlayer =
                MediaPlayer()

            if (
                selectedAudioUri != null
            ) {

                mediaPlayer?.setDataSource(
                    this,
                    selectedAudioUri!!
                )

            } else {

                mediaPlayer?.setDataSource(
                    outputFile
                )
            }

            mediaPlayer?.setOnCompletionListener {

                isPlaying =
                    false

                listenButton.text =
                    "▶  ÉCOUTER MA VOIX"

                mediaPlayer?.release()

                mediaPlayer =
                    null
            }

            mediaPlayer?.prepare()

            mediaPlayer?.start()

            isPlaying =
                true

            listenButton.text =
                "⏹  ARRÊTER L'ÉCOUTE"

        } catch (
            e: Exception
        ) {

            mediaPlayer?.release()

            mediaPlayer =
                null

            isPlaying =
                false

            listenButton.text =
                "▶  ÉCOUTER MA VOIX"

            Toast.makeText(
                this,
                "Impossible de lire la voix sélectionnée.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ARRÊTER LA LECTURE
    // =========================================================

    private fun stopPlayback() {

        try {

            mediaPlayer?.stop()

        } catch (_: Exception) {
        }

        try {

            mediaPlayer?.release()

        } catch (_: Exception) {
        }

        mediaPlayer =
            null

        isPlaying =
            false

        listenButton.text =
            "▶  ÉCOUTER MA VOIX"
    }

    // =========================================================
    // VOIX EXISTANTE
    // =========================================================

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 200 &&
            resultCode == Activity.RESULT_OK
        ) {

            val uri =
                data?.data

            if (uri != null) {

                stopPlayback()

                selectedAudioUri =
                    uri

                outputFile =
                    ""

                listenButton.isEnabled =
                    true

                listenButton.alpha =
                    1.0f

                Toast.makeText(
                    this,
                    "Voix existante sélectionnée.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // SAUVEGARDER DANS LE TÉLÉPHONE
    // =========================================================

    private fun saveRecordingToPhone() {

        if (
            outputFile.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Aucune voix enregistrée à sauvegarder.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val sourceFile =
            File(
                outputFile
            )

        if (
            !sourceFile.exists() ||
            sourceFile.length() == 0L
        ) {

            Toast.makeText(
                this,
                "Le fichier audio est introuvable.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        try {

            val fileName =
                "ma_voix_${System.currentTimeMillis()}.wav"

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            fileName
                        )

                        put(
                            MediaStore.Audio.Media.MIME_TYPE,
                            "audio/wav"
                        )

                        put(
                            MediaStore.Audio.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_MUSIC +
                                "/HARMONY VOICE"
                        )

                        put(
                            MediaStore.Audio.Media.IS_PENDING,
                            1
                        )
                    }

                val uri =
                    contentResolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        values
                    )

                if (uri == null) {

                    Toast.makeText(
                        this,
                        "Impossible de créer le fichier dans le téléphone.",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }

                try {

                    contentResolver.openOutputStream(
                        uri
                    )?.use { output ->

                        FileInputStream(
                            sourceFile
                        ).use { input ->

                            val buffer =
                                ByteArray(
                                    8192
                                )

                            var read: Int

                            while (
                                input.read(
                                    buffer
                                ).also {
                                    read = it
                                } != -1
                            ) {

                                output.write(
                                    buffer,
                                    0,
                                    read
                                )
                            }
                        }
                    }

                    val completedValues =
                        ContentValues().apply {

                            put(
                                MediaStore.Audio.Media.IS_PENDING,
                                0
                            )
                        }

                    contentResolver.update(
                        uri,
                        completedValues,
                        null,
                        null
                    )

                } catch (
                    e: Exception
                ) {

                    contentResolver.delete(
                        uri,
                        null,
                        null
                    )

                    throw e
                }

            } else {

                val musicDirectory =
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC
                    )

                val harmonyDirectory =
                    File(
                        musicDirectory,
                        "HARMONY VOICE"
                    )

                if (
                    !harmonyDirectory.exists()
                ) {

                    harmonyDirectory.mkdirs()
                }

                val savedFile =
                    File(
                        harmonyDirectory,
                        fileName
                    )

                FileInputStream(
                    sourceFile
                ).use { input ->

                    FileOutputStream(
                        savedFile
                    ).use { output ->

                        val buffer =
                            ByteArray(
                                8192
                            )

                        var read: Int

                        while (
                            input.read(
                                buffer
                            ).also {
                                read = it
                            } != -1
                        ) {

                            output.write(
                                buffer,
                                0,
                                read
                            )
                        }
                    }
                }
            }
            Toast.makeText(
                this,
                "💾 Voix sauvegardée dans Musique/HARMONY VOICE.",
                Toast.LENGTH_LONG
            ).show()

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erreur lors de la sauvegarde de la voix.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 100
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                    this,
                    "Microphone autorisé. Appuie de nouveau sur ENREGISTRER.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Le microphone est nécessaire pour enregistrer ta voix.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // NETTOYAGE
    // =========================================================

    override fun onDestroy() {

        handler.removeCallbacks(
            timerRunnable
        )

        isRecording =
            false

        isPcmRecording =
            false

        try {

            audioRecord?.stop()

        } catch (_: Exception) {
        }

        try {

            recordingThread?.join(
                500
            )

        } catch (_: Exception) {
        }

        recordingThread =
            null

        try {

            audioRecord?.release()

        } catch (_: Exception) {
        }

        audioRecord =
            null

        try {

            mediaPlayer?.release()

        } catch (_: Exception) {
        }

        mediaPlayer =
            null

        isPlaying =
            false

        super.onDestroy()
    }
}
    
                        
                
