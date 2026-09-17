package com.harmonyvoice.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val microphonePermission = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
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

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "HARMONY VOICE"
        title.textSize = 30f

        val subtitle = TextView(this)
        subtitle.text = "Crée tes harmonies vocales avec ta propre voix"
        subtitle.textSize = 18f

        val recordButton = Button(this)
        recordButton.text = "🎤 ENREGISTRER MA VOIX"

        val harmonyButton = Button(this)
        harmonyButton.text = "🎶 CRÉER LES HARMONIES"

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(recordButton)
        layout.addView(harmonyButton)

        setContentView(layout)
    }
}
