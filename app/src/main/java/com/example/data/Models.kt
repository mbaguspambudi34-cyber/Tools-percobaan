package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "shorts_drafts")
@JsonClass(generateAdapter = true)
data class ShortsDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val newsTitle: String,
    val newsContent: String,
    val category: String,
    val script: String,
    val subtitlesJson: String, // Stringified list of SubtitleSegment
    val voiceName: String = "Sarinah", // Indonesian vocal theme
    val musicStyle: String = "Upbeat Beats", // energetic backdrop
    val visualPreset: String = "Neon Glow", // "Neon Glow", "Cyberpunk", "Minimalist Bold", "TikTok Classic"
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SubtitleSegment(
    val text: String,
    val highlightWord: String = "",
    val bgTag: String = "breaking", // e.g. breaking, tech, finance, science, sports, default
    val durationMs: Long = 4000L
)

@JsonClass(generateAdapter = true)
data class NewsItem(
    val title: String,
    val description: String,
    val category: String, // Tekno, Bisnis, Sains, Global, Olahraga, Hiburan
    val source: String,
    val date: String
)

@JsonClass(generateAdapter = true)
data class NewsFeedResponse(
    val news: List<NewsItem>
)

@JsonClass(generateAdapter = true)
data class ScriptShortResponse(
    val title: String,
    val script: String,
    val subtitleSegments: List<SubtitleSegment>
)
