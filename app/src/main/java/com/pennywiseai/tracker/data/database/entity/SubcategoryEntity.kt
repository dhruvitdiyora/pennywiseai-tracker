package com.pennywiseai.tracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * One level of taxonomy under [CategoryEntity] (#374).
 *
 * Note the deliberate asymmetry with how transactions attach to this: a
 * subcategory belongs to its parent by **foreign key**, but a transaction refers
 * to a subcategory by **name** (`TransactionEntity.subcategory`, doc 12) — the
 * same way it already refers to its category. That is not an oversight. It means
 * the SMS parser can write a name with no database round-trip, renaming a
 * category never has to rewrite transaction rows, and a backup import cannot
 * fail on a dangling foreign key.
 */
@Entity(
    tableName = "subcategories",
    indices = [
        Index(value = ["category_id"]),
        // Unique *within* a parent, not globally: "Monthly" under both Bills and
        // Subscriptions is legitimate, two "Coffee" under Food & Dining is not.
        Index(value = ["category_id", "name"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class SubcategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /*
     * `categoryId` and `name` carry defaults even though a row with
     * `categoryId = 0` is nonsense. Hard constraint 3 is absolute: without them a
     * backup written by a newer build refuses to restore at all (#414), and a
     * nonsense row that imports and can be cleaned up beats a restore that fails.
     */
    @ColumnInfo(name = "category_id")
    val categoryId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    /** Drawable name, resolved via `IconResolutionUtils`. Never a resource id. */
    @ColumnInfo(name = "icon_name", defaultValue = "")
    val iconName: String = "",

    /** Legacy raw resource id. **Read-only — never write this.** See [CategoryEntity]. */
    @ColumnInfo(name = "icon_res_id", defaultValue = "0")
    val iconResId: Int = 0,

    @ColumnInfo(name = "color", defaultValue = "#757575")
    val color: String = "#757575",

    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,

    /** Null for user-created rows — how reset knows there is nothing to restore. */
    @ColumnInfo(name = "default_name")
    val defaultName: String? = null,

    @ColumnInfo(name = "default_icon_name")
    val defaultIconName: String? = null,

    @ColumnInfo(name = "default_color")
    val defaultColor: String? = null,

    @ColumnInfo(name = "created_at")
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
