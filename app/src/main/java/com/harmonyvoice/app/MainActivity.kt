package com.harmonyvoice.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import java.io.File

class MainActivity : Activity() {

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var outputFile = ""

    private var isRecording = false
    private var isPlaying = false

    private var seconds = 0

    private lateinit var timerText: TextView
    private lateinit var recordButton: Button
    private lateinit var listenButton: Button

    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                seconds++

                val minutes = seconds / 60
                val secs = seconds % 60

                timerText.text =
                    String.format("%02d:%02d", minutes, secs)

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {

        val mainLayout = LinearLayout(this)

        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(28, 24, 28, 24)

        mainLayout.setBackgroundColor(
            android.graphics.Color.rgb(18, 18, 24)
        )

        // TITRE
        val title = TextView(this)

        title.text = "HARMONY VOICE"
        title.textSize = 24f
        title.setTextColor(android.graphics.Color.WHITE)
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(0, 0, 0, 8)

        mainLayout.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // SOUS-TITRE
        val subtitle = TextView(this)

        subtitle.text = "Chante une voix • Crée ton harmonie"
        subtitle.textSize = 14f
        subtitle.setTextColor(
            android.graphics.Color.LTGRAY
        )
        subtitle.gravity = android.view.Gravity.CENTER

        val subtitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        subtitleParams.setMargins(0, 0, 0, 18)

        mainLayout.addView(
            subtitle,
            subtitleParams
        )

        // BLOC TA VOIX
        val voiceBox = LinearLayout(this)

        voiceBox.orientation = LinearLayout.VERTICAL
        voiceBox.gravity = android.view.Gravity.CENTER

        voiceBox.setPadding(20, 16, 20, 16)

        voiceBox.setBackgroundColor(
            android.graphics.Color.rgb(38, 38, 48)
        )

        val voiceBoxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            190
        )

        voiceBoxParams.setMargins(0, 0, 0, 16)

        mainLayout.addView(
            voiceBox,
            voiceBoxParams
        )

        // TA VOIX
        val voiceTitle = TextView(this)

        voiceTitle.text = "TA VOIX"
        voiceTitle.textSize = 18f
        voiceTitle.setTextColor(
            android.graphics.Color.WHITE
        )
        voiceTitle.gravity = android.view.Gravity.CENTER

        voiceBox.addView(
            voiceTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                40
            )
        )

        // MICRO
        val microphone = TextView(this)

        microphone.text = "🎤"
        microphone.textSize = 34f
        microphone.gravity = android.view.Gravity.CENTER

        val micParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            58
        )

        micParams.setMargins(0, 2, 0, 2)

        voiceBox.addView(
            microphone,
            micParams
        )

        // TIMER
        timerText = TextView(this)

        timerText.text = "00:00"
        timerText.textSize = 20f
        timerText.setTextColor(
            android.graphics.Color.WHITE
        )
        timerText.gravity = android.view.Gravity.CENTER

        voiceBox.addView(
            timerText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                45
            )
        )

        // ENREGISTRER
        recordButton = createActionButton(
            "●  ENREGISTRER MA VOIX"
        )

        val recordParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            56
        )

        recordParams.setMargins(0, 0, 0, 10)

        mainLayout.addView(
            recordButton,
            recordParams
        )

        recordButton.setOnClickListener {

            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // ÉCOUTER
        listenButton = createActionButton(
            "▶  ÉCOUTER MA VOIX"
        )

        val listenParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            56
        )

        listenParams.setMargins(0, 0, 0, 18)

        mainLayout.addView(
            listenButton,
            listenParams
        )

        listenButton.isEnabled = false

        listenButton.setOnClickListener {
            playRecording()
        }

        // CHOISIS UNE PARTIE
        val chooseTitle = TextView(this)

        chooseTitle.text = "CHOISIS UNE PARTIE"
        chooseTitle.textSize = 17f
        chooseTitle.setTextColor(
            android.graphics.Color.WHITE
        )
        chooseTitle.gravity = android.view.Gravity.CENTER

        chooseTitle.setPadding(0, 4, 0, 8)

        mainLayout.addView(
            chooseTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                45
            )
        )

        // SOPRANO
        val sopranoButton = createPartButton(
            "SOPRANO"
        )

        mainLayout.addView(
            sopranoButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        )

        // ALTO
        val altoButton = createPartButton(
            "ALTO"
        )

        mainLayout.addView(
            altoButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        )

        // TÉNOR
        val tenorButton = createPartButton(
            "TÉNOR"
        )

        mainLayout.addView(
            tenorButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            ).apply {
                setMargins(0, 0, 0, 14)
            }
        )

        // ÉCOUTER L'HARMONIE
        val harmonyButton = createActionButton(
            "♫  ÉCOUTER L'HARMONIE"
        )

        mainLayout.addView(
            harmonyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56
            )
        )

        harmonyButton.setOnClickListener {

            Toast.makeText(
                this,
                "La génération des harmonies sera ajoutée prochainement.",
                Toast.LENGTH_LONG
            ).show()
        }

        setContentView(mainLayout)
    }

    private fun createActionButton(
        text: String
    ): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 14f

        button.setTextColor(
            android.graphics.Color.WHITE
        )

        button.setBackgroundColor(
            android.graphics.Color.rgb(98, 0, 238)
        )

        button.isAllCaps = false

        return button
    }

    private fun createPartButton(
        text: String
    ): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 15f

        button.setTextColor(
            android.graphics.Color.WHITE
        )

        button.gravity = android.view.Gravity.CENTER

        button.setBackgroundColor(
            android.graphics.Color.rgb(55, 55, 68)
        )

        button.isAllCaps = true

        return button
    }

    private fun startRecording() {

        if (
            android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(
                this,
                "Autorise d'abord le microphone.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        try {

            val directory = getExternalFilesDir(null)

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
                "ma_voix.3gp"
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

            recorder?.setOutputFile(
                outputFile
            )

            recorder?.prepare()
            recorder?.start()

            isRecording = true
            seconds = 0

            timerText.text = "00:00"

            recordButton.text =
                "⏹  ARRÊTER L'ENREGISTREMENT"

            listenButton.isEnabled = false

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

    private fun stopRecording() {

        try {
            recorder?.stop()
        } catch (e: Exception) {
        }

        recorder?.release()
        recorder = null

        isRecording = false

        handler.removeCallbacks(
            timerRunnable
        )

        recordButton.text =
            "●  ENREGISTRER MA VOIX"

        listenButton.isEnabled = true

        Toast.makeText(
            this,
            "Enregistrement terminé",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun playRecording() {

        if (outputFile.isEmpty()) {
            return
        }

        if (!File(outputFile).exists()) {

            Toast.makeText(
                this,
                "Aucun enregistrement disponible.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (isPlaying) {

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            isPlaying = false

            listenButton.text =
                "▶  ÉCOUTER MA VOIX"

            return
        }

        try {

            mediaPlayer = MediaPlayer()

            mediaPlayer?.setDataSource(
                outputFile
            )

            mediaPlayer?.setOnCompletionListener {

                isPlaying = false

                listenButton.text =
                    "▶  ÉCOUTER MA VOIX"

                mediaPlayer?.release()
                mediaPlayer = null
            }

            mediaPlayer?.prepare()
            mediaPlayer?.start()

            isPlaying = true

            listenButton.text =
                "⏹  ARRÊTER L'ÉCOUTE"

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

    override fun onDestroy() {

        handler.removeCallbacks(
            timerRunnable
        )

        recorder?.release()
        recorder = null

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}
