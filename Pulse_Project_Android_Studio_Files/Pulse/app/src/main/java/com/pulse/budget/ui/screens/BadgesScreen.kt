package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.local.BadgeEntity
import com.pulse.budget.ui.components.HexIconTile
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.viewmodel.ExpenseViewModel

private const val XP_PER_LEVEL = 1000

@Composable
fun BadgesScreen(
    viewModel: ExpenseViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val streakDays by viewModel.streakDays.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val level = (xp / XP_PER_LEVEL) + 1
    val xpIntoLevel = xp % XP_PER_LEVEL
    val xpProgress = xpIntoLevel / XP_PER_LEVEL.toFloat()

    Scaffold(topBar = { PulseTopBar(title = "Badges & Streak", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("Streak", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$streakDays", style = MonoDisplay)
                    Text("Days", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$level", style = MonoDisplay)
                    Spacer(Modifier.height(6.dp))
                    SharpProgressBar(progress = xpProgress)
                    Spacer(Modifier.height(4.dp))
                    Text("$xpIntoLevel / $XP_PER_LEVEL XP", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Earned Badges", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            BadgeGrid(badges = badges.filter { it.earned }, earned = true)

            Spacer(Modifier.height(20.dp))
            Text("Locked Badges", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            BadgeGrid(badges = badges.filter { !it.earned }, earned = false)
        }
    }
}

@Composable
private fun BadgeGrid(badges: List<BadgeEntity>, earned: Boolean) {
    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxWidth().height(80.dp * (badges.size / 4 + 1))) {
        items(badges) { badge ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                HexIconTile(
                    icon = if (earned) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                    borderColor = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    iconTint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(4.dp))
                Text(badge.title, style = MonoSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}