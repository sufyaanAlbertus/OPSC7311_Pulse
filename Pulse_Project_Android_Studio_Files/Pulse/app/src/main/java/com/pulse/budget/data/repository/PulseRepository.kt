package com.pulse.budget.data.repository

/*
 * References:
 *  - Firebase (2025) Get realtime updates with Cloud Firestore. https://firebase.google.com/docs/firestore/query-data/listen
 *  - Firebase (2025) Transactions and batched writes. https://firebase.google.com/docs/firestore/manage-data/transactions
 *  - Kotlin (2025) callbackFlow. https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pulse.budget.data.model.BADGE_CATALOG
import com.pulse.budget.data.model.Badge
import com.pulse.budget.data.model.CategoryGoal
import com.pulse.budget.data.model.CategoryType
import com.pulse.budget.data.model.CustomCategory
import com.pulse.budget.data.model.DEFAULT_MAX_MONTHLY_GOAL
import com.pulse.budget.data.model.DEFAULT_MIN_MONTHLY_GOAL
import com.pulse.budget.data.model.Expense
import com.pulse.budget.data.model.RecurrenceType
import com.pulse.budget.data.model.UserProfile
import com.pulse.budget.util.XP_PER_LOG
import com.pulse.budget.util.nextStreak
import com.pulse.budget.util.recurrenceOccurrences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

private const val TAG = "PulseRepository"

/** The single place the app READS from and WRITES to the online database (Firebase Cloud Firestore). */
class PulseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Collection / document references

    private fun userDoc(uid: String) = db.collection(USERS).document(uid)
    private fun expensesRef(uid: String) = userDoc(uid).collection(EXPENSES)
    private fun receiptsRef(uid: String) = userDoc(uid).collection(RECEIPTS)
    private fun goalsRef(uid: String) = userDoc(uid).collection(CATEGORY_GOALS)
    private fun customCategoriesRef(uid: String) = userDoc(uid).collection(CUSTOM_CATEGORIES)
    private fun badgesRef(uid: String) = userDoc(uid).collection(BADGES)

    // User profile (users/{uid})

    /** WRITE: creates the profile document right after registration. */
    suspend fun createUserProfile(uid: String, username: String, profileImageBase64: String?) {
        val data = mapOf(
            "username" to username,
            "createdAtEpochDay" to LocalDate.now().toEpochDay(),
            "profileImageBase64" to profileImageBase64,
            "minMonthlyGoal" to DEFAULT_MIN_MONTHLY_GOAL,
            "maxMonthlyGoal" to DEFAULT_MAX_MONTHLY_GOAL,
            "streakDays" to 0,
            "xp" to 0,
            "lastLogEpochDay" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )
        userDoc(uid).set(data).await()
        Log.d(TAG, "Firestore WRITE users/$uid (profile created for '$username')")
    }

    /** READ (live): the signed-in user's profile document. */
    fun userProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore READ users/$uid failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.toUserProfile())
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateProfileImage(uid: String, base64: String?) {
        userDoc(uid).set(mapOf("profileImageBase64" to base64), SetOptions.merge()).await()
        Log.d(TAG, "Firestore WRITE users/$uid profileImageBase64 (${base64?.length ?: 0} chars)")
    }

    /** WRITE: overall monthly minimum and maximum spending goals. */
    suspend fun setMonthlyGoals(uid: String, min: Double, max: Double) {
        userDoc(uid).set(mapOf("minMonthlyGoal" to min, "maxMonthlyGoal" to max), SetOptions.merge()).await()
        Log.d(TAG, "Firestore WRITE users/$uid monthly goals min=R$min max=R$max")
    }

    /** WRITE (transaction): updates the logging streak and adds XP. */
    suspend fun recordLogEvent(uid: String, todayEpochDay: Long) {
        db.runTransaction { tx ->
            val snap = tx.get(userDoc(uid))
            val lastDay = snap.getLong("lastLogEpochDay")
            val current = snap.getLong("streakDays")?.toInt() ?: 0
            val xp = snap.getLong("xp")?.toInt() ?: 0
            val newStreak = nextStreak(lastDay, todayEpochDay, current)
            tx.set(
                userDoc(uid),
                mapOf("streakDays" to newStreak, "xp" to xp + XP_PER_LOG, "lastLogEpochDay" to todayEpochDay),
                SetOptions.merge()
            )
            Log.d(TAG, "Firestore TX users/$uid streak $current -> $newStreak, xp $xp -> ${xp + XP_PER_LOG}")
            newStreak
        }.await()
    }

    // Expenses (users/{uid}/expenses) + receipts (users/{uid}/receipts)

    /** WRITE: saves an expense and (optionally) its compressed receipt photo in ONE batch. */
    suspend fun addExpense(uid: String, expense: Expense, receiptBase64: String?): String {
        val doc = expensesRef(uid).document()
        val toSave = expense.copy(id = doc.id, hasReceipt = receiptBase64 != null)
        val batch = db.batch()
        batch.set(doc, toSave.toMap())
        if (receiptBase64 != null) {
            batch.set(
                receiptsRef(uid).document(doc.id),
                mapOf("imageBase64" to receiptBase64, "createdAt" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
        Log.d(TAG, "Firestore WRITE users/$uid/expenses/${doc.id} amount=R${expense.amount} " +
                "category=${expense.categoryLabel} recurrence=${expense.recurrence} receipt=${receiptBase64 != null}")
        return doc.id
    }

    /** WRITE: deletes an expense and its receipt (if any). */
    suspend fun deleteExpense(uid: String, expenseId: String) {
        val batch = db.batch()
        batch.delete(expensesRef(uid).document(expenseId))
        batch.delete(receiptsRef(uid).document(expenseId))
        batch.commit().await()
        Log.d(TAG, "Firestore DELETE users/$uid/expenses/$expenseId")
    }

    /** READ (live): expenses whose date falls inside [startDay, endDay]. */
    fun expensesInRange(uid: String, startDay: Long, endDay: Long): Flow<List<Expense>> = callbackFlow {
        val registration = expensesRef(uid)
            .whereGreaterThanOrEqualTo("dateEpochDay", startDay)
            .whereLessThanOrEqualTo("dateEpochDay", endDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore READ expenses [$startDay..$endDay] failed", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toExpense() }
                    .sortedWith(compareByDescending<Expense> { it.dateEpochDay }.thenByDescending { it.startTime })
                Log.d(TAG, "Firestore READ expenses [$startDay..$endDay] -> ${list.size} docs " +
                        "(fromCache=${snapshot?.metadata?.isFromCache})")
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** READ (one-off): the Base64 receipt photo for one expense, or null. */
    suspend fun getReceipt(uid: String, expenseId: String): String? {
        val snap = receiptsRef(uid).document(expenseId).get().await()
        Log.d(TAG, "Firestore READ users/$uid/receipts/$expenseId exists=${snap.exists()}")
        return snap.getString("imageBase64")
    }

    /** Own feature #2 — recurring expenses. */
    suspend fun generateRecurringExpenses(uid: String, today: LocalDate): Int {
        val templates = expensesRef(uid)
            .whereIn("recurrence", listOf(RecurrenceType.WEEKLY.name, RecurrenceType.MONTHLY.name))
            .get().await()
            .documents.mapNotNull { it.toExpense() }
            .filter { it.isRecurringTemplate }

        var created = 0
        for (template in templates) {
            val anchor = LocalDate.ofEpochDay(template.dateEpochDay)
            val lastDone = LocalDate.ofEpochDay(template.lastGeneratedEpochDay ?: template.dateEpochDay)
            val due = recurrenceOccurrences(anchor, template.recurrence, lastDone, today)
            if (due.isEmpty()) continue

            val batch = db.batch()
            due.forEach { date ->
                val id = "${template.id}_${date.toEpochDay()}"
                val copy = template.copy(
                    id = id,
                    dateEpochDay = date.toEpochDay(),
                    recurrence = RecurrenceType.NONE,
                    recurringSourceId = template.id,
                    lastGeneratedEpochDay = null,
                    hasReceipt = false
                )
                batch.set(expensesRef(uid).document(id), copy.toMap())
            }
            batch.update(expensesRef(uid).document(template.id), "lastGeneratedEpochDay", due.last().toEpochDay())
            batch.commit().await()
            created += due.size
            Log.d(TAG, "Recurring: template ${template.id} (${template.recurrence}) -> created ${due.size} " +
                    "copies up to ${due.last()}")
        }
        Log.d(TAG, "Recurring check finished: ${templates.size} templates, $created new expenses")
        return created
    }

    // Category goals (users/{uid}/categoryGoals/{key})

    /** WRITE: minimum and maximum MONTHLY goal for one category. */
    suspend fun setCategoryGoal(uid: String, key: String, min: Double, max: Double) {
        goalsRef(uid).document(key).set(mapOf("key" to key, "minGoal" to min, "maxGoal" to max)).await()
        Log.d(TAG, "Firestore WRITE users/$uid/categoryGoals/$key min=R$min max=R$max")
    }

    /** READ (live) */
    fun categoryGoals(uid: String): Flow<List<CategoryGoal>> = callbackFlow {
        val registration = goalsRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore READ categoryGoals failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { doc ->
                CategoryGoal(
                    key = doc.getString("key") ?: doc.id,
                    minGoal = doc.getDouble("minGoal") ?: 0.0,
                    maxGoal = doc.getDouble("maxGoal") ?: 0.0
                )
            })
        }
        awaitClose { registration.remove() }
    }

    // Custom categories (users/{uid}/customCategories)

    suspend fun addCustomCategory(uid: String, name: String) {
        val clean = name.trim()
        // Document id = lower-case name, so "Gym" and "gym" can't be added twice.
        val id = clean.lowercase().replace("/", "-")
        customCategoriesRef(uid).document(id).set(mapOf("name" to clean)).await()
        Log.d(TAG, "Firestore WRITE users/$uid/customCategories/$id '$clean'")
    }

    fun customCategories(uid: String): Flow<List<CustomCategory>> = callbackFlow {
        val registration = customCategoriesRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore READ customCategories failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty()
                .map { CustomCategory(id = it.id, name = it.getString("name") ?: it.id) }
                .sortedBy { it.name.lowercase() })
        }
        awaitClose { registration.remove() }
    }

    // Badges (users/{uid}/badges) — own feature #1

    /** Adds any catalog badges this account doesn't have yet (safe to call on every login). */
    suspend fun seedBadgesIfMissing(uid: String) {
        val existing = badgesRef(uid).get().await().documents.map { it.id }.toSet()
        val missing = BADGE_CATALOG.filter { it.id !in existing }
        if (missing.isEmpty()) return
        val batch = db.batch()
        missing.forEach { badge ->
            batch.set(
                badgesRef(uid).document(badge.id),
                mapOf("title" to badge.title, "description" to badge.description, "earned" to false, "earnedEpochDay" to null)
            )
        }
        batch.commit().await()
        Log.d(TAG, "Firestore WRITE seeded ${missing.size} badges for users/$uid")
    }

    fun badges(uid: String): Flow<List<Badge>> = callbackFlow {
        val registration = badgesRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore READ badges failed", error)
                return@addSnapshotListener
            }
            val order = BADGE_CATALOG.map { it.id }
            trySend(snapshot?.documents.orEmpty().map { doc ->
                Badge(
                    id = doc.id,
                    title = doc.getString("title") ?: doc.id,
                    description = doc.getString("description") ?: "",
                    earned = doc.getBoolean("earned") ?: false,
                    earnedEpochDay = doc.getLong("earnedEpochDay")
                )
            }.sortedBy { order.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } })
        }
        awaitClose { registration.remove() }
    }

    /** Marks the given badges as earned (if they aren't already). */
    suspend fun unlockBadges(uid: String, badgeIds: Set<String>, todayEpochDay: Long): List<String> {
        if (badgeIds.isEmpty()) return emptyList()
        val current = badgesRef(uid).get().await().documents
        val toUnlock = current.filter { it.id in badgeIds && it.getBoolean("earned") != true }
        if (toUnlock.isEmpty()) return emptyList()
        val batch = db.batch()
        toUnlock.forEach { batch.update(it.reference, mapOf("earned" to true, "earnedEpochDay" to todayEpochDay)) }
        batch.commit().await()
        val titles = toUnlock.map { it.getString("title") ?: it.id }
        Log.d(TAG, "Firestore WRITE badges unlocked for users/$uid: $titles")
        return titles
    }

    // Mapping between Kotlin objects and Firestore documents

    private fun Expense.toMap(): Map<String, Any?> = mapOf(
        "amount" to amount,
        "category" to category.name,
        "customCategoryName" to customCategoryName,
        "dateEpochDay" to dateEpochDay,
        "startTime" to startTime,
        "endTime" to endTime,
        "description" to description,
        "hasReceipt" to hasReceipt,
        "recurrence" to recurrence.name,
        "recurringSourceId" to recurringSourceId,
        "lastGeneratedEpochDay" to lastGeneratedEpochDay,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.toExpense(): Expense? = try {
        Expense(
            id = id,
            amount = getDouble("amount") ?: 0.0,
            category = CategoryType.fromName(getString("category")),
            customCategoryName = getString("customCategoryName"),
            dateEpochDay = getLong("dateEpochDay") ?: 0L,
            startTime = getString("startTime") ?: "",
            endTime = getString("endTime") ?: "",
            description = getString("description") ?: "",
            hasReceipt = getBoolean("hasReceipt") ?: false,
            recurrence = RecurrenceType.fromName(getString("recurrence")),
            recurringSourceId = getString("recurringSourceId"),
            lastGeneratedEpochDay = getLong("lastGeneratedEpochDay")
        )
    } catch (e: Exception) {
        Log.e(TAG, "Skipping malformed expense document $id", e)
        null
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
        uid = id,
        username = getString("username") ?: "",
        createdAtEpochDay = getLong("createdAtEpochDay") ?: 0L,
        profileImageBase64 = getString("profileImageBase64"),
        minMonthlyGoal = getDouble("minMonthlyGoal") ?: DEFAULT_MIN_MONTHLY_GOAL,
        maxMonthlyGoal = getDouble("maxMonthlyGoal") ?: DEFAULT_MAX_MONTHLY_GOAL,
        streakDays = getLong("streakDays")?.toInt() ?: 0,
        xp = getLong("xp")?.toInt() ?: 0,
        lastLogEpochDay = getLong("lastLogEpochDay")
    )

    companion object {
        const val USERS = "users"
        const val EXPENSES = "expenses"
        const val RECEIPTS = "receipts"
        const val CATEGORY_GOALS = "categoryGoals"
        const val CUSTOM_CATEGORIES = "customCategories"
        const val BADGES = "badges"
    }
}
