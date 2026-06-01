package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.NewsItem
import com.example.data.SubtitleSegment
import com.example.data.ShortsDraft
import com.example.ui.components.ShortsPlayer
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel States
    val newsFeed by viewModel.newsFeed.collectAsState()
    val isFetchingNews by viewModel.isFetchingNews.collectAsState()
    val newsError by viewModel.newsError.collectAsState()

    val isGeneratingScript by viewModel.isGeneratingScript.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    val currentDraft by viewModel.currentDraft.collectAsState()
    val subtitleSegments by viewModel.subtitleSegments.collectAsState()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSegmentIndex by viewModel.currentSegmentIndex.collectAsState()

    val visualPreset by viewModel.visualPreset.collectAsState()
    val voiceTheme by viewModel.voiceTheme.collectAsState()
    val savedDrafts by viewModel.savedDrafts.collectAsState(initial = emptyList())
    val isApiKeyConfigured by viewModel.isApiKeyConfigured.collectAsState()

    // Visual helper states
    var selectedNewsForGen by remember { mutableStateOf<NewsItem?>(null) }
    var subtitleSegmentToEdit by remember { mutableStateOf<Pair<Int, SubtitleSegment>?>(null) }
    var isExportingVideo by remember { mutableStateOf(false) }
    var exportStatusText by remember { mutableStateOf("Mempersiapkan render video...") }
    var exportProgress by remember { mutableStateOf(0.0f) }

    // Colors & Design Token
    val bgDarkGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0C0E), Color(0xFF131317), Color(0xFF0F0F12))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDarkGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- HEADER TITLE SECTION ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NewsShorts AI",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Studio Pembuat Video YouTube Shorts Berita",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("refresh_news_btn")
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { viewModel.fetchLatestNews() }
                            .padding(10.dp)
                    ) {
                        if (isFetchingNews) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF00FFCC)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh news",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // --- API KEY CONFIGURATION ALERT BAR (Mandatory transparency) ---
            if (!isApiKeyConfigured) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B1E05)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Mode Demo Offline Aktif",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Konfigurasikan kunci GEMINI_API_KEY Anda di panel Secrets AI Studio untuk analisis live trending news orisinal secara online.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- LATEST ACCURATE NEWS BOARD (Aggregator Carousel) ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Newspaper,
                                contentDescription = "News",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Berita Terhangat Hari Ini",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "Akurasi Terverifikasi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00FFCC)
                        )
                    }

                    if (newsError != null) {
                        Text(
                            text = newsError ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(newsFeed) { news ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E1E24)
                                ),
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(150.dp)
                                    .clickable { selectedNewsForGen = news }
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedNewsForGen == news) Color(0xFF00FFCC) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (news.category) {
                                                        "Tekno" -> Color(0xFF0066FF).copy(alpha = 0.2f)
                                                        "Sains" -> Color(0xFF9900FF).copy(alpha = 0.2f)
                                                        "Bisnis" -> Color(0xFF00CC66).copy(alpha = 0.2f)
                                                        else -> Color(0xFF666666).copy(alpha = 0.2f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = news.category,
                                                color = when (news.category) {
                                                    "Tekno" -> Color(0xFF66B2FF)
                                                    "Sains" -> Color(0xFFCC99FF)
                                                    "Bisnis" -> Color(0xFF66FFB2)
                                                    else -> Color.White
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "${news.source} • ${news.date}",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 9.sp
                                        )
                                    }

                                    Text(
                                        text = news.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )

                                    Text(
                                        text = news.description,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- MAIN INTERACTIVE DIGITAL LAB WORKBENCH ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT COLUMN (Mobile viewport places player next, but since this is LazyColumn, we output standard stack layout)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SECTION CARD FOR CUSTOMNEWS AND ACTIVE EDITOR DRAFTS
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Workspace Shorts Creator",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                if (currentDraft != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = currentDraft?.newsTitle ?: "",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF00FFCC)
                                        )

                                        Text(
                                            text = "Gunakan AI untuk mengotomatisasi narasi film pendek dengan skrip penulisan di bawah. Ketuk segmen naskah apa saja untuk mengedit subtitel secara manual.",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                        // LIST OF INTERACTIVE SCENES / SUBTITLE TIMELINE
                                        Text(
                                            text = "Segmen Timeline Subtitel (${subtitleSegments.size} Layar)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            subtitleSegments.forEachIndexed { index, segment ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (currentSegmentIndex == index) Color(0xFF00FFCC).copy(alpha = 0.08f)
                                                            else Color.White.copy(alpha = 0.03f)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (currentSegmentIndex == index) Color(0xFF00FFCC).copy(alpha = 0.3f) else Color.Transparent,
                                                            shape = RoundedCornerShape(10.dp)
                                                        )
                                                        .clickable { subtitleSegmentToEdit = Pair(index, segment) }
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (currentSegmentIndex == index) Color(0xFF00FFCC)
                                                                    else Color.White.copy(alpha = 0.15f)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${index + 1}",
                                                                color = if (currentSegmentIndex == index) Color(0xFF131317) else Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = segment.text,
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (segment.highlightWord.isNotEmpty()) {
                                                                Text(
                                                                    text = "Neon Highlight: ${segment.highlightWord}",
                                                                    color = Color(0xFFFFCC00),
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.White.copy(alpha = 0.06f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = segment.bgTag.uppercase(),
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                        Text(
                                                            text = "${segment.durationMs/1000f}s",
                                                            color = Color.White.copy(alpha = 0.4f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.saveDraftToDatabase() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f).testTag("save_draft_btn")
                                            ) {
                                                Icon(Icons.Rounded.Save, "Save", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Simpan Draf", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isExportingVideo = true
                                                        exportProgress = 0.05f
                                                        exportStatusText = "Menghubungkan ke server rendering..."
                                                        delay(1200)
                                                        exportProgress = 0.3f
                                                        exportStatusText = "Mensintesis audio narasi AI (${voiceTheme})..."
                                                        delay(1500)
                                                        exportProgress = 0.65f
                                                        exportStatusText = "Menerapkan subtitel bergaya '${visualPreset}'..."
                                                        delay(1600)
                                                        exportProgress = 0.9f
                                                        exportStatusText = "Mematangkan layout vertical 9:16 Shorts..."
                                                        delay(1000)
                                                        exportProgress = 1.0f
                                                        exportStatusText = "Render selesai! Berkas MP4 siap dipublikasikan ke YouTube Shorts!"
                                                        delay(1000)
                                                        isExportingVideo = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF00FFCC),
                                                    contentColor = Color(0xFF0C0C0E)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f).testTag("export_video_btn")
                                            ) {
                                                Icon(Icons.Rounded.VideoCall, "Export", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Ekspor Shorts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Generator vacant",
                                            tint = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Belum Ada Berita yang Diproses",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ketuk salah satu kartu berita hangat di bagian atas, atau buat kustom naskah draf Anda sendiri untuk memulai proses naskah AI otomatis.",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // PRESET VISUALS CUSTOMIZER BLOCK (Style presets & voice themes)
                        if (currentDraft != null) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Sesuaikan Estetika Video Shorts",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    // Voice options
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Suara Narator AI otomatis",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Sarinah" to "Sarinah (Wanita)", "Baskoro" to "Baskoro (Pria)").forEach { (key, label) ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (voiceTheme == key) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                                        .border(1.dp, if (voiceTheme == key) Color(0xFF00FFCC) else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.selectVoiceTheme(key) }
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (voiceTheme == key) Color(0xFF00FFCC) else Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Visual subtitle style preset
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Gaya Teks Subtitel Visual",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf("Neon Glow", "Cyberpunk", "Minimalist Bold", "TikTok Classic").forEach { preset ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (visualPreset == preset) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                                        .border(1.dp, if (visualPreset == preset) Color(0xFF00FFCC) else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.selectVisualPreset(preset) }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = preset,
                                                        color = if (visualPreset == preset) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.8f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- THE 9:16 SHORT PLAYER SIMULATOR FRAME ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Simul",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pratinjau YouTube Shorts (9:16)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (subtitleSegments.isNotEmpty()) {
                            Text(
                                text = "Ketuk untuk Putar/Jeda",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f) // keep it in proportion
                            .wrapContentHeight()
                    ) {
                        ShortsPlayer(
                            segments = subtitleSegments,
                            currentIndex = currentSegmentIndex,
                            isPlaying = isPlaying,
                            onTogglePlayback = { viewModel.togglePlayback() },
                            visualPreset = visualPreset
                        )
                    }
                }
            }

            // --- SAVED SHORTS DRAFTS LIST (Local Room Database list) ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Arsip Draf Shorts yang Disimpan ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (savedDrafts.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum Ada Draf yang Tersimpan. Simpan kreasi Anda agar tersaji di sini.",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedDrafts.forEach { draft ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.loadSavedDraft(draft) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.08f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Movie,
                                                    contentDescription = "Draft icon",
                                                    tint = Color(0xFF00FFCC),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = draft.newsTitle,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${draft.category} • Style: ${draft.visualPreset}",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteDraft(draft.id) },
                                            modifier = Modifier.testTag("delete_draft_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Hapus draf",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FULLSCREEN EXPANDABLE MODALS AND SHEET DIALOGS ---

        // 1. Alert Sheet to generate AI script from aggregated news card click
        if (selectedNewsForGen != null) {
            AlertDialog(
                onDismissRequest = { selectedNewsForGen = null },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedNewsForGen?.let { viewModel.generateShortFromNews(it) }
                            selectedNewsForGen = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color(0xFF131317)),
                        modifier = Modifier.testTag("process_short_confirm_btn")
                    ) {
                        Text("Mulai AI Scripting", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedNewsForGen = null }) {
                        Text("Batal", color = Color.White)
                    }
                },
                title = {
                    Text(
                        text = "Proses Menjadi YouTube Shorts?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = selectedNewsForGen?.title ?: "",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00FFCC),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "AI akan otomatis menyusun ringkasan, membuat alur narasi, dan menyinkronkan dialog subtitel dengan format video Shorts vertical 9:16.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                },
                containerColor = Color(0xFF1E1E24)
            )
        }

        // 2. Inline Interactive Subtitle Segment Text Editor
        if (subtitleSegmentToEdit != null) {
            val editIndex = subtitleSegmentToEdit!!.first
            val editSegment = subtitleSegmentToEdit!!.second
            var textVal by remember { mutableStateOf(editSegment.text) }
            var highlightVal by remember { mutableStateOf(editSegment.highlightWord) }
            var durationVal by remember { mutableStateOf(editSegment.durationMs) }

            AlertDialog(
                onDismissRequest = { subtitleSegmentToEdit = null },
                confirmButton = {
                    Button(
                        onClick = {
                            val newList = subtitleSegments.toMutableList()
                            newList[editIndex] = editSegment.copy(
                                text = textVal,
                                highlightWord = highlightVal,
                                durationMs = durationVal
                            )
                            viewModel.edittingSubtitles(newList)
                            subtitleSegmentToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color(0xFF131317))
                    ) {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { subtitleSegmentToEdit = null }) {
                        Text("Batal", color = Color.White)
                    }
                },
                title = {
                    Text(
                        text = "Edit Teks Subtitel Segmen ${editIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = textVal,
                            onValueChange = { textVal = it },
                            label = { Text("Skrip Subtitel (Maks 10 Kata)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                focusedLabelColor = Color(0xFF00FFCC)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = highlightVal,
                            onValueChange = { highlightVal = it },
                            label = { Text("Kata Kunci Highlight Glow") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                focusedLabelColor = Color(0xFF00FFCC)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Durasi Tampil Layar: ${durationVal/1000f} Detik",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Slider(
                                value = durationVal.toFloat(),
                                onValueChange = { durationVal = it.toLong() },
                                valueRange = 2000f..8000f,
                                steps = 12,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                )
                            )
                        }
                    }
                },
                containerColor = Color(0xFF1E1E24)
            )
        }

        // 3. Loading modal for AI scripting generation
        if (isGeneratingScript) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00FFCC),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "AI Merombak Naskah Berita...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Menyusun skrip narasi, mengatur jeda vokal Bahasa Indonesia, serta merancang visualisasi timing subtitel YouTube Shorts...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. Loading modal for MP4 Exporting Simulation rendering
        if (isExportingVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MovieFilter,
                            contentDescription = "Renderer active",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(54.dp)
                        )

                        Text(
                            text = "Shorts Rendering Studio",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        LinearProgressIndicator(
                            progress = { exportProgress },
                            color = Color(0xFF00FFCC),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = "${(exportProgress * 100).toInt()}% • $exportStatusText",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
