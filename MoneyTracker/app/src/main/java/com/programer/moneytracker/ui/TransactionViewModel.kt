package com.programer.moneytracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.programer.moneytracker.data.Kategori
import com.programer.moneytracker.data.Transaction
import com.programer.moneytracker.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi // <--- PASTIKAN IMPORT INI ADA
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    // --- State untuk Bulan dan Tahun yang Dipilih ---
    private val _selectedMonth = MutableStateFlow(SimpleDateFormat("MM", Locale.getDefault()).format(Date()))
    private val _selectedYear = MutableStateFlow(SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()))

    val selectedMonth: MutableStateFlow<String> = _selectedMonth
    val selectedYear: MutableStateFlow<String> = _selectedYear

    fun setFilterMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setFilterYear(year: String) {
        _selectedYear.value = year
    }

    // --- READ TRANSACTIONS (Difilter berdasarkan Bulan & Tahun) ---
    @OptIn(ExperimentalCoroutinesApi::class) // <--- TAMBAHKAN INI
    val transactionsByMonthAndYear = combine(_selectedMonth, _selectedYear) { month, year ->
        Pair(month, year)
    }.flatMapLatest { (month, year) ->
        if (month == "all") {
            repository.allTransactions
        } else {
            repository.getTransactionsByMonthAndYear(month, year)
        }
    }.asLiveData()

    // --- READ KATEGORI ---
    val allKategori = repository.getAllKategori().asLiveData()
    val incomeKategori = repository.getKategoriByType("income").asLiveData()
    val expenseKategori = repository.getKategoriByType("expense").asLiveData()

    // --- DATA UNTUK GRAFIK (Pengeluaran per Kategori, Difilter Bulan & Tahun) ---
    @OptIn(ExperimentalCoroutinesApi::class) // <--- TAMBAHKAN INI
    val aggregatedExpenseByCategory = combine(_selectedMonth, _selectedYear) { month, year ->
        Pair(month, year)
    }.flatMapLatest { (month, year) ->
        if (month == "all") {
            repository.getAggregatedExpenseByCategory()
        } else {
            repository.getAggregatedExpenseByCategoryByMonth(month, year)
        }
    }.asLiveData()


    // --- CREATE ---
    fun addTransaction(description: String, amount: Double, type: String, kategoriId: Int?) {
        viewModelScope.launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) // Menggunakan Calendar
            val transaction = Transaction(
                tanggal = currentDate,
                description = description,
                amount = amount,
                type = type,
                kategoriId = kategoriId
            )
            repository.insert(transaction)
        }
    }

    // ... (metode updateTransaction, deleteTransaction, deleteTransactionById, addKategori, getNamaKategori) ...
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            val transactionToDelete = repository.getTransactionById(id)
            transactionToDelete?.let {
                repository.delete(it)
            }
        }
    }

    fun addKategori(nama: String, tipe: String) {
        viewModelScope.launch {
            repository.insertKategori(Kategori(namaKategori = nama, tipeKategori = tipe))
        }
    }

    suspend fun getNamaKategori(id: Int?): String? {
        return repository.getNamaKategoriById(id)
    }
}

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
