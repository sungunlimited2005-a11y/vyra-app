package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.*
import com.example.ui.components.StoryAvatarRing
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onSharePostToChat: (Post) -> Unit
) {
    val scope = rememberCoroutineScope()
    val posts by repository.observeFeedPosts().collectAsState(initial = emptyList())
    val stories by repository.observeActiveStories().collectAsState(initial = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())

    // Active story viewer state
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    // Comments sheet state
    var activeCommentPost by remember { mutableStateOf<Post?>(null) }
    // Report dialog state
    var postToReport by remember { mutableStateOf<Post?>(null) }

    val visiblePosts = remember(posts, blockedUsers) {
        posts.filter { it.userId !in blockedUsers }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("feed_list"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- Stories Tray ---
            item {
                StoriesTray(
                    currentUserId = currentUserId,
                    currentUser = currentUser,
                    stories = stories.filter { it.userId !in blockedUsers },
                    onStoryClick = { story ->
                        selectedStory = story
                        scope.launch { repository.markStoryViewed(story.id) }
                    },
                    onAddStoryClick = onNavigateToCreate
                )
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }

            // --- Feed Posts ---
            if (visiblePosts.isEmpty()) {
                item {
                    EmptyFeedCard(onNavigateToCreate = onNavigateToCreate)
                }
            } else {
                items(visiblePosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        currentUserId = currentUserId,
                        repository = repository,
                        onProfileClick = { onNavigateToProfile(post.userId) },
                        onCommentClick = { activeCommentPost = post },
                        onShareClick = { onSharePostToChat(post) },
                        onReportClick = { postToReport = post },
                        onDeleteClick = {
                            scope.launch { repository.deletePost(post.id) }
                        }
                    )
                }
            }
        }
    }

    // --- Story Viewer Modal ---
    if (selectedStory != null) {
        StoryViewerModal(
            story = selectedStory!!,
            currentUserId = currentUserId,
            onDismiss = { selectedStory = null },
            onDeleteStory = {
                scope.launch {
                    repository.deleteStory(selectedStory!!.id)
                    selectedStory = null
                }
            }
        )
    }

    // --- Comments Bottom Sheet ---
    if (activeCommentPost != null) {
        CommentsBottomSheet(
            post = activeCommentPost!!,
            currentUserId = currentUserId,
            currentUser = currentUser,
            repository = repository,
            onDismiss = { activeCommentPost = null }
        )
    }

    // --- Report Dialog ---
    if (postToReport != null) {
        ReportDialog(
            targetId = postToReport!!.id,
            targetType = "post",
            repository = repository,
            onDismiss = { postToReport = null }
        )
    }
}

