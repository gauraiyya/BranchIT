package com.binarybhaskar.branchitandroid.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LiveHelp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.LiveHelp
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.R
//import com.binarybhaskar.branchitandroid.data.ChatRepository
//import com.binarybhaskar.branchitandroid.data.ChatTarget
//import com.binarybhaskar.branchitandroid.data.PostRepository
import com.binarybhaskar.branchitandroid.data.UserRepository
import com.binarybhaskar.branchitandroid.navigation.Destinations
//import com.binarybhaskar.branchitandroid.util.InteractionStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContent(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        // Redirect to login if not logged in
        LaunchedEffect(Unit) {
            navController.navigate(Destinations.LOGIN) {
                popUpTo(Destinations.SCREEN_CONTENT) { inclusive = true }
                launchSingleTop = true
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Redirecting to login...", textAlign = TextAlign.Center)
        }
        return
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Connect", "Post", "Chat", "Profile")
    val outlinedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.PeopleOutline,
        Icons.Outlined.AddCircleOutline,
        Icons.AutoMirrored.Outlined.Message,
        Icons.Outlined.Person
    )
    val filledIcons = listOf(
        Icons.Filled.Home,
        Icons.Filled.People,
        Icons.Default.AddCircle,
        Icons.AutoMirrored.Filled.Message,
        Icons.Filled.Person
    )
    val repo = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    // Seed from cached profile for instant UI
    var firebasePhotoUrl by remember(user.uid) {
        mutableStateOf(UserRepository.getCachedProfile()?.profilePicUrl ?: user.photoUrl?.toString())
    }

    // Background refresh if needed
    LaunchedEffect(user.uid) {
        scope.launch {
            try {
                val profile = repo.getOrCreateProfile()
                if (profile.profilePicUrl?.isNotBlank() == true) {
                    firebasePhotoUrl = profile.profilePicUrl
                } else if (firebasePhotoUrl.isNullOrBlank()) {
                    firebasePhotoUrl = user.photoUrl?.toString()
                }
            } catch (_: Exception) {
                if (firebasePhotoUrl.isNullOrBlank()) {
                    firebasePhotoUrl = user.photoUrl?.toString()
                }
            }
        }
    }

    val googleNavColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )

    // Saveable state holder to preserve each tab's UI state across switches
    val stateHolder = rememberSaveableStateHolder()

    // Keep-alive: all tabs mounted to avoid recomposition cost on first switch
    val mountedTabs = remember { mutableStateListOf(true, true, true, true, true) }
//
//    // Unread indicator for Chat bottom nav
//    val context = LocalContext.current
//    val store = remember(context) { InteractionStore(context) }
//    val chatRepo = remember { ChatRepository() }
//    val postRepo = remember { PostRepository() }
//    var unreadGroups by remember { mutableStateOf(false) }
//    var unreadMine by remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        chatRepo.observeGroups().collectLatest { list ->
//            var any = false
//            for (g in list) {
//                try {
//                    val lastMsg =
//                        withContext(Dispatchers.IO) { chatRepo.getLastMessageTimestampForGroup(g.id) }
//                    val lastOpened = store.lastOpenedFor(ChatTarget.GroupRoom(g.id))
//                    if (lastMsg > 0 && lastMsg > lastOpened) {
//                        any = true; break
//                    }
//                } catch (_: Exception) {
//                }
//            }
//            unreadGroups = any
//        }
//    }
//    LaunchedEffect(Unit) {
//        try {
//            val page = postRepo.loadPage(PostRepository.SortFilter.MINE, 20)
//            var any = false
//            for (p in page.items) {
//                try {
//                    val lastMsg =
//                        withContext(Dispatchers.IO) { chatRepo.getLastMessageTimestampForPost(p.id) }
//                    val lastOpened = store.lastOpenedFor(ChatTarget.PostThread(p.id))
//                    if (lastMsg > 0 && lastMsg > lastOpened) {
//                        any = true; break
//                    }
//                } catch (_: Exception) {
//                }
//            }
//            unreadMine = any
//        } catch (_: Exception) {
//        }
//    }
//    val hasUnreadChat = unreadGroups || unreadMine
//
    Box {
        Scaffold(topBar = {
            TopAppBar(
                title = {
                    Row {
                        val logoScale by animateFloatAsState(
                            targetValue = if (selectedTab != 0) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "LogoScale"
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_branchit),
                            contentDescription = "BranchIT Title",
                            modifier = Modifier
                                .height(48.dp)
                                .scale(logoScale)
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        val url = firebasePhotoUrl
                        if (url != null && url.isNotBlank()) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        navController.navigate(Destinations.SETTINGS) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    })
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Default Profile Picture",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        navController.navigate(Destinations.SETTINGS) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                    }
                },
            )
        }, bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .heightIn(min = 64.dp)
            ) {
                // Restore Row wrapper so items layout as before
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        // Restore original animations but constrain layout to prevent jank
                        val targetIconSize = if (selectedTab == index) 32.dp else 24.dp
                        val targetTextScale = if (selectedTab == index) 1f else 0f
                        val targetTextHeight = if (selectedTab == index) 16.dp else 0.dp

                        val iconSize by animateDpAsState(
                            targetValue = targetIconSize, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessLow
                            ), label = "NavIconSize"
                        )
                        val textScale by animateFloatAsState(
                            targetValue = targetTextScale, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ), label = "NavTextScale"
                        )
                        val textHeight by animateDpAsState(
                            targetValue = targetTextHeight, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ), label = "NavTextHeight"
                        )

                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = if (selectedTab == index) filledIcons[index] else outlinedIcons[index],
                                            contentDescription = tab,
                                            modifier = Modifier.size(iconSize)
                                        )
//                                        if (index == 3 && hasUnreadChat) {
//                                            Box(
//                                                modifier = Modifier
//                                                    .align(Alignment.TopEnd)
//                                                    .size(8.dp)
//                                                    .clip(CircleShape)
//                                                    .background(MaterialTheme.colorScheme.error)
//                                            )
//                                        }
                                    }
                                }
                            },
                            label = {
                                Box(
                                    modifier = Modifier
                                        .height(textHeight)
                                        .scale(textScale)
                                ) {
                                    Text(tab)
                                }
                            },
                            selected = selectedTab == index,
                            onClick = {
                                if (selectedTab != index) {
                                    selectedTab = index
                                }
                            },
                            colors = googleNavColors,
                        )
                    }
                }
            }
        }) { innerPadding ->
            NavBarBox(
                innerPadding, tabs, mountedTabs, selectedTab, stateHolder)
        }
    }
}

@Composable
fun NavBarBox(
    innerPadding: PaddingValues,
    tabs: List<String>,
    mountedTabs: SnapshotStateList<Boolean>,
    selectedTab: Int,
    stateHolder: SaveableStateHolder,
//    onOpenChat: (ChatTarget) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        tabs.indices.forEach { index ->
            if (mountedTabs[index]) {
                val visible = selectedTab == index
                val alpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f, animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ), label = "TabAlpha"

                )
                stateHolder.SaveableStateProvider(key = index) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (visible) 1f else 0f)
                            .graphicsLayer { this.alpha = alpha }
                            .then(if (!visible) Modifier.semantics { hideFromAccessibility() } else Modifier)) {
                        when (index) {
                            0 -> HomeScreen()
                            1 -> ConnectScreen()
                            2 -> CreatePostScreen()
                            3 -> ChatsScreen()
                            4 -> ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}