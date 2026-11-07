package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReelsScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onSharePost: (Post) -> Unit
) {
    val reels by repository.observeReels().collectAsState(initial = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())

    val visibleReels = remember(reels, blockedUsers) {
        reels.filter { it.userId !in blockedUsers }
    }

    if (visibleReels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(VyraButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Reels yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Be the first to publish a high-energy short video or reel on VYRA!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Reel")
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { visibleReels.size })

        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("reels_vertical_pager")
        ) { page ->
            val reel = visibleReels[page]
            SingleReelPage(
                reel = reel,
                currentUserId = currentUserId,
                currentUser = currentUser,
                repository = repository,
                onNavigateToProfile = { onNavigateToProfile(reel.userId) },
                onSharePost = { onSharePost(reel) }
            )
        }
    }
}

@Composable
fun SingleReelPage(
    reel: Post,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateToProfile: () -> Unit,
    onSharePost: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(reel.likesCount) }
    var isSaved by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(reel.id) {
        repository.isPostLiked(reel.id, currentUserId).collect { liked ->
            isLiked = liked
        }
    }

    LaunchedEffect(reel.id) {
        repository.isPostSaved(reel.id).collect { saved ->
            isSaved = saved
        }
    }

    // Increment view count
    LaunchedEffect(reel.id) {
        repository.incrementViews(reel.id)
    }

    // Rotating vinyl animation for music disc
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPlaying = !isPlaying
                showPlayPauseIndicator = true
            }
    ) {
        // Reel Media (Full-bleed)
        AsyncImage(
            model = reel.mediaUrl,
            contentDescription = reel.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay at top and bottom for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Play/Pause center indicator
        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            LaunchedEffect(showPlayPauseIndicator) {
                delay(600)
                showPlayPauseIndicator = false
            }
        }

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reels",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = Color.White
            )
        }

        // Right Action Column (Like, Comment, Share, Save, Music)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val newLiked = !isLiked
                        isLiked = newLiked
                        likesCount += if (newLiked) 1 else -1
                        scope.launch { repository.toggleLikePost(reel, !newLiked) }
                    },
                    modifier = Modifier.testTag("reel_like_button_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (isLiked) VyraMagenta else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "$likesCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment
            if (reel.allowComments) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.testTag("reel_comment_button_${reel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment Reel",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "${reel.commentsCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Share
            IconButton(
                onClick = onSharePost,
                modifier = Modifier.testTag("reel_share_button_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share Reel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Save
            IconButton(
                onClick = {
                    val newSaved = !isSaved
                    isSaved = newSaved
                    scope.launch {
                        if (newSaved) {
                            repository.savePost(reel)
                        } else {
                            repository.unsavePost(reel.id)
                        }
                    }
                },
                modifier = Modifier.testTag("reel_save_button_${reel.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = if (isSaved) VyraViolet else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Vinyl Record Disc
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .rotate(if (isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(VyraMagenta)
                )
            }
        }

        // Bottom Creator Info & Caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            // Creator row with Follow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable(onClick = onNavigateToProfile)
            ) {
                AsyncImage(
                    model = reel.userAvatarUrl,
                    contentDescription = reel.username,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = "@${reel.username}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            if (reel.caption.isNotBlank()) {
                Text(
                    text = reel.caption,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sound indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Original Audio • ${reel.username}",
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }

    if (showCommentsSheet) {
        CommentsBottomSheet(
            post = reel,
            currentUserId = currentUserId,
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showCommentsSheet = false }
        )
    }
}
