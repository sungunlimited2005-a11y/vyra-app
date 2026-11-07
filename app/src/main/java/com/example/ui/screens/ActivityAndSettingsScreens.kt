package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    currentUserId: String,
    repository: VyraRepository,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val notifications by repository.observeNotifications().collectAsState(initial = emptyList())
    val followRequests by repository.observeFollowRequests().collectAsState(initial = emptyList())

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Activity & Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("notifications_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pending Follow Requests (if private account)
            if (followRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Follow Requests (${followRequests.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = VyraMagenta
                    )
                }

                items(followRequests, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToProfile(req.requesterUserId) }
                            ) {
                                AsyncImage(
                                    model = req.requesterAvatarUrl,
                                    contentDescription = req.requesterUsername,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(req.requesterDisplayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("@${req.requesterUsername}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch { repository.respondToFollowRequest(req, accept = true) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VyraViolet),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Confirm", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch { repository.respondToFollowRequest(req, accept = false) }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            if (notifications.isEmpty() && followRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No notifications yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("When someone likes, comments, or follows you, you'll see it here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { repository.markNotificationRead(notif.id) }
                                if (notif.type == "follow") onNavigateToProfile(notif.actorId)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = notif.actorAvatarUrl,
                            contentDescription = notif.actorUsername,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (notif.createdAt != null) {
                                Text(
                                    text = formatTimeAgo(notif.createdAt.toDate()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!notif.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VyraMagenta)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: () -> Unit
) {
    val userPosts by repository.observeUserPosts(currentUserId).collectAsState(initial = emptyList())

    val totalViews = remember(userPosts) { userPosts.sumOf { it.viewsCount } }
    val totalLikes = remember(userPosts) { userPosts.sumOf { it.likesCount } }
    val totalComments = remember(userPosts) { userPosts.sumOf { it.commentsCount } }
    val totalReels = remember(userPosts) { userPosts.count { it.isReel } }
    val totalPosts = remember(userPosts) { userPosts.count { !it.isReel } }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Creator Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Analytics Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Impressions & Reach",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VyraViolet
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalViews",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time content views across all posts and reels",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Likes",
                    value = "$totalLikes",
                    icon = Icons.Filled.Favorite,
                    tint = VyraMagenta,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Comments",
                    value = "$totalComments",
                    icon = Icons.Filled.ChatBubble,
                    tint = VyraViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Followers",
                    value = "${currentUser?.followersCount ?: 0}",
                    icon = Icons.Filled.People,
                    tint = VyraCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Published Content",
                    value = "${userPosts.size} ($totalPosts posts, $totalReels reels)",
                    icon = Icons.Filled.Movie,
                    tint = VyraGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Top Performing Posts
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Content Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (userPosts.isEmpty()) {
                Text(
                    text = "Publish your first post or reel to track performance.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                userPosts.take(5).forEach { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (post.caption.isNotBlank()) post.caption else if (post.isReel) "Reel Video" else "Photo Post",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("❤️ ${post.likesCount}", fontSize = 11.sp, color = VyraMagenta)
                                    Text("💬 ${post.commentsCount}", fontSize = 11.sp, color = VyraViolet)
                                    Text("👁 ${post.viewsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: () -> Unit,
    onSignOutComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isPrivate by remember { mutableStateOf(currentUser?.isPrivate ?: false) }
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())
    val closeFriends by repository.observeCloseFriends().collectAsState(initial = emptyList())

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Account Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VyraViolet)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Private Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isPrivate) "Only approved followers can view your content." else "Anyone can view your public posts and stories.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { checked ->
                                isPrivate = checked
                                scope.launch {
                                    if (currentUser != null) {
                                        repository.saveUserProfile(currentUser.copy(isPrivate = checked))
                                    }
                                }
                            },
                            modifier = Modifier.testTag("settings_privacy_switch")
                        )
                    }
                }
            }

            // Close Friends & Blocked Info
            Text("Audience & Safety", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VyraViolet)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = VyraGreen)
                            Text("Close Friends List", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text("${closeFriends.size} users", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Blocked Accounts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text("${blockedUsers.size} blocked", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // About VYRA
            Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VyraViolet)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VYRA V1 Social Network", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version 1.0 • Built with Jetpack Compose & Cloud Firestore", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out Button
            Button(
                onClick = {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_sign_out"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
