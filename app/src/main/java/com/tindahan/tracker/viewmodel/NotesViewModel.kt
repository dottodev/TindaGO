package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.local.entities.Note
import com.tindahan.tracker.data.repository.TindahanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repo: TindahanRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val items = _query.debounce(150).flatMapLatest { repo.searchNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val count = repo.observeNoteCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setQuery(q: String) { _query.value = q }

    fun save(title: String, body: String, editing: Note?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val r = if (editing == null) repo.addNote(title, body)
            else repo.updateNote(editing.copy(title = title.trim(), body = body.trim()))
            onDone(r.isSuccess)
        }
    }

    fun delete(note: Note) { viewModelScope.launch { repo.deleteNote(note) } }

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NotesViewModel(repo) as T
    }
}
