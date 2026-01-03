package com.binarybhaskar.branchitandroid.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.data.ConnectionRepository
import com.binarybhaskar.branchitandroid.data.UserProfile
import com.binarybhaskar.branchitandroid.data.UserRepository
import com.binarybhaskar.branchitandroid.ui.component.UserProfileCard
import com.google.firebase.auth.FirebaseAuth
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repo = remember { UserRepository() }
    val connectionRepo = remember { ConnectionRepository() }
    val scope = rememberCoroutineScope()

    var connectionCount by remember { mutableIntStateOf(0) }
    var showConnectionsSheet by remember { mutableStateOf(false) }
    val connections = remember { mutableStateListOf<UserProfile>() }

    // Load connection count
    LaunchedEffect(uid) {
        if (uid != null) {
            scope.launch {
                try {
                    val profile = repo.getProfile(uid)
                    connectionCount = profile?.connections?.size ?: 0
                } catch (e: Exception) {
                    connectionCount = 0
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(Modifier.size(8.dp)) }

        // Connection count card
        if (connectionCount > 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showConnectionsSheet = true
                            scope.launch {
                                connections.clear()
                                connections.addAll(connectionRepo.getConnections(uid))
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "$connectionCount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (connectionCount == 1) "Connection" else "Connections",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            if (uid != null) {
                UserProfileCard(userId = uid)
            } else {
                Text("Not signed in", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // Connections bottom sheet
    if (showConnectionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showConnectionsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showConnectionsSheet = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close")
                    }
                    Text(
                        text = "Connections ($connectionCount)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(connections, key = { it.uid }) { user ->
                        ConnectionListItem(user)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionListItem(user: UserProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photoUrl = user.profilePicUrl
            if (photoUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column {
                Text(
                    text = user.displayName.ifBlank { "User" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }) { Text("Download Resume") }
            }

            // Projects - richer display: title, description, link
            val projectsDetailed = projectsDetailedState.value
            if (projectsDetailed.isNotEmpty()) {
                Text("Projects")
                projectsDetailed.forEach { proj ->
                    if (proj.title.isNotBlank()) Text(proj.title, style = MaterialTheme.typography.titleSmall)
                    if (proj.description.isNotBlank()) Text(proj.description, style = MaterialTheme.typography.bodySmall)
                    proj.link?.takeIf { it.isNotBlank() }?.let { link ->
                        TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) }) {
                            Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Achievements
            val achievements = achievementsState.value
            if (achievements.isNotEmpty()) {
                Text("Achievements")
                achievements.forEach { a ->
                    Text("${a.title} — ${a.issuer} (${a.date})", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Extra social links (mail/discord/reddit)
            if (mailState.value.isNotBlank() || discordState.value.isNotBlank() || redditState.value.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mailState.value.isNotBlank()) {
                        TextButton(onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:${mailState.value}".toUri()))
                        }) { Text("Email") }
                    }
                    if (discordState.value.isNotBlank()) {
                        TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, discordState.value.toUri())) }) { Text("Discord") }
                    }
                    if (redditState.value.isNotBlank()) {
                        TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, redditState.value.toUri())) }) { Text("Reddit") }
                    }
                }
            }
        }
    }
}

