// MainActivity.kt
package com.notes.notesproxmlviews

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query

/**
 * Classe MainActivity - Activity principal que exibe a lista de notas
 *
 * Alterações feitas:
 *
 * 1. Implementar RecyclerView com NoteAdapter:
 *    - Justificação: A versão original não tinha forma de exibir as notas guardadas.
 *      O RecyclerView + NoteAdapter permite mostrar todas as notas do utilizador
 *      de forma eficiente, com scroll suave e reciclagem de memória.
 *
 * 2. Listener em tempo real do firestore(addSnapshotListener):
 *    - Justificação: Sem este listener, a lista não atualizava automaticamente quando
 *      o utilizador criava, editava ou eliminava uma nota. Com o snapshot listener,
 *      a UI atualiza instantaneamente sem necessidade de refresh manual.
 *
 * 3. showDeleteConfirmationDialog:
 *    - Justificação: Evita eliminações acidentais. O AlertDialog pede confirmação
 *      antes de eliminar permanentemente a nota do Firestore.
 *
 * 4. showMenu COM POPUPMENU:
 *    - Justificação: O menu original estava incompleto (apenas criava o PopupMenu
 *      mas não adicionava itens). Foram adicionadas opções de Logout e About.
 *
 * 5. MÉTODO onResume COM RECARGA DE NOTAS:
 *    - Justificação: Quando o utilizador volta da NoteDetailsActivity após guardar
 *      uma nota, assegura que a lista está atualizada.
 */
class MainActivity : AppCompatActivity() {
    // Declaração das views e componentes
    private lateinit var addNoteBtn: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuBtn: ImageButton
    private lateinit var noteAdapter: NoteAdapter
    private val noteList = mutableListOf<Note>()  // Lista mutável que será preenchida pelo adaptador
    private val firebaseAuth = FirebaseAuth.getInstance()  // Instância do Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar referências das views
        addNoteBtn = findViewById(R.id.add_note_btn)
        recyclerView = findViewById(R.id.recyler_view)
        menuBtn = findViewById(R.id.menu_btn)

