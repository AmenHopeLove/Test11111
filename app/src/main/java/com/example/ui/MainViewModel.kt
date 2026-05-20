package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GeminiClient
import com.example.data.UserProgressEntity
import com.example.data.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    val videos: StateFlow<List<VideoEntity>>
    val progress: StateFlow<UserProgressEntity>

    // Category filtering
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Current index in active feed
    private val _currentFeedIndex = MutableStateFlow(0)
    val currentFeedIndex: StateFlow<Int> = _currentFeedIndex.asStateFlow()

    // Interactive Quiz Response State
    private val _quizStatus = MutableStateFlow<Map<String, QuizStatus>>(emptyMap())
    val quizStatus: StateFlow<Map<String, QuizStatus>> = _quizStatus.asStateFlow()

    // AI operation logs
    private val _isLoadingGemini = MutableStateFlow(false)
    val isLoadingGemini: StateFlow<Boolean> = _isLoadingGemini.asStateFlow()

    private val _geminiSpeechResult = MutableStateFlow<String?>(null)
    val geminiSpeechResult: StateFlow<String?> = _geminiSpeechResult.asStateFlow()

    private val _aiQuizState = MutableStateFlow<AiQuizData?>(null)
    val aiQuizState: StateFlow<AiQuizData?> = _aiQuizState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        
        videos = repository.allVideos
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        progress = repository.userProgress
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UserProgressEntity()
            )

        viewModelScope.launch {
            repository.initializeOfflineContent()
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _currentFeedIndex.value = 0 // Reset index
    }

    fun setCurrentFeedIndex(index: Int) {
        _currentFeedIndex.value = index
    }

    fun toggleLike(videoId: String) {
        viewModelScope.launch {
            repository.toggleLike(videoId)
        }
    }

    fun toggleBookmark(videoId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(videoId)
        }
    }

    fun submitQuizAnswer(videoId: String, selectedOption: String, correctAnswer: String, xpReward: Int) {
        viewModelScope.launch {
            val isCorrect = selectedOption.trim().lowercase() == correctAnswer.trim().lowercase()
            val currentMap = _quizStatus.value.toMutableMap()
            
            if (isCorrect) {
                currentMap[videoId] = QuizStatus.CORRECT
                _quizStatus.value = currentMap
                repository.awardXp(videoId, xpReward)
            } else {
                currentMap[videoId] = QuizStatus.WRONG
                _quizStatus.value = currentMap
            }
        }
    }

    fun resetQuiz(videoId: String) {
        val currentMap = _quizStatus.value.toMutableMap()
        currentMap.remove(videoId)
        _quizStatus.value = currentMap
    }

    fun updateDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDataSaver(enabled)
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            repository.updateApiKey(key)
        }
    }

    /**
     * Publishes a new micro-learning video directly to Room
     */
    fun createVideo(
        title: String,
        creator: String,
        category: String,
        videoUrl: String,
        description: String,
        question: String,
        options: String,
        answer: String
    ) {
        viewModelScope.launch {
            val randomId = "custom_${System.currentTimeMillis()}"
            val finalVideoUrl = if (videoUrl.isBlank()) {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            } else {
                videoUrl
            }
            val newVideo = VideoEntity(
                id = randomId,
                title = title,
                creator = creator,
                creatorTag = "@${creator.replace(" ", "").lowercase()}",
                category = category,
                videoUrl = finalVideoUrl,
                description = description,
                likesCount = 0,
                isLiked = false,
                isBookmarked = false,
                quizQuestion = question.ifBlank { "What did you learn from this module?" },
                quizOptionsJson = options.ifBlank { "Option A,Option B,Option C,Option D" },
                quizAnswer = answer.ifBlank { "Option A" },
                xpReward = 50,
                initialCaptions = "[00:00] Live from the Creator Studio!\n[00:05] Today we're learning about $category.\n[00:10] $description\n[00:15] Solve our interactive quiz. Earn XP and level up fast!"
            )
            val currentList = ArrayList(videos.value)
            currentList.add(0, newVideo) // Put on top of the list
            
            // Insert in database
            val db = AppDatabase.getDatabase(getApplication())
            db.appDao().insertVideos(listOf(newVideo))
        }
    }

    /**
     * AI Feature: Transcribes/Generates captions dynamically using Gemini API!
     */
    fun generateAiAutoCaptions(videoId: String, title: String, category: String) {
        viewModelScope.launch {
            _isLoadingGemini.value = true
            _geminiSpeechResult.value = null
            
            val prompt = """
                You are Whisper-African-Voice, a high-fidelity auto-transcriber built for modern mobile micro-learning.
                Generate a formatted closed caption / subtitles list (5 lines with timestamps, e.g. [00:01], [00:06]) for a 30-second educational short video about $category titled '$title'. 
                Keep it highly engaging for African youth, adding relevant emojis. Return ONLY the timestamps and subtitle text lines, nothing else!
            """.trimIndent()

            try {
                val apiKey = progress.value.customApiKey
                val responseText = withContext(Dispatchers.IO) {
                    GeminiClient.generateText(prompt, apiKey)
                }
                
                _geminiSpeechResult.value = responseText
                
                // Save updated captions back to database to update this specific video record
                val videoList = videos.value
                val video = videoList.find { it.id == videoId }
                if (video != null) {
                    val updatedVideo = video.copy(initialCaptions = responseText)
                    repository.updateVideo(updatedVideo)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI Caption failure: ${e.message}")
            } finally {
                _isLoadingGemini.value = false
            }
        }
    }

    /**
     * AI Feature: Generates a Custom Interactive Quiz card based on any user concept input!
     */
    fun generateAiInteractiveQuiz(conceptSubject: String) {
        viewModelScope.launch {
            _isLoadingGemini.value = true
            _aiQuizState.value = null

            val prompt = """
                Generate a dynamic multiple-choice test quiz in valid JSON format.
                Topic: $conceptSubject.
                
                Required JSON Structure exactly:
                {
                  "question": "Insert a clear, educational question matching of length less than 15 words here.",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "answer": "Option B"
                }
                
                Ensure the exact correctAnswer is matching one of the options in the list. Return ONLY raw JSON, no Markdown fences or code blocks.
            """.trimIndent()

            try {
                val apiKey = progress.value.customApiKey
                val rawResponseJson = withContext(Dispatchers.IO) {
                    GeminiClient.generateText(prompt, apiKey)
                }

                val sanitized = rawResponseJson
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val json = JSONObject(sanitized)
                val question = json.optString("question", "What is vital about $conceptSubject?")
                val optionsArray = json.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (i in 0 until optionsArray.length()) {
                        options.add(optionsArray.optString(i))
                    }
                } else {
                    options.addAll(listOf("Option A", "Option B", "Option C", "Option D"))
                }
                val correctAnswer = json.optString("answer", options.firstOrNull() ?: "")

                _aiQuizState.value = AiQuizData(
                    conceptSubject = conceptSubject,
                    question = question,
                    options = options,
                    correctAnswer = correctAnswer
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI Quiz generation fail", e)
                // Fallback procedural quiz
                _aiQuizState.value = AiQuizData(
                    conceptSubject = conceptSubject,
                    question = "Which standard guideline is critical when exploring $conceptSubject?",
                    options = listOf("Continuous optimization of bandwidth", "Understanding fundamental terms", "Ignoring feedback", "Relying on old versions"),
                    correctAnswer = "Understanding fundamental terms"
                )
            } finally {
                _isLoadingGemini.value = false
            }
        }
    }

    fun clearAiQuiz() {
        _aiQuizState.value = null
    }

    fun submitAiQuizAnswer(selectedOption: String, correctAnswer: String) {
        val currentState = _aiQuizState.value ?: return
        val isCorrect = selectedOption.trim().lowercase() == correctAnswer.trim().lowercase()
        
        _aiQuizState.value = currentState.copy(
            isAnswered = true,
            selectedAnswer = selectedOption,
            isCorrect = isCorrect
        )

        if (isCorrect) {
            viewModelScope.launch {
                val currentProg = progress.value
                val nextXp = currentProg.totalXp + 50
                val nextLevel = (nextXp / 100) + 1
                repository.updateDataSaver(currentProg.dataSaverEnabled) // simple trigger
                val db = AppDatabase.getDatabase(getApplication())
                db.appDao().insertProgress(
                    currentProg.copy(
                        totalXp = nextXp,
                        level = nextLevel
                    )
                )
            }
        }
    }
}

enum class QuizStatus {
    UNANSWERED,
    CORRECT,
    WRONG
}

data class AiQuizData(
    val conceptSubject: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val isAnswered: Boolean = false,
    val selectedAnswer: String = "",
    val isCorrect: Boolean = false
)
