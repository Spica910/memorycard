package com.mostree.memorycard.model

data class DetailedVocabularyItem(
    val text: String, // Word/phrase
    val example: String, // Example sentence
    val etymology: String? = null, // Etymology
    val mnemonic: String? = null // Mnemonic tip
)
