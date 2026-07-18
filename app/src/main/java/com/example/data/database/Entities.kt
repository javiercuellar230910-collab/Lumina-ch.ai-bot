package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_profiles")
data class CharacterProfile(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatar: String, // Can be URL, local URI, or drawable reference
    val creator: String,
    val isDefault: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "global_chat_messages")
data class GlobalChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