@Composable
fun StoriesTray(
    currentUserId: String,
    currentUser: UserProfile?,
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit
) {
    val userStory = stories.firstOrNull { it.userId == currentUserId }
    val otherStories = stories.filter { it.userId != currentUserId }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("stories_tray"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Current User Story Avatar
        item {
            StoryAvatarRing(
                avatarUrl = currentUser?.avatarUrl ?: "",
                username = currentUser?.username ?: "You",
                isCurrentUser = true,
                hasStory = userStory != null,
                onClick = {
                    if (userStory != null) {
                        onStoryClick(userStory)
                    } else {
                        onAddStoryClick()
                    }
                }
            )
        }

        // Other Users Stories
        items(otherStories, key = { it.id }) { story ->
            StoryAvatarRing(
                avatarUrl = story.userAvatarUrl,
                username = story.username,
                isCurrentUser = false,
                hasStory = true,
                isCloseFriendsOnly = story.isCloseFriendsOnly,
                onClick = { onStoryClick(story) }
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    repository: VyraRepository,
    onProfileClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(post.likesCount) }
    var isSaved by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(post.id) {
        repository.isPostLiked(post.id, currentUserId).collect { liked ->
            isLiked = liked
        }
    }

    LaunchedEffect(post.id) {
        repository.isPostSaved(post.id).collect { saved ->
            isSaved = saved
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Avatar, Username, Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clickable(onClick = onProfileClick)
                        .testTag("post_author_${post.id}")
                ) {
                    if (post.userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.userAvatarUrl,
                            contentDescription = post.username,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(VyraViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.username.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = post.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.createdAt != null) {
                            Text(
                                text = formatTimeAgo(post.createdAt.toDate()),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("post_menu_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Post Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Remove from Saved" else "Save Post") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    if (isSaved) {
                                        repository.unsavePost(post.id)
                                    } else {
                                        repository.savePost(post)
                                    }
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Share to Chat") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            }
                        )

                        if (post.userId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text(if (post.isPinned) "Unpin from Profile" else "Pin to Profile") },
                                leadingIcon = {
                                    Icon(Icons.Filled.PushPin, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        repository.togglePinPost(post.id, post.isPinned)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onReportClick()
                                }
                            )
                        }
                    }
                }
            }

            // Post Media (with double-tap like gesture)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Double tap to like simulation
                        if (!isLiked) {
                            isLiked = true
                            likesCount += 1
                            scope.launch { repository.toggleLikePost(post, false) }
                        }
                        showHeartAnimation = true
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Reel badge
                if (post.isReel) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = "Reel",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Reel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Heart Burst Animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartAnimation,
                    enter = scaleIn(initialScale = 0.3f) + fadeIn(),
                    exit = scaleOut(targetScale = 1.3f) + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Liked",
                        tint = VyraMagenta,
                        modifier = Modifier.size(96.dp)
                    )
                    LaunchedEffect(showHeartAnimation) {
                        delay(650)
                        showHeartAnimation = false
                    }
                }
            }

            // Action Buttons: Like, Comment, Share, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Like Button
                    IconButton(
                        onClick = {
                            val newLiked = !isLiked
                            isLiked = newLiked
                            likesCount += if (newLiked) 1 else -1
                            scope.launch { repository.toggleLikePost(post, !newLiked) }
                        },
                        modifier = Modifier.testTag("like_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) VyraMagenta else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Comment Button
                    if (post.allowComments) {
                        IconButton(
                            onClick = onCommentClick,
                            modifier = Modifier.testTag("comment_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Share Button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("share_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Save Bookmark Button
                IconButton(
                    onClick = {
                        val newSaved = !isSaved
                        isSaved = newSaved
                        scope.launch {
                            if (newSaved) {
                                repository.savePost(post)
                            } else {
                                repository.unsavePost(post.id)
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) VyraViolet else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Likes Count
            if (likesCount > 0) {
                Text(
                    text = "$likesCount ${if (likesCount == 1L) "like" else "likes"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            // Caption
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = "${post.username} ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = post.caption,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                Text(
                    text = post.hashtags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" },
                    fontSize = 13.sp,
                    color = VyraViolet,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }

            // View Comments button
            if (post.allowComments && post.commentsCount > 0) {
                Text(
                    text = "View all ${post.commentsCount} comments",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyFeedCard(onNavigateToCreate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VyraButtonGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to your VYRA feed",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Be the first creator to share a photo, publish a story, or create a vertical reel!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onNavigateToCreate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Post")
            }
        }
    }
}

@Composable
fun StoryViewerModal(
    story: Story,
    currentUserId: String,
    onDismiss: () -> Unit,
    onDeleteStory: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(story.id) {
        val duration = 5000L // 5 seconds per story
        val step = 50L
        val increment = step.toFloat() / duration
        while (progress < 1f) {
            delay(step)
            progress += increment
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Fullscreen story image
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = story.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top overlay: Progress bar & User info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = story.userAvatarUrl,
                            contentDescription = story.username,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = story.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (story.isCloseFriendsOnly) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VyraGreen)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Close Friends", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row {
                        if (story.userId == currentUserId) {
                            IconButton(onClick = onDeleteStory) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom Caption & Viewers count
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (story.userId == currentUserId) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "👁 ${story.viewersCount} viewers",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val comments by repository.observeComments(post.id).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = "Comments (${comments.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 350.dp)
                    .padding(vertical = 8.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Start the conversation!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = comment.userAvatarUrl,
                                    contentDescription = comment.username,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(
                                        text = comment.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = comment.text,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (comment.userId == currentUserId || post.userId == currentUserId) {
                                IconButton(
                                    onClick = {
                                        scope.launch { repository.deleteComment(post.id, comment.id) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete comment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Comment input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            isSubmitting = true
                            scope.launch {
                                repository.addComment(
                                    postId = post.id,
                                    postOwnerId = post.userId,
                                    text = commentText
                                )
                                commentText = ""
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = commentText.isNotBlank() && !isSubmitting,
                    modifier = Modifier.testTag("comment_post_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment",
                        tint = if (commentText.isNotBlank()) VyraViolet else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    targetId: String,
    targetType: String,
    repository: VyraRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val reasons = listOf("Spam or misleading", "Harassment or hate speech", "Violence or harmful", "Nudity or inappropriate", "Other")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report ${targetType.replaceFirstChar { it.uppercase() }}") },
        text = {
            if (isSubmitted) {
                Text("Thank you. Your report has been submitted to VYRA moderation.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Why are you reporting this?", fontSize = 14.sp)
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(reason, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onDismiss) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            repository.reportContent(targetType, targetId, selectedReason, details)
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Submit Report")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

fun formatTimeAgo(date: Date): String {
    val diff = System.currentTimeMillis() - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
