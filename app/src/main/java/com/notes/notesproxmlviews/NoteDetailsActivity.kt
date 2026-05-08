package com.notes.notesproxmlviews

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class NoteDetailsActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveNoteBtn: ImageButton
    private lateinit var pageTitleTextView: TextView
    private lateinit var deleteNoteTextViewBtn: TextView
    private lateinit var btnSelectImage: ImageButton
    private lateinit var btnRemoveImage: ImageButton
    private lateinit var ivNoteImage: androidx.appcompat.widget.AppCompatImageView
    private lateinit var btnSetReminder: TextView
    private lateinit var btnFocusMode: TextView
    private lateinit var tvReminderInfo: TextView
    private lateinit var etFocusDuration: EditText

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var docId: String? = null
    private var isEditMode: Boolean = false
    private var currentImageUri: Uri? = null
    private var currentReminderTime: Timestamp? = null
    private var currentImageUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            currentImageUri = it
            ivNoteImage.setImageURI(it)
            ivNoteImage.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_details)

        initializeViews()
        setupListeners()
        loadExistingData()
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.notes_title_text)
        contentEditText = findViewById(R.id.notes_content_text)
        saveNoteBtn = findViewById(R.id.save_note_btn)
        pageTitleTextView = findViewById(R.id.page_title)
        deleteNoteTextViewBtn = findViewById(R.id.delete_note_text_view_btn)
        btnSelectImage = findViewById(R.id.btn_select_image)
        btnRemoveImage = findViewById(R.id.btn_remove_image)
        ivNoteImage = findViewById(R.id.iv_note_image)
        btnSetReminder = findViewById(R.id.btn_set_reminder)
        btnFocusMode = findViewById(R.id.btn_focus_mode)
        tvReminderInfo = findViewById(R.id.tv_reminder_info)
        etFocusDuration = findViewById(R.id.et_focus_duration)

        if (etFocusDuration.text.isNullOrEmpty()) {
            etFocusDuration.setText("25")
        }
    }

    private fun setupListeners() {
        saveNoteBtn.setOnClickListener { saveNote() }
        deleteNoteTextViewBtn.setOnClickListener { deleteNoteFromFirebase() }
        btnSelectImage.setOnClickListener { pickImage.launch("image/*") }

        btnRemoveImage.setOnClickListener {
            currentImageUri = null
            currentImageUrl = ""
            ivNoteImage.setImageDrawable(null)
            ivNoteImage.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }

        btnSetReminder.setOnClickListener { showReminderDialog() }
        btnFocusMode.setOnClickListener { startFocusMode() }
    }

    private fun loadExistingData() {
        docId = intent.getStringExtra("docId")

        if (!docId.isNullOrEmpty()) {
            isEditMode = true
            loadNoteFromFirestore()
        }

        // Carregar dados passados pela intent (caso existam)
        intent.getStringExtra("title")?.let {
            titleEditText.setText(it)
        }
        intent.getStringExtra("content")?.let {
            contentEditText.setText(it)
        }

        if (isEditMode) {
            pageTitleTextView.text = getString(R.string.edit_your_note)
            deleteNoteTextViewBtn.visibility = View.VISIBLE
        }
    }

    private fun loadNoteFromFirestore() {
        docId?.let { id ->
            Utility.getCollectionReferenceForNotes().document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Converter manualmente para HashMap e depois para Note
                        val data = document.data
                        if (data != null) {
                            titleEditText.setText(data["title"] as? String ?: "")
                            contentEditText.setText(data["content"] as? String ?: "")
                            currentImageUrl = data["imageUrl"] as? String ?: ""

                            if (currentImageUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(currentImageUrl)
                                    .into(ivNoteImage)
                                ivNoteImage.visibility = View.VISIBLE
                                btnRemoveImage.visibility = View.VISIBLE
                            }

                            val reminderTime = data["reminderTime"] as? Timestamp
                            if (reminderTime != null) {
                                currentReminderTime = reminderTime
                                val date = Date(reminderTime.seconds * 1000)
                                tvReminderInfo.text = "⏰ Lembrete: ${formatDate(date)}"
                                tvReminderInfo.visibility = View.VISIBLE
                            }

                            val focusDuration = data["focusDuration"] as? Long
                            if (focusDuration != null && focusDuration > 0) {
                                etFocusDuration.setText(focusDuration.toString())
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar nota: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun formatDate(date: Date): String {
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun showReminderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reminder, null)
        val dpDate = dialogView.findViewById<android.widget.DatePicker>(R.id.datePicker)
        val tpTime = dialogView.findViewById<android.widget.TimePicker>(R.id.timePicker)

        val now = Calendar.getInstance()
        dpDate.updateDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tpTime.hour = now.get(Calendar.HOUR_OF_DAY)
            tpTime.minute = now.get(Calendar.MINUTE)
        }

        AlertDialog.Builder(this)
            .setTitle("⏰ Definir Lembrete")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, dpDate.year)
                    set(Calendar.MONTH, dpDate.month)
                    set(Calendar.DAY_OF_MONTH, dpDate.dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) tpTime.hour else tpTime.currentHour)
                    set(Calendar.MINUTE, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) tpTime.minute else tpTime.currentMinute)
                    set(Calendar.SECOND, 0)
                }

                currentReminderTime = Timestamp(calendar.time)
                tvReminderInfo.text = "⏰ Lembrete: ${formatDate(calendar.time)}"
                tvReminderInfo.visibility = View.VISIBLE
                Toast.makeText(this, "Lembrete definido! Salve a nota para ativar.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startFocusMode() {
        val duration = etFocusDuration.text.toString().toIntOrNull()
        if (duration == null || duration <= 0) {
            Toast.makeText(this, "Digite uma duração válida (minutos)", Toast.LENGTH_SHORT).show()
            return
        }

        val noteTitle = titleEditText.text.toString()
        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Digite um título para a nota antes de iniciar o foco", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🎯 Modo Foco")
            .setMessage("Você entrará em modo foco por $duration minutos.\n\n📝 Nota: $noteTitle\n\n• Timer será exibido\n• Notificação ao final\n• Mantenha o foco!")
            .setPositiveButton("Iniciar Foco") { _, _ ->
                startFocusTimer(duration)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startFocusTimer(minutes: Int) {
        val intent = Intent(this, FocusTimerActivity::class.java).apply {
            putExtra("NOTE_TITLE", titleEditText.text.toString())
            putExtra("NOTE_CONTENT", contentEditText.text.toString())
            putExtra("DURATION_MINUTES", minutes)
        }
        startActivity(intent)
    }

    private fun saveNote() {
        val noteTitle = titleEditText.text.toString()
        val noteContent = contentEditText.text.toString()

        if (noteTitle.isEmpty()) {
            titleEditText.error = "Título é obrigatório"
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentImageUri != null) {
            uploadImageAndSaveNote(noteTitle, noteContent, userId)
        } else {
            saveNoteToFirestore(noteTitle, noteContent, userId, currentImageUrl)
        }
    }

    private fun uploadImageAndSaveNote(title: String, content: String, userId: String) {
        val imageRef = storage.reference.child("note_images/${userId}/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(currentImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveNoteToFirestore(title, content, userId, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao fazer upload: ${e.message}", Toast.LENGTH_SHORT).show()
                saveNoteToFirestore(title, content, userId, "")
            }
    }

    private fun saveNoteToFirestore(title: String, content: String, userId: String, imageUrl: String) {
        val noteData = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "imageUrl" to imageUrl,
            "focusDuration" to (etFocusDuration.text.toString().toIntOrNull() ?: 25),
            "updatedAt" to Timestamp.now()
        )

        if (currentReminderTime != null) {
            noteData["reminderTime"] = currentReminderTime
        }

        if (!isEditMode) {
            noteData["createdAt"] = Timestamp.now()
        }

        val documentReference = if (isEditMode && !docId.isNullOrEmpty()) {
            Utility.getCollectionReferenceForNotes().document(docId!!)
        } else {
            Utility.getCollectionReferenceForNotes().document()
        }

        documentReference.set(noteData)
            .addOnSuccessListener {
                if (currentReminderTime != null) {
                    scheduleNotification(title, currentReminderTime!!)
                }
                Toast.makeText(this, if (isEditMode) "Nota atualizada!" else "Nota criada!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleNotification(title: String, reminderTime: Timestamp) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("NOTE_TITLE", title)
            putExtra("NOTE_ID", docId ?: UUID.randomUUID().toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderTime.seconds.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = reminderTime.seconds * 1000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun deleteNoteFromFirebase() {
        AlertDialog.Builder(this)
            .setTitle("Excluir Nota")
            .setMessage("Tem certeza que deseja excluir esta nota?")
            .setPositiveButton("Excluir") { _, _ ->
                val documentReference = Utility.getCollectionReferenceForNotes().document(docId.toString())
                documentReference.delete()
                    .addOnSuccessListener {
                        if (currentImageUrl.isNotEmpty()) {
                            storage.getReferenceFromUrl(currentImageUrl).delete()
                        }
                        Toast.makeText(this, "Nota excluída com sucesso", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}