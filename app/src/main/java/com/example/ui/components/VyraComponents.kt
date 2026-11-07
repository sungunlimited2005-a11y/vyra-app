package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Post
import com.example.model.Story
import com.example.ui.theme.*

@Composable
fun VyraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(VyraViolet.copy(alpha = 0.4f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.85f)) {
                val w = this.size.width
                val h = this.size.height
                val stroke = 3.5f

                // Stylized V monogram
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.12f, h * 0.15f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.88f, h * 0.15f)
                    lineTo(w * 0.68f, h * 0.55f)
                    lineTo(w * 0.5f, h * 0.25f)
                    lineTo(w * 0.32f, h * 0.55f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = VyraStoryGradient
                )
            }
        }

        if (showWordmark) {
            Text(
                text = "VYRA",
                fontSize = (size.value * 0.7f).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VyraTopAppBar(
    title: @Composable () -> Unit = { VyraLogo(size = 30.dp) },
    unreadNotifications: Int = 0,
    unreadMessages: Int = 0,
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            }
        },
        actions = {
            if (actions != null) {
                actions()
            } else {
                // Notifications bell with badge
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("nav_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(containerColor = VyraMagenta) {
                                    Text(unreadNotifications.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (unreadNotifications > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Direct messages paper plane with badge
                IconButton(
                    onClick = onMessagesClick,
                    modifier = Modifier.testTag("nav_messages_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadMessages > 0) {
                                Badge(containerColor = VyraViolet) {
                                    Text(unreadMessages.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Messages",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

enum class VyraNavTab(val label: String, val testTag: String) {
    HOME("Home", "tab_home"),
    EXPLORE("Explore", "tab_explore"),
    CREATE("Create", "tab_create"),
    REELS("Reels", "tab_reels"),
    MESSAGES("Chats", "tab_messages"),
    PROFILE("Profile", "tab_profile")
}

@Composable
fun VyraBottomNavigation(
    selectedTab: VyraNavTab,
    onTabSelected: (VyraNavTab) -> Unit,
    userAvatarUrl: String? = null,
    unreadMessages: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // 1. Home
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.HOME,
            onClick = { onTabSelected(VyraNavTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == VyraNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            modifier = Modifier.testTag(VyraNavTab.HOME.testTag)
        )

        // 2. Explore
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.EXPLORE,
            onClick = { onTabSelected(VyraNavTab.EXPLORE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == VyraNavTab.EXPLORE) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Explore"
                )
            },
            label = { Text("Explore") },
            modifier = Modifier.testTag(VyraNavTab.EXPLORE.testTag)
        )

        // 3. Create (Glowing primary action)
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.CREATE,
            onClick = { onTabSelected(VyraNavTab.CREATE) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VyraButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = { Text("Create") },
            modifier = Modifier.testTag(VyraNavTab.CREATE.testTag)
        )

        // 4. Reels
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.REELS,
            onClick = { onTabSelected(VyraNavTab.REELS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == VyraNavTab.REELS) Icons.Filled.Movie else Icons.Outlined.Movie,
                    contentDescription = "Reels"
                )
            },
            label = { Text("Reels") },
            modifier = Modifier.testTag(VyraNavTab.REELS.testTag)
        )

        // 5. Messages
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.MESSAGES,
            onClick = { onTabSelected(VyraNavTab.MESSAGES) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadMessages > 0) {
                            Badge(containerColor = VyraViolet) {
                                Text(unreadMessages.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == VyraNavTab.MESSAGES) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble,
                        contentDescription = "Messages"
                    )
                }
            },
            label = { Text("Chats") },
            modifier = Modifier.testTag(VyraNavTab.MESSAGES.testTag)
        )

        // 6. Profile
        NavigationBarItem(
            selected = selectedTab == VyraNavTab.PROFILE,
            onClick = { onTabSelected(VyraNavTab.PROFILE) },
            icon = {
                if (!userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (selectedTab == VyraNavTab.PROFILE) 2.dp else 1.dp,
                                color = if (selectedTab == VyraNavTab.PROFILE) VyraViolet else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (selectedTab == VyraNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile"
                    )
                }
            },
            label = { Text("Profile") },
            modifier = Modifier.testTag(VyraNavTab.PROFILE.testTag)
        )
    }
}

@Composable
fun StoryAvatarRing(
    avatarUrl: String,
    username: String,
    isCurrentUser: Boolean = false,
    hasStory: Boolean = true,
    isCloseFriendsOnly: Boolean = false,
    isViewed: Boolean = false,
    size: Dp = 68.dp,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp)
            .testTag("story_avatar_${username.ifBlank { "me" }}")
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            val ringBrush = when {
                !hasStory -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                isCloseFriendsOnly -> VyraCloseFriendsGradient
                isViewed -> Brush.linearGradient(listOf(DarkTextMuted, DarkTextMuted))
                else -> VyraStoryGradient
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ringBrush)
                    .padding(if (hasStory) 2.5.dp else 0.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.5.dp)
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = username,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.take(1).uppercase().ifEmpty { "V" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isCurrentUser && !hasStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(VyraViolet)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isCurrentUser) "Your story" else username,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = size + 8.dp)
        )
    }
}
