package com.example.ui

import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VideoEntity
import kotlinx.coroutines.delay

@Composable
fun FeedScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val currentCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentFeedIndex.collectAsStateWithLifecycle()

    // Filter videos based on category state
    val filteredVideos = remember(videos, currentCategory) {
        if (currentCategory == "All") videos else videos.filter { it.category == currentCategory }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (filteredVideos.isEmpty()) {
                EmptyFeedState(
                    selectedCategory = currentCategory,
                    onReset = { viewModel.setCategory("All") }
                )
            } else {
                val clampedIndex = currentIndex.coerceIn(0, filteredVideos.size - 1)
                val currentVideo = filteredVideos[clampedIndex]

                // Swipe Gesture Tracker
                var dragAmountY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredVideos.size) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragAmountY < -150f && clampedIndex < filteredVideos.size - 1) {
                                        viewModel.setCurrentFeedIndex(clampedIndex + 1)
                                    } else if (dragAmountY > 150f && clampedIndex > 0) {
                                        viewModel.setCurrentFeedIndex(clampedIndex - 1)
                                    }
                                    dragAmountY = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    dragAmountY += dragAmount
                                }
                            )
                        }
                ) {
                    // Force rebuild players on index changes for robust streaming
                    key(currentVideo.id) {
                        VideoPlayerItem(
                            video = currentVideo,
                            isDataSaver = progress.dataSaverEnabled,
                            viewModel = viewModel
                        )
                    }

                    // Top Level Pill Category Selector
                    CategorySelectorRow(
                        selectedCategory = currentCategory,
                        onCategorySelected = { viewModel.setCategory(it) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )

                    // Navigation Aids (Helpful floating indicators for browser streaming environment)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(50.dp)
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (clampedIndex > 0) {
                                NavigationArrowButton(
                                    icon = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous Video",
                                    onClick = { viewModel.setCurrentFeedIndex(clampedIndex - 1) },
                                    tag = "prev_feed_button"
                                )
                            }
                            Text(
                                text = "${clampedIndex + 1}/${filteredVideos.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                            if (clampedIndex < filteredVideos.size - 1) {
                                NavigationArrowButton(
                                    icon = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next Video",
                                    onClick = { viewModel.setCurrentFeedIndex(clampedIndex + 1) },
                                    tag = "next_feed_button"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerItem(
    video: VideoEntity,
    isDataSaver: Boolean,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var videoPrepared by remember { mutableStateOf(false) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    var totalDurationSeconds by remember { mutableFloatStateOf(30f) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(video.id) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (e: Exception) {
                Log.e("FeedScreen", "Error stopping VideoView on dispose", e)
            } finally {
                videoViewRef = null
            }
        }
    }

    // Closed Captioning Scrolling State
    val lines = remember(video.initialCaptions) {
        video.initialCaptions.split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val match = Regex("\\[(\\d+):(\\d+)\\](.*)").find(line)
                if (match != null) {
                    val min = match.groupValues[1].toInt()
                    val sec = match.groupValues[2].toInt()
                    val text = match.groupValues[3].trim()
                    (min * 60 + sec) to text
                } else {
                    null
                }
            }
    }

    val activeCaption = remember(currentProgressSeconds, lines) {
        val current = currentProgressSeconds.toInt()
        lines.lastOrNull { it.first <= current }?.second ?: "Unlock future skills 🚀"
    }

    // Secondary Screen Trigger state for interactive features
    var showQuizOverlay by remember { mutableStateOf(false) }

    // Mock Video Playback Frame loop if video 준비 holds, or as absolute backup
    LaunchedEffect(isPlaying, videoPrepared) {
        if (isPlaying) {
            while (true) {
                delay(1000)
                currentProgressSeconds += 1.0f
                if (currentProgressSeconds >= totalDurationSeconds) {
                    currentProgressSeconds = 0f
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isDataSaver) {
            // Data Saver Mode: Render Static Optimized Backdrop with dynamic waveform overlay
            val gradientScheme = remember(video.category) {
                getCategoryGradientColors(video.category)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientScheme))
                    .drawBehind {
                        // Drawing futuristic network cache patterns
                        val pathBrush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                        drawRect(brush = pathBrush)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = "Data Saved Offline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "Data-Saver Active 💾",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Low bandwidth playback cached on Local Wi-Fi.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    
                    // Wave Visualizer
                    Row(
                        modifier = Modifier.height(30.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { i ->
                            val infiniteTransition = rememberInfiniteTransition(label = "$i")
                            val heightOffset by infiniteTransition.animateFloat(
                                initialValue = 5.dp.value,
                                targetValue = 28.dp.value,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400 + i * 150, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = ""
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(heightOffset.dp)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        } else {
            // Actual Streaming Player Box (Falls back to elegant presentation if codec fails or url empty)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoViewRef = this
                            if (video.videoUrl.isNotBlank()) {
                                setVideoPath(video.videoUrl)
                            }
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                // Mute to keep it friendly and non-intrusive on load
                                mp.setVolume(0.1f, 0.1f)
                                totalDurationSeconds = (mp.duration / 1000f).coerceAtLeast(10f)
                                videoPrepared = true
                                if (isPlaying) start()
                            }
                            setOnErrorListener { _, _, _ ->
                                Log.w("FeedScreen", "Native VideoView can't play this codec. Displaying high-fi fallback UI.")
                                true // handled perfectly, prevent unhandled native system popup errors or aborts
                            }
                        }
                    },
                    update = { videoView ->
                        try {
                            if (videoPrepared) {
                                if (isPlaying) {
                                    if (!videoView.isPlaying) {
                                        videoView.start()
                                    }
                                } else {
                                    if (videoView.isPlaying) {
                                        videoView.pause()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FeedScreen", "Error updating VideoView", e)
                        }
                    }
                )

                // High fidelity loader background overlay until prepared
                if (!videoPrepared) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }
        }

        // Tap on screen to Play/Pause
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isPlaying = !isPlaying }
        ) {
            // Centered Play overlay when paused
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Bottom Shadow Shader for Subtitle Legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // LEFT-HAND SIDE: Video Info details
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.8f)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp, horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = getCategoryIcon(video.category),
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = video.category,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // Creator Tag Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE280FF), Color(0xFF5F9EFE))
                            ), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.creator.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(
                        text = video.creator,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = video.creatorTag,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Title
            Text(
                text = video.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            Text(
                text = video.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            // Live SUBTITLES block near bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = "Active Subtitle",
                        tint = Color.Cyan,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = activeCaption,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // RIGHT-HAND SIDE: Interactive Controls Bar (Familiar Floating Pills)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Button
            EngagementFloatingButton(
                icon = if (video.isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                tint = if (video.isLiked) Color.Red else Color.White,
                label = video.likesCount.toString(),
                onClick = { viewModel.toggleLike(video.id) },
                tag = "like_count_btn"
            )

            // Bookmark Button
            EngagementFloatingButton(
                icon = if (video.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                tint = if (video.isBookmarked) Color.Yellow else Color.White,
                label = if (video.isBookmarked) "Saved" else "Save",
                onClick = { viewModel.toggleBookmark(video.id) },
                tag = "bookmark_count_btn"
            )

            // Auto-Caption Regenerate Spark (Powered by Gemini AI)
            val isGeminiLoading by viewModel.isLoadingGemini.collectAsStateWithLifecycle()
            Box {
                EngagementFloatingButton(
                    icon = Icons.Default.AutoAwesome,
                    tint = Color.Cyan,
                    label = "AI Captions",
                    onClick = { viewModel.generateAiAutoCaptions(video.id, video.title, video.category) },
                    tag = "auto_caption_spark_btn"
                )
                if (isGeminiLoading) {
                    CircularProgressIndicator(
                        color = Color.Cyan,
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.Center),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Interactive Quiz trigger
            val quizStatusMap by viewModel.quizStatus.collectAsStateWithLifecycle()
            val status = quizStatusMap[video.id] ?: QuizStatus.UNANSWERED
            val quizBtnTint = when (status) {
                QuizStatus.CORRECT -> Color(0xFF4CAF50)
                QuizStatus.WRONG -> Color(0xFFE91E63)
                else -> Color.White
            }
            EngagementFloatingButton(
                icon = Icons.Default.Quiz,
                tint = quizBtnTint,
                label = when (status) {
                    QuizStatus.CORRECT -> "Passed"
                    QuizStatus.WRONG -> "Retry"
                    else -> "+50 XP"
                },
                onClick = { showQuizOverlay = true },
                tag = "interactive_quiz_btn"
            )
        }

        // SLIDE-UP OVERLAY FOR INTERACTIVE QUIZZES
        if (showQuizOverlay) {
            InteractiveQuizCard(
                video = video,
                viewModel = viewModel,
                onDismiss = { showQuizOverlay = false }
            )
        }
    }
}

@Composable
fun InteractiveQuizCard(
    video: VideoEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val quizStatusMap by viewModel.quizStatus.collectAsStateWithLifecycle()
    val status = quizStatusMap[video.id] ?: QuizStatus.UNANSWERED
    var selectedOption by remember { mutableStateOf("") }
    
    val options = remember(video.quizOptionsJson) {
        video.quizOptionsJson.split(",").filter { it.isNotBlank() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .clickable { /* prevent click through */ }
                .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Bite-Sized Quiz",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Quiz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Question Text
                Text(
                    text = video.quizQuestion,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                // Render options
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    options.forEach { option ->
                        val isSelected = selectedOption == option
                        val isCorrectAns = option == video.quizAnswer
                        
                        val containerCardColor = when {
                            status == QuizStatus.CORRECT && isCorrectAns -> Color(0xFFE8F5E9)
                            status == QuizStatus.WRONG && isSelected -> Color(0xFFFFEBEE)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }

                        val borderOutlineColor = when {
                            status == QuizStatus.CORRECT && isCorrectAns -> Color(0xFF4CAF50)
                            status == QuizStatus.WRONG && isSelected -> Color(0xFFE91E63)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> Color.Transparent
                        }

                        val contentTextColor = when {
                            status == QuizStatus.CORRECT && isCorrectAns -> Color(0xFF2E7D32)
                            status == QuizStatus.WRONG && isSelected -> Color(0xFFC2185B)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.5.dp, borderOutlineColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = status == QuizStatus.UNANSWERED) { selectedOption = option }
                                .testTag("quiz_option_${option.trim()}"),
                            colors = CardDefaults.cardColors(containerColor = containerCardColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { if (status == QuizStatus.UNANSWERED) selectedOption = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    enabled = status == QuizStatus.UNANSWERED
                                )
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = contentTextColor
                                )
                            }
                        }
                    }
                }

                // Action Footer
                when (status) {
                    QuizStatus.UNANSWERED -> {
                        Button(
                            onClick = {
                                if (selectedOption.isNotBlank()) {
                                    viewModel.submitQuizAnswer(
                                        video.id,
                                        selectedOption,
                                        video.quizAnswer,
                                        video.xpReward
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_quiz_answer_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedOption.isNotBlank()
                        ) {
                            Text(
                                text = "Submit to Earn +${video.xpReward} XP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    QuizStatus.CORRECT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "Awesome Work! Correct answer verified. Earned +${video.xpReward} XP! 🎉",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    QuizStatus.WRONG -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE91E63), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63)
                                )
                                Text(
                                    text = "Incorrect Option. Double check your study notes!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2185B)
                                )
                            }
                            Button(
                                onClick = { viewModel.resetQuiz(video.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectorRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Coding & Tech", "AI Skills", "Business & Entrepreneurship", "Forex & Crypto", "University Prep & Survival", "English Proficiency")
    
    // Auto-scroll list of content pillars
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(categories.size) { index ->
            val cat = categories[index]
            val isSelected = cat == selectedCategory
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
            val borderSpec = if (isSelected) Modifier else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .then(borderSpec)
                    .background(containerColor)
                    .clickable { onCategorySelected(cat) }
                    .padding(vertical = 6.dp, horizontal = 12.dp)
                    .testTag("category_pill_${cat.replace(" ", "_")}")
            ) {
                Text(
                    text = cat,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NavigationArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun EngagementFloatingButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptyFeedState(
    selectedCategory: String,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Empty Category",
                tint = Color.LightGray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Videos Published Yet in '$selectedCategory'",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first! Use our Educator Creator Studio \ntab to draft and publish a video instantly.",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Show All Pillars", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Coding & Tech" -> Icons.Default.Code
        "AI Skills" -> Icons.Default.SmartToy
        "Business & Entrepreneurship" -> Icons.Default.TrendingUp
        "Forex & Crypto" -> Icons.Default.CurrencyExchange
        "University Prep & Survival" -> Icons.Default.School
        "English Proficiency" -> Icons.Default.Translate
        else -> Icons.Default.School
    }
}

fun getCategoryGradientColors(category: String): List<Color> {
    return when (category) {
        "Coding & Tech" -> listOf(Color(0xFF1E2640), Color(0xFF0F121F))
        "AI Skills" -> listOf(Color(0xFF321A5C), Color(0xFF130925))
        "Business & Entrepreneurship" -> listOf(Color(0xFF1A3E2F), Color(0xFF0B1B14))
        "Forex & Crypto" -> listOf(Color(0xFF4C3E1B), Color(0xFF221A08))
        "University Prep & Survival" -> listOf(Color(0xFF3C1F1F), Color(0xFF1D0F0F))
        "English Proficiency" -> listOf(Color(0xFF1F3D4C), Color(0xFF0F1E26))
        else -> listOf(Color(0xFF212529), Color(0xFF000000))
    }
}
