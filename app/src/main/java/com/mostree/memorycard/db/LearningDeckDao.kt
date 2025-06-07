package com.example.mymaterialapp.db

import androidx.room.*
import com.example.mymaterialapp.model.LearningDeck
import kotlinx.coroutines.flow.Flow // For Flow-based observation

@Dao
interface LearningDeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: LearningDeck)

    @Update
    suspend fun updateDeck(deck: LearningDeck)

    @Delete
    suspend fun deleteDeck(deck: LearningDeck)

    @Query("SELECT * FROM learning_decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<LearningDeck>> // Observe changes with Flow

    @Query("SELECT * FROM learning_decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: String): LearningDeck?
}
