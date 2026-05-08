package com.notes.notesproxmlviews

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "Nota"
        val noteId = intent.getStringExtra("NOTE_ID") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "note_reminder_channel",
                "Lembretes de Notas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receba lembretes para revisar suas notas importantes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, NoteDetailsActivity::class.java).apply {
            putExtra("docId", noteId)
            putExtra("title", noteTitle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            notificationIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "note_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("📝 Lembrete: $noteTitle")
            .setContentText("Hora de revisar sua nota! Toque para abrir.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Não deixe suas ideias escaparem! Revise sua nota agora."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(noteId.hashCode(), notification)
    }
}