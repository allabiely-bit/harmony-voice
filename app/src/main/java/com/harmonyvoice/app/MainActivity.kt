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

    private val handler = android.os.Handler(
        android.os.Looper.getMainLooper()
    )

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

        button.gravity = Gravity.CENTER
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
        // ZONE PRINCIPALE SCROLLABLE
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
        // BOUTON ENREGISTRER
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
        // BOUTON UTILISER UNE VOIX EXISTANTE
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
        // BOUTON ÉCOUTER
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

        // =====================================================
        // AJOUT DE LA ZONE SCROLLABLE
        // =====================================================

        scrollView.addView(
            content
        )

        // =====================================================
        // BARRE DU BAS
        // MES VOIX / HARMONIE / PROFIL
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

        // =====================================================
        // MES VOIX
        // =====================================================

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

        // =====================================================
        // HARMONIE
        // =====================================================

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

        // =====================================================
        // PROFIL
        // =====================================================

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

        // =====================================================
        // AJOUT DES ÉLÉMENTS À LA BARRE
        // =====================================================

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

        setContentView(
            root
        )
    }

    // =========================================================
    // ÉLÉMENT DE NAVIGATION DU BAS
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

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                RECORD_AUDIO_REQUEST
            )

            return
        }

        if (isRecording) {
            return
        }

        val started =
            startPcmRecording()

        if (!started) {

            Toast.makeText(
                this,
                "Impossible de démarrer l'enregistrement.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        isRecording = true

        Toast.makeText(
            this,
            "Enregistrement en cours...",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // DÉMARRER L'ENREGISTREMENT PCM
    // =========================================================

    private fun startPcmRecording(): Boolean {

        try {

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )

            if (
                minBufferSize ==
                AudioRecord.ERROR ||
                minBufferSize ==
                AudioRecord.ERROR_BAD_VALUE
            ) {

                return false
            }

            val bufferSize =
                minBufferSize * 2

            audioRecord =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

            if (
                audioRecord?.state !=
                AudioRecord.STATE_INITIALIZED
            ) {

                audioRecord?.release()
                audioRecord = null

                return false
            }

            val pcmFile =
                File(
                    cacheDir,
                    "ma_voix_${System.currentTimeMillis()}.pcm"
                )

            pcmFilePath =
                pcmFile.absolutePath

            isPcmRecording = true

            audioRecord?.startRecording()

            recordingThread =
                Thread {

                    val buffer =
                        ShortArray(
                            bufferSize / 2
                        )

                    try {

                        FileOutputStream(
                            pcmFile
                        ).use { output ->

                            while (
                                isPcmRecording
                            ) {

                                val read =
                                    audioRecord?.read(
                                        buffer,
                                        0,
                                        buffer.size
                                    ) ?: 0

                                if (read > 0) {

                                    val bytes =
                                        ByteArray(
                                            read * 2
                                        )

                                    var index =
                                        0

                                    for (
                                        i in 0 until read
                                    ) {

                                        val sample =
                                            buffer[i].toInt()

                                        bytes[index++] =
                                            (
                                                sample and
                                                    0xFF
                                            ).toByte()

                                        bytes[index++] =
                                            (
                                                (sample shr 8)
                                                    and 0xFF
                                            ).toByte()
                                    }

                                    output.write(
                                        bytes
                                    )
                                }
                            }
                        }

                    } catch (
                        e: Exception
                    ) {

                        e.printStackTrace()
                    }
                }

            recordingThread?.start()

            return true

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            isPcmRecording = false

            audioRecord?.release()
            audioRecord = null

            return false
        }
    }


    // =========================================================
    // ARRÊTER L'ENREGISTREMENT
    // =========================================================

    private fun stopRecording() {

        if (!isRecording) {
            return
        }

        isRecording = false

        stopPcmRecording()

        val pcmPath =
            pcmFilePath

        if (
            pcmPath == null
        ) {

            Toast.makeText(
                this,
                "Aucun enregistrement trouvé.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val pcmFile =
            File(
                pcmPath
            )

        if (
            !pcmFile.exists() ||
            pcmFile.length() <= 0
        ) {

            Toast.makeText(
                this,
                "L'enregistrement est vide.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val wavFile =
            File(
                cacheDir,
                "ma_voix_${System.currentTimeMillis()}.wav"
            )

        try {

            convertPcmToWav(
                pcmFile,
                wavFile
            )

            outputFile =
                wavFile.absolutePath

            listenButton?.isEnabled =
                true

            showVoiceReadyDialog()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Erreur lors de la conversion audio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // ARRÊTER LE PCM PROPREMENT
    // =========================================================

    private fun stopPcmRecording() {

        isPcmRecording = false

        try {

            audioRecord?.stop()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }

        try {

            recordingThread?.join(
                1000
            )

        } catch (
            e: InterruptedException
        ) {

            e.printStackTrace()
        }

        recordingThread =
            null

        try {

            audioRecord?.release()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }

        audioRecord =
            null
    }
        // =========================================================
    // CONVERSION PCM → WAV
    // =========================================================

    private fun convertPcmToWav(
        pcmFile: File,
        wavFile: File
    ) {

        val pcmSize =
            pcmFile.length()

        val dataSize =
            pcmSize

        val totalSize =
            36 + dataSize

        FileInputStream(
            pcmFile
        ).use { input ->

            FileOutputStream(
                wavFile
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
                    totalSize.toInt()
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

                // PCM format = 1
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
                    SAMPLE_RATE
                )

                // Byte rate
                val byteRate =
                    SAMPLE_RATE * 2

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
                    dataSize.toInt()
                )

                val buffer =
                    ByteArray(
                        4096
                    )

                var read:

                    Int

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


    // =========================================================
    // ÉCRIRE UN ENTIER LITTLE-ENDIAN
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


    // =========================================================
    // ÉCRIRE UN SHORT LITTLE-ENDIAN
    // =========================================================

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
    // DIALOGUE : TA VOIX EST PRÊTE
    // =========================================================

    private fun showVoiceReadyDialog() {

        val dialog =
            AlertDialog.Builder(
                this
            ).create()

        val container =
            LinearLayout(
                this
            )

        container.orientation =
            LinearLayout.VERTICAL

        container.setPadding(
            dp(28),
            dp(24),
            dp(28),
            dp(24)
        )

        val title =
            TextView(
                this
            )

        title.text =
            "🎤  Ta voix est prête"

        title.setTextColor(
            Color.WHITE
        )

        title.setTextSize(
            21f
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        title.gravity =
            Gravity.CENTER

        container.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            container,
            12
        )

        val message =
            TextView(
                this
            )

        message.text =
            "Que veux-tu faire avec cet enregistrement ?"

        message.setTextColor(
            Color.LTGRAY
        )

        message.setTextSize(
            15f
        )

        message.gravity =
            Gravity.CENTER

        container.addView(
            message,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            container,
            20
        )

        // =====================================================
        // ÉCOUTER
        // =====================================================

        val listenButtonDialog =
            createTextButton(
                "▶  ÉCOUTER",
                Color.rgb(
                    70,
                    70,
                    80
                ),
                Color.WHITE,
                14f,
                52
            )

        listenButtonDialog.setOnClickListener {

            playRecording()
        }

        container.addView(
            listenButtonDialog,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            container,
            10
        )

        // =====================================================
        // UTILISER POUR LES HARMONIES
        // =====================================================

        val useVoiceButton =
            createTextButton(
                "🎶  UTILISER CETTE VOIX POUR LES HARMONIES",
                Color.rgb(
                    124,
                    0,
                    255
                ),
                Color.WHITE,
                14f,
                52
            )

        useVoiceButton.setOnClickListener {

            dialog.dismiss()

            Toast.makeText(
                this,
                "Ta voix sera utilisée pour créer les harmonies.",
                Toast.LENGTH_LONG
            ).show()
        }

        container.addView(
            useVoiceButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            container,
            10
        )

        // =====================================================
        // SAUVEGARDER
        // =====================================================

        val saveButtonDialog =
            createTextButton(
                "💾  SAUVEGARDER DANS LE TÉLÉPHONE",
                Color.rgb(
                    45,
                    45,
                    55
                ),
                Color.WHITE,
                14f,
                52
            )

        saveButtonDialog.setOnClickListener {

            saveRecordingToPhone()
        }

        container.addView(
            saveButtonDialog,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(
            container,
            10
        )

        // =====================================================
        // RECOMMENCER
        // =====================================================

        val restartButtonDialog =
            createTextButton(
                "🔄  RECOMMENCER",
                Color.rgb(
                    55,
                    55,
                    65
                ),
                Color.WHITE,
                14f,
                52
            )

        restartButtonDialog.setOnClickListener {

            dialog.dismiss()

            restartRecording()
        }

        container.addView(
            restartButtonDialog,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        dialog.setView(
            container
        )

        dialog.window?.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(
                    Color.rgb(
                        30,
                        30,
                        38
                    )
                )

                cornerRadius =
                    dp(20).toFloat()
            }
        )

        dialog.show()

        dialog.window?.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(
                    Color.rgb(
                        30,
                        30,
                        38
                    )
                )

                cornerRadius =
                    dp(20).toFloat()
            }
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
        // =========================================================
    // RECOMMENCER UN NOUVEL ENREGISTREMENT
    // =========================================================

    private fun restartRecording() {

        stopPcmRecording()

        try {
            mediaPlayer?.stop()
        } catch (
            e: Exception
        ) {
            e.printStackTrace()
        }

        mediaPlayer?.release()
        mediaPlayer = null

        val pcmPath =
            pcmFilePath

        if (pcmPath != null) {

            try {

                File(
                    pcmPath
                ).delete()

            } catch (
                e: Exception
            ) {

                e.printStackTrace()
            }
        }

        val wavPath =
            outputFile

        if (wavPath != null) {

            try {

                File(
                    wavPath
                ).delete()

            } catch (
                e: Exception
            ) {

                e.printStackTrace()
            }
        }

        pcmFilePath =
            null

        outputFile =
            null

        selectedAudioUri =
            null

        isRecording =
            false

        isPcmRecording =
            false

        listenButton?.isEnabled =
            false

        Toast.makeText(
            this,
            "Tu peux recommencer l'enregistrement.",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // LIRE L'ENREGISTREMENT
    // =========================================================

    private fun playRecording() {

        try {

            mediaPlayer?.stop()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }

        mediaPlayer?.release()
        mediaPlayer = null

        // =====================================================
        // VOIX EXISTANTE SÉLECTIONNÉE
        // =====================================================

        if (
            selectedAudioUri != null
        ) {

            mediaPlayer =
                MediaPlayer.create(
                    this,
                    selectedAudioUri
                )

            if (
                mediaPlayer == null
            ) {

                Toast.makeText(
                    this,
                    "Impossible de lire cette voix.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            mediaPlayer?.setOnCompletionListener {

                it.release()

                if (
                    mediaPlayer === it
                ) {
                    mediaPlayer = null
                }
            }

            mediaPlayer?.start()

            return
        }

        // =====================================================
        // NOUVEL ENREGISTREMENT WAV
        // =====================================================

        val wavPath =
            outputFile

        if (
            wavPath == null
        ) {

            Toast.makeText(
                this,
                "Aucun enregistrement disponible.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val wavFile =
            File(
                wavPath
            )

        if (
            !wavFile.exists()
        ) {

            Toast.makeText(
                this,
                "Le fichier audio est introuvable.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        try {

            mediaPlayer =
                MediaPlayer()

            mediaPlayer?.setDataSource(
                wavFile.absolutePath
            )

            mediaPlayer?.prepare()

            mediaPlayer?.setOnCompletionListener {

                it.release()

                if (
                    mediaPlayer === it
                ) {
                    mediaPlayer = null
                }
            }

            mediaPlayer?.start()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            mediaPlayer?.release()
            mediaPlayer = null

            Toast.makeText(
                this,
                "Impossible de lire l'enregistrement.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // RÉSULTAT : UTILISER UNE VOIX EXISTANTE
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
            requestCode ==
            PICK_AUDIO_REQUEST &&
            resultCode ==
            RESULT_OK
        ) {

            val uri =
                data?.data

            if (
                uri == null
            ) {

                Toast.makeText(
                    this,
                    "Aucun fichier audio sélectionné.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            selectedAudioUri =
                uri

            outputFile =
                null

            pcmFilePath =
                null

            listenButton?.isEnabled =
                true

            Toast.makeText(
                this,
                "Voix sélectionnée avec succès.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // SAUVEGARDER DANS LE TÉLÉPHONE
    // =========================================================

    private fun saveRecordingToPhone() {

        val wavPath =
            outputFile

        if (
            wavPath == null
        ) {

            Toast.makeText(
                this,
                "Aucun nouvel enregistrement à sauvegarder.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val sourceFile =
            File(
                wavPath
            )

        if (
            !sourceFile.exists() ||
            sourceFile.length() <= 0
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
                "HARMONY_VOICE_${System.currentTimeMillis()}.wav"

            // =================================================
            // ANDROID 10 ET PLUS
            // =================================================

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

                val resolver =
                    contentResolver

                val uri =
                    resolver.insert(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        values
                    )

                if (
                    uri == null
                ) {

                    Toast.makeText(
                        this,
                        "Impossible de créer le fichier.",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }

                resolver.openOutputStream(
                    uri
                ).use { output ->

                    if (
                        output == null
                    ) {
                        throw Exception(
                            "OutputStream indisponible"
                        )
                    }

                    FileInputStream(
                        sourceFile
                    ).use { input ->

                        val buffer =
                            ByteArray(
                                8192
                            )

                        var read:

                            Int

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

                values.clear()

                values.put(
                    MediaStore.Audio.Media.IS_PENDING,
                    0
                )

                resolver.update(
                    uri,
                    values,
                    null,
                    null
                )

                Toast.makeText(
                    this,
                    "Voix sauvegardée dans Music/HARMONY VOICE.",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                // =============================================
                // ANDROID 9 ET MOINS
                // =============================================

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

                val destination =
                    File(
                        harmonyDirectory,
                        fileName
                    )

                FileInputStream(
                    sourceFile
                ).use { input ->

                    FileOutputStream(
                        destination
                    ).use { output ->

                        val buffer =
                            ByteArray(
                                8192
                            )

                        var read:

                            Int

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

                Toast.makeText(
                    this,
                    "Voix sauvegardée dans Music/HARMONY VOICE.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Erreur lors de la sauvegarde.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
        // =========================================================
    // RÉSULTAT DE LA DEMANDE DE PERMISSION MICRO
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
            requestCode ==
            RECORD_AUDIO_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                    this,
                    "Permission microphone accordée.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "La permission microphone est nécessaire pour enregistrer ta voix.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // NETTOYAGE À LA FERMETURE
    // =========================================================

    override fun onDestroy() {

        isRecording =
            false

        stopPcmRecording()

        try {

            mediaPlayer?.stop()

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}
