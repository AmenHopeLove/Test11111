package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.CreatorScreen
import com.example.ui.FeedScreen
import com.example.ui.MainViewModel
import com.example.ui.ProfileScreen
import com.example.ui.SkillTreesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigationContainer(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: "feed"

    // Define main bottom navbar options
    val items = listOf(
        NavBarItem(
            route = "feed",
            title = "For You",
            selectedIcon = Icons.Filled.PlayCircle,
            unselectedIcon = Icons.Outlined.PlayCircle,
            tag = "nav_feed"
        ),
        NavBarItem(
            route = "skills",
            title = "Skill Path",
            selectedIcon = Icons.Filled.Hub,
            unselectedIcon = Icons.Outlined.Hub,
            tag = "nav_skills"
        ),
        NavBarItem(
            route = "creator",
            title = "Creator",
            selectedIcon = Icons.Filled.AddBox,
            unselectedIcon = Icons.Outlined.AddBox,
            tag = "nav_creator"
        ),
        NavBarItem(
            route = "profile",
            title = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            tag = "nav_profile"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                items.forEach { item ->
                    val isSelected = currentDestination == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentDestination != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("feed") {
                FeedScreen(viewModel = viewModel)
            }
            composable("skills") {
                SkillTreesScreen(
                    viewModel = viewModel,
                    onNavigateToFeed = {
                        navController.navigate("feed") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("creator") {
                CreatorScreen(
                    viewModel = viewModel,
                    onNavigateToFeed = {
                        navController.navigate("feed") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavBarItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)
