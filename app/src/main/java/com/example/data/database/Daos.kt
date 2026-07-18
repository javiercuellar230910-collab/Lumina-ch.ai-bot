package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character_profiles ORDER BY timestamp DESC")
    fun getAllCharacters(): Flow<List<CharacterProfile>>

    @Query("SELECT * FROM character_profiles WHERE id = :id LIMIT 1")
    suspend fun getCharacterById(id: String): CharacterProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterProfile)

    @Delete
    suspend fun deleteCharacter(character: CharacterProfile)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun getMessagesForCharacter(characterId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForCharacter(characterId: String): ChatMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun deleteMessagesForCharacter(characterId: String)
}

@Dao
interface GlobalMessageDao {
    @Query("SELECT * FROM global_chat_messages ORDER BY timestamp ASC")
    fun getAllGlobalMessages(): Flow<List<GlobalChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalMessage(message: GlobalChatMessage)

    @Query("DELETE FROM global_chat_messages")
    suspend fun clearAllGlobalMessages()
}
