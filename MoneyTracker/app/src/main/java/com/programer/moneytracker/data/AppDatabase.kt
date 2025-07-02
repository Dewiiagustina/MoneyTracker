package com.programer.moneytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Database(
    entities = [Transaction::class, Kategori::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun kategoriDao(): KategoriDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback() : RoomDatabase.Callback() { // <--- UBAH INI!
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val kategoriDao = database.kategoriDao()
                    // Contoh kategori awal
                    kategoriDao.insert(Kategori(namaKategori = "Gaji", tipeKategori = "income"))
                    kategoriDao.insert(Kategori(namaKategori = "Investasi", tipeKategori = "income"))
                    kategoriDao.insert(Kategori(namaKategori = "Makanan", tipeKategori = "expense"))
                    kategoriDao.insert(Kategori(namaKategori = "Transportasi", tipeKategori = "expense"))
                    kategoriDao.insert(Kategori(namaKategori = "Hiburan", tipeKategori = "expense"))
                    kategoriDao.insert(Kategori(namaKategori = "Belanja", tipeKategori = "expense"))
                    kategoriDao.insert(Kategori(namaKategori = "Lain-lain", tipeKategori = "expense"))
                    kategoriDao.insert(Kategori(namaKategori = "Lain-lain", tipeKategori = "income"))
                }
            }
        }
    }
}
