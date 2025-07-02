package com.programer.moneytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    // Mengambil semua transaksi (tanpa filter bulan/tahun)
    @Query("SELECT * FROM transactions ORDER BY tanggal DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // Mengambil transaksi berdasarkan bulan dan tahun
    @Query("SELECT * FROM transactions WHERE SUBSTR(tanggal, 1, 4) = :year AND SUBSTR(tanggal, 6, 2) = :month ORDER BY tanggal DESC, id DESC")
    fun getTransactionsByMonth(month: String, year: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): Transaction?

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Int)

    // --- FUNGSI UNTUK AGREGASI DATA GRAFIK (Semua Bulan) ---
    @Query("SELECT kategoriId, SUM(amount) as totalAmount FROM transactions WHERE type = 'expense' GROUP BY kategoriId")
    fun getCategoryExpenses(): Flow<List<CategoryExpense>>

    // --- FUNGSI BARU UNTUK AGREGASI DATA GRAFIK (Berdasarkan Bulan dan Tahun) ---
    @Query("SELECT kategoriId, SUM(amount) as totalAmount FROM transactions WHERE type = 'expense' AND SUBSTR(tanggal, 1, 4) = :year AND SUBSTR(tanggal, 6, 2) = :month GROUP BY kategoriId")
    fun getCategoryExpensesByMonth(month: String, year: String): Flow<List<CategoryExpense>>
}
