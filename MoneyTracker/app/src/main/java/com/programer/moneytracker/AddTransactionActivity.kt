package com.programer.moneytracker

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.programer.moneytracker.data.AppDatabase
import com.programer.moneytracker.data.Kategori
import com.programer.moneytracker.data.Transaction
import com.programer.moneytracker.data.TransactionRepository
import com.programer.moneytracker.ui.TransactionViewModel
import com.programer.moneytracker.ui.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var transactionViewModel: TransactionViewModel

    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var etTanggal: EditText
    private lateinit var rgType: RadioGroup
    private lateinit var rbIncome: RadioButton
    private lateinit var rbExpense: RadioButton
    private lateinit var spinnerKategori: Spinner
    private lateinit var btnAddTransaction: Button

    private var selectedKategoriId: Int? = null
    private var currentKategoriList: List<Kategori> = emptyList() // Untuk menyimpan daftar kategori saat ini

    // Variabel untuk menyimpan transaksi yang sedang diedit (jika ada)
    private var currentTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_transaction_title) // Default title

        // Inisialisasi UI komponen
        etDescription = findViewById(R.id.et_description)
        etAmount = findViewById(R.id.et_amount)
        etTanggal = findViewById(R.id.et_tanggal)
        rgType = findViewById(R.id.rg_type)
        rbIncome = findViewById(R.id.rb_income)
        rbExpense = findViewById(R.id.rb_expense)
        spinnerKategori = findViewById(R.id.spinner_kategori)
        btnAddTransaction = findViewById(R.id.btn_add_transaction)

        // Inisialisasi ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(database.transactionDao(), database.kategoriDao())
        val viewModelFactory = TransactionViewModelFactory(repository)
        transactionViewModel = ViewModelProvider(this, viewModelFactory)[TransactionViewModel::class.java]

        // --- Cek apakah ada objek Transaction yang dikirim (mode edit) ---
        currentTransaction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(MainActivity.EXTRA_TRANSACTION, Transaction::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(MainActivity.EXTRA_TRANSACTION)
        }

        if (currentTransaction != null) {
            // Mode Edit: Isi UI dengan data transaksi yang sudah ada
            supportActionBar?.title = getString(R.string.dialog_edit_title) // Ubah judul ke "Edit Transaksi"
            btnAddTransaction.text = getString(R.string.button_save) // Ubah teks tombol ke "Simpan"

            etDescription.setText(currentTransaction?.description)
            etAmount.setText(currentTransaction?.amount.toString())
            etTanggal.setText(currentTransaction?.tanggal)

            if (currentTransaction?.type == "income") {
                rbIncome.isChecked = true
            } else {
                rbExpense.isChecked = true
            }
            // Logic untuk Spinner kategori akan ditangani oleh observer `updateKategoriSpinner`
        } else {
            // Mode Tambah: Set tanggal hari ini secara default
            val defaultDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            etTanggal.setText(defaultDate)
        }


        // --- Logika DatePicker ---
        etTanggal.setOnClickListener {
            showDatePickerDialog(etTanggal)
        }

        // --- Logika Spinner Kategori ---
        rgType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.rb_income) "income" else "expense"
            transactionViewModel.incomeKategori.removeObservers(this)
            transactionViewModel.expenseKategori.removeObservers(this)

            if (type == "income") {
                transactionViewModel.incomeKategori.observe(this) { kategoriList ->
                    updateKategoriSpinner(kategoriList, currentTransaction?.kategoriId)
                }
            } else {
                transactionViewModel.expenseKategori.observe(this) { kategoriList ->
                    updateKategoriSpinner(kategoriList, currentTransaction?.kategoriId)
                }
            }
        }
        // Panggil ini sekali di awal untuk mengisi spinner default (pengeluaran) atau berdasarkan transaksi edit
        if (currentTransaction == null || currentTransaction?.type == "expense") {
            rgType.check(R.id.rb_expense)
        } else {
            rgType.check(R.id.rb_income)
        }


        // --- Listener untuk Tombol "Tambah" atau "Simpan" ---
        btnAddTransaction.setOnClickListener {
            val description = etDescription.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val tanggal = etTanggal.text.toString().trim()
            val type = if (rgType.checkedRadioButtonId == R.id.rb_income) "income" else "expense"

            if (description.isEmpty() || amountStr.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(this, "Deskripsi, jumlah, dan tanggal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Jumlah tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedKategoriId == null) {
                Toast.makeText(this, "Pilih kategori", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentTransaction != null) {
                // Mode Edit: Perbarui transaksi yang sudah ada
                val updatedTransaction = currentTransaction!!.copy( // Menggunakan !! karena sudah dipastikan tidak null
                    description = description,
                    amount = amount,
                    tanggal = tanggal,
                    type = type,
                    kategoriId = selectedKategoriId
                )
                transactionViewModel.updateTransaction(updatedTransaction)
            } else {
                // Mode Tambah: Tambah transaksi baru
                transactionViewModel.addTransaction(description, amount, type, selectedKategoriId)
            }

            // Set hasil OK dan tutup Activity
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate))
            }, year, month, day)
        datePickerDialog.show()
    }

    private fun updateKategoriSpinner(kategoriList: List<Kategori>?, currentKategoriId: Int?) {
        kategoriList?.let { list ->
            currentKategoriList = list
            val kategoriNames = list.map { it.namaKategori }.toMutableList()
            if (kategoriNames.isEmpty()) {
                kategoriNames.add(getString(R.string.no_category_available))
            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                kategoriNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategori.adapter = adapter

            // Set pilihan spinner ke kategori yang sesuai dengan transaksi yang diedit
            if (currentKategoriId != null && list.isNotEmpty()) {
                val index = list.indexOfFirst { it.idKategori == currentKategoriId }
                if (index != -1) {
                    spinnerKategori.setSelection(index)
                    selectedKategoriId = currentKategoriId
                } else if (list.isNotEmpty()) {
                    // Jika kategori lama tidak ada, pilih item pertama
                    spinnerKategori.setSelection(0)
                    selectedKategoriId = list[0].idKategori
                } else {
                    selectedKategoriId = null
                }
            } else if (list.isNotEmpty()) {
                // Jika tidak ada kategori yang dipilih, pilih item pertama secara default
                spinnerKategori.setSelection(0)
                selectedKategoriId = list[0].idKategori
            } else {
                selectedKategoriId = null
            }

            spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (list.isNotEmpty()) {
                        selectedKategoriId = list[position].idKategori
                    } else {
                        selectedKategoriId = null
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedKategoriId = null
                }
            }
        }
    }
}