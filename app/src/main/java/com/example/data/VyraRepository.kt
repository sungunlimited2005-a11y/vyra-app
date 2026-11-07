package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VyraRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing this action.")
    }

    // --- User Profiles ---

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) doc.toObject(UserProfile::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "username" to profile.username.trim().lowercase(),
            "displayName" to profile.displayName.trim(),
            "email" to (auth.currentUser?.email ?: profile.email),
            "bio" to profile.bio,
            "websiteUrl" to profile.websiteUrl,
            "avatarUrl" to profile.avatarUrl,
            "isPrivate" to profile.isPrivate,
            "themePreference" to profile.themePreference,
            "postsCount" to profile.postsCount,
            "followersCount" to profile.followersCount,
            "followingCount" to profile.followingCount,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (profile.createdAt == null) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            val userEmail = auth.currentUser?.email ?: profile.email
            reserveUsername(profile.username.trim().lowercase(), uid, userEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getEmailForUsername(username: String): String? {
        val clean = username.trim().lowercase().removePrefix("@")
        val path = "usernames/$clean"
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (doc.exists() && !doc.getString("email").isNullOrBlank()) {
                doc.getString("email")
            } else {
                // Fallback: check users collection by username
                val userDocs = db.collection("users")
                    .whereEqualTo("username", clean)
                    .limit(1)
                    .get()
                    .await()
                if (!userDocs.isEmpty) {
                    userDocs.documents[0].getString("email")
                } else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        val clean = username.trim().lowercase().removePrefix("@")
        if (clean.length < 3) return false
        val path = "usernames/$clean"
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (!doc.exists()) return true
            val claimedBy = doc.getString("userId")
            claimedBy == currentUserId()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun reserveUsername(username: String, userId: String, email: String) {
        val clean = username.trim().lowercase().removePrefix("@")
        val docRef = db.collection("usernames").document(clean)
        val payload = mapOf(
            "username" to clean,
            "userId" to userId,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(UserProfile::class.java) }

    // --- Posts & Reels ---

    suspend fun createPost(
        mediaUrl: String,
        mediaType: String,
        caption: String,
        hashtags: List<String>,
        isReel: Boolean,
        isPrivate: Boolean,
        allowComments: Boolean
    ): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val docRef = db.collection("posts").document()
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "isReel" to isReel,
            "caption" to caption,
            "hashtags" to hashtags,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "viewsCount" to 0L,
            "isPrivate" to isPrivate,
            "allowComments" to allowComments,
            "isPinned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // increment user posts count
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(1)).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeFeedPosts(): Flow<List<Post>> =
        db.collection("posts")
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeUserPosts(userId: String): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeReels(): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("isReel", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun toggleLikePost(post: Post, isLiked: Boolean) {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
        val postRef = db.collection("posts").document(post.id)
        try {
            if (isLiked) {
                // remove like
                likeRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                // add like
                val payload = mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(payload).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()

                if (post.userId != uid) {
                    val sender = getUserProfile(uid)
                    createNotification(
                        recipientUserId = post.userId,
                        type = "like",
                        targetId = post.id,
                        message = "${sender?.username ?: "Someone"} liked your post"
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            throw e
        }
    }

    fun isPostLiked(postId: String, userId: String): Flow<Boolean> =
        db.collection("posts").document(postId).collection("likes").document(userId)
            .snapshots()
            .map { it.exists() }

    suspend fun deletePost(postId: String) {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            postRef.delete().await()
            db.collection("users").document(uid).update("postsCount", FieldValue.increment(-1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            throw e
        }
    }

    suspend fun incrementViews(postId: String) {
        try {
            db.collection("posts").document(postId).update("viewsCount", FieldValue.increment(1)).await()
        } catch (ignored: Exception) {}
    }

    suspend fun togglePinPost(postId: String, isPinned: Boolean) {
        try {
            db.collection("posts").document(postId).update("isPinned", !isPinned).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
        }
    }

    // --- Comments ---

    suspend fun addComment(postId: String, postOwnerId: String, text: String) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val commentsRef = db.collection("posts").document(postId).collection("comments").document()
        val payload = mapOf(
            "postId" to postId,
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "text" to text.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            commentsRef.set(payload).await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(1)).await()
            if (postOwnerId != uid) {
                createNotification(
                    recipientUserId = postOwnerId,
                    type = "comment",
                    targetId = postId,
                    message = "${user?.username ?: "Someone"} commented: \"${text.take(40)}\""
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentsRef.path)
            throw e
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> =
        db.collection("posts").document(postId).collection("comments")
            .snapshots()
            .map { snap ->
                snap.toObjects(Comment::class.java)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun deleteComment(postId: String, commentId: String) {
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        try {
            commentRef.delete().await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(-1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            throw e
        }
    }

    // --- Stories ---

    suspend fun publishStory(mediaUrl: String, mediaType: String, caption: String, isCloseFriendsOnly: Boolean): String {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val storyRef = db.collection("stories").document()
        val now = Timestamp.now()
        val expiresAt = Timestamp(now.seconds + 86400, now.nanoseconds) // 24 hours
        val payload = mapOf(
            "userId" to uid,
            "username" to (user?.username ?: "user"),
            "userAvatarUrl" to (user?.avatarUrl ?: ""),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "isCloseFriendsOnly" to isCloseFriendsOnly,
            "viewersCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )
        try {
            storyRef.set(payload).await()
            return storyRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, storyRef.path)
            throw e
        }
    }

    fun observeActiveStories(): Flow<List<Story>> =
        db.collection("stories")
            .snapshots()
            .map { snap ->
                val nowSeconds = Timestamp.now().seconds
                snap.toObjects(Story::class.java)
                    .filter { (it.expiresAt?.seconds ?: Long.MAX_VALUE) > nowSeconds }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun markStoryViewed(storyId: String) {
        val uid = requireUserId()
        val viewerRef = db.collection("stories").document(storyId).collection("viewers").document(uid)
        try {
            val exists = viewerRef.get().await().exists()
            if (!exists) {
                viewerRef.set(mapOf("viewerUserId" to uid, "viewedAt" to FieldValue.serverTimestamp())).await()
                db.collection("stories").document(storyId).update("viewersCount", FieldValue.increment(1)).await()
            }
        } catch (ignored: Exception) {}
    }

    suspend fun deleteStory(storyId: String) {
        val storyRef = db.collection("stories").document(storyId)
        try {
            storyRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, storyRef.path)
        }
    }

    // --- Saved Content ---

    suspend fun savePost(post: Post) {
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid).collection("saved").document(post.id)
        val payload = mapOf(
            "userId" to uid,
            "postId" to post.id,
            "mediaUrl" to post.mediaUrl,
            "mediaType" to post.mediaType,
            "caption" to post.caption,
            "authorUsername" to post.username,
            "isReel" to post.isReel,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            savedRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, savedRef.path)
            throw e
        }
    }

    suspend fun unsavePost(postId: String) {
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid).collection("saved").document(postId)
        try {
            savedRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, savedRef.path)
            throw e
        }
    }

    fun isPostSaved(postId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        emit(db.collection("users").document(uid).collection("saved").document(postId).get().await().exists())
    }

    fun observeSavedPosts(): Flow<List<SavedPost>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("saved")
            .snapshots()
            .map { snap ->
                snap.toObjects(SavedPost::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    // --- Follows & Follow Requests ---

    suspend fun followUser(targetUser: UserProfile) {
        val uid = requireUserId()
        val currentUser = getUserProfile(uid)

        if (targetUser.isPrivate) {
            // Send follow request
            val reqRef = db.collection("users").document(targetUser.userId).collection("follow_requests").document(uid)
            val payload = mapOf(
                "requesterUserId" to uid,
                "targetUserId" to targetUser.userId,
                "requesterUsername" to (currentUser?.username ?: "user"),
                "requesterDisplayName" to (currentUser?.displayName ?: "User"),
                "requesterAvatarUrl" to (currentUser?.avatarUrl ?: ""),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            reqRef.set(payload).await()
            createNotification(
                recipientUserId = targetUser.userId,
                type = "follow_request",
                targetId = uid,
                message = "${currentUser?.username ?: "Someone"} requested to follow you"
            )
        } else {
            // Direct follow
            val followerDoc = db.collection("users").document(targetUser.userId).collection("followers").document(uid)
            val followingDoc = db.collection("users").document(uid).collection("following").document(targetUser.userId)

            val followerPayload = mapOf(
                "userId" to uid,
                "username" to (currentUser?.username ?: "user"),
                "displayName" to (currentUser?.displayName ?: "User"),
                "avatarUrl" to (currentUser?.avatarUrl ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )
            val followingPayload = mapOf(
                "userId" to targetUser.userId,
                "username" to targetUser.username,
                "displayName" to targetUser.displayName,
                "avatarUrl" to targetUser.avatarUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )

            followerDoc.set(followerPayload).await()
            followingDoc.set(followingPayload).await()

            db.collection("users").document(targetUser.userId).update("followersCount", FieldValue.increment(1)).await()
            db.collection("users").document(uid).update("followingCount", FieldValue.increment(1)).await()

            createNotification(
                recipientUserId = targetUser.userId,
                type = "follow",
                targetId = uid,
                message = "${currentUser?.username ?: "Someone"} started following you"
            )
        }
    }

    suspend fun unfollowUser(targetUserId: String) {
        val uid = requireUserId()
        val followerDoc = db.collection("users").document(targetUserId).collection("followers").document(uid)
        val followingDoc = db.collection("users").document(uid).collection("following").document(targetUserId)

        try {
            followerDoc.delete().await()
            followingDoc.delete().await()
            db.collection("users").document(targetUserId).update("followersCount", FieldValue.increment(-1)).await()
            db.collection("users").document(uid).update("followingCount", FieldValue.increment(-1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followerDoc.path)
        }
    }

    fun isFollowing(targetUserId: String): Flow<Boolean> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("following").document(targetUserId)
            .snapshots()
            .map { it.exists() }
    }

    suspend fun respondToFollowRequest(requester: FollowRequest, accept: Boolean) {
        val uid = requireUserId()
        val reqDoc = db.collection("users").document(uid).collection("follow_requests").document(requester.requesterUserId)

        if (accept) {
            val targetUser = getUserProfile(uid)
            val followerDoc = db.collection("users").document(uid).collection("followers").document(requester.requesterUserId)
            val followingDoc = db.collection("users").document(requester.requesterUserId).collection("following").document(uid)

            val followerPayload = mapOf(
                "userId" to requester.requesterUserId,
                "username" to requester.requesterUsername,
                "displayName" to requester.requesterDisplayName,
                "avatarUrl" to requester.requesterAvatarUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val followingPayload = mapOf(
                "userId" to uid,
                "username" to (targetUser?.username ?: "user"),
                "displayName" to (targetUser?.displayName ?: "User"),
                "avatarUrl" to (targetUser?.avatarUrl ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )

            followerDoc.set(followerPayload).await()
            followingDoc.set(followingPayload).await()

            db.collection("users").document(uid).update("followersCount", FieldValue.increment(1)).await()
            db.collection("users").document(requester.requesterUserId).update("followingCount", FieldValue.increment(1)).await()

            createNotification(
                recipientUserId = requester.requesterUserId,
                type = "follow",
                targetId = uid,
                message = "${targetUser?.username ?: "User"} accepted your follow request"
            )
        }

        reqDoc.delete().await()
    }

    fun observeFollowRequests(): Flow<List<FollowRequest>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("follow_requests")
            .snapshots()
            .map { snap -> snap.toObjects(FollowRequest::class.java) }
    }

    // --- Close Friends & Blocked Users ---

    suspend fun toggleCloseFriend(friendUserId: String, isFriend: Boolean) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("close_friends").document(friendUserId)
        if (isFriend) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to friendUserId, "createdAt" to FieldValue.serverTimestamp())).await()
        }
    }

    fun observeCloseFriends(): Flow<List<String>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("close_friends")
            .snapshots()
            .map { snap -> snap.documents.map { it.id } }
    }

    suspend fun toggleBlockUser(targetUserId: String, isBlocked: Boolean) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("blocked_users").document(targetUserId)
        if (isBlocked) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to targetUserId, "createdAt" to FieldValue.serverTimestamp())).await()
            // auto unfollow
            unfollowUser(targetUserId)
        }
    }

    fun observeBlockedUsers(): Flow<List<String>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("blocked_users")
            .snapshots()
            .map { snap -> snap.documents.map { it.id } }
    }

    // --- Conversations & Messages ---

    suspend fun getOrCreateDirectConversation(otherUser: UserProfile): String {
        val uid = requireUserId()
        val memberIds = listOf(uid, otherUser.userId).sorted()
        val convId = "dm_${memberIds[0]}_${memberIds[1]}"
        val convRef = db.collection("conversations").document(convId)

        val existing = convRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "type" to "direct",
                "name" to otherUser.displayName,
                "avatarUrl" to otherUser.avatarUrl,
                "memberIds" to memberIds,
                "adminIds" to listOf(uid),
                "lastMessageText" to "",
                "lastMessageSenderId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            convRef.set(payload).await()
        }
        return convId
    }

    suspend fun createGroupConversation(name: String, memberIds: List<String>): String {
        val uid = requireUserId()
        val allMembers = (memberIds + uid).distinct()
        require(allMembers.size <= 50) { "Groups cannot exceed 50 members." }
        val convRef = db.collection("conversations").document()
        val payload = mapOf(
            "type" to "group",
            "name" to name.trim(),
            "avatarUrl" to "",
            "memberIds" to allMembers,
            "adminIds" to listOf(uid),
            "lastMessageText" to "Group created",
            "lastMessageSenderId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        convRef.set(payload).await()
        return convRef.id
    }

    fun observeUserConversations(): Flow<List<Conversation>> {
        val uid = requireUserId()
        return db.collection("conversations")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java)
                    .sortedByDescending { it.lastMessageTimestamp ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String = "",
        sharedPostId: String = ""
    ) {
        val uid = requireUserId()
        val user = getUserProfile(uid)
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document()
        val payload = mapOf(
            "conversationId" to conversationId,
            "senderId" to uid,
            "senderUsername" to (user?.username ?: "user"),
            "senderAvatarUrl" to (user?.avatarUrl ?: ""),
            "text" to text.trim(),
            "mediaUrl" to mediaUrl,
            "sharedPostId" to sharedPostId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            msgRef.set(payload).await()
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessageText" to (if (text.isNotBlank()) text else "Shared media"),
                    "lastMessageSenderId" to uid,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        db.collection("conversations").document(conversationId).collection("messages")
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    // --- Notifications ---

    fun observeNotifications(): Flow<List<NotificationItem>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(NotificationItem::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun markNotificationRead(notificationId: String) {
        val uid = requireUserId()
        try {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (ignored: Exception) {}
    }

    suspend fun createNotification(
        recipientUserId: String,
        type: String,
        targetId: String,
        message: String
    ) {
        val uid = requireUserId()
        if (recipientUserId == uid) return
        val user = getUserProfile(uid)
        val notifRef = db.collection("users").document(recipientUserId).collection("notifications").document()
        val payload = mapOf(
            "userId" to recipientUserId,
            "actorId" to uid,
            "actorUsername" to (user?.username ?: "user"),
            "actorAvatarUrl" to (user?.avatarUrl ?: ""),
            "type" to type,
            "targetId" to targetId,
            "message" to message,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            notifRef.set(payload).await()
        } catch (ignored: Exception) {}
    }

    // --- Safety & Moderation ---

    suspend fun reportContent(targetType: String, targetId: String, reason: String, details: String) {
        val uid = requireUserId()
        val reportRef = db.collection("reports").document()
        val payload = mapOf(
            "reporterId" to uid,
            "targetType" to targetType,
            "targetId" to targetId,
            "reason" to reason,
            "details" to details,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            reportRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
        }
    }
}
