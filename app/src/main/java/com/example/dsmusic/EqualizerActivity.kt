package com.example.dsmusic

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EqualizerActivity : AppCompatActivity() {

    private var equalizer: Equalizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)

        val sessionId = intent.getIntExtra("AUDIO_SESSION_ID", 0)
        if (sessionId == 0) {
            finish()
            return
        }

        equalizer = Equalizer(0, sessionId).apply { enabled = true }

        val layout = findViewById<LinearLayout>(R.id.equalizerLayout)
        val bands = equalizer!!.numberOfBands
        val min = equalizer!!.bandLevelRange[0]
        val max = equalizer!!.bandLevelRange[1]

        for (i in 0 until bands) {
            val band = i.toShort()

            val label = TextView(this)
            label.text = "${equalizer!!.getCenterFreq(band) / 1000} Hz"
            layout.addView(label)

            val seekBar = SeekBar(this)
            seekBar.max = max - min
            seekBar.progress = equalizer!!.getBandLevel(band) - min
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    equalizer!!.setBandLevel(band, (progress + min).toShort())
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            layout.addView(seekBar)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizer?.release()
    }
}
