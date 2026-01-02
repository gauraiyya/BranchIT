package com.binarybhaskar.branchitandroid.ui.component

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.data.Achievement
import com.binarybhaskar.branchitandroid.data.Project
import com.binarybhaskar.branchitandroid.data.UserRepository

// Helper function for safe intent launching
private fun safeLaunchUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No app found to open this link", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // Modern, minimal card container
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with background and profile image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background image or gradient
                val background = backgroundState.value
                if (background.isNotBlank()) {
                    AsyncImage(
                        model = background,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    // Gradient fallback using Material colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                    )
                }

                // Edit button (top right)
                onEdit?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Profile picture at bottom, slightly overlapping
                val photo = photoState.value
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp)
                        .offset(y = 40.dp)
                ) {
                    if (photo.isNotBlank()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_ggv_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Profile info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Name and username
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val displayName = nameState.value.ifBlank { "User" }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val username = usernameState.value
                        if (username.isNotBlank()) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // XP - minimal design
                    if (xpState.value > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${xpState.value} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // GGV info with icon
                if (departmentState.value.isNotBlank() || batchState.value.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = listOfNotNull(
                                departmentState.value.takeIf { it.isNotBlank() },
                                batchState.value.takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // About section
                val about = aboutState.value
                if (about.isNotBlank()) {
                    Text(
                        text = about,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                // Social links - Icon only, minimal
                val hasSocials = linkedinState.value.isNotBlank() ||
                                githubState.value.isNotBlank() ||
                                instagramState.value.isNotBlank() ||
                                websiteState.value.isNotBlank() ||
                                mailState.value.isNotBlank()

                if (hasSocials) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LinkedIn
                        if (linkedinState.value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        safeLaunchUrl(ctx, "https://www.${linkedinState.value}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_linkedin),
                                    contentDescription = "LinkedIn",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // GitHub
                        if (githubState.value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        safeLaunchUrl(ctx, "https://www.${githubState.value}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_github),
                                    contentDescription = "GitHub",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Instagram
                        if (instagramState.value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        safeLaunchUrl(ctx, "https://www.${instagramState.value}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(com.binarybhaskar.branchitandroid.R.drawable.ic_instagram),
                                    contentDescription = "Instagram",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Website
                        if (websiteState.value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        safeLaunchUrl(ctx, "https://www.${websiteState.value}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = "Website",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Email
                        if (mailState.value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        safeLaunchUrl(ctx, "mailto:${mailState.value}")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Skills - minimal tags
                val skills = skillsState.value
                if (skills.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Skills",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skills.take(10).forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text = skill,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Resume download - minimal button
                val resumeUrl = resumeState.value
                if (resumeUrl.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                safeLaunchUrl(ctx, resumeUrl)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WorkOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Resume",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Projects - clean list
                val projectsDetailed = projectsDetailedState.value
                if (projectsDetailed.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Projects",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        projectsDetailed.take(3).forEach { proj ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = proj.link?.isNotBlank() == true) {
                                        proj.link?.let { link ->
                                            safeLaunchUrl(ctx, link)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (proj.title.isNotBlank()) {
                                        Text(
                                            text = proj.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (proj.description.isNotBlank()) {
                                        Text(
                                            text = proj.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (badgesState.value.isNotEmpty()) {
                    badgesState.value.take(3).forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Achievements - minimal list
                val achievements = achievementsState.value
                if (achievements.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        achievements.take(3).forEach { achievement ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = achievement.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${achievement.issuer} • ${achievement.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
