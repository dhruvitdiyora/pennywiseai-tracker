package com.pennywiseai.tracker.ui.screens.settings

import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileLogicTest {
    private val profiles = listOf(
        ProfileEntity(ProfileEntity.PERSONAL_ID, "Personal", "#1E88E5", 0),
        ProfileEntity(ProfileEntity.BUSINESS_ID, "Business", "#8E24AA", 1),
        ProfileEntity(7L, "Shared", "#43A047", 5),
    )

    @Test
    fun blankAndDuplicateNamesAreRejected() {
        assertEquals(
            ProfileSaveError.BLANK_NAME,
            validateProfileName("   ", profiles, excludedId = null),
        )
        assertEquals(
            ProfileSaveError.DUPLICATE_NAME,
            validateProfileName(" personal ", profiles, excludedId = null),
        )
    }

    @Test
    fun editingCurrentProfileMayKeepItsName() {
        assertNull(
            validateProfileName("Business", profiles, excludedId = ProfileEntity.BUSINESS_ID),
        )
    }

    @Test
    fun newProfileUsesLatestDatabaseIdsAndSortOrder() {
        val created = newProfile(profiles, "  Travel  ", "#F4511E")

        assertEquals(8L, created.id)
        assertEquals(6, created.sortOrder)
        assertEquals("Travel", created.name)
        assertEquals("#F4511E", created.colorHex)
    }
}
