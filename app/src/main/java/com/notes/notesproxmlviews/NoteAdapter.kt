// NoteAdapter.kt
package com.notes.notesproxmlviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

/**
 * Classe NoteAdapter - Adaptador para o RecyclerView que exibe a lista de notas
 *
 * Porque é que esta classe é necessária?
 * =====================================
 * O RecyclerView necessita de um adaptador para:
 * 1. Gerir a criação de layouts para cada item da lista
 * 2. Ligar os dados (notas) às views (elementos visuais)
 * 3. Reciclar views que já não são visíveis para otimizar performance
 * 4. Responder a interações do utilizador (cliques, cliques longos)
 *
 * Execução:
 * ==============
 * - O adaptador atua como uma "ponte" entre os dados (lista de notas) e a UI
 * - Quando o utilizador faz scroll, o RecyclerView pede ao adaptador:
 *   * Quantos itens existem (getItemCount)
 *   * Como criar uma nova view para um item (onCreateViewHolder)
 *   * Como preencher uma view com dados (onBindViewHolder)
 * - O padrão ViewHolder guarda referências às views para evitar chamadas
 *   repetitivas ao findViewById, melhorando a performance
 */
class NoteAdapter(
    private val noteList: MutableList<Note>,  // Lista mutável de notas a exibir
    private val onNoteClick: (Note) -> Unit,   // Callback para quando uma nota é clicada
    private val onNoteLongClick: (Note) -> Unit // Callback para clique longo (eliminar)
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    // Registar listener do Firestore para atualizações em tempo real
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * ViewHolder - Guarda referências a todos os elementos visuais de um item
     *
     * Porque isto é importante:
     * O ViewHolder cacheia as referências às views. Sem isto, cada vez que
     * o utilizador faz scroll, o sistema teria de chamar findViewById para
     * cada elemento, o que é lento e consome bateria.
     */
    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referências para todos os elementos visuais no layout do item
        val titleTextView: TextView = itemView.findViewById(R.id.note_title_text_view)
        val contentTextView: TextView = itemView.findViewById(R.id.note_content_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.note_date_text_view)
        val reminderIcon: ImageView = itemView.findViewById(R.id.reminder_icon)
        val focusIcon: ImageView = itemView.findViewById(R.id.focus_icon)
        val noteImageView: ImageView = itemView.findViewById(R.id.note_image_view)
    }

    /**
     * onCreateViewHolder - Cria um novo ViewHolder quando necessário
     *
     * Este método é chamado pelo RecyclerView quando precisa de criar
     * uma nova view para um item. Infla o layout XML e cria o ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        // Inflar (carregar) o layout XML do item da nota
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item_layout, parent, false)
        return NoteViewHolder(view)
    }

    /**
     * onBindViewHolder - Liga os dados de uma nota específica à view
     *
     * Este método é chamado sempre que o RecyclerView precisa mostrar
     * uma nota na posição especificada. Preenche todos os elementos
     * visuais com os dados da nota.
     */
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]  // Obter a nota na posição atual

        // Preencher título e conteúdo (limitar conteúdo a 100 caracteres para preview)
        holder.titleTextView.text = note.title
        holder.contentTextView.text = note.content.take(100)

        // Formatar e mostrar a data da nota
        val timestamp = note.timestamp ?: note.createdAt
        if (timestamp != null) {
            val date = Date(timestamp.seconds * 1000)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.dateTextView.text = format.format(date)
        }

        // Mostrar ícone de lembrete se a nota tiver um lembrete definido
        holder.reminderIcon.visibility = if (note.reminderTime != null) View.VISIBLE else View.GONE

        // Mostrar ícone de foco se o modo foco estiver ativo para esta nota
        holder.focusIcon.visibility = if (note.focusModeActive) View.VISIBLE else View.GONE

        // Carregar imagem da nota se existir (usando Glide para carregamento eficiente)
        if (!note.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(note.imageUrl)  // URL da imagem no Firebase Storage
                .into(holder.noteImageView)  // Destino da imagem
            holder.noteImageView.visibility = View.VISIBLE
        } else {
            holder.noteImageView.visibility = View.GONE
        }

        // Configurar listener para clique na nota (abrir para edição)
        holder.itemView.setOnClickListener {
            onNoteClick(note)  // Chama o callback definido na MainActivity
        }

        // Configurar listener para clique longo (eliminar nota)
        holder.itemView.setOnLongClickListener {
            onNoteLongClick(note)  // Chama o callback definido na MainActivity
            true  // Indica que o evento foi consumido
        }
    }

    /**
     * getItemCount - Retorna o número total de itens na lista
     *
     * O RecyclerView usa este método para saber quantos itens deve desenhar
     */
    override fun getItemCount(): Int = noteList.size

    /**
     * updateNotes - Atualiza a lista de notas e notifica o RecyclerView
     *
     * Este método é chamado quando há alterações nos dados (nova nota,
     * nota eliminada, etc.). Limpa a lista atual, adiciona as novas notas
     * e notifica o adaptador que os dados mudaram.
     */
    fun updateNotes(newNotes: List<Note>) {
        noteList.clear()           // Limpar lista existente
        noteList.addAll(newNotes)  // Adicionar novas notas
        notifyDataSetChanged()     // Notificar RecyclerView para redesenhar
    }

    /**
     * setListenerRegistration - Guarda o listener do Firestore
     */
    fun setListenerRegistration(registration: ListenerRegistration) {
        listenerRegistration = registration
    }

    /**
     * stopListening - Para de ouvir atualizações do Firestore
     *
     * Importante para evitar memory leaks. Quando a Activity é destruída,
     * devemos remover o listener para não continuar a receber atualizações.
     */
    fun stopListening() {
        listenerRegistration?.remove()
    }
}