package com.notes.notesproxmlviews

import android.Manifest
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity

class FocusTimerActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvNoteTitle: TextView
    private lateinit var tvNotePreview: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStopFocus: Button

    private var timer: CountDownTimer? = null
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_timer)

        // Inicializar views
        tvTimer = findViewById(R.id.tvTimer)
        tvNoteTitle = findViewById(R.id.tvNoteTitle)
        tvNotePreview = findViewById(R.id.tvNotePreview)
        progressBar = findViewById(R.id.progressBar)
        btnStopFocus = findViewById(R.id.btnStopFocus)

        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "Nota"
        val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: ""
        val durationMinutes = intent.getIntExtra("DURATION_MINUTES", 25)

        tvNoteTitle.text = noteTitle
        if (noteContent.isNotEmpty()) {
            tvNotePreview.text = noteContent.take(100)
            tvNotePreview.visibility = android.view.View.VISIBLE
        }

        startTimer(durationMinutes * 60 * 1000L)

        btnStopFocus.setOnClickListener {
            timer?.cancel()
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun startTimer(millisInFuture: Long) {
        isRunning = true
        timer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)

                val progress = ((millisInFuture - millisUntilFinished) * 100 / millisInFuture).toInt()
                progressBar.progress = progress
            }

            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun onFinish() {
                isRunning = false
                tvTimer.text = "00:00"
                progressBar.progress = 100

                Toast.makeText(this@FocusTimerActivity, "🎉 Parabéns! Foco concluído! 🎉", Toast.LENGTH_LONG).show()

                // Vibrar
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(500)
                }

                // Som padrão do sistema
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
                    ringtone.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fechar após 3 segundos
                tvTimer.postDelayed({
                    finish()
                }, 3000)
            }
        }.start()
    }

    override fun onBackPressed() {
        if (isRunning) {
            Toast.makeText(this, "Modo foco ativo! Complete o tempo ou clique em Interromper.", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}