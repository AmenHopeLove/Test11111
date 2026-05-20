package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val creator: String,
    val creatorTag: String,
    val category: String, // Coding, Business, Forex, University, English, AI
    val videoUrl: String,
    val description: String,
    val likesCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val quizQuestion: String,
    val quizOptionsJson: String, // Comma separated list of options, e.g. "Python,Java,C++,Fortran"
    val quizAnswer: String,
    val xpReward: Int = 50,
    val initialCaptions: String // Default captions if offline
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val completedQuizIds: String = "", // Comma-separated list of quiz video IDs
    val bookmarkedVideoIds: String = "", // Comma-separated list of bookmark video IDs
    val dataSaverEnabled: Boolean = false,
    val customApiKey: String = "" // In-app custom override for Gemini API Key if desired
)
