package com.pulse.budget.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.budget.ui.theme.MonoBody

/** 0dp radius, 1dp border card — the base building block for every panel in the app. */
@Composable
fun SharpCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun PulsePrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text.uppercase(), fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}

@Composable
fun PulseOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PulseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label.uppercase()) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        textStyle = if (isMonospace) MonoBody else MaterialTheme.typography.bodyLarge,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            KeyboardOptions.Default
        }
    )
}

/** Shared top bar: screen title + theme toggle, used on every screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTopBar(title: String, isDark: Boolean, onToggleTheme: () -> Unit, onBack: (() -> Unit)? = null) {
    CenterAlignedTopAppBar(
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
            } else {
                // App logo, shown on every top-level screen (no back button) instead of
                // leaving that space empty.
                HexIconTile(icon = Icons.Outlined.Bolt, size = 40.dp, modifier = Modifier.padding(start = 12.dp))
            }
        },
        actions = {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/** Simple horizontal progress bar with sharp corners, used across Categories, Goals, and XP. */
@Composable
fun SharpProgressBar(progress: Float, modifier: Modifier = Modifier, overLimit: Boolean = false) {
    val trackColor = MaterialTheme.colorScheme.outline
    val fillColor = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(trackColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            if (clamped > 0f) {
                Box(
                    modifier = Modifier
                        .weight(clamped)
                        .height(6.dp)
                        .background(fillColor)
                )
            }
            Box(modifier = Modifier.weight((1f - clamped).coerceAtLeast(0.0001f)))
        }
    }
}