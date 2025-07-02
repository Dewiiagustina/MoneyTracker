package com.programer.moneytracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val kategoriDao: KategoriDao
) {
    // Fungsi untuk mendapatkan semua transaksi (tanpa filter bulan/tahun)
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Fungsi baru untuk mendapatkan transaksi berdasarkan bulan dan tahun
    fun getTransactionsByMonthAndYear(month: String, year: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByMonth(month, year)
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getAllKategori(): Flow<List<Kategori>> {
        return kategoriDao.getAllKategori()
    }

    fun getKategoriByType(type: String): Flow<List<Kategori>> {
        return kategoriDao.getKategoriByType(type)
    }

    suspend fun getNamaKategoriById(id: Int?): String? {
        return kategoriDao.getNamaKategoriById(id)
    }

    suspend fun insertKategori(kategori: Kategori) {
        kategoriDao.insert(kategori)
    }

    // --- FUNGSI UNTUK AGREGASI DATA GRAFIK (Semua Bulan) ---
    fun getAggregatedExpenseByCategory(): Flow<Map<String, Double>> {
        return transactionDao.getCategoryExpenses()
            .combine(kategoriDao.getAllKategori()) { expenseList, kategoriList ->
                val result = mutableMapOf<String, Double>()
                for (expense in expenseList) {
                    val kategoriName = kategoriList.find { it.idKategori == expense.kategoriId }?.namaKategori ?: "Tanpa Kategori"
                    result[kategoriName] = expense.totalAmount
                }
                result
            }
    }

    // --- FUNGSI BARU UNTUK AGREGASI DATA GRAFIK (Berdasarkan Bulan dan Tahun) ---
    fun getAggregatedExpenseByCategoryByMonth(month: String, year: String): Flow<Map<String, Double>> {
        return transactionDao.getCategoryExpensesByMonth(month, year)
            .combine(kategoriDao.getAllKategori()) { expenseList, kategoriList ->
                val result = mutableMapOf<String, Double>()
                for (expense in expenseList) {
                    val kategoriName = kategoriList.find { it.idKategori == expense.kategoriId }?.namaKategori ?: "Tanpa Kategori"
                    result[kategoriName] = expense.totalAmount
                }
                result
            }
    }
}
