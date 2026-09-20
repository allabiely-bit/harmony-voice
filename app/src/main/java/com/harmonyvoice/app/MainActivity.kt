package com.harmonyvoice.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class MainActivity : Activity() {

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var isRecording = false
    private var isPlaying = false

    private var outputFile = ""

    private lateinit var timerText: TextView
    private lateinit var recordButton: Button
    private lateinit var listenButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }

        createInterface()
    }

    private fun createInterface() {

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 20, 20, 20)
        root.setBackgroundColor(android.graphics.Color.WHITE)

        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 28f
        title.setTextColor(android.graphics.Color.rgb(40, 25, 70))
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 16f
        subtitle.setTextColor(android.graphics.Color.rgb(80, 170, 220))
        subtitle.gravity = Gravity.CENTER

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        )

        val voiceBox = LinearLayout(this)
        voiceBox.orientation = LinearLayout.VERTICAL
        voiceBox.gravity = Gravity.CENTER
        voiceBox.setPadding(20, 20, 20, 20)
        voiceBox.setBackgroundColor(android.graphics.Color.rgb(45, 25, 75))

        val voiceTitle = TextView(this)
        voiceTitle.text = "TA VOIX"
        voiceTitle.textSize = 20f
        voiceTitle.setTextColor(android.graphics.Color.WHITE)
        voiceTitle.gravity = Gravity.CENTER
        voiceTitle.setTypeface(null, android.graphics.Typeface.BOLD)

        voiceBox.addView(voiceTitle)

        val microphone = TextView(this)
        microphone.text = "🎤"
        microphone.textSize = 50f
        microphone.gravity = Gravity.CENTER

        voiceBox.addView(microphone)

        timerText = TextView(this)
        timerText.text = "00:00"
        timerText.textSize = 24f
        timerText.setTextColor(android.graphics.Color.WHITE)
        timerText.gravity = Gravity.CENTER

        voiceBox.addView(timerText)

        recordButton = Button(this)
        recordButton.text = "●  ENREGISTRER MA VOIX"
        recordButton.textSize = 14f
        recordButton.setTextColor(android.graphics.Color.WHITE)
        recordButton.setBackgroundColor(android.graphics.Color.rgb(98, 0, 238))

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        voiceBox.addView(
            recordButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        listenButton = Button(this)
        listenButton.text = "▶  ÉCOUTER MA VOIX"
        listenButton.textSize = 14f
        listenButton.isEnabled = false
        listenButton.setOnClickListener {
            playRecording()
        }

        voiceBox.addView(
            listenButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        root.addView(
            voiceBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                330
            )
        )

        val chooseTitle = TextView(this)
        chooseTitle.text = "CHOISIS UNE PARTIE"
        chooseTitle.textSize = 18f
        chooseTitle.setTextColor(android.graphics.Color.rgb(45, 25, 75))
        chooseTitle.gravity = Gravity.CENTER
        chooseTitle.setTypeface(null, android.graphics.Typeface.BOLD)

        root.addView(
            chooseTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        )

        val parts = LinearLayout(this)
        parts.orientation = LinearLayout.HORIZONTAL
        parts.gravity = Gravity.CENTER

        parts.addView(
            createPartButton("SOPRANO"),
            LinearLayout.LayoutParams(0, 60, 1f)
        )

        parts.addView(
            createPartButton("ALTO"),
            LinearLayout.LayoutParams(0, 60, 1f)
        )

        parts.addView(
            createPartButton("TÉNOR"),
            LinearLayout.LayoutParams(0, 60, 1f)
        )

        root.addView(parts)

        val harmonyButton = Button(this)
        harmonyButton.text = "▶  ÉCOUTER L'HARMONIE"
        harmonyButton.textSize = 13f
        harmonyButton.setTextColor(android.graphics.Color.rgb(45, 25, 75))
        harmonyButton.setBackgroundColor(android.graphics.Color.rgb(180, 225, 245))

        harmonyButton.setOnClickListener {
            Toast.makeText(
                this,
                "La génération des harmonies sera ajoutée ensuite.",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(
            harmonyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75
            )
        )

        val bottom = TextView(this)
        bottom.text = "Mes voix  •  Harmonies  •  Profil"
        bottom.textSize = 14f
        bottom.setTextColor(android.graphics.Color.DKGRAY)
        bottom.gravity = Gravity.CENTER

        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                55
            )
        )

        setContentView(root)
    }

    private fun createPartButton(text: String): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 13f
        button.setTextColor(android.graphics.Color.rgb(180, 225, 245))
        button.setBackgroundColor(android.graphics.Color.rgb(45, 25, 75))

        button.setPadding(10, 5, 10, 5)

        button.setOnClickListener {
            Toast.makeText(
                this,
                "$text sélectionné",
                Toast.LENGTH_SHORT
            ).show()
        }

        return button
    }

    private fun startRecording() {

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
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

            recorder?.setOutputFile(outputFile)

            recorder?.prepare()
            recorder?.start()

            isRecording = true
            seconds = 0
            timerText.text = "00:00"

            recordButton.text = "⏹  ARRÊTER L'ENREGISTREMENT"
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
        handler.removeCallbacks(timerRunnable)

        recordButton.text = "●  ENREGISTRER MA VOIX"
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
            listenButton.text = "▶  ÉCOUTER MA VOIX"

            return
        }

        try {

            mediaPlayer = MediaPlayer()

            mediaPlayer?.setDataSource(outputFile)

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

    override fun onDestroy() {

        handler.removeCallbacks(timerRunnable)

        recorder?.release()
        recorder = null

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}
