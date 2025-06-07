package com.mostree.memorycard.db

import androidx.room.*
import com.mostree.memorycard.model.LearningCard
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: LearningCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<LearningCard>) // For adding multiple cards

    @Update
    suspend fun updateCard(card: LearningCard)

    @Delete
    suspend fun deleteCard(card: LearningCard)

    @Query("SELECT * FROM learning_cards WHERE deckId = :deckId ORDER BY createdAt ASC")
    fun getCardsForDeck(deckId: String): Flow<List<LearningCard>>

    @Query("SELECT * FROM learning_cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): LearningCard?

    @Query("DELETE FROM learning_cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: String) // When a deck is deleted

    @Query("SELECT * FROM learning_cards WHERE deckId = :deckId AND isKnown = 0 ORDER BY createdAt ASC")
    fun getUnlearnedCardsForDeck(deckId: String): Flow<List<LearningCard>>
}
