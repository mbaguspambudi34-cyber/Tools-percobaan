package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SubtitleSegment
import kotlin.math.sin

fun getBackdropImageUrl(segment: SubtitleSegment?): String {
    if (segment == null) {
        return "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=500&q=80" // Breaking news microphone
    }
    
    val textLower = segment.text.lowercase()
    
    return when {
        // Space / satellites context
        textLower.contains("meluncurkan") || textLower.contains("satelit") || textLower.contains("nanosatelit") || textLower.contains("orbit") || textLower.contains("angkasa") || textLower.contains("lapan") -> {
            "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=500&q=80" // Satellite orbital earth
        }
        // Archaeology and Gunung Padang
        textLower.contains("gunung padang") || textLower.contains("arkeologi") || textLower.contains("sejarah") || textLower.contains("piramida") || textLower.contains("kuno") or textLower.contains("penelitian") -> {
            "https://images.unsplash.com/photo-1608155686393-8fdd966d784d?auto=format&fit=crop&w=500&q=80" // Ancient stone ruins
        }
        // Astronomy, Galaxy and Bimasakti
        textLower.contains("bimasakti") || textLower.contains("bintang") || textLower.contains("galaxy") || textLower.contains("galaksi") || textLower.contains("lubang hitam") -> {
            "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&w=500&q=80" // Milky way galaxy star map
        }
        // Crypto / Investment
        textLower.contains("crypto") || textLower.contains("investasi") || textLower.contains("saham") || textLower.contains("triliun") || textLower.contains("keuangan") || textLower.contains("ekonomi") -> {
            "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=500&q=80" // Trading chart candle trend
        }
        // AI / Robots / tech
        textLower.contains("robot") || textLower.contains("ai") || textLower.contains("kecerdasan buatan") || textLower.contains("artificial") || textLower.contains("teknologi") || textLower.contains("superkomputer") -> {
            "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=500&q=80" // AI network human-robot interaction
        }
        // Disaster / Tsunami / BMKG
        textLower.contains("gempa") || textLower.contains("tsunami") || textLower.contains("bmkg") || textLower.contains("bencana") || textLower.contains("alam") -> {
            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=500&q=80" // Screen display graph tracking alert
        }
        // Sports, Olympic athlete, Rock-climbing
        textLower.contains("climbing") || textLower.contains("atlet") || textLower.contains("panjat tebing") || textLower.contains("rekor") || textLower.contains("juara") || textLower.contains("olahraga") -> {
            "https://images.unsplash.com/photo-1522163182402-834f871fd851?auto=format&fit=crop&w=500&q=80" // Dynamic mountain climbing climber
        }
        // Comments / interaction questions
        textLower.contains("komentar") || textLower.contains("komen") || textLower.contains("pendapatmu") || textLower.contains("bagikan") || textLower.contains("tonton") -> {
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=500&q=80" // Recording mic / broadcasting setting
        }
        
        // Tag-based defaults
        segment.bgTag == "breaking" -> "https://images.unsplash.com/photo-1588681664899-f142ff2c31b4?auto=format&fit=crop&w=500&q=80" // Broadcast newsroom desk
        segment.bgTag == "tech" -> "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=500&q=80" // Technology circuit board
        segment.bgTag == "finance" -> "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=500&q=80" // Skyscrapers office lights
        segment.bgTag == "science" -> "https://images.unsplash.com/photo-1507668077129-56e32842fceb?auto=format&fit=crop&w=500&q=80" // Atmospheric chemical flask glow
        segment.bgTag == "sports" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=500&q=80" // Speed running stadium tracks
        
        else -> {
            "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=500&q=80" // Default trending global news
        }
    }
}

