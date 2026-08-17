package com.example.komunalka.data.dao

import androidx.room.*
import com.example.komunalka.data.UtilityType
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityTypeDao {
    @Query("SELECT * FROM utility_types ORDER BY name ASC")
    fun observeAll(): Flow<List<UtilityType>>

    @Query("SELECT * FROM utility_types ORDER BY name ASC")
    suspend fun getAllOnce(): List<UtilityType>

    @Query("SELECT * FROM utility_types WHERE id = :id")
    suspend fun getById(id: Long): UtilityType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: UtilityType): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<UtilityType>)

    @Update
    suspend fun update(type: UtilityType)

    @Delete
    suspend fun delete(type: UtilityType)
}
