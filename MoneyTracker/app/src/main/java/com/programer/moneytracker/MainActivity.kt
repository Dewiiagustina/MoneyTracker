package com.programer.moneytracker

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView // <--- Pastikan import ini ada
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.programer.moneytracker.data.AppDatabase
import com.programer.moneytracker.data.Kategori
import com.programer.moneytracker.data.Transaction
import com.programer.moneytracker.data.TransactionRepository
import com.programer.moneytracker.TransactionAdapter
import com.programer.moneytracker.ui.TransactionViewModel
import com.programer.moneytracker.ui.TransactionViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    private lateinit var rvTransactions: RecyclerView
    private lateinit var fabAddTransaction: Button

    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var tvSaldo: TextView

    private lateinit var spinnerMonthFilter: Spinner
    private lateinit var spinnerYearFilter: Spinner
    private lateinit var bottomNavigationView: BottomNavigationView
    companion object {
        const val EXTRA_TRANSACTION = "extra_transaction"
    }

    private val addTransactionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Operasi dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi UI komponen
        rvTransactions = findViewById(R.id.rv_transactions)
        fabAddTransaction = findViewById(R.id.fab_add_transaction)

        tvTotalPemasukan = findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = findViewById(R.id.tv_total_pengeluaran)
        tvSaldo = findViewById(R.id.tv_saldo)

        spinnerMonthFilter = findViewById(R.id.spinner_month_filter)
        spinnerYearFilter = findViewById(R.id.spinner_year_filter)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(database.transactionDao(), database.kategoriDao())
        val viewModelFactory = TransactionViewModelFactory(repository)
        transactionViewModel = ViewModelProvider(this, viewModelFactory)[TransactionViewModel::class.java]

        transactionAdapter = TransactionAdapter(
            onEditClick = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra(EXTRA_TRANSACTION, transaction)
                addTransactionLauncher.launch(intent)
            },
            onDeleteClick = { transaction -> showDeleteConfirmationDialog(transaction) },
            viewModel = transactionViewModel
        )
        rvTransactions.adapter = transactionAdapter

        transactionViewModel.transactionsByMonthAndYear.observe(this, Observer<List<Transaction>?> { transactions ->
            transactions?.let {
                transactionAdapter.submitList(it)
                updateTotal(it)
            }
        })

        fabAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            addTransactionLauncher.launch(intent)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Sudah di Home, tidak perlu melakukan apa-apa atau refresh
                    true
                }
                R.id.nav_graph -> {
                    val intent = Intent(this, ReportActivity::class.java) // Arahkan ke ReportActivity untuk Graph
                    startActivity(intent)
                    true
                }

                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java) // Arahkan ke ProfileActivity
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Atur item terpilih secara default saat MainActivity dimuat
        bottomNavigationView.selectedItemId = R.id.nav_home // <--- Pastikan baris ini ada

        setupMonthSpinner()
        setupYearSpinner()
    }

    private fun updateTotal(transactions: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.type == "income") {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }

        val saldo = totalIncome - totalExpense

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        currencyFormat.minimumFractionDigits = 0
        currencyFormat.maximumFractionDigits = 2

        tvTotalPemasukan.text = currencyFormat.format(totalIncome)
        tvTotalPengeluaran.text = currencyFormat.format(totalExpense)
        tvSaldo.text = currencyFormat.format(saldo)
    }

    private fun setupMonthSpinner() {
        val months = resources.getStringArray(R.array.months_array).toMutableList()
        months.add(0, getString(R.string.month_all))
 
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months.mapIndexed { index, name ->
            if (index == 0) name else String.format(Locale.getDefault(), "%02d - %s", index, name)
        })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonthFilter.adapter = adapter

        val currentMonth = SimpleDateFormat("MM", Locale.getDefault()).format(Calendar.getInstance().time)
        val monthIndex = currentMonth.toInt()
        spinnerMonthFilter.setSelection(monthIndex)


        spinnerMonthFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val monthValue = if (position == 0) "all" else String.format(Locale.getDefault(), "%02d", position)
                transactionViewModel.setFilterMonth(monthValue)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                transactionViewModel.setFilterMonth("all")
            }
        }
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).map { it.toString() }.toMutableList()
        years.add(0, getString(R.string.year_all))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYearFilter.adapter = adapter

        val yearIndex = years.indexOf(currentYear.toString())
        if (yearIndex != -1) {
            spinnerYearFilter.setSelection(yearIndex + 1)
        } else {
            spinnerYearFilter.setSelection(0)
        }

        spinnerYearFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = parent.getItemAtPosition(position).toString()
                val yearValue = if (position == 0) "all" else selectedYear
                transactionViewModel.setFilterYear(yearValue)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                transactionViewModel.setFilterYear(Calendar.getInstance().get(Calendar.YEAR).toString())
            }
        }
    }


    private fun showEditDialog(transaction: Transaction) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Transaction")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_transaction, null)
        val etEditDescription: EditText = view.findViewById(R.id.et_edit_description)
        val etEditAmount: EditText = view.findViewById(R.id.et_edit_amount)
        val etEditTanggal: EditText = view.findViewById(R.id.et_edit_tanggal)
        val rgEditType: RadioGroup = view.findViewById(R.id.rg_edit_type)
        val rbEditIncome: RadioButton = view.findViewById(R.id.rb_edit_income)
        val rbEditExpense: RadioButton = view.findViewById(R.id.rb_edit_expense)
        val spinnerEditKategori: Spinner = view.findViewById(R.id.spinner_edit_kategori)

        etEditDescription.setText(transaction.description)
        etEditAmount.setText(transaction.amount.toString())
        etEditTanggal.setText(transaction.tanggal)
        if (transaction.type == "income") {
            rbEditIncome.isChecked = true
        } else {
            rbEditExpense.isChecked = true
        }

        etEditTanggal.setOnClickListener {
            showDatePickerDialog(etEditTanggal)
        }

        var editSelectedKategoriId: Int? = transaction.kategoriId
        val editKategoriAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        editKategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEditKategori.adapter = editKategoriAdapter

        val initialType = transaction.type
        fun observeEditKategori(type: String) {
            val liveData = if (type == "income") transactionViewModel.incomeKategori else transactionViewModel.expenseKategori
            liveData.removeObservers(this@MainActivity)
            liveData.observe(this@MainActivity) { kategoriList ->
                kategoriList?.let { list ->
                    val kategoriNames = list.map { it.namaKategori }
                    editKategoriAdapter.clear()
                    editKategoriAdapter.addAll(kategoriNames)
                    editKategoriAdapter.notifyDataSetChanged()

                    val currentKategoriIndex = list.indexOfFirst { it.idKategori == transaction.kategoriId }
                    if (currentKategoriIndex != -1) {
                        spinnerEditKategori.setSelection(currentKategoriIndex)
                        editSelectedKategoriId = list[currentKategoriIndex].idKategori
                    } else if (list.isNotEmpty()) {
                        spinnerEditKategori.setSelection(0)
                        editSelectedKategoriId = list[0].idKategori
                    } else {
                        editSelectedKategoriId = null
                    }

                    spinnerEditKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            if (list.isNotEmpty()) {
                                editSelectedKategoriId = list[position].idKategori
                            } else {
                                editSelectedKategoriId = null
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) { editSelectedKategoriId = null }
                    }
                }
            }
        }
        observeEditKategori(initialType)

        rgEditType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.rb_edit_income) "income" else "expense"
            observeEditKategori(type)
        }

        builder.setView(view)

        builder.setPositiveButton(R.string.button_save) { dialog, _ ->
            val newDescription = etEditDescription.text.toString().trim()
            val newAmountStr = etEditAmount.text.toString().trim()
            val newTanggal = etEditTanggal.text.toString().trim()
            val newType = if (rgEditType.checkedRadioButtonId == R.id.rb_edit_income) "income" else "expense"

            if (newDescription.isEmpty() || newAmountStr.isEmpty() || newTanggal.isEmpty()) {
                Toast.makeText(this, "Deskripsi, jumlah, dan tanggal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (editSelectedKategoriId == null) {
                Toast.makeText(this, "Pilih kategori", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val newAmount = newAmountStr.toDoubleOrNull()
            if (newAmount == null || newAmount <= 0) {
                Toast.makeText(this, "Jumlah tidak valid", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updatedTransaction = transaction.copy(
                description = newDescription,
                amount = newAmount,
                tanggal = newTanggal,
                type = newType,
                kategoriId = editSelectedKategoriId
            )
            transactionViewModel.updateTransaction(updatedTransaction)
            Toast.makeText(this, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.button_cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.create().show()
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, transaction.description))
            .setPositiveButton(R.string.button_delete) { dialog, _ ->
                transactionViewModel.deleteTransaction(transaction)
                Toast.makeText(this, "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
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
}
