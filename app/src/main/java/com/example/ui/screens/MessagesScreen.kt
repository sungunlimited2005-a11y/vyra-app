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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    initialConversationId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val conversations by repository.observeUserConversations().collectAsState(initial = emptyList())
    val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())

    LaunchedEffect(initialConversationId, conversations) {
        if (initialConversationId != null && activeConversation == null) {
            val target = conversations.firstOrNull { it.id == initialConversationId }
            if (target != null) {
                activeConversation = target
            }
        }
    }

    if (activeConversation != null) {
        BackHandler {
            activeConversation = null
        }
        ChatThreadView(
            conversation = activeConversation!!,
            currentUserId = currentUserId,
            currentUser = currentUser,
            repository = repository,
            onBack = { activeConversation = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Direct Messages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.testTag("button_new_chat")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "New Message", tint = VyraViolet)
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
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your Messages",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Send direct messages, share posts and photos, or start group chats with up to 50 friends.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showNewChatDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
                            ) {
                                Text("Start a Conversation")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("conversations_list"),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val otherUserId = conv.memberIds.firstOrNull { it != currentUserId }
                            val otherUser = allUsers.firstOrNull { it.userId == otherUserId }

                            val title = if (conv.type == "group") conv.name.ifBlank { "Group Chat" }
                            else otherUser?.displayName ?: "Chat"

                            val avatar = if (conv.type == "group") conv.avatarUrl else otherUser?.avatarUrl ?: ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeConversation = conv }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("conversation_item_${conv.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (avatar.isNotBlank()) {
                                    AsyncImage(
                                        model = avatar,
                                        contentDescription = title,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (conv.type == "group") VyraMagenta else VyraViolet),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (conv.type == "group") Icons.Filled.Group else Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (conv.lastMessageTimestamp != null) {
                                            Text(
                                                text = formatTimeAgo(conv.lastMessageTimestamp.toDate()),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = conv.lastMessageText.ifBlank { "Start chatting" },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        if (showNewChatDialog) {
            NewChatDialog(
                currentUserId = currentUserId,
                allUsers = allUsers.filter { it.userId != currentUserId && it.userId !in blockedUsers },
                repository = repository,
                onDismiss = { showNewChatDialog = false },
                onConversationCreated = { newConvId ->
                    showNewChatDialog = false
                    val created = conversations.firstOrNull { it.id == newConvId }
                    if (created != null) {
                        activeConversation = created
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadView(
    conversation: Conversation,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by repository.observeMessages(conversation.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = conversation.name.ifBlank { "Chat" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (conversation.type == "group") "${conversation.memberIds.size} members" else "Active on VYRA",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        if (!isMine && conversation.type == "group") {
                            Text(
                                text = msg.senderUsername,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                            )
                        }

                        // Shared post preview inside chat bubble
                        if (msg.sharedPostId.isNotBlank()) {
                            Surface(
                                color = if (isMine) VyraViolet.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (msg.mediaUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = msg.mediaUrl,
                                            contentDescription = "Shared Media",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = msg.text.ifBlank { "Shared a post" },
                                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = if (isMine) VyraViolet else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        if (msg.createdAt != null) {
                            Text(
                                text = formatTimeAgo(msg.createdAt.toDate()),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Message...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            isSending = true
                            scope.launch {
                                repository.sendMessage(
                                    conversationId = conversation.id,
                                    text = messageText
                                )
                                messageText = ""
                                isSending = false
                            }
                        }
                    },
                    enabled = messageText.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) VyraViolet else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
    currentUserId: String,
    allUsers: List<UserProfile>,
    repository: VyraRepository,
    onDismiss: () -> Unit,
    onConversationCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isGroupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isGroupMode) "New Group Chat (Max 50)" else "New Direct Message")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isGroupMode,
                        onClick = { isGroupMode = false; selectedUserIds.clear() },
                        label = { Text("1-on-1 Chat") }
                    )
                    FilterChip(
                        selected = isGroupMode,
                        onClick = { isGroupMode = true },
                        label = { Text("Group Chat") }
                    )
                }

                if (isGroupMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Members:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(allUsers, key = { it.userId }) { user ->
                        val isSelected = user.userId in selectedUserIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isGroupMode) {
                                        if (isSelected) selectedUserIds.remove(user.userId)
                                        else if (selectedUserIds.size < 49) selectedUserIds.add(user.userId)
                                    } else {
                                        selectedUserIds.clear()
                                        selectedUserIds.add(user.userId)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (isGroupMode) {
                                        if (checked == true && selectedUserIds.size < 49) selectedUserIds.add(user.userId)
                                        else selectedUserIds.remove(user.userId)
                                    } else {
                                        selectedUserIds.clear()
                                        if (checked == true) selectedUserIds.add(user.userId)
                                    }
                                }
                            )

                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Column {
                                Text(user.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("@${user.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUserIds.isNotEmpty()) {
                        isCreating = true
                        scope.launch {
                            val convId = if (isGroupMode) {
                                repository.createGroupConversation(
                                    name = groupName.ifBlank { "Group" },
                                    memberIds = selectedUserIds.toList()
                                )
                            } else {
                                val targetUser = allUsers.first { it.userId == selectedUserIds[0] }
                                repository.getOrCreateDirectConversation(targetUser)
                            }
                            isCreating = false
                            onConversationCreated(convId)
                        }
                    }
                },
                enabled = selectedUserIds.isNotEmpty() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text(if (isGroupMode) "Create Group" else "Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
