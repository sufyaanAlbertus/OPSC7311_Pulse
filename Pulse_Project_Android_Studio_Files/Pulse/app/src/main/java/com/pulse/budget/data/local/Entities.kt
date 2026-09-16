package com.pulse.budget.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType {
    HOUSING, GROCERIES, TRANSPORT, FOOD_DINING, UTILITIES,
    ENTERTAINMENT, HEALTH, SHOPPING, EDUCATION, INCOME, SAVINGS, OTHER
}

enum class RecurrenceType { NONE, WEEKLY, MONTHLY }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val createdAtEpochDay: Long,
    /** Content Uri (as a String) of the user's chosen profile picture, null if never set. */
    val profileImageUri: String? = null
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Owning account. Every query against this table must filter on this column — without it,
     *  one account's expenses leak into another account's screens (this is what caused data
     *  from a previous login to show up after logging out and registering a new user). */
    val userId: Long,
    val amount: Double,
    val category: CategoryType,
    val dateEpochDay: Long,
    /** e.g. "14:30" — brief (Part 2) requires a start and end time per entry, not just a date. */
    val startTime: String = "",
    val endTime: String = "",
    val description: String,
    val receiptUri: String? = null,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    /** Non-null when this expense was logged against a user-created category rather than the
     *  built-in CategoryType set. When set, `category` is stored as CategoryType.OTHER so every
     *  existing query keeps working, and the UI displays customCategoryName instead. */
    val customCategoryName: String? = null
)

/** User-created categories, on top of the fixed CategoryType set (Part 2 brief: "the user must
 *  be able to create categories"). Displayed with the OTHER hexagon icon. */
@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Owning account — see ExpenseEntity.userId for why this matters. */
    val userId: Long,
    val name: String
)

/** Per-user, per-category spend limit. Primary key is (userId, category) rather than just
 *  category, so each account can set its own limits without overwriting another account's. */
@Entity(tableName = "category_limits", primaryKeys = ["userId", "category"])
data class CategoryLimitEntity(
    val userId: Long,
    val category: CategoryType,
    val limit: Double
)

/** Primary key is (userId, id) rather than just id, so each account gets its own copy of the
 *  badge catalog and earns badges independently. */
@Entity(tableName = "badges", primaryKeys = ["userId", "id"])
data class BadgeEntity(
    val userId: Long,
    val id: String,
    val title: String,
    val earned: Boolean = false,
    val earnedEpochDay: Long? = null
)