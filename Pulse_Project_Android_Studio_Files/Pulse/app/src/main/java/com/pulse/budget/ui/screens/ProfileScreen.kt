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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulse.budget.ui.components.HexIconTile
import com.pulse.budget.ui.components.HexagonShape
import com.pulse.budget.ui.components.PulseOutlinedButton
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.AuthViewModel
import com.pulse.budget.viewmodel.ExpenseViewModel
import java.time.LocalDate

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    expenseViewModel: ExpenseViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val username by authViewModel.currentUsername.collectAsState()
    val memberSinceEpochDay by authViewModel.memberSinceEpochDay.collectAsState()
    val profileImageUri by authViewModel.currentProfileImageUri.collectAsState()
    val streakDays by expenseViewModel.streakDays.collectAsState()
    val xp by expenseViewModel.xp.collectAsState()
    val monthTotal by expenseViewModel.monthTotal.collectAsState()
    val monthExpenses by expenseViewModel.monthExpenses.collectAsState()

    val memberSinceLabel = memberSinceEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "—"

    // Tap the avatar at any time (not just during registration) to change the profile picture.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) authViewModel.updateProfilePicture(uri.toString()) }

    Scaffold(topBar = { PulseTopBar(title = "Profile", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(HexagonShape())
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(72.dp).clip(HexagonShape()),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    HexIconTile(icon = Icons.Outlined.Bolt, size = 72.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Tap to change photo", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))
            Text(username ?: "—", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(2.dp))
            Text("Member since $memberSinceLabel", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("Streak", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$streakDays", style = MonoDisplay)
                }
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("XP", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$xp", style = MonoDisplay)
                }
            }

            Spacer(Modifier.height(12.dp))
            SharpCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("This month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatZar(monthTotal), style = MonoBody)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Expenses logged", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${monthExpenses.size}", style = MonoBody)
                }
            }

            Spacer(Modifier.height(28.dp))
            PulseOutlinedButton(text = "Log out", onClick = { authViewModel.logout() })
        }
    }
}