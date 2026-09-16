package com.pulse.budget.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.budget.ui.components.HexIconTile
import com.pulse.budget.ui.components.HexagonShape
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTextField
import com.pulse.budget.viewmodel.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    // Only used during registration — picks a picture to store as the new account's avatar.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) profileImageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and heading always come first, in both Login and Register mode.
        HexIconTile(icon = Icons.Outlined.Bolt, size = 88.dp)
        Spacer(Modifier.height(16.dp))
        Text("PULSE", style = MaterialTheme.typography.headlineMedium, letterSpacing = 4.sp)
        Text(
            "TRACK. CONTROL. EVOLVE.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        if (isRegisterMode) {
            // Profile picture upload sits below the logo and heading, above the form fields.
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(HexagonShape())
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Selected profile picture",
                        modifier = Modifier.size(88.dp).clip(HexagonShape()),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Add profile picture", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (profileImageUri != null) "Tap to change photo" else "Add a profile picture (optional)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))

        PulseTextField(value = username, onValueChange = { username = it; authViewModel.clearError() }, label = "Username")
        Spacer(Modifier.height(12.dp))
        PulseTextField(
            value = password,
            onValueChange = { password = it; authViewModel.clearError(); confirmPasswordError = false },
            label = "Password",
            isPassword = true
        )

        // Confirm password — register mode only.
        if (isRegisterMode) {
            Spacer(Modifier.height(12.dp))
            PulseTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = false },
                label = "Confirm Password",
                isPassword = true
            )
            if (confirmPasswordError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Passwords do not match.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
        } else {
            PulsePrimaryButton(
                text = if (isRegisterMode) "Register" else "Login",
                onClick = {
                    if (isRegisterMode) {
                        if (password != confirmPassword) {
                            confirmPasswordError = true
                            return@PulsePrimaryButton
                        }
                        authViewModel.register(
                            username = username,
                            password = password,
                            profileImageUri = profileImageUri?.toString(),
                            onSuccess = onLoginSuccess
                        )
                    } else {
                        authViewModel.login(username, password, onSuccess = onLoginSuccess)
                    }
                }
            )
        }
        Spacer(Modifier.height(12.dp))

        if (!isRegisterMode) {
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = if (isRegisterMode) "Have an account? Login" else "New here? Register",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable {
                    isRegisterMode = !isRegisterMode
                    authViewModel.clearError()
                    confirmPasswordError = false
                    confirmPassword = ""
                }
        )
    }
}