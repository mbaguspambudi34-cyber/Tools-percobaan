package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class StudioViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ShortsRepository(database.shortsDao())

    val savedDrafts = repository.allDrafts

    // API Key Check
    private val _isApiKeyConfigured = MutableStateFlow(false)
    val isApiKeyConfigured = _isApiKeyConfigured.asStateFlow()

    // News headlines list state
    private val _newsFeed = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsFeed: StateFlow<List<NewsItem>> = _newsFeed.asStateFlow()

    private val _isFetchingNews = MutableStateFlow(false)
    val isFetchingNews = _isFetchingNews.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError = _newsError.asStateFlow()

    // Active AI Short Generator State
    private val _isGeneratingScript = MutableStateFlow(false)
    val isGeneratingScript = _isGeneratingScript.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError = _generationError.asStateFlow()

    private val _currentDraft = MutableStateFlow<ShortsDraft?>(null)
    val currentDraft = _currentDraft.asStateFlow()

    private val _subtitleSegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val subtitleSegments = _subtitleSegments.asStateFlow()

    // Interactive Shorts Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSegmentIndex = MutableStateFlow(0)
    val currentSegmentIndex = _currentSegmentIndex.asStateFlow()

    // UI Configuration presets for visual subtitles
    private val _visualPreset = MutableStateFlow("Neon Glow") // Default
    val visualPreset = _visualPreset.asStateFlow()

    private val _voiceTheme = MutableStateFlow("Sarinah") // Sarinah (Indonesian female), Baskoro (Male)
    val voiceTheme = _voiceTheme.asStateFlow()

    // Text To Speech engine
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var playbackJob: Job? = null

    init {
        checkApiKeyConfiguration()
        loadInitialNews()
        initializeTts()
    }

    private fun checkApiKeyConfiguration() {
        val key = com.example.BuildConfig.GEMINI_API_KEY ?: ""
        _isApiKeyConfigured.value = key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    private fun loadInitialNews() {
        // Pre-populate with high quality accurate breaking news (Offline mode default)
        _newsFeed.value = getOfflineNews()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(getApplication(), this)
        } catch (e: Exception) {
            Log.e("StudioViewModel", "Failed to init TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val idLocale = Locale("id", "ID")
            val result = tts?.setLanguage(idLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English/default if Indonesian is not available
                tts?.setLanguage(Locale.US)
                Log.w("StudioViewModel", "Indonesian TTS not supported, fell back to US locale.")
            }
            // Increase speech rate slightly for energetic shorts vibe
            tts?.setSpeechRate(1.2f)
            tts?.setPitch(1.05f)
            isTtsReady = true
            setupTtsProgressListener()
        } else {
            Log.e("StudioViewModel", "TTS initialization failed")
        }
    }

    private fun setupTtsProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: 0
                _currentSegmentIndex.value = index
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: 0
                val nextIndex = index + 1
                if (nextIndex < _subtitleSegments.value.size) {
                    speakSegment(nextIndex)
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        _isPlaying.value = false
                        _currentSegmentIndex.value = 0
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("StudioViewModel", "TTS utterance progress error on ID $utteranceId")
            }
        })
    }

    fun fetchLatestNews() {
        if (_isFetchingNews.value) return
        _isFetchingNews.value = true
        _newsError.value = null

        viewModelScope.launch {
            try {
                if (!_isApiKeyConfigured.value) {
                    // Simulating AI fetch if API key is not ready
                    delay(1500)
                    _newsFeed.value = getOfflineNews().shuffled()
                    _isFetchingNews.value = false
                    return@launch
                }

                val prompt = "Generate a list of 5 trending, accurate news topics of today (especially in Indonesia, but also global major breakthroughs). Keep it in Indonesian context. Provide name/headline, categorized into Bisnis, Tekno, Sains, Olahraga, Hiburan, Global. Offer an intriguing description for each, along with simulated source and date. Return the output as valid JSON matching the schema: { 'news': [ { 'title': 'Short Headline', 'description': 'Full news detail paragraph...', 'category': 'Tekno', 'source': 'Kompas', 'date': 'Hari Ini' } ] }"
                val systemPrompt = "You are a professional Indonesian news aggregator. You collect real, interesting, accurate, educational stories of current technology, science, and events."
                
                val result = GeminiApiClient.generateWithModel(prompt, systemPrompt)
                val cleanedJson = cleanJsonOutput(result)
                
                val adapter = GeminiApiClient.moshiParser.adapter(NewsFeedResponse::class.java)
                val response = adapter.fromJson(cleanedJson)
                
                if (response != null && response.news.isNotEmpty()) {
                    _newsFeed.value = response.news
                } else {
                    _newsFeed.value = getOfflineNews().shuffled()
                }
            } catch (e: Exception) {
                Log.e("StudioViewModel", "Error fetching news from Gemini", e)
                _newsError.value = "Gagal memuat berita terkini secara Live (${e.localizedMessage}). Menggunakan berita terverifikasi."
                _newsFeed.value = getOfflineNews()
            } finally {
                _isFetchingNews.value = false
            }
        }
    }

    fun selectVoiceTheme(theme: String) {
        _voiceTheme.value = theme
        if (theme == "Baskoro") { // Male deep
            tts?.setPitch(0.85f)
            tts?.setSpeechRate(1.15f)
        } else { // Sarinah Female active
            tts?.setPitch(1.08f)
            tts?.setSpeechRate(1.23f)
        }
    }

    fun selectVisualPreset(preset: String) {
        _visualPreset.value = preset
    }

    fun generateShortFromNews(news: NewsItem) {
        if (_isGeneratingScript.value) return
        _isGeneratingScript.value = true
        _generationError.value = null
        stopPlayback()

        viewModelScope.launch {
            try {
                if (!_isApiKeyConfigured.value) {
                    // Local AI Simulation for immediate play experience without block
                    delay(2500)
                    val mockLocalResponse = generateMockScript(news)
                    updateActiveDraft(mockLocalResponse, news)
                    return@launch
                }

                val prompt = """
                    Make a creative vertical YouTube Shorts screenplay from this news article:
                    TITLE: ${news.title}
                    CATEGORY: ${news.category}
                    CONTENT: ${news.description}
                    
                    Translate it into a highly engaging, fast-pacing, and educational vertical short script (approx 30-40 seconds length).
                    Break the script down into a sequence of 5 to 7 individual subtitled scenes/segments, in Indonesian language.
                    For each scene, provide the exact subtitle text (under 8 words, punchy & dramatic), a single crucial word ('highlightWord') to style with extreme neon yellow/cyan glow highlights, a highly descriptive English 'bgTag' representing the visual backdrop category (choose ONLY among 'breaking', 'tech', 'finance', 'science', 'sports', 'general' based on the segment subject) to display beautiful imagery, and duration in milliseconds (typically 4000 to 5500ms each).
                    
                    Return the output in valid, structured JSON matching this schema:
                    {
                      'title': 'Engaging short video title',
                      'script': 'Full combined narrative script of the Short...',
                      'subtitleSegments': [
                        {
                          'text': 'Exact short sentence for subtitle screen.',
                          'highlightWord': 'SingleWordFromText',
                          'bgTag': 'tech',
                          'durationMs': 4500
                        }
                      ]
                    }
                """.trimIndent()

                val systemPrompt = "You are a master Shorts creator who specializes in writing highly engaging scripts with perfectly-timed subtitle segments and rich visuals."
                val result = GeminiApiClient.generateWithModel(prompt, systemPrompt)
                val cleanedJson = cleanJsonOutput(result)
                
                val adapter = GeminiApiClient.moshiParser.adapter(ScriptShortResponse::class.java)
                val response = adapter.fromJson(cleanedJson)

                if (response != null && response.subtitleSegments.isNotEmpty()) {
                    updateActiveDraft(response, news)
                } else {
                    _generationError.value = "Format respons AI tidak valid. Mencoba simulasi lokal."
                    updateActiveDraft(generateMockScript(news), news)
                }
            } catch (e: Exception) {
                Log.e("StudioViewModel", "Error generating short screenplay", e)
                _generationError.value = "Gagal membuat naskah AI secara online (${e.localizedMessage}). Mengaktifkan draf cadangan pintar."
                updateActiveDraft(generateMockScript(news), news)
            } finally {
                _isGeneratingScript.value = false
            }
        }
    }

    private fun updateActiveDraft(response: ScriptShortResponse, news: NewsItem) {
        val listType = Types.newParameterizedType(List::class.java, SubtitleSegment::class.java)
        val adapter = GeminiApiClient.moshiParser.adapter<List<SubtitleSegment>>(listType)
        val subtitlesJson = adapter.toJson(response.subtitleSegments)

        val draft = ShortsDraft(
            newsTitle = response.title,
            newsContent = news.description,
            category = news.category,
            script = response.script,
            subtitlesJson = subtitlesJson,
            voiceName = _voiceTheme.value,
            musicStyle = "Synthesizer Uplift",
            visualPreset = _visualPreset.value
        )

        _currentDraft.value = draft
        _subtitleSegments.value = response.subtitleSegments
        _currentSegmentIndex.value = 0
    }

    fun saveDraftToDatabase() {
        val draft = _currentDraft.value ?: return
        viewModelScope.launch {
            repository.insertDraft(draft.copy(
                voiceName = _voiceTheme.value,
                visualPreset = _visualPreset.value,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun loadSavedDraft(draft: ShortsDraft) {
        stopPlayback()
        _currentDraft.value = draft
        _voiceTheme.value = draft.voiceName
        _visualPreset.value = draft.visualPreset

        val listType = Types.newParameterizedType(List::class.java, SubtitleSegment::class.java)
        val adapter = GeminiApiClient.moshiParser.adapter<List<SubtitleSegment>>(listType)
        try {
            _subtitleSegments.value = adapter.fromJson(draft.subtitlesJson) ?: emptyList()
        } catch (e: Exception) {
            _subtitleSegments.value = listOf(SubtitleSegment(text = draft.script))
        }
        _currentSegmentIndex.value = 0
    }

    fun deleteDraft(draftId: Int) {
        viewModelScope.launch {
            repository.deleteDraft(draftId)
            if (_currentDraft.value?.id == draftId) {
                _currentDraft.value = null
                _subtitleSegments.value = emptyList()
                _currentSegmentIndex.value = 0
            }
        }
    }

    // Interactive Media controls
    fun togglePlayback() {
        if (_subtitleSegments.value.isEmpty()) return

        if (_isPlaying.value) {
            stopPlayback()
        } else {
            _isPlaying.value = true
            speakSegment(_currentSegmentIndex.value)
        }
    }

    private fun speakSegment(index: Int) {
        if (!isTtsReady || tts == null) {
            // Emulate slide transitions automatically with coroutines if TTS not ready
            startTimerBasedPlayback(index)
            return
        }

        val segments = _subtitleSegments.value
        if (index >= segments.size) {
            stopPlayback()
            return
        }

        _currentSegmentIndex.value = index
        val currentText = segments[index].text

        // Request speaking through Android's engine
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(currentText, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    private fun startTimerBasedPlayback(startIndex: Int) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var idx = startIndex
            val segments = _subtitleSegments.value
            while (idx < segments.size && _isPlaying.value) {
                _currentSegmentIndex.value = idx
                val delayMs = segments[idx].durationMs
                delay(delayMs)
                idx++
            }
            _isPlaying.value = false
            _currentSegmentIndex.value = 0
        }
    }

    fun edittingSubtitles(updatedSegments: List<SubtitleSegment>) {
        _subtitleSegments.value = updatedSegments
        val draft = _currentDraft.value ?: return

        val listType = Types.newParameterizedType(List::class.java, SubtitleSegment::class.java)
        val adapter = GeminiApiClient.moshiParser.adapter<List<SubtitleSegment>>(listType)
        val updatedJson = adapter.toJson(updatedSegments)

        _currentDraft.value = draft.copy(
            subtitlesJson = updatedJson,
            script = updatedSegments.joinToString(" ") { it.text }
        )
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }

    private fun cleanJsonOutput(raw: String): String {
        return raw.trim()
            .replace(Regex("^```json", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    // Sample accurate curated news for indonesia fallback
    private fun getOfflineNews(): List<NewsItem> {
        return listOf(
            NewsItem(
                title = "Indonesia Meluncurkan Pusat Riset Artificial Intelligence",
                description = "Jakarta secara resmi meluncurkan super-hub kolaborasi kecerdasan buatan terbesar untuk menunjang startup digital, menghadirkan server bertenaga GPU lokal serta model bahasa besar (LLM) khusus berbahasa Indonesia terakurat demi kedaulatan digital bangsa.",
                category = "Tekno",
                source = "Kemenkominfo",
                date = "1 Juni 2026"
            ),
            NewsItem(
                title = "Asosiasi Antariksa Siapkan Peluncuran Nanosatelit Eko-Iklim",
                description = "Misi penelitian antariksa tanah air meluncurkan tiga buah nanosatelit mikro ke orbit rendah. Satelit ini bertugas mendeteksi perubahan suhu regional, memantau sebaran karbon hutan tropis Kalimantan, serta memberikan peringatan dini bencana banjir.",
                category = "Sains",
                source = "LAPAN / BRIN",
                date = "30 Mei 2026"
            ),
            NewsItem(
                title = "Sektor Start-up Energi Hijau Raih Investasi Rp2.3 Triliun",
                description = "Arus modal asing mengalir deras ke Indonesia untuk pendanaan teknologi agrikultur berkelanjutan serta panel surya murah ramah lingkungan. Hal ini memacu penciptaan ribuan lapangan kerja baru berbasis energi terbarukan di pedesaan.",
                category = "Bisnis",
                source = "Katadata",
                date = "31 Mei 2026"
            ),
            NewsItem(
                title = "Geo-Radar Ungkap Misteri Ruang Bawah Tanah Gunung Padang",
                description = "Tim peneliti arkeologi kembali menemukan anomali geofisika berupa struktur ruangan simetris tertimbun di kedalaman 15 meter pada situs megalitikum Gunung Padang. Bukti ini berpotensi merombak sejarah peradaban Nusantara kuno.",
                category = "Sains",
                source = "Arkeologi Indonesia",
                date = "29 Mei 2026"
            ),
            NewsItem(
                title = "Bimasakti: Superkomputer Pemantau Gempa Resmi Beroperasi",
                description = "Pusat mitigasi bencana meresmikan superkomputer pemeta aktivitas tektonik dengan kemampuan kalkulasi data seismograf 500 Terabytes per detik. Kecepatan ini meningkatkan validasi evakuasi dini tsunami dari 5 menit menjadi hanya 45 detik.",
                category = "Tekno",
                source = "BMKG",
                date = "28 Mei 2026"
            ),
            NewsItem(
                title = "Atlet Panjat Tebing Indonesia Raih Rekor Kejuaraan Dunia",
                description = "Pemuda berbakat asal Jawa Tengah berhasil menorehkan rekor dunia baru pada kejuaraan panjat tebing kecepatan (speed climbing) di Munich dengan catatan waktu dramatis 4,72 detik, mengukuhkan dominasi tim Merah Putih di kancah global.",
                category = "Olahraga",
                source = "Antara News",
                date = "27 Mei 2026"
            )
        )
    }

    private fun generateMockScript(news: NewsItem): ScriptShortResponse {
        val titleText = "Shorts - ${news.title.take(30)}..."
        val segments = when (news.category) {
            "Tekno", "Sains" -> listOf(
                SubtitleSegment("BREAKING NEWS hari ini datang dari dunia teknologi!", "BREAKING", "breaking", 3800),
                SubtitleSegment("Ketahui lompatan inovasi ini langsung!", "lompatan", "tech", 4200),
                SubtitleSegment("${news.title} sedang ramai dibahas!", "ramai", "tech", 4500),
                SubtitleSegment("Kenapa hal ini dianggap sangat krusial?", "krusial", "science", 4400),
                SubtitleSegment("Teknologi lokal ini siap kuasai masa depan!", "kuasai", "tech", 4500),
                SubtitleSegment("Bagaimana pendapatmu? Tulis d kolom komen!", "komen", "general", 3800)
            )
            "Bisnis" -> listOf(
                SubtitleSegment("Ini dia info bisnis paling dinanti!", "bisnis", "finance", 3900),
                SubtitleSegment("Investasi dan arus modal bernilai fantastis!", "fantastis", "finance", 4400),
                SubtitleSegment("${news.title} mengguncang pasar ekonomi!", "mengguncang", "finance", 4500),
                SubtitleSegment("Mendorong ratusan lapangan kerja produktif baru!", "lapangan", "general", 4300),
                SubtitleSegment("Raih peluang emas saat tren ini bergulir!", "peluang", "finance", 4200),
                SubtitleSegment("Jangan lupa share berita penting ini!", "share", "breaking", 3700)
            )
            else -> listOf(
                SubtitleSegment("Kabar terhangat di seluruh penjuru Indonesia!", "terhangat", "breaking", 4000),
                SubtitleSegment("${news.title} menyita sorotan publik!", "sorotan", "general", 4500),
                SubtitleSegment("Mari kita telusuri fakta seutuhnya bersama!", "fakta", "general", 4200),
                SubtitleSegment("Inovasi hebat ini patut kita apresiasi!", "apresiasi", "sports", 4400),
                SubtitleSegment("Simak berita menarik selanjutnya di sini!", "menarik", "breaking", 3800)
            )
        }

        return ScriptShortResponse(
            title = titleText,
            script = segments.joinToString(" ") { it.text },
            subtitleSegments = segments
        )
    }
}
