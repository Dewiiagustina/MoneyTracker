package com.programer.moneytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kategori")
data class Kategori(
    @PrimaryKey(autoGenerate = true)
    val idKategori: Int = 0,
    val namaKategori: String = "",
    val tipeKategori: String = ""
)