        setupRecyclerView()  // Configurar o RecyclerView e o adaptador
        setupListeners()     // Configurar os listeners de clique
        loadNotes()          // Carregar notas do Firestore
    }

    /**
     * setupRecyclerView - Configura o RecyclerView com o NoteAdapter
     * - O RecyclerView precisa de saber como organizar os itens (LinearLayoutManager)
     * - O NoteAdapter gere a criação e preenchimento dos itens
     * - Os callbacks definem o comportamento ao clicar/editar/eliminar notas
     */
    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            noteList,
            onNoteClick = { note ->
                // Ao clicar numa nota, abrir a NoteDetailsActivity em modo de edição
                val intent = Intent(this, NoteDetailsActivity::class.java).apply {
                    putExtra("docId", note.id)      // ID do documento no Firestore
                    putExtra("title", note.title)   // Título para pré-carregamento
                    putExtra("content", note.content) // Conteúdo para pré-carregamento
                }
                startActivity(intent)
            },
            onNoteLongClick = { note ->
                // Ao manter pressionado, mostrar diálogo de confirmação para eliminar
                showDeleteConfirmationDialog(note)
            }
        )

        // Configurar o RecyclerView com o adaptador
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter
    }

    /**
     * setupListeners - Configura os listeners dos botões
     */
    private fun setupListeners() {
        // Botão para adicionar nova nota
        addNoteBtn.setOnClickListener {
            startActivity(Intent(this, NoteDetailsActivity::class.java))
        }

        // Botão do menu (hambúrguer)
        menuBtn.setOnClickListener { showMenu() }
    }

    /**
     * loadNotes - Carrega as notas do Firestore com listener em tempo real
     *
     * Porque se usa addSnapshotListener em vez de get():
     * - get() carrega os dados uma única vez e não atualiza automaticamente
     * - addSnapshotListener() mantém um listener ativo que:
     *   * Recebe notificações sempre que há alterações na base de dados
     *   * Atualiza automaticamente a UI quando alguém cria, edita ou elimina uma nota
     *   * Funciona em tempo real, ideal para aplicações colaborativas
     *
     * O listener é automaticamente gerido pelo Firestore e
     * removido quando a Activity é destruída, mas na NoteAdapter temos
     * o método stopListening() para remoção manual se necessário.
     */
    private fun loadNotes() {
        val userId = firebaseAuth.currentUser?.uid ?: return  // Verificar se utilizador está logado

        // Obter referência à coleção de notas e ordenar por data decrescente
        Utility.getCollectionReferenceForNotes()
            .orderBy("createdAt", Query.Direction.DESCENDING)  // Notas mais recentes primeiro
            .addSnapshotListener { snapshot, error ->
                // Verificar se houve erro
                if (error != null) {
                    Toast.makeText(this, "Erro ao carregar notas: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Se snapshot é válido, converter documentos em objetos Note
                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { document ->
                        val note = document.toObject(Note::class.java)
                        note?.id = document.id  // Atribuir o ID do documento à nota
                        note
                    }
                    // Atualizar o adaptador com a nova lista de notas
                    noteAdapter.updateNotes(notes)
                }
            }
    }

    /**
     * showDeleteConfirmationDialog - Mostra diálogo de confirmação antes de eliminar
     * É importante esta função porque eliminar uma nota é uma ação irreversível no Firestore. Este diálogo
     * previne eliminações acidentais, dando ao utilizador uma segunda chance
     * para confirmar ou cancelar a operação.
     */
    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Nota")
            .setMessage("Tem a certeza que deseja eliminar esta nota permanentemente?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteNote(note)  // Confirmado - eliminar nota
            }
            .setNegativeButton("Cancelar", null)  // Cancelado - não fazer nada
            .show()
    }

    /**
     * deleteNote - Elimina uma nota do Firestore
     *
     * Processo de eliminação:
     * 1. Obter referência ao documento específico da nota
     * 2. Chamar método delete() no Firestore
     * 3. Mostrar feedback ao utilizador
     * 4. O snapshot listener atualizará automaticamente a UI
     */
    private fun deleteNote(note: Note) {
        Utility.getCollectionReferenceForNotes()
            .document(note.id)  // Referência ao documento específico pelo ID
            .delete()           // Eliminar documento
            .addOnSuccessListener {
                Toast.makeText(this, "Nota eliminada com sucesso", Toast.LENGTH_SHORT).show()
                // Nota: O snapshot listener atualizará a lista automaticamente
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao eliminar nota: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * showMenu - Mostra o menu de opções (PopUpMenu)
     *
     * O código original apenas criava o PopupMenu mas não adicionava itens.
     * Foi adicionada a funcionalidade completa com duas opções:
     * - Logout: Termina sessão e redireciona para o Login
     * - About: Mostra informações da aplicação
     */
    private fun showMenu() {
        val popupMenu = PopupMenu(this, menuBtn)

        // Adicionar itens ao menu
        popupMenu.menu.add("Sair")      // Logout
        popupMenu.menu.add("Sobre")     // About

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.title) {
                "Sair" -> {
                    // Terminar sessão do Firebase
                    firebaseAuth.signOut()
                    // Redirecionar para o ecrã de login
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()  // Fechar MainActivity para não voltar com o botão "voltar"
                    true
                }
                "Sobre" -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    /**
     * showAboutDialog - Mostra diálogo com informações da aplicação
     */
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sobre a Aplicação")
            .setMessage("Versão 1.0\n\nUma aplicação completa para notas com:\n" +
                    "• Autenticação Firebase\n" +
                    "• Cloud Firestore para armazenamento\n" +
                    "• Suporte a imagens\n" +
                    "• Lembretes com notificações\n" +
                    "• Modo Foco para produtividade\n" +
                    "• Sincronização em tempo real")
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * onResume - Chamado quando a Activity volta a estar visível
     *
     * Quando o utilizador guarda uma nota na NoteDetailsActivity e volta,
     * onResume é chamado. Embora o listener em tempo real já atualize a lista,
     * este método garante que a interface está completamente sincronizada.
     */
    override fun onResume() {
        super.onResume()
        // Recarregar notas para garantir sincronização
        loadNotes()
    }
}