@Composable
fun ShortsPlayer(
    segments: List<SubtitleSegment>,
    currentIndex: Int,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    visualPreset: String,
    modifier: Modifier = Modifier
) {
    val activeSegment = segments.getOrNull(currentIndex)
    
    // Waveform and visual beat animations
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val beatOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "beat"
    )

    // Base background brush based on segment's bgTag
    val backgroundBrush = remember(activeSegment?.bgTag) {
        val colors = when (activeSegment?.bgTag) {
            "breaking" -> listOf(Color(0xFF1E0E0E), Color(0xFF3E1212), Color(0xFF0F0505))
            "tech" -> listOf(Color(0xFF0C101B), Color(0xFF142442), Color(0xFF050812))
            "finance" -> listOf(Color(0xFF081C15), Color(0xFF1B4332), Color(0xFF030705))
            "science" -> listOf(Color(0xFF120320), Color(0xFF2C1147), Color(0xFF07010C))
            "sports" -> listOf(Color(0xFF251005), Color(0xFF5D2409), Color(0xFF0E0502))
            else -> listOf(Color(0xFF131316), Color(0xFF25252C), Color(0xFF0D0D0F))
        }
        Brush.verticalGradient(colors)
    }

    Box(
        modifier = modifier
            .testTag("shorts_player_container")
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .border(2.dp, Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                    Color.Transparent
                )
            ), RoundedCornerShape(24.dp))
            .clickable { onTogglePlayback() }
    ) {
        // Realistic Background imagery with sleek cross-fade transition and custom color category vignettes
        val backdropUrl = remember(activeSegment) { getBackdropImageUrl(activeSegment) }

        AnimatedContent(
            targetState = backdropUrl,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)).togetherWith(
                    fadeOut(animationSpec = tween(500))
                )
            },
            label = "backdrop_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { url ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = url,
                    contentDescription = "Shorts Backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Color layer vignette matching active news category tag
                val overlayBrush = remember(activeSegment?.bgTag) {
                    val colorAccent = when (activeSegment?.bgTag) {
                        "breaking" -> Color(0xFF320E0E)
                        "tech" -> Color(0xFF0E1A34)
                        "finance" -> Color(0xFF102D24)
                        "science" -> Color(0xFF1B0B2D)
                        "sports" -> Color(0xFF3F1A07)
                        else -> Color(0xFF121217)
                    }
                    Brush.verticalGradient(
                        colors = listOf(
                            colorAccent.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayBrush)
                )
            }
        }

        // Aesthetic moving dynamic stars or grids background
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
            val width = size.width
            val height = size.height
            val cells = 8
            val cellW = width / cells
            val cellH = height / cells
            
            for (i in 0..cells) {
                val offset = (sin((beatOffset + i * 40).toDouble() * Math.PI / 180f) * 8f).toFloat()
                // Horizontal lines
                drawLine(
                    color = Color.White,
                    start = Offset(0f, i * cellH + offset),
                    end = Offset(width, i * cellH + offset),
                    strokeWidth = 1.dp.toPx()
                )
                // Vertical lines
                drawLine(
                    color = Color.White,
                    start = Offset(i * cellW + offset, 0f),
                    end = Offset(i * cellW + offset, height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Active Synthesizer Audio Waveform Visualizer
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .alpha(0.45f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barCount = 20
                    val barSpacing = 8.dp.toPx()
                    val barWidth = (width - (barCount * barSpacing)) / barCount
                    
                    for (i in 0 until barCount) {
                        val multiplier = sin((beatOffset + i * 15).toDouble() * Math.PI / 180f)
                        val barHeight = (height * 0.2f) + (height * 0.6f * Math.abs(multiplier).toFloat())
                        
                        val x = i * (barWidth + barSpacing) + barSpacing / 2
                        val y = (height - barHeight) / 2
                        
                        drawRoundRect(
                            color = Color(0xFF00FFCC),
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
        }

        // Decorative Shorts Tags Overlay (e.g., #shorts badge)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SHORTS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Audio synced",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "AI Narasi",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Playback State indicator Overlays
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .align(Alignment.Center)
                    .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play preview",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }
        }

        // --- SUBTITLE STYLER ACCORDING TO USER CONFIGURATION PRESETS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeSegment != null) {
                AnimatedContent(
                    targetState = activeSegment,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.95f, animationSpec = tween(180)))
                            .togetherWith(fadeOut(animationSpec = tween(120)))
                    },
                    label = "subtitleTrigger"
                ) { targetSegment ->
                    
                    val textStr = targetSegment.text
                    val highlight = targetSegment.highlightWord
                    
                    // Compose annotated string to isolate and style the highlightWord
                    val subtitleContent = buildAnnotatedString {
                        if (highlight.isNotEmpty() && textStr.contains(highlight, ignoreCase = true)) {
                            val parts = textStr.split(Regex("(?i)\\b$highlight\\b"))
                            
                            val index = textStr.indexOf(highlight, ignoreCase = true)
                            val originalWord = textStr.substring(index, index + highlight.length)
                            
                            // Let's weave segments
                            if (parts.isNotEmpty()) {
                                append(parts[0])
                            }
                            
                            withStyle(
                                style = SpanStyle(
                                    color = when (visualPreset) {
                                        "Neon Glow" -> Color(0xFFFFEE00)
                                        "Cyberpunk" -> Color(0xFF00FFCC)
                                        "Minimalist Bold" -> MaterialTheme.colorScheme.tertiary
                                        else -> Color(0xFFFFD700)
                                    },
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(originalWord)
                            }
                            
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        } else {
                            append(textStr)
                        }
                    }

                    // RenderSubtitle according to selected layout preset
                    when (visualPreset) {
                        "Neon Glow" -> {
                            Text(
                                text = subtitleContent,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.SansSerif,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = Color(0xFF00FF99),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 14f
                                    )
                                ),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .shadow(elevation = 0.dp)
                            )
                        }
                        
                        "Cyberpunk" -> {
                            Box(
                                modifier = Modifier
                                    .drawBehind {
                                        // Draw asymmetric electric yellow brackets around the segment box
                                        drawRect(
                                            color = Color(0xFFFFE600),
                                            topLeft = Offset(-16.dp.toPx(), -8.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(
                                                size.width + 32.dp.toPx(),
                                                size.height + 16.dp.toPx()
                                            ),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = subtitleContent,
                                    color = Color(0xFFFFE600),
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                )
                            }
                        }
                        
                        "Minimalist Bold" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = subtitleContent,
                                    color = Color(0xFF111115),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 12.dp)
                                )
                            }
                        }
                        
                        else -> { // "TikTok Classic" style subtites at lower-third
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = subtitleContent,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Pilih Berita & Ketuk 'Generate' untuk Memulai Studio Shorts AI",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        // Animated bottom progress timeline bar reflecting indices
        if (segments.isNotEmpty()) {
            val progressWidth = (currentIndex.toFloat() + 1) / segments.size.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00FFCC), MaterialTheme.colorScheme.primary)
                            )
                        )
                )
            }
        }
    }
}
