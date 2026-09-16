package com.pulse.budget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.local.CategoryType

fun CategoryType.icon(): ImageVector = when (this) {
    CategoryType.HOUSING -> Icons.Outlined.Home
    CategoryType.GROCERIES -> Icons.Outlined.ShoppingCart
    CategoryType.TRANSPORT -> Icons.Outlined.DirectionsCar
    CategoryType.FOOD_DINING -> Icons.Outlined.Restaurant
    CategoryType.UTILITIES -> Icons.Outlined.Bolt
    CategoryType.ENTERTAINMENT -> Icons.Outlined.SportsEsports
    CategoryType.HEALTH -> Icons.Outlined.FavoriteBorder
    CategoryType.SHOPPING -> Icons.Outlined.ShoppingBag
    CategoryType.EDUCATION -> Icons.Outlined.School
    CategoryType.INCOME -> Icons.Outlined.AttachMoney
    CategoryType.SAVINGS -> Icons.Outlined.Savings
    CategoryType.OTHER -> Icons.Outlined.MoreHoriz
}

fun CategoryType.label(): String = when (this) {
    CategoryType.HOUSING -> "Housing"
    CategoryType.GROCERIES -> "Groceries"
    CategoryType.TRANSPORT -> "Transport"
    CategoryType.FOOD_DINING -> "Food & Dining"
    CategoryType.UTILITIES -> "Utilities"
    CategoryType.ENTERTAINMENT -> "Entertainment"
    CategoryType.HEALTH -> "Health"
    CategoryType.SHOPPING -> "Shopping"
    CategoryType.EDUCATION -> "Education"
    CategoryType.INCOME -> "Income"
    CategoryType.SAVINGS -> "Savings"
    CategoryType.OTHER -> "Other"
}

/**
 * Hexagon-outline icon tile. Shared by the category row on Dashboard, the
 * Expense List rows, Category Breakdown, and the badge grids (pass any icon
 * for badges instead of a category).
 */
@Composable
fun HexIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    dashed: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .border(width = 1.dp, color = borderColor, shape = HexagonShape())
            .background(color = Color.Transparent, shape = HexagonShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun CategoryIcon(category: CategoryType, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    HexIconTile(icon = category.icon(), modifier = modifier, size = size)
}
