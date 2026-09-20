package com.harmonyvoice.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val microphonePermission = 100

    private val backgroundColor = Color.rgb(12, 10, 32)
    private val cardColor = Color.rgb(27, 23, 55)
    private val accentColor = Color.rgb(85, 210, 255)
    private val purpleColor = Color.rgb(135, 92, 255)
    private val whiteColor = Color.WHITE
    private val grayColor = Color.rgb(190, 190, 210)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMicrophonePermission()
        createMainScreen()
    }

    private fun requestMicrophonePermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                microphonePermission
            )
        }
    }

    private fun createMainScreen() {

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setBackgroundColor(backgroundColor)
        mainLayout.setPadding(30, 45, 30, 20)

        // TITRE
        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 28f
        title.setTextColor(whiteColor)
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        mainLayout.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // SOUS-TITRE
        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 16f
        subtitle.setTextColor(accentColor)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 8, 0, 25)

        mainLayout.addView(subtitle)

        // MESSAGE
        val description = TextView(this)
        description.text =
            "Enregistre ta voix et prépare tes harmonies vocales."
        description.textSize = 15f
        description.setTextColor(grayColor)
        description.gravity = Gravity.CENTER
        description.setPadding(10, 0, 10, 25)

        mainLayout.addView(description)

        // GRAND BOUTON MICRO
        val recordButton = Button(this)
        recordButton.text = "🎤\nENREGISTRER MA VOIX"
        recordButton.textSize = 17f
        recordButton.setTextColor(whiteColor)
        recordButton.setAllCaps(false)
        recordButton.background = roundedBackground(
            purpleColor,
            40
        )

        val recordParams = LinearLayout.LayoutParams(
            260,
            115
        )
        recordParams.gravity = Gravity.CENTER
        recordParams.bottomMargin = 25

        mainLayout.addView(recordButton, recordParams)

        // ACTION DU BOUTON
        recordButton.setOnClickListener {
            recordButton.text = "🔴\nENREGISTREMENT EN COURS..."
        }

        // TITRE HARMONIES
        val harmonyTitle = TextView(this)
        harmonyTitle.text = "CHOISIS TA PARTIE VOCALE"
        harmonyTitle.textSize = 16f
        harmonyTitle.setTextColor(whiteColor)
        harmonyTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        harmonyTitle.gravity = Gravity.CENTER
        harmonyTitle.setPadding(0, 5, 0, 15)

        mainLayout.addView(harmonyTitle)

        // LIGNE DES PARTIES VOCALES
        val harmonyLayout = LinearLayout(this)
        harmonyLayout.orientation = LinearLayout.HORIZONTAL
        harmonyLayout.gravity = Gravity.CENTER

        val soprano = createHarmonyButton("SOPRANO")
        val alto = createHarmonyButton("ALTO")
        val tenor = createHarmonyButton("TÉNOR")

        harmonyLayout.addView(soprano)
        harmonyLayout.addView(alto)
        harmonyLayout.addView(tenor)

        mainLayout.addView(
            harmonyLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // FORME D'ONDE VISUELLE
        val waveform = TextView(this)
        waveform.text =
            "〰〰〰〰〰〰〰〰〰〰〰〰〰〰〰"
        waveform.textSize = 23f
        waveform.setTextColor(accentColor)
        waveform.gravity = Gravity.CENTER
        waveform.setPadding(0, 18, 0, 18)

        mainLayout.addView(waveform)

        // BOUTON ÉCOUTER
        val listenButton = Button(this)
        listenButton.text = "▶  ÉCOUTER L'HARMONIE"
        listenButton.textSize = 16f
        listenButton.setTextColor(whiteColor)
        listenButton.setAllCaps(false)
        listenButton.background = roundedBackground(
            accentColor,
            25
        )

        mainLayout.addView(
            listenButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
        )

        listenButton.setOnClickListener {
            listenButton.text = "⏸  LECTURE EN COURS..."
        }

        // ESPACE
        val space = View(this)
        mainLayout.addView(
            space,
            LinearLayout.LayoutParams(
                1,
                18
            )
        )

        // PETITES CARTES
        val optionsLayout = LinearLayout(this)
        optionsLayout.orientation = LinearLayout.HORIZONTAL
        optionsLayout.gravity = Gravity.CENTER

        val recordings = createSmallButton("📁\nMes enregistrements")
        val harmonies = createSmallButton("🎶\nMes harmonies")
        val settings = createSmallButton("⚙️\nParamètres")

        optionsLayout.addView(recordings)
        optionsLayout.addView(harmonies)
        optionsLayout.addView(settings)

        mainLayout.addView(
            optionsLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75
            )
        )

        // NAVIGATION BASSE
        val navigation = LinearLayout(this)
        navigation.orientation = LinearLayout.HORIZONTAL
        navigation.gravity = Gravity.CENTER
        navigation.setPadding(0, 15, 0, 0)

        val home = createNavigationItem("⌂\nAccueil")
        val voices = createNavigationItem("🎤\nMes voix")
        val harmony = createNavigationItem("🎶\nHarmonies")
        val profile = createNavigationItem("👤\nProfil")

        navigation.addView(home)
        navigation.addView(voices)
        navigation.addView(harmony)
        navigation.addView(profile)

        mainLayout.addView(
            navigation,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        setContentView(mainLayout)
    }

    private fun createHarmonyButton(text: String): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 12f
        button.setTextColor(whiteColor)
        button.setAllCaps(false)
        button.background = roundedBackground(
            cardColor,
            18
        )

        val params = LinearLayout.LayoutParams(
            0,
            55,
            1f
        )

        params.setMargins(5, 0, 5, 0)

        button.layoutParams = params

        button.setOnClickListener {
            button.background = roundedBackground(
                purpleColor,
                18
            )
        }

        return button
    }

    private fun createSmallButton(text: String): Button {

        val button = Button(this)

        button.text = text
        button.textSize = 11f
        button.setTextColor(whiteColor)
        button.setAllCaps(false)
        button.background = roundedBackground(
            cardColor,
            18
        )

        val params = LinearLayout.LayoutParams(
            0,
            65,
            1f
        )

        params.setMargins(4, 0, 4, 0)

        button.layoutParams = params

        return button
    }

    private fun createNavigationItem(text: String): TextView {

        val item = TextView(this)

        item.text = text
        item.textSize = 11f
        item.setTextColor(grayColor)
        item.gravity = Gravity.CENTER

        val params = LinearLayout.LayoutParams(
            0,
            60,
            1f
        )

        item.layoutParams = params

        item.setOnClickListener {
            item.setTextColor(accentColor)
        }

        return item
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {

        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius.toFloat()

        return drawable
    }
}
