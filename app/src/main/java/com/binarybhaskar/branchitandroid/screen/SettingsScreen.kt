package com.binarybhaskar.branchitandroid.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.binarybhaskar.branchitandroid.R
import com.binarybhaskar.branchitandroid.data.GGVDetails
import com.binarybhaskar.branchitandroid.data.Project
import com.binarybhaskar.branchitandroid.data.SocialLinks
import com.binarybhaskar.branchitandroid.data.UserProfile
import com.binarybhaskar.branchitandroid.data.UserRepository
import com.binarybhaskar.branchitandroid.data.UsernameRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.os.Build
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun SettingsScreen(navController: NavController, prefs: SharedPreferences) {
    val context: Context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
    val scope = rememberCoroutineScope()

    // Local repo and state
    val repo = remember { UserRepository() }
    val usernameRepo = remember { UsernameRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }
    var isSaving by remember { mutableStateOf(false) }

    // Username validation states
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }

    // Departments list from Firebase
    val departments = remember { mutableStateListOf<String>() }
    var isDepartmentsLoading by remember { mutableStateOf(true) }

    // Batch years list (current year to 10 years back)
    val batchYears = remember { (2070 downTo 1970).map { it.toString() } }

    // Pull from memory cache if available so UI paints instantly
    val cached = remember(user?.uid) { UserRepository.getCachedProfile() }

    // Profile editable fields using NEW UserProfile model
    var displayName by remember(user?.uid) {
        mutableStateOf((cached?.displayName ?: (user?.displayName ?: "User")).take(50))
    }
    var username by remember(user?.uid) { mutableStateOf(cached?.username ?: "") }
    var profilePicUrl by remember(user?.uid) {
        mutableStateOf(cached?.profilePicUrl ?: user?.photoUrl?.toString().orEmpty())
    }
    var backgroundPicUrl by remember(user?.uid) {
        mutableStateOf(cached?.backgroundPicUrl.orEmpty())
    }
    var about by remember(user?.uid) { mutableStateOf((cached?.about ?: "").take(1000)) }

    // GGV Details
    var department by remember(user?.uid) { mutableStateOf(cached?.ggvInfo?.department ?: "") }
    var batchYear by remember(user?.uid) { mutableStateOf(cached?.ggvInfo?.batchYear ?: "") }
    var enrolmentNumber by remember(user?.uid) { mutableStateOf(cached?.ggvInfo?.enrolmentNumber ?: "") }

    // Social Links
    var linkedIn by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.linkedIn ?: "") }
    var github by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.github ?: "") }
    var instagram by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.instagram ?: "") }
    var mail by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.mail ?: "") }
    var discord by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.discord ?: "") }
    var reddit by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.reddit ?: "") }
    var website by remember(user?.uid) { mutableStateOf(cached?.socialLinks?.website ?: "") }

    val skills = remember(user?.uid) {
        mutableStateListOf<String>().also { list ->
            cached?.skills?.let { list.addAll(it) }
        }
    }
    var resumeUrl by remember(user?.uid) { mutableStateOf(cached?.resumeUrl ?: "") }

    // Projects (detailed) - up to 5, dynamically managed
    val projects = remember(user?.uid) {
        mutableStateListOf<Project>().also { list ->
            cached?.projects?.let { list.addAll(it) }
        }
    }


    // Image picker for profile
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    try {
                        isSaving = true
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val uid = user?.uid ?: throw IllegalStateException("No user")
                            val ref = FirebaseStorage.getInstance()
                                .reference.child("users/$uid/profile_${System.currentTimeMillis()}.jpg")
                            val uploadTask = ref.putBytes(bytes)
                            val result = uploadTask.await()
                            if (result.task.isSuccessful) {
                                val url = ref.downloadUrl.await().toString()
                                profilePicUrl = url
                                Toast.makeText(context, "Profile image uploaded", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Upload failed: ${result.error?.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to read image", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Upload failed", Toast.LENGTH_LONG)
                            .show()
                    } finally {
                        isSaving = false
                    }
                }
            }
        }

    // Background image picker
    val backgroundPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    try {
                        isSaving = true
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val uid = user?.uid ?: throw IllegalStateException("No user")
                            val ref = FirebaseStorage.getInstance()
                                .reference.child("users/$uid/background_${System.currentTimeMillis()}.jpg")
                            val uploadTask = ref.putBytes(bytes)
                            val result = uploadTask.await()
                            if (result.task.isSuccessful) {
                                val url = ref.downloadUrl.await().toString()
                                backgroundPicUrl = url
                                Toast.makeText(context, "Background image uploaded", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Upload failed: ${result.error?.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to read image", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Upload failed", Toast.LENGTH_LONG)
                            .show()
                    } finally {
                        isSaving = false
                    }
                }
            }
        }

    // PDF picker
    val pdfPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    try {
                        isSaving = true
                        // Read the file content first to avoid "object doesn't exist" error
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            if (bytes.size > 1_000_000) {
                                Toast.makeText(context, "File too large (max 1 MB)", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            val uid = user?.uid ?: throw IllegalStateException("No user")
                            val ref = FirebaseStorage.getInstance()
                                .reference.child("users/$uid/resume_${System.currentTimeMillis()}.pdf")
                            ref.putBytes(bytes).await()
                            val url = ref.downloadUrl.await().toString()
                            resumeUrl = url
                            Toast.makeText(context, "Resume uploaded", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to read file", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Upload failed", Toast.LENGTH_LONG)
                            .show()
                    } finally {
                        isSaving = false
                    }
                }
            }
        }

    // Initial profile snapshot for change detection
    var initialProfile by remember(user?.uid) {
        mutableStateOf(cached ?: UserProfile(uid = user?.uid ?: ""))
    }

    // Permission variables and state
    val imagePermission = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
    val pdfPermission = if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_DOCUMENTS" else android.Manifest.permission.READ_EXTERNAL_STORAGE
    var imagePermissionGranted by remember { mutableStateOf(true) }
    var pdfPermissionGranted by remember { mutableStateOf(true) }
    var pendingPicker by remember { mutableStateOf<String?>(null) } // "image", "background", "pdf"

    val requestImagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        imagePermissionGranted = granted
        if (granted && pendingPicker != null) {
            when (pendingPicker) {
                "image" -> imagePicker.launch("image/*")
                "background" -> backgroundPicker.launch("image/*")
            }
            pendingPicker = null
        } else if (!granted) {
            Toast.makeText(context, "Permission denied. Cannot select image.", Toast.LENGTH_LONG).show()
        }
    }
    val requestPdfPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pdfPermissionGranted = granted
        if (granted && pendingPicker == "pdf") {
            pdfPicker.launch("application/pdf")
            pendingPicker = null
        } else if (!granted) {
            Toast.makeText(context, "Permission denied. Cannot select PDF.", Toast.LENGTH_LONG).show()
        }
    }

    // Load departments from Firebase
    LaunchedEffect(Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("config").document("departments").get().await()
            val deptList = snapshot.get("list") as? List<String>
            if (deptList != null) {
                departments.clear()
                departments.addAll(deptList)
            } else {
                // Fallback to default departments if not configured in Firebase
                departments.addAll(
                    listOf(
                        "Computer Science & Engineering",
                        "Information Technology",
                        "Electronics & Communication Engineering",
                        "Electrical Engineering",
                        "Mechanical Engineering",
                        "Civil Engineering",
                        "Chemical Engineering",
                        "Biotechnology",
                        "Master of Computer Applications (MCA)",
                        "Master of Business Administration (MBA)"
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback to default departments
            departments.addAll(
                listOf(
                    "Computer Science & Engineering",
                    "Information Technology",
                    "Electronics & Communication Engineering",
                    "Electrical Engineering",
                    "Mechanical Engineering",
                    "Civil Engineering",
                    "Chemical Engineering",
                    "Biotechnology",
                    "Master of Computer Applications (MCA)",
                    "Master of Business Administration (MBA)"
                )
            )
        } finally {
            isDepartmentsLoading = false
        }
    }

    // Load profile from cloud (refresh UI if changed)
    LaunchedEffect(user?.uid) {
        if (user == null) {
            // not signed in, nothing to do
        } else {
            try {
                val profile = repo.getOrCreateProfile()
                displayName = profile.displayName.ifBlank { user.displayName ?: "User" }.take(50)
                username = profile.username
                profilePicUrl = profile.profilePicUrl ?: user.photoUrl?.toString().orEmpty()
                backgroundPicUrl = profile.backgroundPicUrl.orEmpty()
                about = (profile.about ?: "").take(1000)

                department = profile.ggvInfo.department
                batchYear = profile.ggvInfo.batchYear
                enrolmentNumber = profile.ggvInfo.enrolmentNumber

                linkedIn = profile.socialLinks.linkedIn ?: ""
                github = profile.socialLinks.github ?: ""
                instagram = profile.socialLinks.instagram ?: ""
                mail = profile.socialLinks.mail ?: ""
                discord = profile.socialLinks.discord ?: ""
                reddit = profile.socialLinks.reddit ?: ""
                website = profile.socialLinks.website ?: ""

                skills.clear(); skills.addAll(profile.skills)
                resumeUrl = profile.resumeUrl ?: ""

                projects.clear()
                projects.addAll(profile.projects)

                initialProfile = profile
            } catch (_: Exception) {
                // swallow; UI shows defaults
            }
        }
    }

    // Detect profile changes
    val profileChanged = remember(
        displayName,
        username,
        profilePicUrl,
        backgroundPicUrl,
        about,
        department,
        batchYear,
        enrolmentNumber,
        linkedIn,
        github,
        instagram,
        mail,
        discord,
        reddit,
        website,
        skills.toList(),
        resumeUrl,
        projects.toList(),
        initialProfile
    ) {
        displayName != initialProfile.displayName ||
        username != initialProfile.username ||
        profilePicUrl != (initialProfile.profilePicUrl ?: "") ||
        backgroundPicUrl != (initialProfile.backgroundPicUrl ?: "") ||
        (about != (initialProfile.about ?: "")) ||
        department != initialProfile.ggvInfo.department ||
        batchYear != initialProfile.ggvInfo.batchYear ||
        enrolmentNumber != initialProfile.ggvInfo.enrolmentNumber ||
        linkedIn != (initialProfile.socialLinks.linkedIn ?: "") ||
        github != (initialProfile.socialLinks.github ?: "") ||
        instagram != (initialProfile.socialLinks.instagram ?: "") ||
        mail != (initialProfile.socialLinks.mail ?: "") ||
        discord != (initialProfile.socialLinks.discord ?: "") ||
        reddit != (initialProfile.socialLinks.reddit ?: "") ||
        website != (initialProfile.socialLinks.website ?: "") ||
        skills.toList() != initialProfile.skills ||
        resumeUrl != (initialProfile.resumeUrl ?: "") ||
        projects.filter { it.title.isNotBlank() } != initialProfile.projects.filter { it.title.isNotBlank() }
    }

    // Dialog state for unsaved changes
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Intercept back navigation if profile changed
    BackHandler(enabled = profileChanged) {
        showUnsavedDialog = true
    }

    fun saveProfile() {
        scope.launch {
            try {
                isSaving = true

                // Prevent empty username
                if (username.isBlank()) {
                    usernameError = "Username cannot be empty"
                    Toast.makeText(context, "Please enter a valid username", Toast.LENGTH_LONG).show()
                    isSaving = false
                    return@launch
                }

                // Validate username if changed
                if (username.isNotBlank() && username != initialProfile.username) {
                    // Check if username is valid
                    val pattern = "^(?=.{3,30}$)(?!.*[._]{2})[a-z][a-z0-9._]+(?<![._])$"
                    if (!username.matches(Regex(pattern))) {
                        usernameError = "Invalid username format"
                        Toast.makeText(context, "Please fix username errors", Toast.LENGTH_LONG).show()
                        isSaving = false
                        return@launch
                    }

                    // Reserve or change username
                    try {
                        if (initialProfile.username.isBlank()) {
                            // Reserve new username
                            usernameRepo.reserveUsername(username).await()
                        } else {
                            // Change username
                            usernameRepo.changeUsername(username).await()
                        }
                    } catch (e: Exception) {
                        val errorMsg = when {
                            e.message?.contains("USERNAME_TAKEN") == true -> "Username already taken"
                            e.message?.contains("INVALID_USERNAME") == true -> "Invalid username format"
                            else -> "Failed to update username: ${e.message}"
                        }
                        usernameError = errorMsg
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val normalizedProjects = projects.filter { it.title.isNotBlank() }
                val profile = UserProfile(
                    uid = user?.uid ?: "",
                    username = username,
                    displayName = displayName,
                    about = about,
                    profilePicUrl = profilePicUrl,
                    backgroundPicUrl = backgroundPicUrl,
                    ggvInfo = GGVDetails(
                        department = department,
                        batchYear = batchYear,
                        enrolmentNumber = enrolmentNumber
                    ),
                    skills = skills.toList(),
                    resumeUrl = resumeUrl,
                    projects = normalizedProjects,
                    socialLinks = SocialLinks(
                        linkedIn = linkedIn.takeIf { it.isNotBlank() },
                        github = github.takeIf { it.isNotBlank() },
                        instagram = instagram.takeIf { it.isNotBlank() },
                        mail = mail.takeIf { it.isNotBlank() },
                        discord = discord.takeIf { it.isNotBlank() },
                        reddit = reddit.takeIf { it.isNotBlank() },
                        website = website.takeIf { it.isNotBlank() }
                    )
                )
                repo.saveProfile(profile)
                // Update baseline so Save button disappears
                initialProfile = profile.copy()
                usernameError = null
                Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }


    // Modern Custom UI with background image
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image with blur
        if (backgroundPicUrl.isNotBlank()) {
            AsyncImage(
                model = backgroundPicUrl,
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Custom top bar with glass effect
                val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                        .padding(statusBarPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable(onClick = {
                                        if (profileChanged) {
                                            showUnsavedDialog = true
                                        } else {
                                            navController.popBackStack()
                                        }
                                    }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go Back",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Settings",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (profileChanged) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(enabled = !isSaving) { saveProfile() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Save",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile header with background and profile pic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    // Background banner
                    if (backgroundPicUrl.isNotBlank()) {
                        AsyncImage(
                            model = backgroundPicUrl,
                            contentDescription = "Profile Background",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Change background button
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .clickable(enabled = !isSaving) {
                                    if (ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED) {
                                        backgroundPicker.launch("image/*")
                                    } else {
                                        pendingPicker = "background"
                                        requestImagePermissionLauncher.launch(imagePermission)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Change Banner",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Profile image
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(
                                            4.dp,
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profilePicUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = profilePicUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.size(96.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = "Default Profile Picture",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                // Change photo mini button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable(enabled = !isSaving) {
                                            if (ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED) {
                                                imagePicker.launch("image/*")
                                            } else {
                                                pendingPicker = "image"
                                                requestImagePermissionLauncher.launch(imagePermission)
                                            }
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "📷",
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (backgroundPicUrl.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (username.isNotBlank()) {
                                    Text(
                                        "@$username",
                                        fontSize = 14.sp,
                                        color = if (backgroundPicUrl.isNotBlank()) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom section: Basic Info
                CustomSection(title = "Basic Information") {
                    CustomTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(50) },
                        label = "Display Name",
                        enabled = !isSaving
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column {
                        CustomTextField(
                            value = username,
                            onValueChange = { newValue ->
                                val trimmed = newValue.trim().lowercase()
                                username = trimmed
                                usernameError = null

                                // Validate format (Instagram-like: a-z, 0-9, dot, underscore; 3-30 chars)
                                if (trimmed.isNotEmpty()) {
                                    val pattern = "^(?=.{3,30}$)(?!.*[._]{2})[a-z][a-z0-9._]+(?<![._])$"
                                    if (!trimmed.matches(Regex(pattern))) {
                                        usernameError = "Invalid format. Use 3-30 chars: lowercase, numbers, dots, underscores. Must start with letter."
                                    } else if (trimmed != initialProfile.username) {
                                        // Check availability
                                        isCheckingUsername = true
                                        scope.launch {
                                            try {
                                                usernameRepo.isUsernameAvailable(trimmed).addOnCompleteListener { task ->
                                                    isCheckingUsername = false
                                                    if (task.isSuccessful) {
                                                        val available = task.result
                                                        if (!available) {
                                                            usernameError = "Username already taken"
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                isCheckingUsername = false
                                            }
                                        }
                                    }
                                }
                            },
                            label = "Username",
                            enabled = !isSaving,
                            prefix = "@"
                        )
                        if (usernameError != null) {
                            Text(
                                usernameError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                        if (isCheckingUsername) {
                            Text(
                                "Checking availability...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column {
                        CustomTextField(
                            value = about,
                            onValueChange = { about = it.take(1000) },
                            label = "About / Bio (${about.length}/1000)",
                            enabled = !isSaving,
                            minLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GGV Details Section
                CustomSection(title = "GGV Details") {
                    var departmentExpanded by remember { mutableStateOf(false) }
                    var batchYearExpanded by remember { mutableStateOf(false) }

                    // Department Dropdown
                    Column {
                        Text(
                            "Department",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isSaving && !isDepartmentsLoading) {
                                        departmentExpanded = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (department.isBlank()) "Select department" else department,
                                    color = if (department.isBlank())
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = departmentExpanded,
                                onDismissRequest = { departmentExpanded = false }
                            ) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept) },
                                        onClick = {
                                            department = dept
                                            departmentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Batch Year Dropdown
                    Column {
                        Text(
                            "Batch Year",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isSaving) {
                                        batchYearExpanded = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (batchYear.isBlank()) "Select batch year" else batchYear,
                                    color = if (batchYear.isBlank())
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = batchYearExpanded,
                                onDismissRequest = { batchYearExpanded = false }
                            ) {
                                batchYears.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            batchYear = year
                                            batchYearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = enrolmentNumber,
                        onValueChange = { enrolmentNumber = it },
                        label = "Enrolment Number",
                        enabled = !isSaving
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Social Links Section
                CustomSection(title = "Social Links") {
                    CustomTextField(
                        value = linkedIn,
                        onValueChange = { linkedIn = it },
                        label = "LinkedIn",
                        enabled = !isSaving,
                        placeholder = "linkedin.com/in/yourname"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = github,
                        onValueChange = { github = it },
                        label = "GitHub",
                        enabled = !isSaving,
                        placeholder = "github.com/yourname"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        label = "Instagram",
                        enabled = !isSaving,
                        placeholder = "instagram.com/yourname"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = "Website",
                        enabled = !isSaving,
                        placeholder = "mysite.com"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = mail,
                        onValueChange = { mail = it },
                        label = "Email",
                        enabled = !isSaving,
                        placeholder = "myname@mysite.com"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skills Section
                CustomSection(title = "Skills") {
                    var newSkill by remember { mutableStateOf("") }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        CustomTextField(
                            value = newSkill,
                            onValueChange = { newSkill = it },
                            label = "Add skill",
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    val s = newSkill.trim()
                                    if (s.isNotEmpty() && s !in skills) {
                                        skills.add(s)
                                        newSkill = ""
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "Add",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(skills.size) { i ->
                            val s = skills[i]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { skills.removeAt(i) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        s,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Projects Section
                CustomSection(title = "Projects (up to 5)") {
                    projects.forEachIndexed { idx, project ->
                        if (idx > 0) Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Project ${idx + 1}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )

                            // Remove button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable(enabled = !isSaving) {
                                        projects.removeAt(idx)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Remove Project",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        CustomTextField(
                            value = project.title,
                            onValueChange = { projects[idx] = project.copy(title = it) },
                            label = "Title",
                            enabled = !isSaving
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        CustomTextField(
                            value = project.description,
                            onValueChange = { projects[idx] = project.copy(description = it) },
                            label = "Description",
                            enabled = !isSaving,
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        CustomTextField(
                            value = project.link ?: "",
                            onValueChange = { projects[idx] = project.copy(link = it) },
                            label = "Link",
                            enabled = !isSaving
                        )
                    }

                    // Add Project Button
                    if (projects.size < 5) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isSaving) {
                                    projects.add(Project())
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Project",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add Project",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resume Section
                CustomSection(title = "Resume") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(enabled = !isSaving) {
                                    pdfPicker.launch("application/pdf")
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                if (resumeUrl.isBlank()) "Upload PDF (< 1 MB)" else "Replace PDF",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (resumeUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable { resumeUrl = "" }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "Remove",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Theme Section
                CustomSection(title = "App Theme") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(
                            selected = !dynamicColor,
                            title = "BranchIT Theme",
                            subtitle = "Blue (Light) • Yellow (Dark)",
                            onClick = {
                                dynamicColor = false
                                prefs.edit { putBoolean("dynamic_color", false) }
                            }
                        )
                        ThemeOption(
                            selected = dynamicColor,
                            title = "Material You",
                            subtitle = "Dynamic colors from wallpaper",
                            onClick = {
                                dynamicColor = true
                                prefs.edit { putBoolean("dynamic_color", true) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Section
                CustomSection(title = "About") {
                    Text(
                        text = "App version ${
                            try {
                                context.packageManager.getPackageInfo(
                                    context.packageName,
                                    0
                                ).versionName
                            } catch (_: Exception) {
                                ""
                            }
                        }",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    val pkg = context.packageName
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "market://details?id=$pkg".toUri()
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://play.google.com/store/apps/details?id=$pkg".toUri()
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Updates",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    val versionName = try {
                                        context.packageManager.getPackageInfo(
                                            context.packageName,
                                            0
                                        ).versionName
                                    } catch (_: Exception) {
                                        ""
                                    }
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:".toUri()
                                        putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf("bhaskar.patel.mail+helpbranchit@gmail.com")
                                        )
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "Feedback for BranchIT App $versionName"
                                        )
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (_: Exception) {
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://www.youtube.com/watch?v=dQw4w9WgXcQ".toUri()
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(webIntent)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Feedback",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            FirebaseAuth
                                .getInstance()
                                .signOut()
                            googleSignInClient.signOut()
                            googleSignInClient.revokeAccess()
                            prefs.edit { putBoolean("is_logged_in", false) }
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Unsaved changes dialog
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("Unsaved Profile Changes") },
                text = { Text("Do you want to continue without saving?") },
                confirmButton = {
                    Button(onClick = {
                        saveProfile()
                        showUnsavedDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    Button(onClick = {
                        showUnsavedDialog = false
                        navController.popBackStack()
                    }) { Text("Cancel and Exit") }
                })
        }
    }
}

// Custom composables for modern UI
@Composable
fun CustomSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prefix: String? = null,
    placeholder: String? = null,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = if (minLines == 1) Alignment.CenterVertically else Alignment.Top
        ) {
            if (prefix != null) {
                Text(
                    prefix,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                },
                minLines = minLines
            )
        }
    }
}

@Composable
fun ThemeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(
                2.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
