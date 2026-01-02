package com.binarybhaskar.branchitandroid.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ConnectionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ============ FOLLOW SYSTEM (No acceptance needed) ============

    // Follow a user (instant, no acceptance needed)
    suspend fun followUser(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            if (currentUserId == targetUserId) {
                return Result.failure(Exception("Cannot follow yourself"))
            }

            val batch = db.batch()

            // Add to current user's following list
            val currentUserRef = db.collection("users").document(currentUserId)
            batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))

            // Add to target user's followers list
            val targetUserRef = db.collection("users").document(targetUserId)
            batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to follow user", e)
            Result.failure(e)
        }
    }

    // Unfollow a user
    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            val batch = db.batch()

            // Remove from current user's following list
            val currentUserRef = db.collection("users").document(currentUserId)
            batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))

            // Remove from target user's followers list
            val targetUserRef = db.collection("users").document(targetUserId)
            batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to unfollow user", e)
            Result.failure(e)
        }
    }

    // ============ CONNECTION SYSTEM (Mutual acceptance required) ============

    // Send connection request
    suspend fun sendConnectionRequest(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            if (currentUserId == targetUserId) {
                return Result.failure(Exception("Cannot connect to yourself"))
            }

            val batch = db.batch()

            // Add to sender's pendingConnectionsSent
            val senderRef = db.collection("users").document(currentUserId)
            batch.update(senderRef, "pendingConnectionsSent", FieldValue.arrayUnion(targetUserId))

            // Add to receiver's pendingConnectionsReceived
            val receiverRef = db.collection("users").document(targetUserId)
            batch.update(receiverRef, "pendingConnectionsReceived", FieldValue.arrayUnion(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to send request", e)
            Result.failure(e)
        }
    }

    // Accept connection request
    suspend fun acceptConnectionRequest(requesterId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            val batch = db.batch()

            // Remove from pending lists
            val currentUserRef = db.collection("users").document(currentUserId)
            batch.update(currentUserRef, "pendingConnectionsReceived", FieldValue.arrayRemove(requesterId))

            val requesterRef = db.collection("users").document(requesterId)
            batch.update(requesterRef, "pendingConnectionsSent", FieldValue.arrayRemove(currentUserId))

            // Add to connections (both sides)
            batch.update(currentUserRef, "connections", FieldValue.arrayUnion(requesterId))
            batch.update(requesterRef, "connections", FieldValue.arrayUnion(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to accept request", e)
            Result.failure(e)
        }
    }

    // Reject connection request
    suspend fun rejectConnectionRequest(requesterId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            val batch = db.batch()

            // Remove from pending lists
            val currentUserRef = db.collection("users").document(currentUserId)
            batch.update(currentUserRef, "pendingConnectionsReceived", FieldValue.arrayRemove(requesterId))

            val requesterRef = db.collection("users").document(requesterId)
            batch.update(requesterRef, "pendingConnectionsSent", FieldValue.arrayRemove(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to reject request", e)
            Result.failure(e)
        }
    }

    // Cancel sent connection request
    suspend fun cancelConnectionRequest(targetUserId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            val batch = db.batch()

            // Remove from sender's pendingConnectionsSent
            val senderRef = db.collection("users").document(currentUserId)
            batch.update(senderRef, "pendingConnectionsSent", FieldValue.arrayRemove(targetUserId))

            // Remove from receiver's pendingConnectionsReceived
            val receiverRef = db.collection("users").document(targetUserId)
            batch.update(receiverRef, "pendingConnectionsReceived", FieldValue.arrayRemove(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to cancel request", e)
            Result.failure(e)
        }
    }

    // Remove connection
    suspend fun removeConnection(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            val batch = db.batch()

            // Remove from both sides
            val currentUserRef = db.collection("users").document(currentUserId)
            batch.update(currentUserRef, "connections", FieldValue.arrayRemove(userId))

            val otherUserRef = db.collection("users").document(userId)
            batch.update(otherUserRef, "connections", FieldValue.arrayRemove(currentUserId))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to remove connection", e)
            Result.failure(e)
        }
    }

    // ============ STATUS & RELATIONSHIP CHECKS ============

    // Get comprehensive relationship status
    suspend fun getRelationshipStatus(targetUserId: String): RelationshipStatus {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return RelationshipStatus()

            if (currentUserId == targetUserId) {
                return RelationshipStatus(isSelf = true)
            }

            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val profile = currentUserDoc.toObject(UserProfile::class.java) ?: return RelationshipStatus()

            RelationshipStatus(
                isFollowing = profile.following.contains(targetUserId),
                isFollower = profile.followers.contains(targetUserId),
                isConnected = profile.connections.contains(targetUserId),
                hasConnectionRequestSent = profile.pendingConnectionsSent.contains(targetUserId),
                hasConnectionRequestReceived = profile.pendingConnectionsReceived.contains(targetUserId)
            )
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get relationship status", e)
            RelationshipStatus()
        }
    }

    // ============ SEARCH & DISCOVERY ============

    // Search users with advanced filters
    suspend fun searchUsers(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
        limit: Int = 20
    ): List<UserProfile> {
        return try {
            if (query.isBlank()) return emptyList()

            val currentUserId = auth.currentUser?.uid ?: return emptyList()
            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val currentProfile = currentUserDoc.toObject(UserProfile::class.java) ?: return emptyList()

            val searchLower = query.lowercase().trim()

            // Search by username
            val usernameResults = db.collection("users")
                .whereGreaterThanOrEqualTo("username", searchLower)
                .whereLessThanOrEqualTo("username", searchLower + "\uf8ff")
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(UserProfile::class.java)

            // Search by display name (client-side filter)
            val allUsers = db.collection("users")
                .limit(100)
                .get()
                .await()
                .toObjects(UserProfile::class.java)
                .filter { it.displayName.lowercase().contains(searchLower) }

            var results = (usernameResults + allUsers).distinctBy { it.uid }.filter { it.uid != currentUserId }

            // Apply filters
            results = when (filter) {
                SearchFilter.CONNECTIONS -> results.filter { currentProfile.connections.contains(it.uid) }
                SearchFilter.DEPARTMENT -> results.filter { it.ggvInfo.department == currentProfile.ggvInfo.department }
                SearchFilter.BATCH -> results.filter { it.ggvInfo.batchYear == currentProfile.ggvInfo.batchYear }
                SearchFilter.MUTUAL_CONNECTIONS -> results.filter { user ->
                    currentProfile.connections.any { connId -> user.connections.contains(connId) }
                }
                SearchFilter.SKILLS -> {
                    val mySkills = currentProfile.skills.map { it.lowercase() }
                    results.filter { user ->
                        user.skills.any { skill -> mySkills.contains(skill.lowercase()) }
                    }
                }
                SearchFilter.ALL -> results
            }

            // Sort: Connections first, then others
            results.sortedWith(compareByDescending<UserProfile> { currentProfile.connections.contains(it.uid) }
                .thenByDescending { currentProfile.following.contains(it.uid) }
                .thenBy { it.displayName })
                .take(limit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to search users", e)
            emptyList()
        }
    }

    // Get suggested connections (same department/batch, mutual connections, not already connected)
    suspend fun getSuggestedConnections(limit: Int = 10, includeRandom: Boolean = false): List<UserProfile> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return emptyList()
            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val currentProfile = currentUserDoc.toObject(UserProfile::class.java) ?: return emptyList()

            val department = currentProfile.ggvInfo.department
            val batch = currentProfile.ggvInfo.batchYear

            val excludeIds = currentProfile.connections +
                           currentProfile.pendingConnectionsSent +
                           currentProfile.pendingConnectionsReceived +
                           listOf(currentUserId)

            val suggestions = mutableListOf<UserProfile>()

            // Get users from same department
            if (department.isNotBlank()) {
                val deptUsers = db.collection("users")
                    .whereEqualTo("ggvInfo.department", department)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(UserProfile::class.java)
                    .filter { !excludeIds.contains(it.uid) }

                suggestions.addAll(deptUsers)
            }

            // Get users from same batch if we need more
            if (suggestions.size < limit && batch.isNotBlank()) {
                val batchUsers = db.collection("users")
                    .whereEqualTo("ggvInfo.batchYear", batch)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(UserProfile::class.java)
                    .filter { !excludeIds.contains(it.uid) && !suggestions.any { s -> s.uid == it.uid } }

                suggestions.addAll(batchUsers)
            }

            // Add 2 random users from different department/batch if requested
            if (includeRandom && suggestions.size < limit) {
                val randomUsers = db.collection("users")
                    .limit(50)
                    .get()
                    .await()
                    .toObjects(UserProfile::class.java)
                    .filter {
                        !excludeIds.contains(it.uid) &&
                        !suggestions.any { s -> s.uid == it.uid } &&
                        (it.ggvInfo.department != department || it.ggvInfo.batchYear != batch)
                    }
                    .shuffled()
                    .take(2)

                suggestions.addAll(randomUsers)
            }

            suggestions.take(limit)
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get suggestions", e)
            emptyList()
        }
    }

    // Get pending connection requests received
    suspend fun getPendingConnectionRequests(): List<UserProfile> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return emptyList()
            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val profile = currentUserDoc.toObject(UserProfile::class.java) ?: return emptyList()

            profile.pendingConnectionsReceived.mapNotNull { userId ->
                try {
                    db.collection("users").document(userId).get().await()
                        .toObject(UserProfile::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get pending requests", e)
            emptyList()
        }
    }

    // Get user's connections
    suspend fun getConnections(userId: String? = null): List<UserProfile> {
        return try {
            val targetUserId = userId ?: auth.currentUser?.uid ?: return emptyList()
            val userDoc = db.collection("users").document(targetUserId).get().await()
            val profile = userDoc.toObject(UserProfile::class.java) ?: return emptyList()

            profile.connections.mapNotNull { connectionId ->
                try {
                    db.collection("users").document(connectionId).get().await()
                        .toObject(UserProfile::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get connections", e)
            emptyList()
        }
    }

    // Get followers
    suspend fun getFollowers(userId: String? = null): List<UserProfile> {
        return try {
            val targetUserId = userId ?: auth.currentUser?.uid ?: return emptyList()
            val userDoc = db.collection("users").document(targetUserId).get().await()
            val profile = userDoc.toObject(UserProfile::class.java) ?: return emptyList()

            profile.followers.mapNotNull { followerId ->
                try {
                    db.collection("users").document(followerId).get().await()
                        .toObject(UserProfile::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get followers", e)
            emptyList()
        }
    }

    // Get following
    suspend fun getFollowing(userId: String? = null): List<UserProfile> {
        return try {
            val targetUserId = userId ?: auth.currentUser?.uid ?: return emptyList()
            val userDoc = db.collection("users").document(targetUserId).get().await()
            val profile = userDoc.toObject(UserProfile::class.java) ?: return emptyList()

            profile.following.mapNotNull { followingId ->
                try {
                    db.collection("users").document(followingId).get().await()
                        .toObject(UserProfile::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionRepo", "Failed to get following", e)
            emptyList()
        }
    }
}

// Relationship status with another user
data class RelationshipStatus(
    val isSelf: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val isConnected: Boolean = false,
    val hasConnectionRequestSent: Boolean = false,
    val hasConnectionRequestReceived: Boolean = false
)

// Search filters
enum class SearchFilter {
    ALL,
    CONNECTIONS,
    DEPARTMENT,
    BATCH,
    MUTUAL_CONNECTIONS,
    SKILLS
}

// Old enum for backward compatibility (deprecated)
@Deprecated("Use RelationshipStatus instead")
enum class ConnectionStatus {
    SELF,
    CONNECTED,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    NOT_CONNECTED
}

