package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val CURATED_MEDIA_PRESETS = listOf(
    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
    "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
    "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800",
    "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800",
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
    "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=800"
)

enum class CreateMode {
    POST, REEL, STORY
}

@Composable
fun CreateScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onPublishSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(CreateMode.POST) }

    var selectedMediaUrl by remember { mutableStateOf(CURATED_MEDIA_PRESETS[0]) }
    var customUrlInput by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var allowComments by remember { mutableStateOf(true) }
    var isCloseFriendsOnly by remember { mutableStateOf(false) }

    var isPublishing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for compliant media selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUrl = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("create_content_screen")
    ) {
        Text(
            text = "Create Content",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Selector: Post, Reel, Story
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreateMode.values().forEach { m ->
                val isSelected = mode == m
                FilterChip(
                    selected = isSelected,
                    onClick = { mode = m },
                    label = {
                        Text(
                            text = when (m) {
                                CreateMode.POST -> "Photo Post"
                                CreateMode.REEL -> "Vertical Reel"
                                CreateMode.STORY -> "24h Story"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (m) {
                            CreateMode.POST -> VyraViolet
                            CreateMode.REEL -> VyraMagenta
                            CreateMode.STORY -> VyraCyan
                        },
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (mode == CreateMode.REEL || mode == CreateMode.STORY) 9f / 13f else 1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = selectedMediaUrl,
                    contentDescription = "Selected Media Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mode.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pick from device button
                FilledTonalButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .testTag("pick_device_media_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device Gallery", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Curated Presets
        Text("Choose from Aesthetic Presets", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CURATED_MEDIA_PRESETS) { preset ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selectedMediaUrl == preset) 2.5.dp else 1.dp,
                            color = if (selectedMediaUrl == preset) VyraViolet else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedMediaUrl = preset }
                ) {
                    AsyncImage(
                        model = preset,
                        contentDescription = "Preset thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom URL Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customUrlInput,
                onValueChange = { customUrlInput = it },
                placeholder = { Text("Or paste image / video URL") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_custom_media_url"),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (customUrlInput.isNotBlank()) {
                        selectedMediaUrl = customUrlInput.trim()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Caption Input
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text(if (mode == CreateMode.STORY) "Story Caption (Optional)" else "Write a caption...") },
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_caption"),
            shape = RoundedCornerShape(12.dp)
        )

        // Hashtag quick inserts
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val suggestions = listOf("#vyra", "#creators", "#photography", "#cinematic", "#reels", "#sunset", "#vibe")
            items(suggestions) { tag ->
                SuggestionChip(
                    onClick = {
                        if (!caption.contains(tag)) {
                            caption = if (caption.isBlank()) tag else "$caption $tag"
                        }
                    },
                    label = { Text(tag, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Feature Toggles
        if (mode == CreateMode.STORY) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = VyraGreen)
                    Column {
                        Text("Close Friends Only", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Only users on your close friends list can view", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isCloseFriendsOnly,
                    onCheckedChange = { isCloseFriendsOnly = it },
                    modifier = Modifier.testTag("switch_close_friends")
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = VyraViolet)
                    Column {
                        Text("Private (Followers Only)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Only approved followers can see this in their feed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    modifier = Modifier.testTag("switch_post_privacy")
                )
            }

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
                    Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = VyraMagenta)
                    Column {
                        Text("Allow Comments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Let viewers discuss and comment on this post", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = allowComments,
                    onCheckedChange = { allowComments = it },
                    modifier = Modifier.testTag("switch_allow_comments")
                )
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Publish Button
        Button(
            onClick = {
                if (selectedMediaUrl.isBlank()) {
                    statusMessage = "Please select or enter media."
                    return@Button
                }

                isPublishing = true
                statusMessage = null

                val extractedHashtags = caption.split(" ", "\n")
                    .filter { it.startsWith("#") && it.length > 1 }
                    .map { it.removePrefix("#").lowercase() }

                scope.launch {
                    try {
                        if (mode == CreateMode.STORY) {
                            repository.publishStory(
                                mediaUrl = selectedMediaUrl,
                                mediaType = "image",
                                caption = caption,
                                isCloseFriendsOnly = isCloseFriendsOnly
                            )
                        } else {
                            repository.createPost(
                                mediaUrl = selectedMediaUrl,
                                mediaType = if (mode == CreateMode.REEL) "video" else "image",
                                caption = caption,
                                hashtags = extractedHashtags,
                                isReel = mode == CreateMode.REEL,
                                isPrivate = isPrivate,
                                allowComments = allowComments
                            )
                        }
                        isPublishing = false
                        onPublishSuccess()
                    } catch (e: Exception) {
                        isPublishing = false
                        statusMessage = e.localizedMessage ?: "Failed to publish."
                    }
                }
            },
            enabled = !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("button_publish_content"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (mode) {
                    CreateMode.POST -> VyraViolet
                    CreateMode.REEL -> VyraMagenta
                    CreateMode.STORY -> VyraCyan
                }
            )
        ) {
            if (isPublishing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = when (mode) {
                        CreateMode.POST -> "Share to Feed"
                        CreateMode.REEL -> "Publish Reel"
                        CreateMode.STORY -> "Add to Story"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
