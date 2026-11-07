package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.VyraTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed interface VyraDestination {
    data class MainTab(val tab: VyraNavTab) : VyraDestination
    data class UserProfileView(val userId: String) : VyraDestination
    data object Notifications : VyraDestination
    data object Settings : VyraDestination
    data object CreatorStudio : VyraDestination
    data class ChatThread(val conversationId: String) : VyraDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VyraTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedApp(currentUserId = uid)
        }
    }
}

@Composable
fun AuthenticatedApp(currentUserId: String) {
    val context = LocalContext.current
    val repository = remember(currentUserId) { VyraRepository(context) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        repository.observeUserProfile(currentUserId).collect { p ->
            userProfile = p
            isProfileLoaded = true
        }
    }

    if (!isProfileLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            VyraLogo(size = 48.dp)
        }
    } else if (userProfile == null || userProfile?.username.isNullOrBlank()) {
        ProfileSetupScreen(
            currentUserId = currentUserId,
            repository = repository,
            onSetupComplete = {
                // profile saved; observeUserProfile listener will automatically pick it up
            }
        )
    } else {
        VyraMainScaffold(
            currentUserId = currentUserId,
            currentUser = userProfile!!,
            repository = repository
        )
    }
}

@Composable
fun VyraMainScaffold(
    currentUserId: String,
    currentUser: UserProfile,
    repository: VyraRepository
) {
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf<VyraDestination>(VyraDestination.MainTab(VyraNavTab.HOME)) }

    val notifications by repository.observeNotifications().collectAsState(initial = emptyList())
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    val conversations by repository.observeUserConversations().collectAsState(initial = emptyList())
    val unreadMessagesCount = 0

    Scaffold(
        topBar = {
            when (val dest = currentDestination) {
                is VyraDestination.MainTab -> {
                    if (dest.tab != VyraNavTab.REELS && dest.tab != VyraNavTab.MESSAGES) {
                        VyraTopAppBar(
                            unreadNotifications = unreadNotificationsCount,
                            unreadMessages = unreadMessagesCount,
                            onNotificationsClick = {
                                currentDestination = VyraDestination.Notifications
                            },
                            onMessagesClick = {
                                currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                            }
                        )
                    }
                }
                else -> {
                    // Secondary screens define their own custom top app bar
                }
            }
        },
        bottomBar = {
            if (currentDestination is VyraDestination.MainTab &&
                (currentDestination as VyraDestination.MainTab).tab != VyraNavTab.REELS
            ) {
                VyraBottomNavigation(
                    selectedTab = (currentDestination as VyraDestination.MainTab).tab,
                    onTabSelected = { tab ->
                        currentDestination = VyraDestination.MainTab(tab)
                    },
                    userAvatarUrl = currentUser.avatarUrl,
                    unreadMessages = unreadMessagesCount
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val dest = currentDestination) {
                is VyraDestination.MainTab -> {
                    when (dest.tab) {
                        VyraNavTab.HOME -> {
                            FeedScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onNavigateToCreate = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.CREATE)
                                },
                                onNavigateToMessages = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                },
                                onSharePostToChat = { post ->
                                    scope.launch {
                                        val convs = conversations
                                        if (convs.isNotEmpty()) {
                                            repository.sendMessage(
                                                conversationId = convs.first().id,
                                                text = "Check out this post on VYRA: ${post.caption}",
                                                mediaUrl = post.mediaUrl,
                                                sharedPostId = post.id
                                            )
                                            currentDestination = VyraDestination.ChatThread(convs.first().id)
                                        } else {
                                            currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                        }
                                    }
                                }
                            )
                        }
                        VyraNavTab.EXPLORE -> {
                            ExploreScreen(
                                currentUserId = currentUserId,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onSelectPost = { post ->
                                    currentDestination = VyraDestination.UserProfileView(post.userId)
                                }
                            )
                        }
                        VyraNavTab.CREATE -> {
                            CreateScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onPublishSuccess = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                                }
                            )
                        }
                        VyraNavTab.REELS -> {
                            ReelsScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToProfile = { targetUid ->
                                    currentDestination = VyraDestination.UserProfileView(targetUid)
                                },
                                onNavigateToCreate = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.CREATE)
                                },
                                onSharePost = { post ->
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                                }
                            )
                        }
                        VyraNavTab.MESSAGES -> {
                            MessagesScreen(
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateBack = {
                                    currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                                }
                            )
                        }
                        VyraNavTab.PROFILE -> {
                            ProfileScreen(
                                profileUserId = currentUserId,
                                currentUserId = currentUserId,
                                currentUser = currentUser,
                                repository = repository,
                                onNavigateToSettings = {
                                    currentDestination = VyraDestination.Settings
                                },
                                onNavigateToCreatorStudio = {
                                    currentDestination = VyraDestination.CreatorStudio
                                }
                            )
                        }
                    }
                }

                is VyraDestination.UserProfileView -> {
                    ProfileScreen(
                        profileUserId = dest.userId,
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        },
                        onNavigateToChat = { convId ->
                            currentDestination = VyraDestination.ChatThread(convId)
                        }
                    )
                }

                is VyraDestination.Notifications -> {
                    NotificationsScreen(
                        currentUserId = currentUserId,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.HOME)
                        },
                        onNavigateToProfile = { targetUid ->
                            currentDestination = VyraDestination.UserProfileView(targetUid)
                        }
                    )
                }

                is VyraDestination.Settings -> {
                    SettingsScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        },
                        onSignOutComplete = {
                            // Automatically triggers authStateListener and navigates to AuthScreen
                        }
                    )
                }

                is VyraDestination.CreatorStudio -> {
                    CreatorStudioScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.PROFILE)
                        }
                    )
                }

                is VyraDestination.ChatThread -> {
                    MessagesScreen(
                        currentUserId = currentUserId,
                        currentUser = currentUser,
                        repository = repository,
                        initialConversationId = dest.conversationId,
                        onNavigateBack = {
                            currentDestination = VyraDestination.MainTab(VyraNavTab.MESSAGES)
                        }
                    )
                }
            }
        }
    }
}
