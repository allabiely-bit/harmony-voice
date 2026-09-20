package com.harmonyvoice.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private var recorder: MediaRecorder? = null
    private var outputFile: String = ""
    private var isRecording = false

    private lateinit var recordButton: Button
    private lateinit var recordText: TextView

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius
        return drawable
    }

    private fun styleButton(
        button: Button,
        backgroundColor: Int,
        textColor: Int
    ) {
        button.background = roundedBackground(backgroundColor, 45f)
        button.setTextColor(textColor)
        button.setTypeface(null, Typeface.BOLD)
        button.setPadding(25, 5, 25, 5)
        button.isAllCaps = false
    }

    private fun addSpace(
        layout: LinearLayout,
        height: Int
    ) {
        val space = TextView(this)
        layout.addView(
            space,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(10, 8, 32)
        val cardColor = Color.rgb(25, 21, 55)
        val purple = Color.rgb(115, 65, 220)
        val cyan = Color.rgb(75, 210, 255)
        val white = Color.WHITE
        val softWhite = Color.rgb(215, 215, 235)

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(background)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(28, 45, 28, 35)

        scrollView.addView(layout)

        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 30f
        title.setTextColor(white)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER

        layout.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 17f
        subtitle.setTextColor(cyan)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 8, 0, 0)
        layout.addView(subtitle)

        addSpace(layout, 35)

        val micCard = LinearLayout(this)
        micCard.orientation = LinearLayout.VERTICAL
        micCard.gravity = Gravity.CENTER
        micCard.setPadding(25, 25, 25, 25)
        micCard.background = roundedBackground(cardColor, 35f)

        val micTitle = TextView(this)
        micTitle.text = "TA VOIX"
        micTitle.textSize = 16f
        micTitle.setTextColor(softWhite)
        micTitle.setTypeface(null, Typeface.BOLD)
        micTitle.gravity = Gravity.CENTER
        micCard.addView(micTitle)

        addSpace(micCard, 15)

        recordButton = Button(this)
        recordButton.text = "🎤"
        recordButton.textSize = 42f
        recordButton.setTextColor(white)
        recordButton.background = roundedBackground(purple, 100f)
        recordButton.setPadding(20, 20, 20, 20)

        micCard.addView(
            recordButton,
            LinearLayout.LayoutParams(145, 145)
        )

        addSpace(micCard, 15)

        recordText = TextView(this)
        recordText.text = "ENREGISTRER MA VOIX"
        recordText.textSize = 17f
        recordText.setTextColor(white)
        recordText.setTypeface(null, Typeface.BOLD)
        recordText.gravity = Gravity.CENTER
        micCard.addView(recordText)

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        layout.addView(
            micCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(layout, 30)

        val harmonyTitle = TextView(this)
        harmonyTitle.text = "CHOISIS UNE PARTIE"
        harmonyTitle.textSize = 17f
        harmonyTitle.setTextColor(white)
        harmonyTitle.setTypeface(null, Typeface.BOLD)
        harmonyTitle.gravity = Gravity.CENTER
        layout.addView(harmonyTitle)

        addSpace(layout, 15)

        val soprano = Button(this)
        soprano.text = "SOPRANO"
        soprano.textSize = 13f
        styleButton(soprano, cardColor, cyan)

        layout.addView(
            soprano,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 10)

        val alto = Button(this)
        alto.text = "ALTO"
        alto.textSize = 13f
        styleButton(alto, cardColor, cyan)

        layout.addView(
            alto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 10)

        val tenor = Button(this)
        tenor.text = "TÉNOR"
        tenor.textSize = 13f
        styleButton(tenor, cardColor, cyan)

        layout.addView(
            tenor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 30)

        val listenButton = Button(this)
        listenButton.text = "▶  ÉCOUTER L'HARMONIE"
        listenButton.textSize = 17f
        styleButton(listenButton, cyan, Color.rgb(8, 8, 28))

        layout.addView(
            listenButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                75
            )
        )

        addSpace(layout, 30)

        val footer = TextView(this)
        footer.text = "Mes voix   •   Harmonies   •   Profil"
        footer.textSize = 14f
        footer.setTextColor(softWhite)
        footer.gravity = Gravity.CENTER
        layout.addView(footer)

        setContentView(scrollView)
    }

    private fun startRecording() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
            return
        }

        try {
            val file = File(
                getExternalFilesDir(null),
                "ma_voix.3gp"
            )

            outputFile = file.absolutePath

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            isRecording = true
            recordButton.text = "⏹"
            recordText.text = "ARRÊTER L'ENREGISTREMENT"

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
                "Impossible de démarrer l'enregistrement",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopRecording() {

        try {
            recorder?.stop()
        } catch (_: Exception) {
        }

        recorder?.release()
        recorder = null

        isRecording = false
        recordButton.text = "🎤"
        recordText.text = "ENREGISTRER MA VOIX"

        Toast.makeText(
            this,
            "Enregistrement terminé",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            Toast.makeText(
                this,
                "L'autorisation du microphone est nécessaire",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
        }

        recorder?.release()
        recorder = null

        super.onDestroy()
    }
}
