package com.harmonyvoice.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

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

        // TITRE
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

        // SOUS-TITRE
        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 17f
        subtitle.setTextColor(cyan)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 8, 0, 0)

        layout.addView(subtitle)

        addSpace(layout, 35)

        // CARTE MICROPHONE
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

        // GRAND BOUTON MICRO
        val recordButton = Button(this)
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

        val recordText = TextView(this)
        recordText.text = "ENREGISTRER MA VOIX"
        recordText.textSize = 17f
        recordText.setTextColor(white)
        recordText.setTypeface(null, Typeface.BOLD)
        recordText.gravity = Gravity.CENTER

        micCard.addView(recordText)

        layout.addView(
            micCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(layout, 30)

        // SECTION HARMONIES
        val harmonyTitle = TextView(this)
        harmonyTitle.text = "CHOISIS UNE PARTIE"
        harmonyTitle.textSize = 17f
        harmonyTitle.setTextColor(white)
        harmonyTitle.setTypeface(null, Typeface.BOLD)
        harmonyTitle.gravity = Gravity.CENTER

        layout.addView(harmonyTitle)

        addSpace(layout, 15)

        // SOPRANO
        val soprano = Button(this)
        soprano.text = "SOPRANO"
        soprano.textSize = 14f
        styleButton(soprano, cardColor, cyan)

        layout.addView(
            soprano,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 10)

        // ALTO
        val alto = Button(this)
        alto.text = "ALTO"
        alto.textSize = 14f
        styleButton(alto, cardColor, cyan)

        layout.addView(
            alto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 10)

        // TENOR
        val tenor = Button(this)
        tenor.text = "TÉNOR"
        tenor.textSize = 14f
        styleButton(tenor, cardColor, cyan)

        layout.addView(
            tenor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        addSpace(layout, 30)

        // ÉCOUTER
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

        // BAS DE PAGE
        val footer = TextView(this)
        footer.text = "Mes voix   •   Harmonies   •   Profil"
        footer.textSize = 14f
        footer.setTextColor(softWhite)
        footer.gravity = Gravity.CENTER

        layout.addView(footer)

        setContentView(scrollView)
    }
}
