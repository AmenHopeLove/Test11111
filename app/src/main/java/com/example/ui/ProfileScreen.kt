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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Key overrides
    var keyInput by remember(progress.customApiKey) { mutableStateOf(progress.customApiKey) }
    var adsEnabled by remember { mutableStateOf(true) }
    var premiumSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Black) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // USER ID HEADER CARD WITH XP
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE280FF), Color(0xFF5F9EFE))
                                ), CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Youth Learner 🇿🇦",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Joined May 2026 • Level ${progress.level} Scholar",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(vertical = 2.dp, horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.Gold(), modifier = Modifier.size(12.dp))
                            Text("Active Certificate Path", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // DATA-SAVER OPTIMIZER SECTION (CRITICAL FOR AFRICA'S BROADBAND)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkWifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Data-Saver Optimization 💾", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Compresses vertical stream & caches on Wi-Fi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = progress.dataSaverEnabled,
                            onCheckedChange = { viewModel.updateDataSaver(it) },
                            modifier = Modifier.testTag("data_saver_switch")
                        )
                    }
                }
            }

            // GEMINI AI DEVELOPER PORTAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(0xFFE280FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Gemini AI API Key Config",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Adding a custom key from the AI Studio Secrets panel enables real-time dynamic captioning & custom AI tests.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("AI Studio API Key...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.updateApiKey(keyInput) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_api_key_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Dev Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // VERIFIED DIGITAL LINKEDIN CERTIFICATES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text("Verified Microlearning Certificates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Earn certifications by solving module quizzes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (progress.totalXp >= 150) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                    ), RoundedCornerShape(12.dp)
                                )
                                .border(1.5.dp, Color.Gold(), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.Gold(), modifier = Modifier.size(36.dp))
                                Text(
                                    text = "SKILLBITE ACADEMY CERTIFICATE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.LightGray,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Youth Learner",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Has satisfied all microlearning course metrics, accumulating ${progress.totalXp} XP across Coding, AI, and Business pillars.",
                                    fontSize = 10.sp,
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Verified Blockchain Hash: 0x9f32e...44a",
                                    fontSize = 8.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Cyan
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text(
                                text = "Path Locked: Complete More Quizzes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Earn at least 150 Total XP to claim your verified credential. Just solve 3 video quizzes in the For You Feed! Current balance: ${progress.totalXp}/150 XP.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // MONETIZATION MARKETPLACE MODEL PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Monetization & Commercialization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "SkillBite fosters a self-sustaining ecosystem giving creators brand sponsorships and learners ad-free premium download capabilities.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Ads control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Skippable Feed Ads (Revenue Stream)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = { adsEnabled = it }
                        )
                    }

                    // Premium checkout block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6E48AA), Color(0xFF9D50BB))
                                ), RoundedCornerShape(12.dp)
                            )
                            .clickable { premiumSuccessDialog = true }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Premium Ad-Free Plus 👑", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("\$0.99 / mo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                "Unlock unlimited offline caching, verified certificate examinations, and premium deep-dive lectures.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Text(
                                "Tap here to simulatedly subscribe now",
                                color = Color.Yellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Success subscription dialogue
    if (premiumSuccessDialog) {
        AlertDialog(
            onDismissRequest = { premiumSuccessDialog = false },
            title = { Text("Subscription Successful! 🎉") },
            text = { Text("Congratulations! You are now an Ad-Free Premium member. Enjoy unlimited caches of tech, business, and Forex video units across Africa.") },
            confirmButton = {
                Button(onClick = { premiumSuccessDialog = false }) {
                    Text("Awesome")
                }
            }
        )
    }
}

fun Color.Companion.Gold(): Color {
    return Color(0xFFFFD700)
}
