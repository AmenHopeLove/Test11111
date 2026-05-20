package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: MainViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Publish Video, 1: Analytics Dashboard

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Educator Studio", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tab Controls
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Publish Lesson", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("publish_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Analytics Dash", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("analytics_tab")
                )
            }

            // Tab Content Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    PublishLessonForm(viewModel, onNavigateToFeed)
                } else {
                    AnalyticsDashboardPanel()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishLessonForm(
    viewModel: MainViewModel,
    onNavigateToFeed: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("Teach Africa") }
    var description by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    
    // Category dropdown trigger
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Coding & Tech", "AI Skills", "Business & Entrepreneurship", "Forex & Crypto", "University Prep & Survival", "English Proficiency")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    // Quiz subform
    var quizQuestion by remember { mutableStateOf("") }
    var quizOptionsInput by remember { mutableStateOf("Option A,Option B,Option C,Option D") }
    var quizCorrectAnswer by remember { mutableStateOf("Option A") }

    // AI Caption Generator helper
    var showAiCaptionAssistant by remember { mutableStateOf(false) }
    var aiCaptionLanguage by remember { mutableStateOf("English / Swahili mix") }
    val isAiLoading by viewModel.isLoadingGemini.collectAsStateWithLifecycle()
    val aiSpeechResult by viewModel.geminiSpeechResult.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Create Learning Video",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        CreatorOutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Video Title") },
            placeholder = { Text("e.g. Master Binary Trees in 60s") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Dropdown menu for Content Pillars
        ExposedDropdownMenuBox(
            expanded = categoryDropdownExpanded,
            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Educational Vertical (Category)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }
        }

        CreatorOutlinedTextField(
            value = creator,
            onValueChange = { creator = it },
            label = { Text("Your Educator Tag / Pen Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        CreatorOutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Short Module Description") },
            placeholder = { Text("Enter the core concept explained in the video...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        CreatorOutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            label = { Text("Streaming Video URL (Optional)") },
            placeholder = { Text("https://url_to_video.mp4") },
            helperText = { Text("Leave blank for high-fidelity fallback presentation stream.") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // INTERACTIVE QUIZ OVERLAY CREATOR SUB-FORM
        Text(
            text = "Interactive Lesson Quiz",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        CreatorOutlinedTextField(
            value = quizQuestion,
            onValueChange = { quizQuestion = it },
            label = { Text("Quiz Question") },
            placeholder = { Text("e.g. Which big-O runtime does an array search take?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        CreatorOutlinedTextField(
            value = quizOptionsInput,
            onValueChange = { quizOptionsInput = it },
            label = { Text("Comma-separated Options (Exactly 4)") },
            placeholder = { Text("O(1),O(n),O(log n),O(n log n)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        CreatorOutlinedTextField(
            value = quizCorrectAnswer,
            onValueChange = { quizCorrectAnswer = it },
            label = { Text("Exact Correct Answer Match") },
            placeholder = { Text("O(n)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // NLP AUTO_CAPTION TRANSCRIBER TOOLS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Auto-Caption Editor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Generate and style localized Swahili/Yoruba/English captions in sync before going live.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showAiCaptionAssistant) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CreatorOutlinedTextField(
                            value = aiCaptionLanguage,
                            onValueChange = { aiCaptionLanguage = it },
                            label = { Text("Primary NLP Language") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.generateAiAutoCaptions("preview", title, selectedCategory)
                            },
                            enabled = title.isNotBlank() && !isAiLoading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Spark Subtitles", fontSize = 11.sp)
                        }
                    }

                    if (isAiLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else if (aiSpeechResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = aiSpeechResult!!,
                                color = Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                maxLines = 4
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAiCaptionAssistant = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Caption Studio", fontSize = 12.sp)
                    }
                }
            }
        }

        // PUBLISH FORM ACTION
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.createVideo(
                        title = title,
                        creator = creator,
                        category = selectedCategory,
                        videoUrl = videoUrl,
                        description = description,
                        question = quizQuestion,
                        options = quizOptionsInput,
                        answer = quizCorrectAnswer
                    )
                    viewModel.setCategory(selectedCategory)
                    onNavigateToFeed()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_video_action_btn"),
            shape = RoundedCornerShape(14.dp),
            enabled = title.isNotBlank() && creator.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish Micro-Lesson to Feed 🚀", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
fun AnalyticsDashboardPanel() {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Video Performance Dashboard",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // General numbers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsValueCard(
                label = "Total Learners",
                value = "1.8K",
                trend = "+14% Swipes",
                modifier = Modifier.weight(1f)
            )
            AnalyticsValueCard(
                label = "Avg. Completion",
                value = "42.8s",
                trend = "+8% Focus",
                modifier = Modifier.weight(1f)
            )
        }

        // CANVAS AUDIENCE RETENTION GRAPH
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Audience Retention Curve (15s to 60s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "African learners maintain peak retention in the first 25 seconds of microlearning.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Draw Custom Retention Chart using Slate Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                ) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw grid lines
                    val gridPaintBrush = Color.White.copy(alpha = 0.1f)
                    repeat(4) { idx ->
                        val yOffset = height * (idx + 1) / 5f
                        drawLine(
                            color = gridPaintBrush,
                            start = Offset(0f, yOffset),
                            end = Offset(width, yOffset),
                            strokeWidth = 1f
                        )
                    }

                    // Curve path drawing
                    val curvePath = Path().apply {
                        moveTo(0f, height * 0.1f) // Start at 100% at time 0
                        quadraticTo(
                            width * 0.35f, height * 0.2f, // 85% at 20s
                            width * 0.6f, height * 0.5f  // Drop-off starts 50% at 40s
                        )
                        quadraticTo(
                            width * 0.85f, height * 0.75f, // 30% at 50s
                            width, height * 0.8f          // Flatten at 25% near 60s
                        )
                    }

                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE280FF), Color(0xFF5F9EFE))
                        ),
                        style = Stroke(width = 6f)
                    )

                    // Draw dots on high pivots
                    drawCircle(
                        color = Color(0xFFE280FF),
                        radius = 8f,
                        center = Offset(width * 0.35f, height * 0.22f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0s (100%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("30s (78%)", fontSize = 10.sp, color = Color(0xFFE280FF), fontWeight = FontWeight.Bold)
                    Text("60s (25%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // DEMOGRAPHIC METRO CHANNELS (Nairobi, Lagos, Addis, etc.)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Metro Viewers Distribution",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                DemographicMetroItem(city = "Lagos, Nigeria 🇳🇬", percent = 0.45f, views = "840 views", color = Color(0xFFE91E63))
                DemographicMetroItem(city = "Nairobi, Kenya 🇰🇪", percent = 0.30f, views = "560 views", color = Color(0xFF3F51B5))
                DemographicMetroItem(city = "Addis Ababa, Ethiopia 🇪🇹", percent = 0.15f, views = "280 views", color = Color(0xFF4CAF50))
                DemographicMetroItem(city = "Kigali, Rwanda 🇷🇼", percent = 0.10f, views = "180 views", color = Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun AnalyticsValueCard(
    label: String,
    value: String,
    trend: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(trend, fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DemographicMetroItem(
    city: String,
    percent: Float,
    views: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(city, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("$views (${(percent * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    }
}

@Composable
fun CreatorOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    helperText: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            minLines = minLines,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth(),
            shape = shape
        )
        if (helperText != null) {
            Box(modifier = Modifier.padding(start = 4.dp)) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) {
                        helperText()
                    }
                }
            }
        }
    }
}
