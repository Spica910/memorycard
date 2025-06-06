package com.example.mymaterialapp.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "learning_decks")
data class LearningDeck(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val topic: String,
    // This list will be populated via a separate query, not stored directly as a column
    @Ignore var cards: MutableList<LearningCard> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var lastLearnedAt: Long? = null
)
