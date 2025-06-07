package com.example.mymaterialapp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "learning_cards",
    foreignKeys = [ForeignKey(
        entity = LearningDeck::class,
        parentColumns = ["id"],
        childColumns = ["deckId"],
        onDelete = ForeignKey.CASCADE // If a deck is deleted, its cards are also deleted
    )],
    indices = [Index(value = ["deckId"])] // Index for faster queries on deckId
)
data class LearningCard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String, // Foreign key to LearningDeck
    val text: String, // English word
    val translation: String?, // Korean translation
    var isKnown: Boolean = false,
    val etymology: String? = null,
    val mnemonic: String? = null,
    val pronunciationAudioPath: String? = null,
    val examples: List<String>? = null, // Will use TypeConverter
    val createdAt: Long = System.currentTimeMillis()
)
