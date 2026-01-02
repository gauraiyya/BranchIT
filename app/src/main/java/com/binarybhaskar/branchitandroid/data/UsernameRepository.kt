package com.binarybhaskar.branchitandroid.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import java.util.regex.Pattern

/**
 * Repository to manage unique usernames using Firestore transactions.
 * - users collection: users/{uid} (user profile docs)
 * - usernames collection: usernames/{usernameId} (doc id = normalized lowercase username)
 *
 * This implementation uses Tasks API (no coroutines) and keeps logic simple.
 */
class UsernameRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val usersColl = "users"
    private val usernamesColl = "usernames"

    // Regex: allow a-z, 0-9, dot, underscore; 3-20 chars; no leading/trailing dot/underscore
    private val USERNAME_PATTERN: Pattern = Pattern.compile("^(?=.{3,30}$)(?!.*[._]{2})[a-z][a-z0-9._]+(?<![._])$")

    private val DEFAULT_PREFIX = "user_"
    private val MAX_USERNAME_LENGTH = 30
    private val MAX_SUFFIX_TRIES = 20

    // Number of days a user must wait between username changes. For now set to 0 (no wait).
    private val USERNAME_CHANGE_COOLDOWN_DAYS = 0L

    private fun normalize(username: String): String = username.trim().lowercase(Locale.US)

    private fun validateOrThrow(username: String) {
        val norm = normalize(username)
        if (!USERNAME_PATTERN.matcher(norm).matches()) {
            throw IllegalArgumentException("INVALID_USERNAME")
        }
    }

    fun isUsernameAvailable(username: String): Task<Boolean> {
        val uname = normalize(username)
        try {
            if (!USERNAME_PATTERN.matcher(uname).matches()) {
                return Tasks.forException(IllegalArgumentException("INVALID_USERNAME"))
            }
        } catch (e: Exception) {
            return Tasks.forException(e)
        }

        val docRef = firestore.collection(usernamesColl).document(uname)
        return docRef.get().continueWith { task ->
            if (!task.isSuccessful) throw task.exception ?: RuntimeException("NETWORK_ERROR")
            val snap = task.result
            snap == null || !snap.exists()
        }
    }

    suspend fun reserveUsername(username: String): Task<Void> {
        if (username.isBlank()) throw IllegalArgumentException("Username cannot be empty")

        val currentUser = auth.currentUser ?: return Tasks.forException(FirebaseFirestoreException("NOT_AUTHENTICATED", FirebaseFirestoreException.Code.PERMISSION_DENIED))
        val uid = currentUser.uid
        val uname = normalize(username)

        try {
            validateOrThrow(uname)
        } catch (e: Exception) {
            return Tasks.forException(e)
        }

        val usernameDoc = firestore.collection(usernamesColl).document(uname)
        val userDoc = firestore.collection(usersColl).document(uid)

        return firestore.runTransaction { txn ->
            val usernameSnap = txn.get(usernameDoc)
            if (usernameSnap.exists()) {
                throw FirebaseFirestoreException("USERNAME_TAKEN", FirebaseFirestoreException.Code.ABORTED)
            }

            // create username mapping
            val data = mapOf(
                "uid" to uid,
                "createdAt" to Timestamp.now()
            )
            txn.set(usernameDoc, data)

            // update user's profile username field
            val userUpdate = mapOf(
                "username" to uname,
                "usernameUpdatedAt" to Timestamp.now().toDate().time
            )
            txn.set(userDoc, userUpdate, SetOptions.merge())
            null
        }
    }

    suspend fun changeUsername(newUsername: String): Task<Void> {
        if (newUsername.isBlank()) throw IllegalArgumentException("Username cannot be empty")

        val currentUser = auth.currentUser ?: return Tasks.forException(FirebaseFirestoreException("NOT_AUTHENTICATED", FirebaseFirestoreException.Code.PERMISSION_DENIED))
        val uid = currentUser.uid
        val newU = normalize(newUsername)

        try {
            validateOrThrow(newU)
        } catch (e: Exception) {
            return Tasks.forException(e)
        }

        val newDoc = firestore.collection(usernamesColl).document(newU)
        val userDoc = firestore.collection(usersColl).document(uid)

        return firestore.runTransaction { txn ->
            // Read user profile first
            val userSnap: DocumentSnapshot = txn.get(userDoc)
            val oldU = userSnap.getString("username") ?: ""

            // Enforce cooldown between username changes
            if (USERNAME_CHANGE_COOLDOWN_DAYS > 0) {
                val lastUpdated = userSnap.getLong("usernameUpdatedAt") ?: 0L
                val cooldownMillis = USERNAME_CHANGE_COOLDOWN_DAYS * 24 * 60 * 60 * 1000L
                val now = Timestamp.now().toDate().time
                if (now - lastUpdated < cooldownMillis) {
                    throw FirebaseFirestoreException("USERNAME_COOLDOWN", FirebaseFirestoreException.Code.PERMISSION_DENIED)
                }
            }

            if (oldU == newU) {
                // no-op
                return@runTransaction null
            }

            // Read new username doc and (if needed) the old username doc BEFORE any writes
            val newSnap = txn.get(newDoc)
            val oldDocRef = if (oldU.isNotBlank()) firestore.collection(usernamesColl).document(oldU) else null
            val oldSnap = oldDocRef?.let { txn.get(it) }

            if (newSnap.exists() && newSnap.getString("uid") != uid) {
                throw FirebaseFirestoreException("USERNAME_TAKEN", FirebaseFirestoreException.Code.ABORTED)
            }

            // Now perform writes: create new username doc (or overwrite if owned by this uid)
            val newData = mapOf("uid" to uid, "createdAt" to Timestamp.now())
            txn.set(newDoc, newData)

            // delete old username mapping if present and owned by this user
            if (oldU.isNotBlank() && oldSnap != null && oldSnap.exists()) {
                if (oldSnap.getString("uid") == uid) {
                    val oldDocRef = firestore.collection(usernamesColl).document(oldU)
                    txn.delete(oldDocRef)
                }
            }

            // update user's username field
            val userUpdate = mapOf("username" to newU, "usernameUpdatedAt" to Timestamp.now().toDate().time)
            txn.set(userDoc, userUpdate, SetOptions.merge())
            null
        }
    }

    fun releaseUsername(username: String): Task<Void> {
        val currentUser = auth.currentUser ?: return Tasks.forException(FirebaseFirestoreException("NOT_AUTHENTICATED", FirebaseFirestoreException.Code.PERMISSION_DENIED))
        val uid = currentUser.uid
        val uname = normalize(username)

        val usernameDoc = firestore.collection(usernamesColl).document(uname)
        val userDoc = firestore.collection(usersColl).document(uid)

        return firestore.runTransaction { txn ->
            // Read both the username mapping and the user's profile BEFORE any writes
            val usernameSnap = txn.get(usernameDoc)
            if (!usernameSnap.exists()) {
                // idempotent success
                return@runTransaction null
            }
            val owner = usernameSnap.getString("uid")
            if (owner != uid) {
                throw FirebaseFirestoreException("PERMISSION_DENIED", FirebaseFirestoreException.Code.PERMISSION_DENIED)
            }

            val userSnap = txn.get(userDoc)
            val currentUsername = userSnap.getString("username")

            // Now safe to perform writes
            txn.delete(usernameDoc)

            // unset username on user profile only if it matches
            if (currentUsername == uname) {
                val userUpdate = mapOf("username" to "", "usernameUpdatedAt" to Timestamp.now().toDate().time)
                txn.set(userDoc, userUpdate, SetOptions.merge())
            }
            null
        }
    }

    /**
     * Ensure the current user has a username. If missing, assigns a username of the form
     * `user_<email_head>` (normalized). If that username is taken, tries suffixes up to a limit.
     * Returns a Task that completes when the username has been ensured/created.
     */
    fun ensureUsernameIfMissing(email: String?): Task<Void> {
        val currentUser = auth.currentUser ?: return Tasks.forException(FirebaseFirestoreException("NOT_AUTHENTICATED", FirebaseFirestoreException.Code.PERMISSION_DENIED))
        val uid = currentUser.uid

        // derive email head
        val rawHead = email?.substringBefore('@') ?: uid.take(8)
        // normalize and truncate to fit limits
        val maxHeadLen = MAX_USERNAME_LENGTH - DEFAULT_PREFIX.length
        var head = normalize(rawHead)
        if (head.length > maxHeadLen) head = head.substring(0, maxHeadLen)

        // ensure head starts with a letter; if not, prefix with 'u'
        if (head.isEmpty() || head[0] !in 'a'..'z') {
            head = "u$head"
            if (head.length > maxHeadLen) head = head.substring(0, maxHeadLen)
        }

        val base = normalize("$DEFAULT_PREFIX$head")

        val userDocRef = firestore.collection(usersColl).document(uid)

        return firestore.runTransaction { txn ->
            val userSnap = txn.get(userDocRef)
            val existing = userSnap.getString("username") ?: ""
            if (existing.isNotBlank()) {
                // user already has a username
                return@runTransaction null
            }

            // try base and suffixes
            for (i in 0..MAX_SUFFIX_TRIES) {
                val candidate = if (i == 0) base else {
                    val suffix = i.toString()
                    val allowedHeadLen = MAX_USERNAME_LENGTH - DEFAULT_PREFIX.length - suffix.length
                    var trimmedHead = head
                    if (trimmedHead.length > allowedHeadLen) trimmedHead = trimmedHead.substring(0, allowedHeadLen)
                    normalize("$DEFAULT_PREFIX$trimmedHead$suffix")
                }

                // validate candidate against pattern
                if (!USERNAME_PATTERN.matcher(candidate).matches()) continue

                val candidateDoc = firestore.collection(usernamesColl).document(candidate)
                val candSnap = txn.get(candidateDoc)
                if (!candSnap.exists() || candSnap.getString("uid") == uid) {
                    // reserve candidate
                    val newData = mapOf("uid" to uid, "createdAt" to Timestamp.now())
                    txn.set(candidateDoc, newData)

                    // update user's profile username field
                    val userUpdate = mapOf("username" to candidate, "usernameUpdatedAt" to Timestamp.now().toDate().time)
                    txn.set(userDocRef, userUpdate, SetOptions.merge())
                    return@runTransaction null
                }
                // else taken by someone else -> try next
            }

            // if no candidate found
            throw FirebaseFirestoreException("USERNAME_TAKEN", FirebaseFirestoreException.Code.ABORTED)
        }
    }
}
