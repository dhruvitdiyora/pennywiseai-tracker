package com.pennywiseai.tracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "color")
    val color: String,
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
    
    @ColumnInfo(name = "is_income")
    val isIncome: Boolean = false,
    
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 999,
    
    @ColumnInfo(name = "created_at")
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    /** Drawable name in `drawable-nodpi`; resolved via `IconResolutionUtils`. */
    @ColumnInfo(name = "icon_name", defaultValue = "")
    val iconName: String = "",

    /**
     * Legacy raw resource id. **Read-only — never write this.** Android renumbers
     * resource ids between builds, so [iconName] is the source of truth. Kept only
     * so a row written by an importer that had an id and no name still renders.
     */
    @ColumnInfo(name = "icon_res_id", defaultValue = "0")
    val iconResId: Int = 0,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    /*
     * The `default*` columns hold what a system category originally looked like, so
     * the user can rename / recolour / re-icon a built-in and still restore it.
     * They are null for user-created categories, which have nothing to restore —
     * that null is also how `resetCategoryToDefault` knows to refuse.
     *
     * Cashiro also stores `default_icon_res_id`; deliberately not ported. Keeping an
     * unstable resource id as a *restore target* is precisely the bug `iconName`
     * exists to avoid.
     */
    @ColumnInfo(name = "default_name")
    val defaultName: String? = null,

    @ColumnInfo(name = "default_color")
    val defaultColor: String? = null,

    @ColumnInfo(name = "default_icon_name")
    val defaultIconName: String? = null,

    @ColumnInfo(name = "default_description")
    val defaultDescription: String? = null
)