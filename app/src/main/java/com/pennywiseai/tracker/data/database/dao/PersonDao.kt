package com.pennywiseai.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE is_archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getActivePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllPeopleSync(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :personId")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :personId")
    fun getPersonByIdFlow(personId: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun getPersonByNormalizedName(normalizedName: String): PersonEntity?

    @Query("SELECT * FROM people WHERE normalized_name = :normalizedName AND is_archived = 0 ORDER BY id LIMIT 1")
    suspend fun getActivePersonByNormalizedName(normalizedName: String): PersonEntity?

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert
    suspend fun insertPeople(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()
}
