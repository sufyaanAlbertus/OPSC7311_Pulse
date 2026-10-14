package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.model.Badge
import com.pulse.budget.ui.components.HexIconTile
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.ui.theme.OnTrackGreen
import com.pulse.budget.util.BadgeStats
import com.pulse.budget.util.XP_PER_LEVEL
import com.pulse.budget.util.badgeProgress
import com.pulse.budget.util.levelForXp
import com.pulse.budget.util.xpToNextLevel
import com.pulse.budget.viewmodel.ExpenseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Own feature #1 — gamification. */
@Composable
fun BadgesScreen(
    viewModel: ExpenseViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val streakDays by viewModel.streakDays.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val stats by viewModel.badgeStats.collectAsState()

    val level = levelForXp(xp)
    val xpIntoLevel = xp % XP_PER_LEVEL
    val xpProgress = xpIntoLevel / XP_PER_LEVEL.toFloat()
    val earned = badges.filter { it.earned }
    // Locked badges sorted so the one the user is closest to finishing comes first.
    val locked = badges.filter { !it.earned }
        .sortedByDescending { badgeProgress(it.id, stats)?.fraction ?: 0f }

    Scaffold(topBar = { PulseTopBar(title = "Badges & Streak", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Streak and level
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("Streak", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$streakDays", style = MonoDisplay)
                    Text(if (streakDays == 1) "day in a row" else "days in a row", style = MonoSmall)
                }
                SharpCard(modifier = Modifier.weight(1f)) {
                    Text("Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$level", style = MonoDisplay)
                    Spacer(Modifier.height(4.dp))
                    SharpProgressBar(progress = xpProgress)
                    Spacer(Modifier.height(4.dp))
                    Text("$xpIntoLevel / $XP_PER_LEVEL XP", style = MonoSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${xpToNextLevel(xp)} XP to level ${level + 1}",
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Locked badges
            Spacer(Modifier.height(24.dp))
            Text(
                "Locked (${locked.size})",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(10.dp))
            if (locked.isEmpty()) {
                Text("All badges unlocked.", style = MaterialTheme.typography.bodyMedium)
            }
            locked.forEach { badge ->
                BadgeRow(badge = badge, stats = stats)
                Spacer(Modifier.height(10.dp))
            }

            // Earned badges
            Spacer(Modifier.height(14.dp))
            Text(
                "Earned (${earned.size} of ${badges.size})",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(10.dp))
            if (earned.isEmpty()) {
                Text(
                    "None yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            earned.forEach { badge ->
                BadgeRow(badge = badge, stats = stats)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private val EARNED_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun BadgeRow(badge: Badge, stats: BadgeStats) {
    val progress = badgeProgress(badge.id, stats)
    val accent = if (badge.earned) OnTrackGreen else MaterialTheme.colorScheme.primary
    val readOut = when {
        badge.earned -> "${badge.title}, earned. ${badge.description}"
        progress != null -> "${badge.title}, locked. ${badge.description} Progress ${progress.current} of ${progress.target} ${progress.unit}."
        else -> "${badge.title}, locked. ${badge.description}"
    }

    SharpCard(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = readOut },
        padding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            HexIconTile(
                icon = if (badge.earned) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                size = 44.dp,
                borderColor = if (badge.earned) OnTrackGreen else MaterialTheme.colorScheme.outline,
                iconTint = if (badge.earned) OnTrackGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(badge.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(if (badge.earned) "EARNED" else "LOCKED", style = MonoSmall, color = accent)
                }
                // The unlock rule, always visible — no need to tap to find out.
                Text(badge.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (badge.earned) {
                    badge.earnedEpochDay?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Earned on ${LocalDate.ofEpochDay(it).format(EARNED_DATE)}", style = MonoSmall, color = OnTrackGreen)
                    }
                } else if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    SharpProgressBar(progress = progress.fraction)
                    Spacer(Modifier.height(4.dp))
                    Text("${progress.current} / ${progress.target} ${progress.unit}", style = MonoSmall)
                }
            }
        }
    }
}
