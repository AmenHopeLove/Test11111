package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AppRepository(private val appDao: AppDao) {

    val allVideos: Flow<List<VideoEntity>> = appDao.getAllVideos()
    val userProgress: Flow<UserProgressEntity> = appDao.getUserProgress()
        .map { it ?: UserProgressEntity() }

    fun getVideosByCategory(category: String): Flow<List<VideoEntity>> =
        appDao.getVideosByCategory(category)

    suspend fun updateVideo(video: VideoEntity) {
        appDao.updateVideo(video)
    }

    suspend fun toggleLike(videoId: String) {
        val videos = allVideos.firstOrNull() ?: return
        val video = videos.find { it.id == videoId } ?: return
        val updated = video.copy(
            isLiked = !video.isLiked,
            likesCount = if (video.isLiked) video.likesCount - 1 else video.likesCount + 1
        )
        appDao.updateVideo(updated)
    }

    suspend fun toggleBookmark(videoId: String) {
        val progress = userProgress.firstOrNull() ?: UserProgressEntity()
        val bookmarkedIds = progress.bookmarkedVideoIds.split(",")
            .filter { it.isNotBlank() }
            .toMutableList()

        if (bookmarkedIds.contains(videoId)) {
            bookmarkedIds.remove(videoId)
        } else {
            bookmarkedIds.add(videoId)
        }

        val updatedProgress = progress.copy(
            bookmarkedVideoIds = bookmarkedIds.joinToString(",")
        )
        appDao.insertProgress(updatedProgress)

        // Sync with video entity
        val videos = allVideos.firstOrNull() ?: return
        val video = videos.find { it.id == videoId } ?: return
        val updatedVideo = video.copy(isBookmarked = !video.isBookmarked)
        appDao.updateVideo(updatedVideo)
    }

    suspend fun awardXp(videoId: String, xp: Int) {
        val progress = userProgress.firstOrNull() ?: UserProgressEntity()
        
        // Don't award XP again if quiz is already completed for this video
        val completedQuizIds = progress.completedQuizIds.split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
            
        if (completedQuizIds.contains(videoId)) return
        
        completedQuizIds.add(videoId)

        val nextXp = progress.totalXp + xp
        // Level logic: 100 XP per level
        val nextLevel = (nextXp / 100) + 1

        val updatedProgress = progress.copy(
            totalXp = nextXp,
            level = nextLevel,
            completedQuizIds = completedQuizIds.joinToString(",")
        )
        appDao.insertProgress(updatedProgress)
    }

    suspend fun updateDataSaver(enabled: Boolean) {
        val progress = userProgress.firstOrNull() ?: UserProgressEntity()
        appDao.insertProgress(progress.copy(dataSaverEnabled = enabled))
    }

    suspend fun updateApiKey(key: String) {
        val progress = userProgress.firstOrNull() ?: UserProgressEntity()
        appDao.insertProgress(progress.copy(customApiKey = key))
    }

    suspend fun initializeOfflineContent() {
        // Seeding database if empty
        val existing = allVideos.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val seedVideos = listOf(
                VideoEntity(
                    id = "coding_kotlin",
                    title = "Mastering Variables in Kotlin",
                    creator = "Abdi Dev",
                    creatorTag = "@abdi_codes",
                    category = "Coding & Tech",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    description = "Supercharge your Kotlin coding! Understand variables (val vs var) and scopes for cleaner African mobile development.",
                    likesCount = 245,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "What keyword is used to declare a read-only local variable in Kotlin?",
                    quizOptionsJson = "var,val,let,const",
                    quizAnswer = "val",
                    xpReward = 50,
                    initialCaptions = "[00:00] Hi developers! Let's talk Kotlin!\n[00:05] Today we're mastering variables.\n[00:10] val makes things immutable!\n[00:15] var allows dynamic reassignment.\n[00:20] Use val by default for pristine thread safety!"
                ),
                VideoEntity(
                    id = "ai_prompting",
                    title = "AI Prompt Engineering Basics",
                    creator = "Nneka AI",
                    creatorTag = "@nneka_ai",
                    category = "AI Skills",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    description = "Learn the high-value art of instruction tuning. Master temperature and role definition to get solid JSON from Gemini AI.",
                    likesCount = 389,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "Which model parameter is used to control randomness or creativity?",
                    quizOptionsJson = "TopP,TopK,Temperature,SystemInstruction",
                    quizAnswer = "Temperature",
                    xpReward = 50,
                    initialCaptions = "[00:00] Welcome to AI Prompt Engineering!\n[00:05] Level up your system instruction weight.\n[00:10] Define a clear persona for Gemini first.\n[00:15] Then supply an accurate schema format.\n[00:20] Now you can build world-class AI agents!"
                ),
                VideoEntity(
                    id = "business_startup",
                    title = "African Startup Market Validation",
                    creator = "Musa Ventures",
                    creatorTag = "@musa_ventures",
                    category = "Business & Entrepreneurship",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    description = "Scaling a tech startup in Lagos, Kigali, or Nairobi? validate demand with localized MVPs and Swahili localization.",
                    likesCount = 182,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "What does MVP stand for in professional startup frameworks?",
                    quizOptionsJson = "Most Valuable Peer,Minimum Viable Product,Maximum Venture Profit,Market Value Price",
                    quizAnswer = "Minimum Viable Product",
                    xpReward = 50,
                    initialCaptions = "[00:00] Scaling startups in Africa!\n[00:05] Validate demand fast. Build an MVP first.\n[00:10] Ask Swap partners or Swahili readers.\n[00:15] Iterate based on true numeric engagement.\n[00:20] Let's build profitable micro economies!"
                ),
                VideoEntity(
                    id = "forex_crypto",
                    title = "Trading Psychology & Capital Risk",
                    creator = "Chidi FX",
                    creatorTag = "@chidi_fx",
                    category = "Forex & Crypto",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    description = "Avoid financial scams and cut losses early. Secure a strict 1:2 risk-to-reward ratio to keep your trading capital safe.",
                    likesCount = 512,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "What is the industry-recommended maximum capital risk per single trade?",
                    quizOptionsJson = "1% to 2%,5% to 10%,15% to 20%,50%",
                    quizAnswer = "1% to 2%",
                    xpReward = 50,
                    initialCaptions = "[00:00] FX and trading checklist!\n[00:04] Trading psychology is 90% of your career.\n[00:08] Never seek instant overnight gains.\n[00:12] Keep max risk per trade below 2 percent.\n[00:16] Cut losses fast. Preserve your emotional energy!"
                ),
                VideoEntity(
                    id = "univ_prep",
                    title = "Engineering Calculus: Integration Rule",
                    creator = "Prof. Joseph",
                    creatorTag = "@prof_joseph",
                    category = "University Prep & Survival",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                    description = "Crush university math! Learn the LIATE priority rule to easily find parts in integration calculations.",
                    likesCount = 304,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "What prioritization rule determines the 'u' vector in Integration by Parts?",
                    quizOptionsJson = "PEMDAS,LIATE,FOIL,SOHCAHTOA",
                    quizAnswer = "LIATE",
                    xpReward = 50,
                    initialCaptions = "[00:01] Confused about complex integrals?\n[00:05] Remember the LIATE rule for calculus!\n[00:10] Logarithmic, Inverse trig, Algebraic...\n[00:15] Trigonometric, Exponential. This defines 'u'.\n[00:20] Score an easy A in engineering math!"
                ),
                VideoEntity(
                    id = "english_prof",
                    title = "Professional English for Job Pitches",
                    creator = "Loveness Communications",
                    creatorTag = "@loveness_pr",
                    category = "English Proficiency",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    description = "Grammar hacks and communication tactics to successfully negotiate with international remote tech employers.",
                    likesCount = 422,
                    isLiked = false,
                    isBookmarked = false,
                    quizQuestion = "Which word is most professional when politely asking about a client status update?",
                    quizOptionsJson = "Bug,Pester,Inquire,Ping",
                    quizAnswer = "Inquire",
                    xpReward = 50,
                    initialCaptions = "[00:00] Ace your remote interview!\n[00:04] Use 'Inquire' instead of 'bug' or 'pester'.\n[00:08] Direct structures increase credibility.\n[00:12] Pitch your technical qualifications clearly.\n[00:16] Speak with authority and secure the role!"
                )
            )
            appDao.insertVideos(seedVideos)
        }

        // Seeding user progress
        val progress = appDao.getUserProgress().firstOrNull()
        if (progress == null) {
            appDao.insertProgress(UserProgressEntity(id = 1))
        }
    }
}
