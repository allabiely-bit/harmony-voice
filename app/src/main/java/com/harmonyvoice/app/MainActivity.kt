package com.harmonyvoice.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
        root.gravity = Gravity.TOP
        root.setPadding(20, 12, 20, 16)
        root.setBackgroundColor(Color.WHITE)

        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 27f
        title.setTextColor(Color.rgb(40, 25, 70))
        title.gravity = Gravity.CENTER
        title.setTypeface(null, Typeface.BOLD)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                55
            )
        )

        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 15f
        subtitle.setTextColor(Color.rgb(80, 170, 220))
        subtitle.gravity = Gravity.CENTER

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                35
            )
        )

        val voiceBox = LinearLayout(this)
        voiceBox.orientation = LinearLayout.VERTICAL
        voiceBox.gravity = Gravity.CENTER_HORIZONTAL
        voiceBox.setPadding(18, 16, 18, 16)
        voiceBox.setBackgroundColor(Color.rgb(45, 25, 75))

        val voiceTitle = TextView(this)
        voiceTitle.text = "TA VOIX"
        voiceTitle.textSize = 18f
        voiceTitle.setTextColor(Color.WHITE)
        voiceTitle.gravity = Gravity.CENTER
        voiceTitle.setTypeface(null, Typeface.BOLD)

        voiceBox.addView(
            voiceTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                35
            )
        )

        val microphone = TextView(this)
        microphone.text = "🎤"
        microphone.textSize = 42f
        microphone.gravity = Gravity.CENTER

        voiceBox.addView(
            microphone,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
        )

        timerText = TextView(this)
        timerText.text = "00:00"
        timerText.textSize = 22f
        timerText.setTextColor(Color.WHITE)
        timerText.gravity = Gravity.CENTER
        timerText.setTypeface(null, Typeface.BOLD)

        voiceBox.addView(
            timerText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                38
            )
        )

        recordButton = createActionButton(
            "●  ENREGISTRER MA VOIX",
            Color.rgb(98, 0, 238),
            Color.WHITE
        )

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        val recordParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            54
        )
        recordParams.topMargin = 10

        voiceBox.addView(
            recordButton,
            recordParams
        )

        listenButton = createActionButton(
            "▶  ÉCOUTER MA VOIX",
            Color.rgb(115, 75, 150),
            Color.WHITE
        )

        listenButton.isEnabled = false

        listenButton.setOnClickListener {
            playRecording()
        }

        val listenParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            54
        )
        listenParams.topMargin = 8

        voiceBox.addView(
            listenButton,
            listenParams
        )

        val voiceParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            275
        )
        voiceParams.topMargin = 18

        root.addView(
            voiceBox,
            voiceParams
        )

        val chooseTitle = TextView(this)
        chooseTitle.text = "CHOISIS UNE PARTIE"
        chooseTitle.textSize = 17f
        chooseTitle.setTextColor(Color.rgb(45, 25, 75))
        chooseTitle.gravity = Gravity.CENTER_VERTICAL
        chooseTitle.setTypeface(null, Typeface.BOLD)

        val chooseParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            45
        )
        chooseParams.topMargin = 10

        root.addView(
            chooseTitle,
            chooseParams
        )

        val parts = LinearLayout(this)
        parts.orientation = LinearLayout.HORIZONTAL
        parts.gravity = Gravity.CENTER

        val soprano = createPartButton("SOPRANO")
        val alto = createPartButton("ALTO")
        val tenor = createPartButton("TÉNOR")

        val partParams1 = LinearLayout.LayoutParams(0, 56, 1f)
        partParams1.rightMargin = 7

        val partParams2 = LinearLayout.LayoutParams(0, 56, 1f)
        partParams2.leftMargin = 3
        partParams2.rightMargin = 3

        val partParams3 = LinearLayout.LayoutParams(0, 56, 1f)
        partParams3.leftMargin = 7

        parts.addView(soprano, partParams1)
        parts.addView(alto, partParams2)
        parts.addView(tenor, partParams3)

        root.addView(
            parts,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56
            )
        )

        val harmonyButton = createActionButton(
            "▶  ÉCOUTER L'HARMONIE",
            Color.rgb(180, 225, 245),
            Color.rgb(45, 25, 75)
        )

        harmonyButton.setOnClickListener {
            Toast.makeText(
                this,
                "La génération des harmonies sera ajoutée ensuite.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val harmonyParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            62
        )
        harmonyParams.topMargin = 18

        root.addView(
            harmonyButton,
            harmonyParams
        )

        val bottom = TextView(this)
        bottom.text = "Mes voix  •  Harmonies  •  Profil"
        bottom.textSize = 13f
        bottom.setTextColor(Color.DKGRAY)
        bottom.gravity = Gravity.CENTER

        val bottomParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            40
        )
        bottomParams.topMargin = 8

        root.addView(
            bottom,
            bottomParams
        )

        setContentView(root)
    }

    private fun createActionButton(
        text: String,
        background: Int,
        textColor: Int
    ): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 13f
        button.setTextColor(textColor)
        button.setTypeface(null, Typeface.BOLD)
        button.setBackgroundColor(background)

        button.setPadding(5, 0, 5, 0)

        return button
    }

    private fun createPartButton(text: String): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 12f
        button.setTextColor(Color.rgb(180, 225, 245))
        button.setTypeface(null, Typeface.BOLD)
        button.setBackgroundColor(Color.rgb(45, 25, 75))

        button.setPadding(2, 0, 2, 0)

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
