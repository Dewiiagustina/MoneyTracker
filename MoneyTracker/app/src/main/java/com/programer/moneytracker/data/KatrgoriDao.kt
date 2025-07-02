package com.programer.moneytracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kategori: Kategori)

    @Query("SELECT * FROM kategori")
    fun getAllKategori(): Flow<List<Kategori>>

    @Query("SELECT * FROM kategori WHERE tipeKategori = :type")
    fun getKategoriByType(type: String): Flow<List<Kategori>>

    @Query("SELECT namaKategori FROM kategori WHERE idKategori = :id")
    suspend fun getNamaKategoriById(id: Int?): String?
}