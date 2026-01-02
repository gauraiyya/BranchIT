package com.binarybhaskar.branchitandroid.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UserProfile(
    @Keep val uid: String = "",
    @Keep val username: String = "",
    @Keep val displayName: String = "",
    @Keep val about: String? = null,
    @Keep val profilePicUrl: String? = null,
    @Keep val backgroundPicUrl: String? = null,
    @Keep var usernameUpdatedAt: Long = 0L,
    @Keep var updatedAt: Long = 0L,


    // GGV Specific Info
    @Keep val ggvInfo: GGVDetails = GGVDetails(),

    // Professional Portfolio
    @Keep val skills: List<String> = emptyList(),
    @Keep val resumeUrl: String? = null,
    @Keep val projects: List<Project> = emptyList(),
    @Keep val achievements: List<Achievement> = emptyList(),

    // Social Links
    @Keep val socialLinks: SocialLinks = SocialLinks(),

    // Gamification
    @Keep val xpPoints: Int = 0,
    @Keep val badges: List<String> = emptyList(),

    // Follow System (Instagram/Twitter-like) - No acceptance needed
    @Keep val following: List<String> = emptyList(), // Users this user follows
    @Keep val followers: List<String> = emptyList(), // Users following this user

    // Connections (LinkedIn-like) - Mutual acceptance required
    @Keep val connections: List<String> = emptyList(), // List of connected user UIDs
    @Keep val pendingConnectionsSent: List<String> = emptyList(), // Connection requests sent
    @Keep val pendingConnectionsReceived: List<String> = emptyList(), // Connection requests received

    // Security & Status
    @Keep val isPrivate: Boolean = false,
    @Keep val searchHash: String = "",
    @Keep val encryptedMetadata: String? = null
)

@Keep
@Serializable
data class GGVDetails(
    @Keep val department: String = "",
    @Keep val batchYear: String = "",
    @Keep val enrolmentNumber: String = "",
)

@Keep
@Serializable
data class Project(
    @Keep val title: String = "",
    @Keep val description: String = "",
    @Keep val techStack: List<String> = emptyList(),
    @Keep val link: String? = null
)

@Keep
@Serializable
data class SocialLinks(
    @Keep val linkedIn: String? = null,
    @Keep val github: String? = null,
    @Keep val instagram: String? = null,
    @Keep val mail: String? = null,
    @Keep val discord: String? = null,
    @Keep val reddit: String? = null,
    @Keep val website: String? = null,
)

@Keep
@Serializable
data class Achievement(
    @Keep val title: String = "",
    @Keep val date: String = "",
    @Keep val issuer: String = ""
)
