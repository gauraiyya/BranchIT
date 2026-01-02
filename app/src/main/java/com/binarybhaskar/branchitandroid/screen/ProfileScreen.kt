package com.binarybhaskar.branchitandroid.screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.data.UserRepository
import com.binarybhaskar.branchitandroid.data.Project
import com.binarybhaskar.branchitandroid.data.Achievement
import com.google.firebase.auth.FirebaseAuth
import androidx.core.net.toUri
import java.util.UUID

@Composable
fun ProfileScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(Modifier.size(8.dp)) }

        item {
            if (uid != null) {
                UserProfileCard(userId = uid)
            } else {
                Text("Not signed in", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileCard(userId: String, onEdit: (() -> Unit)? = null) {
    val repo = remember { UserRepository() }
    val ctx = LocalContext.current

    val nameState = remember { mutableStateOf("") }
    val usernameState = remember { mutableStateOf("") }
    val photoState = remember { mutableStateOf("") }
    val backgroundState = remember { mutableStateOf("") }
    val aboutState = remember { mutableStateOf("") }
    val linkedinState = remember { mutableStateOf("") }
    val githubState = remember { mutableStateOf("") }
    val instagramState = remember { mutableStateOf("") }
    val mailState = remember { mutableStateOf("") }
    val discordState = remember { mutableStateOf("") }
    val redditState = remember { mutableStateOf("") }
    val websiteState = remember { mutableStateOf("") }
    val skillsState = remember { mutableStateOf(listOf<String>()) }
    val resumeState = remember { mutableStateOf("") }
    val projectsState = remember { mutableStateOf(listOf<String>()) }
    val projectsDetailedState = remember { mutableStateOf(listOf<Project>()) }

    val departmentState = remember { mutableStateOf("") }
    val batchState = remember { mutableStateOf("") }
    val enrolmentState = remember { mutableStateOf("") }

    val xpState = remember { mutableStateOf(0) }
    val badgesState = remember { mutableStateOf(listOf<String>()) }
    val achievementsState = remember { mutableStateOf(listOf<Achievement>()) }

    LaunchedEffect(userId) {
        try {
            val p = repo.getProfile(userId)
            if (p != null) {
                nameState.value = p.displayName
                usernameState.value = p.username
                photoState.value = p.profilePicUrl.orEmpty()
                backgroundState.value = p.backgroundPicUrl.orEmpty()
                aboutState.value = p.about.orEmpty()

                // social links
                linkedinState.value = p.socialLinks.linkedIn ?: ""
                githubState.value = p.socialLinks.github ?: ""
                instagramState.value = p.socialLinks.instagram ?: ""
                mailState.value = p.socialLinks.mail ?: ""
                discordState.value = p.socialLinks.discord ?: ""
                redditState.value = p.socialLinks.reddit ?: ""
                websiteState.value = p.socialLinks.website ?: ""

                skillsState.value = p.skills
                resumeState.value = p.resumeUrl ?: ""

                // projects: keep both simple links and detailed objects for richer UI
                projectsState.value = p.projects.mapNotNull { it.link?.takeIf { l -> l.isNotBlank() } }.take(3)
                projectsDetailedState.value = p.projects

                // GGV info
                departmentState.value = p.ggvInfo.department
                batchState.value = p.ggvInfo.batchYear
                enrolmentState.value = p.ggvInfo.enrolmentNumber

                // Gamification
                xpState.value = p.xpPoints
                badgesState.value = p.badges

                // Achievements
                achievementsState.value = p.achievements
            }
        } catch (e: Exception) {
            Log.w("UserProfileCard", "failed to load profile", e)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Background banner (if present)
            val background = backgroundState.value
            if (background.isNotBlank()) {
                AsyncImage(
                    model = background,
                    contentDescription = "Background image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val photo = photoState.value
                if (photo.isNotBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // simple placeholder
                    Image(
                        painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_ggv_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.size(12.dp))

                Column(Modifier.weight(1f)) {
                    val displayName = nameState.value.ifBlank { "User" }
                    Text(displayName, style = MaterialTheme.typography.titleMedium)
                    val username = usernameState.value
                    if (username.isNotBlank()) {
                        Text("@${username}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                onEdit?.let {
                    Button(onClick = it) { Text("Edit") }
                }
            }

            // Social icons (existing)
            val linkedin = linkedinState.value
            val github = githubState.value
            if (linkedin.isNotBlank() || github.isNotBlank() || instagramState.value.isNotBlank() || websiteState.value.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (linkedin.isNotBlank()) {
                        Icon(
                            painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_linkedin),
                            contentDescription = "LinkedIn",
                            modifier = Modifier.clickable(onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        linkedin.toUri()
                                    )
                                )
                            })
                        )
                    }
                    if (github.isNotBlank()) {
                        Icon(
                            painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.clickable(onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        github.toUri()
                                    )
                                )
                            })
                        )
                    }
                    if (instagramState.value.isNotBlank()) {
                        TextButton(onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, instagramState.value.toUri()))
                        }) { Text("Instagram") }
                    }
                    if (websiteState.value.isNotBlank()) {
                        TextButton(onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, websiteState.value.toUri()))
                        }) { Text("Website") }
                    }
                }
            }

            val about = aboutState.value
            if (about.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(about, Modifier.padding(12.dp))
                }
            }

            // GGV info
            if (departmentState.value.isNotBlank() || batchState.value.isNotBlank() || enrolmentState.value.isNotBlank()) {
                Text("GGV Details", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (departmentState.value.isNotBlank()) Text(departmentState.value)
                    if (batchState.value.isNotBlank()) Text(batchState.value)
                    if (enrolmentState.value.isNotBlank()) Text(enrolmentState.value)
                }
            }

            val skills = skillsState.value
            if (skills.isNotEmpty()) {
                Text("Skills")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.take(8).forEach { s ->
                        SuggestionChip(
                            modifier = Modifier
                                .size(32.dp)
                                .wrapContentWidth(),
                            label = { Text(s) },
                            onClick = {}
                        )
                    }
                }
            }

            // XP and badges
            if (xpState.value > 0 || badgesState.value.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (xpState.value > 0) Text("XP: ${xpState.value}")
                    badgesState.value.forEach { b ->
                        SuggestionChip(label = { Text(b) }, onClick = {})
                    }
                }
            }

            val resumeUrl = resumeState.value
            if (resumeUrl.isNotBlank()) {
                Button(onClick = {
                    ctx.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            resumeUrl.toUri()
                        )
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
