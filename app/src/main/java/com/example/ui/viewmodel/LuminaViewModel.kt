package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CharacterProfile
import com.example.data.database.ChatMessage
import com.example.data.database.GlobalChatMessage
import com.example.data.repository.LuminaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LuminaViewModel(
    application: Application,
    private val repository: LuminaRepository
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("lumina_prefs", Context.MODE_PRIVATE)

    // Onboarding state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _chatBackground = MutableStateFlow<String?>(null)
    val chatBackground: StateFlow<String?> = _chatBackground.asStateFlow()

    // Character state
    val characters: StateFlow<List<CharacterProfile>> = repository.allCharacters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCharacterId = MutableStateFlow<String?>(null)
    val activeCharacterId: StateFlow<String?> = _activeCharacterId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var messagesJob: kotlinx.coroutines.Job? = null

    // Global Chat state
    val globalMessages: StateFlow<List<GlobalChatMessage>> = repository.allGlobalMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading states
    private val _isAITyping = MutableStateFlow(false)
    val isAITyping: StateFlow<Boolean> = _isAITyping.asStateFlow()

    init {
        // Load settings from SharedPreferences
        val savedName = prefs.getString("user_name", "") ?: ""
        val savedKey = prefs.getString("api_key", "") ?: ""
        val savedBg = prefs.getString("chat_bg", null)

        _userName.value = savedName
        _apiKey.value = savedKey
        _chatBackground.value = savedBg

        if (savedName.isNotEmpty()) {
            _isLoggedIn.value = true
        }

        // Check and pre-populate DB characters if empty
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
    }

    fun login(name: String, key: String) {
        val trimmedName = name.trim()
        val trimmedKey = key.trim()

        prefs.edit()
            .putString("user_name", trimmedName)
            .putString("api_key", trimmedKey)
            .apply()

        _userName.value = trimmedName
        _apiKey.value = trimmedKey
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit()
            .putString("user_name", "")
            .putString("api_key", "")
            .apply()

        _userName.value = ""
        _apiKey.value = ""
        _isLoggedIn.value = false
        selectCharacter(null)
    }

    fun setChatBackground(base64OrUri: String?) {
        prefs.edit().putString("chat_bg", base64OrUri).apply()
        _chatBackground.value = base64OrUri
    }

    fun selectCharacter(charId: String?) {
        _activeCharacterId.value = charId
        messagesJob?.cancel()
        if (charId != null) {
            messagesJob = viewModelScope.launch {
                repository.getMessagesForCharacter(charId).collect {
                    _messages.value = it
                }
            }
        } else {
            _messages.value = emptyList()
        }
    }

    fun createCharacter(name: String, description: String, avatarUrlOrBase64: String?) {
        viewModelScope.launch {
            val finalAvatar = avatarUrlOrBase64?.ifEmpty { null }
                ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${UriEncode(name)}&backgroundColor=f8fafc"

            val newChar = CharacterProfile(
                id = "char_" + System.currentTimeMillis(),
                name = name.trim(),
                description = description.trim(),
                avatar = finalAvatar,
                creator = _userName.value.ifEmpty { "Lumina" },
                isDefault = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertCharacter(newChar)
            selectCharacter(newChar.id)
        }
    }

    fun deleteCharacter(character: CharacterProfile) {
        viewModelScope.launch {
            if (activeCharacterId.value == character.id) {
                selectCharacter(null)
            }
            repository.deleteCharacter(character)
        }
    }

    fun clearActiveChatHistory() {
        val charId = activeCharacterId.value ?: return
        viewModelScope.launch {
            repository.clearHistory(charId)
        }
    }

    fun sendChatMessage(text: String) {
        val charId = activeCharacterId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _isAITyping.value = true
            try {
                repository.sendMessage(
                    characterId = charId,
                    apiKey = _apiKey.value,
                    userText = trimmed,
                    userName = _userName.value
                )
            } catch (e: Exception) {
                // Error handled inside sendMessage or reported as response
            } finally {
                _isAITyping.value = false
            }
        }
    }

    fun sendGlobalMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repository.sendGlobalMessage(_userName.value.ifEmpty { "Usuario" }, trimmed)
        }
    }

    private fun UriEncode(value: String): String {
        return value.replace(" ", "%20")
    }

    class Factory(
        private val application: Application,
        private val repository: LuminaRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LuminaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LuminaViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
