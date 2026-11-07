package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.SavedPost
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ProfileTab {
    POSTS, REELS, SAVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileUserId: String,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCreatorStudio: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onSelectPost: (Post) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOwnProfile = profileUserId == currentUserId

    var userProfile by remember { mutableStateOf<UserProfile?>(if (isOwnProfile) currentUser else null) }
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val userPosts by repository.observeUserPosts(profileUserId).collectAsState(initial = emptyList())
    val savedPosts by repository.observeSavedPosts().collectAsState(initial = emptyList())
    val isFollowing by repository.isFollowing(profileUserId).collectAsState(initial = false)
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())
    val isBlocked = profileUserId in blockedUsers

    LaunchedEffect(profileUserId) {
        if (!isOwnProfile) {
            repository.observeUserProfile(profileUserId).collect { p ->
                userProfile = p
            }
        }
    }

    if (onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    Text(
                        text = userProfile?.username?.let { "@$it" } ?: "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = onNavigateToCreatorStudio) {
                            Icon(Icons.Filled.Insights, contentDescription = "Creator Studio", tint = VyraMagenta)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    } else {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Big Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, VyraViolet, CircleShape)
                    ) {
                        if (!userProfile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile?.avatarUrl,
                                contentDescription = userProfile?.username,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VyraViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile?.username?.take(1)?.uppercase() ?: "V",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Stats: Posts, Followers, Following
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(count = userPosts.size.toLong(), label = "Posts")
                        StatItem(count = userProfile?.followersCount ?: 0L, label = "Followers")
                        StatItem(count = userProfile?.followingCount ?: 0L, label = "Following")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display Name & Username
                Text(
                    text = userProfile?.displayName ?: "Creator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (userProfile?.isPrivate == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Private", tint = VyraMagenta, modifier = Modifier.size(14.dp))
                        Text("Private Account", fontSize = 12.sp, color = VyraMagenta, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Bio
                if (!userProfile?.bio.isNullOrBlank()) {
                    Text(
                        text = userProfile?.bio ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // Website Link
                if (!userProfile?.websiteUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(userProfile?.websiteUrl))
                                    context.startActivity(intent)
                                } catch (ignored: Exception) {}
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = VyraCyan, modifier = Modifier.size(13.dp))
                        Text(
                            text = userProfile?.websiteUrl ?: "",
                            fontSize = 13.sp,
                            color = VyraCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Edit Profile or Follow / Message
                if (isOwnProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_edit_profile"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Edit Profile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToCreatorStudio,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_creator_studio"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Creator Studio", color = VyraMagenta, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (isFollowing) {
                                        repository.unfollowUser(profileUserId)
                                    } else {
                                        if (userProfile != null) {
                                            repository.followUser(userProfile!!)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_follow_user"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else VyraViolet
                            )
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (userProfile != null) {
                                    scope.launch {
                                        val convId = repository.getOrCreateDirectConversation(userProfile!!)
                                        onNavigateToChat(convId)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_message_user"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile Tabs: Posts, Reels, Saved
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = VyraViolet
            ) {
                Tab(
                    selected = selectedTab == ProfileTab.POSTS,
                    onClick = { selectedTab = ProfileTab.POSTS },
                    icon = { Icon(Icons.Filled.GridOn, contentDescription = "Posts") }
                )
                Tab(
                    selected = selectedTab == ProfileTab.REELS,
                    onClick = { selectedTab = ProfileTab.REELS },
                    icon = { Icon(Icons.Filled.Movie, contentDescription = "Reels") }
                )
                if (isOwnProfile) {
                    Tab(
                        selected = selectedTab == ProfileTab.SAVED,
                        onClick = { selectedTab = ProfileTab.SAVED },
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved") }
                    )
                }
            }

            // Grid Content
            when (selectedTab) {
                ProfileTab.POSTS -> {
                    val postsList = userPosts.filter { !it.isReel }
                    if (postsList.isEmpty()) {
                        EmptyTabBox("No photo posts yet.")
                    } else {
                        PostGrid(posts = postsList, onSelectPost = onSelectPost)
                    }
                }
                ProfileTab.REELS -> {
                    val reelsList = userPosts.filter { it.isReel }
                    if (reelsList.isEmpty()) {
                        EmptyTabBox("No reels published yet.")
                    } else {
                        PostGrid(posts = reelsList, onSelectPost = onSelectPost)
                    }
                }
                ProfileTab.SAVED -> {
                    if (savedPosts.isEmpty()) {
                        EmptyTabBox("No saved posts. Bookmark posts from feed to see them here.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("saved_posts_grid"),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(savedPosts, key = { it.id }) { saved ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = saved.mediaUrl,
                                        contentDescription = saved.caption,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog && userProfile != null) {
        EditProfileModal(
            currentProfile = userProfile!!,
            repository = repository,
            onDismiss = { showEditProfileDialog = false },
            onProfileUpdated = { updated ->
                userProfile = updated
                showEditProfileDialog = false
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Profile Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.toggleBlockUser(profileUserId, isBlocked) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBlocked) "Unblock User" else "Block User", color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.reportContent("user", profileUserId, "Inappropriate profile", "") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Report User", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StatItem(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PostGrid(posts: List<Post>, onSelectPost: (Post) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_posts_grid"),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onSelectPost(post) }
            ) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (post.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTabBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModal(
    currentProfile: UserProfile,
    repository: VyraRepository,
    onDismiss: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(currentProfile.displayName) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var websiteUrl by remember { mutableStateOf(currentProfile.websiteUrl) }
    var selectedAvatar by remember { mutableStateOf(currentProfile.avatarUrl) }
    var isPrivate by remember { mutableStateOf(currentProfile.isPrivate) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar choices
                Text("Change Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESET_AVATARS.take(4).forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selectedAvatar == url) 2.dp else 0.dp,
                                    color = if (selectedAvatar == url) VyraViolet else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = url },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Private Account")
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val updated = currentProfile.copy(
                            displayName = displayName.trim(),
                            bio = bio.trim(),
                            websiteUrl = websiteUrl.trim(),
                            avatarUrl = selectedAvatar,
                            isPrivate = isPrivate
                        )
                        repository.saveUserProfile(updated)
                        isSaving = false
                        onProfileUpdated(updated)
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
