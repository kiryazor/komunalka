package com.example.komunalka.data.dao

import androidx.room.*
import com.example.komunalka.data.Apartment
import kotlinx.coroutines.flow.Flow

@Dao
interface ApartmentDao {
    @Query("SELECT * FROM apartments ORDER BY name ASC")
    fun observeAll(): Flow<List<Apartment>>

    @Query("SELECT * FROM apartments WHERE id = :id")
    suspend fun getById(id: Long): Apartment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apartment: Apartment): Long

    @Update
    suspend fun update(apartment: Apartment)

    @Delete
    suspend fun delete(apartment: Apartment)

    @Query("SELECT COUNT(*) FROM apartments")
    suspend fun count(): Int
}
