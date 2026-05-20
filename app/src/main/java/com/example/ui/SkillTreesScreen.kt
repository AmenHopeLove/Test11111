package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreesScreen(
    viewModel: MainViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val aiQuizState by viewModel.aiQuizState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isLoadingGemini.collectAsStateWithLifecycle()

    var customConceptInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Learning Path",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // PROGRESS DASHBOARD HEADER
            ProgressDashboardHeader(
                level = progress.level,
                totalXp = progress.totalXp
            )

            // DYNAMIC DEDICATED GEMINI AI TUTOR QUIZ SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .testTag("ai_tutor_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.cardContainerColor()
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE280FF).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFE280FF)
                            )
                        }
                        Column {
                            Text(
                                text = "Gemini AI Smart Tutor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Test your knowledge on any topic to earn +50 XP",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (aiQuizState == null) {
                        OutlinedTextField(
                            value = customConceptInput,
                            onValueChange = { customConceptInput = it },
                            placeholder = { Text("e.g. Python functions, Forex indices, Swahili verbs...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_tutor_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (isAiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (customConceptInput.isNotBlank()) {
                                    viewModel.generateAiInteractiveQuiz(customConceptInput)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_ai_quiz_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = customConceptInput.isNotBlank() && !isAiLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Generate Custom Quiz ✨", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // RENDER GENERATED GEMINI QUIZ CARD
                        AiGeneratedQuizInteraction(
                            quiz = aiQuizState!!,
                            viewModel = viewModel,
                            onClear = {
                                customConceptInput = ""
                                viewModel.clearAiQuiz()
                            }
                        )
                    }
                }
            }

            // PILLAR SKILL TREES
            Text(
                text = "Pillar Skill Trees",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            val pillarsList = listOf(
                PillarData("Coding & Tech", Icons.Default.Code, "Novice syntax, Algorithms, Mobile engineering", Color(0xFF5F9EFE)),
                PillarData("AI Skills", Icons.Default.SmartToy, "System instructions, LLM tuning, Python builders", Color(0xFFE280FF)),
                PillarData("Business & Entrepreneurship", Icons.Default.TrendingUp, "MVP market fits, Swahili localization, Capital growth", Color(0xFF4CAF50)),
                PillarData("Forex & Crypto", Icons.Default.CurrencyExchange, "Compound psychology, Stop loss ratios, Anti-scam metrics", Color(0xFFFFC107)),
                PillarData("University Prep & Survival", Icons.Default.School, "Calculus parts, Engineering biology, Study hacks", Color(0xFFF44336)),
                PillarData("English Proficiency", Icons.Default.Translate, "Foreign pitches, Professional grammar, Interview structures", Color(0xFF00BCD4))
            )

            pillarsList.forEach { pillar ->
                PillarSkillTreeRow(
                    pillar = pillar,
                    onStartLearning = {
                        viewModel.setCategory(pillar.name)
                        onNavigateToFeed()
                    }
                )
            }
        }
    }
}

@Composable
fun AiGeneratedQuizInteraction(
    quiz: AiQuizData,
    viewModel: MainViewModel,
    onClear: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Topic: ${quiz.conceptSubject}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Close Quiz",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onClear() }
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
        }

        Text(
            text = quiz.question,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        quiz.options.forEach { option ->
            val isSelected = quiz.selectedAnswer == option
            val isCorrectOption = option == quiz.correctAnswer
            
            val containerColor = when {
                quiz.isAnswered && isCorrectOption -> Color(0xFFE8F5E9)
                quiz.isAnswered && isSelected && !quiz.isCorrect -> Color(0xFFFFEBEE)
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }

            val borderColor = when {
                quiz.isAnswered && isCorrectOption -> Color(0xFF4CAF50)
                quiz.isAnswered && isSelected && !quiz.isCorrect -> Color(0xFFE91E63)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable(enabled = !quiz.isAnswered) {
                        viewModel.submitAiQuizAnswer(option, quiz.correctAnswer)
                    }
                    .testTag("ai_quiz_option_${option.trim()}"),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (quiz.isAnswered) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (quiz.isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (quiz.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (quiz.isCorrect) Color(0xFF4CAF50) else Color(0xFFE91E63)
                )
                Text(
                    text = if (quiz.isCorrect) "Brilliant! Verified +50 XP and saved!" else "Almost! Try another concept to unlock your level up progress.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (quiz.isCorrect) Color(0xFF2E7D32) else Color(0xFFC2185B)
                )
            }
        }
    }
}

@Composable
fun ProgressDashboardHeader(
    level: Int,
    totalXp: Int
) {
    // Calculus for dynamic XP bar: e.g. each level needs 100 XP
    val currentLevelXp = totalXp % 100
    val progressPercent = currentLevelXp / 100f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Progress Level",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Level $level Learner",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Level badge circular visual
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L$level",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentLevelXp / 100 XP to L${level + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Lifetime: $totalXp XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardStatItem(
                    label = "Unlocks",
                    value = "${(totalXp / 50).coerceIn(0, 16)} Certificates",
                    icon = Icons.Default.WorkspacePremium
                )
                DashboardStatItem(
                    label = "Active Streak",
                    value = "3 Days 🔥",
                    icon = Icons.Default.LocalFireDepartment
                )
            }
        }
    }
}

@Composable
fun DashboardStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PillarSkillTreeRow(
    pillar: PillarData,
    onStartLearning: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(pillar.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pillar.icon,
                            contentDescription = null,
                            tint = pillar.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = pillar.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = pillar.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand Branch",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Skill trees branches Nodes (Tree representation)
                    SkillTreeNodeItem(
                        nodeTitle = "Level 1 Node: Novice Basics",
                        nodeDescription = "Learn active terminologies and fundamental definitions.",
                        isCompleted = true,
                        badgeColor = pillar.color
                    )

                    SkillTreeNodeItem(
                        nodeTitle = "Level 2 Node: Compound Core",
                        nodeDescription = "Apply the concepts to small development and market scenarios.",
                        isCompleted = false,
                        badgeColor = pillar.color
                    )

                    SkillTreeNodeItem(
                        nodeTitle = "Level 3 Node: Master Thesis",
                        nodeDescription = "Secure advanced professional certificates and pitch outputs.",
                        isCompleted = false,
                        badgeColor = pillar.color
                    )

                    Button(
                        onClick = onStartLearning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pillar.color),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Watch Micro-Lessons 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillTreeNodeItem(
    nodeTitle: String,
    nodeDescription: String,
    isCompleted: Boolean,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
                .background(
                    if (isCompleted) badgeColor else Color.Transparent,
                    CircleShape
                )
                .border(2.dp, badgeColor, CircleShape)
        )
        Column {
            Text(
                text = nodeTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = nodeDescription,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class PillarData(
    val name: String,
    val icon: ImageVector,
    val subtitle: String,
    val color: Color
)

@Composable
fun ColorScheme.cardContainerColor(): Color {
    return surfaceVariant.copy(alpha = 0.3f)
}
