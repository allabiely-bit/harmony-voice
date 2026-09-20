package com.harmonyvoice.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(15, 12, 40)
        val white = Color.WHITE
        val cyan = Color.rgb(80, 210, 255)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(30, 60, 30, 30)
        layout.setBackgroundColor(background)

        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 30f
        title.setTextColor(white)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(this)
        subtitle.text = "Chante. Harmonise. Partage."
        subtitle.textSize = 18f
        subtitle.setTextColor(cyan)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 10, 0, 40)

        val recordButton = Button(this)
        recordButton.text = "🎤  ENREGISTRER MA VOIX"
        recordButton.textSize = 17f

        val harmonyTitle = TextView(this)
        harmonyTitle.text = "CHOISIR UNE PARTIE"
        harmonyTitle.textSize = 16f
        harmonyTitle.setTextColor(white)
        harmonyTitle.setTypeface(null, Typeface.BOLD)
        harmonyTitle.setPadding(0, 35, 0, 15)

        val soprano = Button(this)
        soprano.text = "SOPRANO"

        val alto = Button(this)
        alto.text = "ALTO"

        val tenor = Button(this)
        tenor.text = "TÉNOR"

        val listenButton = Button(this)
        listenButton.text = "▶  ÉCOUTER L'HARMONIE"
        listenButton.textSize = 17f

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(recordButton)
        layout.addView(harmonyTitle)
        layout.addView(soprano)
        layout.addView(alto)
        layout.addView(tenor)

        val space = TextView(this)
        space.text = ""
        space.setPadding(0, 20, 0, 20)
        layout.addView(space)

        layout.addView(listenButton)

        setContentView(layout)
    }
}
