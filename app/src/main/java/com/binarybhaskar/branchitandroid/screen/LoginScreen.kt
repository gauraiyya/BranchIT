package com.binarybhaskar.branchitandroid.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binarybhaskar.branchitandroid.R
//import com.binarybhaskar.branchitandroid.notifications.NotificationSettingsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.core.net.toUri
import androidx.core.content.edit

const val TOS_LINK =
    "bhaskarpatel.me"

@Composable
fun LoginScreen(prefs: SharedPreferences, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repo = com.binarybhaskar.branchitandroid.data.UsernameRepository(
        com.google.firebase.firestore.FirebaseFirestore.getInstance(), auth
    )
    var isLoading by remember { mutableStateOf(false) }
    val handleLoginSuccess: () -> Unit = {
        val user = auth.currentUser
        if (user != null) {
            repo.ensureUsernameIfMissing(user.email)
                .addOnSuccessListener { _: Void? -> onLoginSuccess() }
                .addOnFailureListener { _: Exception -> onLoginSuccess() }
        } else {
            onLoginSuccess()
        }
    }
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                firebaseAuthWithGoogle(account, auth, prefs, handleLoginSuccess)
            } catch (_: Exception) {
                isLoading = false
            }
        }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_branchit),
            contentDescription = "BranchIT Logo",
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
            modifier = Modifier.size(width = 160.dp, height = 160.dp)
        ) // BranchIT Logo
        Spacer(modifier = Modifier.size(18.dp))
        Text(
            text = "Unlock Your\nCommunity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W900,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            color = Color(0xFF2F2F2F),
            textAlign = TextAlign.Center,
        ) // Unlock Your Community
        Spacer(modifier = Modifier.size(18.dp))
        Text(
            text = "Connect, Explore, Grow.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.Gray,
        ) // Connect, Learn, Grow.
        Spacer(modifier = Modifier.size(18.dp))
        Box(modifier = Modifier, contentAlignment = Alignment.Center) {
            GoogleSignInButton(isLoading, onClick = {
                isLoading = true
                launcher.launch(googleSignInClient.signInIntent)
            })
        }
        Spacer(modifier = Modifier.size(18.dp))
        Text(
            buildAnnotatedString {
                append("By signing in, you agree to our ")
                withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("Terms of Service") }
                append(" and ")
                withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("Privacy Policy") }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF888888),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW, TOS_LINK.toUri()
                    )
                    context.startActivity(intent)
                })
        ) // Terms of Service and Privacy Policy
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            "This app is not affiliated with GGV.",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) // Not affiliated with GGV
    }
}

private fun firebaseAuthWithGoogle(
    acct: GoogleSignInAccount?,
    auth: FirebaseAuth,
    prefs: SharedPreferences,
    onLoginSuccess: () -> Unit
) {
    if (acct == null) return
    val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
    auth.signInWithCredential(credential).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            prefs.edit { putBoolean("is_logged_in", true) }
            // Now that user is available, sync topic subscriptions based on saved preferences
//            NotificationSettingsManager.sync(prefs)
            onLoginSuccess()
        }
    }
}

@Composable
fun GoogleSignInButton(
    isLoading: Boolean, onClick: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFFEA4335), // Google Red
        Color(0xFF4285F4), // Google Blue
        Color(0xFFFBBC05), // Google Yellow
        Color(0xFF34A853)  // Google Green
    )

    Box(
        modifier = Modifier
            .width(240.dp)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(32.dp),
            )
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White, contentColor = Color.Black
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo_48),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLoading) "Signing in..." else "Login with Google",

                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}