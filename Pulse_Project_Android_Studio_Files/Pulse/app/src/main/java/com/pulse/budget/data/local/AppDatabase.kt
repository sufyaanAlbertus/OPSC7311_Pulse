package com.pulse.budget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ExpenseEntity::class, CategoryLimitEntity::class, BadgeEntity::class,
        UserEntity::class, CustomCategoryEntity::class
    ],
    // Bumped 4 -> 5: expenses, category_limits, badges and custom_categories all gained a
    // userId column (see Entities.kt) to fix data from one account leaking into another.
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryLimitDao(): CategoryLimitDao
    abstract fun badgeDao(): BadgeDao
    abstract fun userDao(): UserDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulse.db"
                )
                    // No migration path is defined yet for this prototype stage, so a schema
                    // change wipes local data on next install rather than crashing. Replace
                    // with a real Migration before this holds real user data. This also means
                    // the version 4 -> 5 bump above will clear existing local data once on
                    // next run, which is expected and fine for this prototype.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}