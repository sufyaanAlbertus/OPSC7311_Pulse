package com.pulse.budget.ui.components

/*
 * References:
 *  - NIST (2017) SP 800-63B Digital Identity Guidelines: memorized secret verifiers (block common
 *    passwords, allow show-password). https://pages.nist.gov/800-63-3/sp800-63b.html
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.ui.theme.GoalMinAmber
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.ui.theme.OnTrackGreen
import com.pulse.budget.util.AuthValidation

/** A live strength bar plus a checklist of every password rule. */
@Composable
fun PasswordStrengthMeter(password: String, username: String, modifier: Modifier = Modifier) {
    val strength = AuthValidation.passwordStrength(password, username)
    val rules = AuthValidation.passwordRules(password, username)
    val (label, color) = when (strength) {
        0 -> "Enter a password" to MaterialTheme.colorScheme.onSurfaceVariant
        1 -> "Weak" to MaterialTheme.colorScheme.error
        2 -> "Fair" to GoalMinAmber
        3 -> "Strong" to OnTrackGreen
        else -> "Very strong" to OnTrackGreen
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 4-segment strength bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(if (i < strength) color else MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Password strength: $label", style = MonoSmall, color = color)
        Spacer(Modifier.height(6.dp))
        rules.forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(22.dp)) {
                Icon(
                    imageVector = if (rule.passed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (rule.passed) "Done" else "Not yet",
                    tint = if (rule.passed) OnTrackGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    rule.label,
                    style = MonoSmall,
                    color = if (rule.passed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
