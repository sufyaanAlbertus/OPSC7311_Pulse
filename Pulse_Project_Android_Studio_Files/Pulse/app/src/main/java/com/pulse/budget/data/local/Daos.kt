package com.pulse.budget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses WHERE userId = :userId AND dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay DESC, id DESC")
    fun getExpensesInRange(userId: Long, startDay: Long, endDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND dateEpochDay BETWEEN :startDay AND :endDay")
    fun getTotalInRange(userId: Long, startDay: Long, endDay: Long): Flow<Double>

    @Query("SELECT category, COALESCE(SUM(amount), 0) as total FROM expenses WHERE userId = :userId AND dateEpochDay BETWEEN :startDay AND :endDay GROUP BY category")
    fun getCategoryTotalsInRange(userId: Long, startDay: Long, endDay: Long): Flow<List<CategoryTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND dateEpochDay = :day")
    fun getTotalForDay(userId: Long, day: Long): Flow<Double>

    @Query("SELECT dateEpochDay as day, COALESCE(SUM(amount), 0) as total FROM expenses WHERE userId = :userId AND dateEpochDay BETWEEN :startDay AND :endDay GROUP BY dateEpochDay")
    fun getDailyTotalsInRange(userId: Long, startDay: Long, endDay: Long): Flow<List<DayTotal>>

    @Query("SELECT DISTINCT dateEpochDay FROM expenses WHERE userId = :userId AND dateEpochDay BETWEEN :startDay AND :endDay")
    fun getDaysWithExpenses(userId: Long, startDay: Long, endDay: Long): Flow<List<Long>>
}

data class CategoryTotal(val category: CategoryType, val total: Double)
data class DayTotal(val day: Long, val total: Double)

@Dao
interface CategoryLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: CategoryLimitEntity)

    @Query("SELECT * FROM category_limits WHERE userId = :userId")
    fun getAll(userId: Long): Flow<List<CategoryLimitEntity>>
}

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(badges: List<BadgeEntity>)

    @Update
    suspend fun update(badge: BadgeEntity)

    @Query("SELECT * FROM badges WHERE userId = :userId")
    fun getAll(userId: Long): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE userId = :userId AND id = :id")
    suspend fun getById(userId: Long, id: String): BadgeEntity?
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("UPDATE users SET profileImageUri = :uri WHERE id = :id")
    suspend fun updateProfileImage(id: Long, uri: String?)
}

@Dao
interface CustomCategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CustomCategoryEntity): Long

    @Query("SELECT * FROM custom_categories WHERE userId = :userId ORDER BY name ASC")
    fun getAll(userId: Long): Flow<List<CustomCategoryEntity>>
}