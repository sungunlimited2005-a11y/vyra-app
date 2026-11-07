package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val websiteUrl: String = "",
    val avatarUrl: String = "",
    val isPrivate: Boolean = false,
    val themePreference: String = "dark",
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" or "video"
    val isReel: Boolean = false,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val isPrivate: Boolean = false,
    val allowComments: Boolean = true,
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = "",
    val isCloseFriendsOnly: Boolean = false,
    val viewersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val name: String = "",
    val avatarUrl: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val sharedPostId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorAvatarUrl: String = "",
    val type: String = "like", // like, comment, follow, follow_request, message
    val targetId: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = "",
    val authorUsername: String = "",
    val isReel: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRequest(
    @DocumentId val id: String = "",
    val requesterUserId: String = "",
    val targetUserId: String = "",
    val requesterUsername: String = "",
    val requesterDisplayName: String = "",
    val requesterAvatarUrl: String = "",
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // user, post, comment, message
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null
)
