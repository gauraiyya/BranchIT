package com.binarybhaskar.branchitandroid.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Simple in-memory cache shared across repository instances
    companion object {
        @Volatile
        private var cachedProfile: UserProfile? = null

        @Volatile
        private var cachedProfileUid: String? = null

        // Only return cached profile when it belongs to the currently signed-in user
        fun getCachedProfile(): UserProfile? {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            return if (cachedProfile != null && cachedProfileUid != null && cachedProfileUid == currentUid) cachedProfile else null
        }

        private fun setCachedProfile(uid: String?, profile: UserProfile?) {
            cachedProfileUid = uid
            cachedProfile = profile
        }
    }

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun getOrCreateProfile(): UserProfile {
        // Serve from memory if available
        getCachedProfile()?.let { return it }

        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val snapshot = userDoc(uid).get().await()
        val profile = if (snapshot.exists()) {
            // Try to map; if mapping returns null, provide a minimal default with uid
            snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
        } else {
            val user = auth.currentUser
            val fallback = UserProfile(
                uid = uid,
                displayName = user?.displayName.orEmpty(),
                profilePicUrl = user?.photoUrl?.toString().orEmpty(),
                updatedAt = System.currentTimeMillis()
            )
            userDoc(uid).set(fallback).await()
            fallback
        }
        // update memory cache
        setCachedProfile(auth.currentUser?.uid, profile)
        return profile
    }

    suspend fun saveProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val value = profile.copy(updatedAt = System.currentTimeMillis())
        userDoc(uid).set(value).await()
        // keep cache consistent
        setCachedProfile(uid, value)
    }

    suspend fun uploadProfileImage(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val ref = storage.reference.child("users/$uid/profile.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadResumePdf(contentResolver: ContentResolver, uri: Uri): String {
        val size = queryFileSize(contentResolver, uri)
        if (size != null && size > 1_000_000) {
            throw IllegalArgumentException("Resume exceeds 1 MB")
        }
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        val ref = storage.reference.child("users/$uid/resume.pdf")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private fun queryFileSize(resolver: ContentResolver, uri: Uri): Long? {
        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) cursor.getLong(idx) else null
            } else null
        } finally {
            cursor?.close()
        }
    }

    suspend fun getProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        Log.d("UserRepository", "getProfile called for uid=$uid")
        val snap = userDoc(uid).get(Source.SERVER)
            .await() // Force server fetch
        val data = snap.data
        // Log the raw data for debugging
        Log.d("UserRepository", "Firestore raw data for $uid: $data")
        // Try mapping to UserProfile, fallback to manual extraction
        if (!snap.exists()) {
            Log.d("UserRepository", "Document does not exist for uid=$uid")
            return null
        }

        // First try mapping to the data class
        val mappedProfile = try {
            snap.toObject(UserProfile::class.java)
                .also { Log.d("UserRepository", "mappedProfile for $uid: $it") }
        } catch (e: Exception) {
            Log.w("UserRepository", "mapping to UserProfile failed for $uid", e)
            null
        }

        val finalProfile = mappedProfile ?: UserProfile(uid = uid)

        // Update in-memory cache for current UID so subsequent calls are fast
        setCachedProfile(uid, finalProfile)

        return finalProfile
    }
}
