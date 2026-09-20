package com.harmonyvoice.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)
        text.text = "HARMONY VOICE\n\nL'application fonctionne !"
        text.textSize = 24f
        text.setTextColor(Color.WHITE)
        text.setBackgroundColor(Color.rgb(20, 15, 45))
        text.gravity = android.view.Gravity.CENTER
        text.setPadding(30, 30, 30, 30)

        setContentView(text)
    }
}
