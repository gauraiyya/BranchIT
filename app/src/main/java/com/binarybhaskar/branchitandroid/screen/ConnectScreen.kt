package com.binarybhaskar.branchitandroid.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.data.ConnectionRepository
import com.binarybhaskar.branchitandroid.data.SearchFilter
import com.binarybhaskar.branchitandroid.data.UserProfile
import com.binarybhaskar.branchitandroid.ui.component.UserProfileCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val connectionRepo = remember { ConnectionRepository() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending Connections", "My Connections")

    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<UserProfile>() }
    var isSearching by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val pendingRequests = remember { mutableStateListOf<UserProfile>() }
    val suggestions = remember { mutableStateListOf<UserProfile>() }
    val connections = remember { mutableStateListOf<UserProfile>() }

    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var showUserProfile by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        scope.launch {
            isLoading = true
            try {
                when (selectedTab) {
                    0 -> {
                        pendingRequests.clear()
                        suggestions.clear()
                        pendingRequests.addAll(connectionRepo.getPendingConnectionRequests())
                        suggestions.addAll(connectionRepo.getSuggestedConnections(20, includeRandom = true))
                    }
                    1 -> {
                        connections.clear()
                        connections.addAll(connectionRepo.getConnections())
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(searchQuery, searchFilter) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            scope.launch {
                isSearching = true
                try {
                    val results = connectionRepo.searchUsers(searchQuery, searchFilter, 20)
                    searchResults.clear()
                    searchResults.addAll(results)
                } catch (e: Exception) {
                    searchResults.clear()
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchResults.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Connect",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search users...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Box {
                IconButton(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier
                        .background(
                            if (searchFilter != SearchFilter.ALL) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(Icons.Filled.FilterList, "Filter")
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    SearchFilter.values().forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.name.replace("_", " ")) },
                            onClick = {
                                searchFilter = filter
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            if (isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(searchResults, key = { it.uid }) { user ->
                        UserCard(user) {
                            selectedUser = user
                            showUserProfile = true
                        }
                    }
                }
            }
        } else {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> PendingTab(pendingRequests, suggestions, connectionRepo, scope, context, { selectedUser = it; showUserProfile = true }) {
                        scope.launch {
                            pendingRequests.clear()
                            suggestions.clear()
                            pendingRequests.addAll(connectionRepo.getPendingConnectionRequests())
                            suggestions.addAll(connectionRepo.getSuggestedConnections(20, true))
                        }
                    }
                    1 -> ConnectionsTab(connections) { selectedUser = it; showUserProfile = true }
                }
            }
        }
    }

    if (showUserProfile && selectedUser != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showUserProfile = false },
            sheetState = sheetState
        ) {
            UserProfilePopup(selectedUser!!, connectionRepo, { showUserProfile = false }) {
                scope.launch {
                    when (selectedTab) {
                        0 -> {
                            pendingRequests.clear()
                            suggestions.clear()
                            pendingRequests.addAll(connectionRepo.getPendingConnectionRequests())
                            suggestions.addAll(connectionRepo.getSuggestedConnections(20, true))
                        }
                        1 -> {
                            connections.clear()
                            connections.addAll(connectionRepo.getConnections())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingTab(
    pending: List<UserProfile>,
    suggestions: List<UserProfile>,
    repo: ConnectionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onUserClick: (UserProfile) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (pending.isNotEmpty()) {
            item {
                Text("Pending Requests (${pending.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(pending, key = { it.uid }) { user ->
                RequestCard(user, repo, scope, context, onUserClick, onRefresh)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Text(if (pending.isEmpty()) "Suggested Connections" else "You May Know", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(suggestions, key = { it.uid }) { user ->
                UserCard(user, onUserClick)
            }
        }

        if (pending.isEmpty() && suggestions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Person, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No pending requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionsTab(connections: List<UserProfile>, onUserClick: (UserProfile) -> Unit) {
    if (connections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Person, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("No connections yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(connections, key = { it.uid }) { user ->
                UserCard(user, onUserClick)
            }
        }
    }
}

@Composable
fun UserCard(user: UserProfile, onClick: (UserProfile) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { onClick(user) }),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val photoUrl = user.profilePicUrl
            if (photoUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            } else {
                Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.displayName.ifBlank { "User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (user.username.isNotBlank()) {
                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (user.ggvInfo.department.isNotBlank()) {
                    Text(
                        "${user.ggvInfo.department}${if (user.ggvInfo.batchYear.isNotBlank()) " • ${user.ggvInfo.batchYear}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    user: UserProfile,
    repo: ConnectionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onUserClick: (UserProfile) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth().clickable { onUserClick(user) }, verticalAlignment = Alignment.CenterVertically) {
                val photoUrl = user.profilePicUrl
                if (photoUrl?.isNotBlank() == true) {
                    AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.displayName.ifBlank { "User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (user.username.isNotBlank()) {
                        Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = repo.acceptConnectionRequest(user.uid)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Connection accepted!", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = repo.rejectConnectionRequest(user.uid)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun UserProfilePopup(
    user: UserProfile,
    repo: ConnectionRepository,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var status by remember { mutableStateOf(com.binarybhaskar.branchitandroid.data.RelationshipStatus()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user.uid) {
        scope.launch {
            isLoading = true
            status = repo.getRelationshipStatus(user.uid)
            isLoading = false
        }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        UserProfileCard(userId = user.uid)
        Spacer(Modifier.height(16.dp))

        if (!isLoading && !status.isSelf) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = if (status.isFollowing) repo.unfollowUser(user.uid) else repo.followUser(user.uid)
                            if (result.isSuccess) {
                                Toast.makeText(context, if (status.isFollowing) "Unfollowed" else "Following!", Toast.LENGTH_SHORT).show()
                                status = repo.getRelationshipStatus(user.uid)
                                onRefresh()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (status.isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (status.isFollowing) "Following" else "Follow")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = when {
                                status.isConnected -> { Toast.makeText(context, "Already connected", Toast.LENGTH_SHORT).show(); return@launch }
                                status.hasConnectionRequestSent -> repo.cancelConnectionRequest(user.uid)
                                status.hasConnectionRequestReceived -> repo.acceptConnectionRequest(user.uid)
                                else -> repo.sendConnectionRequest(user.uid)
                            }
                            if (result.isSuccess) {
                                Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show()
                                status = repo.getRelationshipStatus(user.uid)
                                onRefresh()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(when {
                        status.isConnected -> "Connected"
                        status.hasConnectionRequestSent -> "Pending"
                        status.hasConnectionRequestReceived -> "Accept"
                        else -> "Connect"
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { Toast.makeText(context, "Chat feature coming soon!", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chat (Coming Soon)")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

