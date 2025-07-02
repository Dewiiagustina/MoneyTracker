package com.programer.moneytracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.programer.moneytracker.data.AppDatabase
import com.programer.moneytracker.data.TransactionRepository
import com.programer.moneytracker.ui.TransactionViewModel
import com.programer.moneytracker.ui.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var pieChart: PieChart
    private lateinit var tvNoChartData: TextView

    // Spinner filter bulan dan tahun
    private lateinit var spinnerMonthFilter: Spinner
    private lateinit var spinnerYearFilter: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.report_page_title)

        // Inisialisasi ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(database.transactionDao(), database.kategoriDao())
        val viewModelFactory = TransactionViewModelFactory(repository)
        transactionViewModel = ViewModelProvider(this, viewModelFactory)[TransactionViewModel::class.java]

        // Inisialisasi PieChart dan TextView no data
        pieChart = findViewById(R.id.pie_chart_expense)
        tvNoChartData = findViewById(R.id.tv_no_chart_data)

        // Inisialisasi Spinner filter
        spinnerMonthFilter = findViewById(R.id.spinner_month_filter)
        spinnerYearFilter = findViewById(R.id.spinner_year_filter)

        // Konfigurasi dasar PieChart
        setupPieChartBasic()

        // Amati data agregasi untuk grafik
        transactionViewModel.aggregatedExpenseByCategory.observe(this, Observer<Map<String, Double>?> { aggregatedData ->
            if (aggregatedData != null && aggregatedData.isNotEmpty()) {
                Log.d("ReportActivity", "Aggregated Expense Data: $aggregatedData")
                tvNoChartData.visibility = View.GONE
                pieChart.visibility = View.VISIBLE
                setupPieChartData(aggregatedData)
            } else {
                Log.d("ReportActivity", "No expense data to display chart.")
                tvNoChartData.visibility = View.VISIBLE
                pieChart.visibility = View.GONE
            }
        })

        // --- Setup Spinner Bulan dan Tahun ---
        setupMonthSpinner()
        setupYearSpinner()
    }

    // Tangani tombol kembali di ActionBar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // --- Fungsi untuk Spinner Bulan (sama seperti di MainActivity) ---
    private fun setupMonthSpinner() {
        val months = resources.getStringArray(R.array.months_array).mapIndexed { index, name ->
            if (index == 0) name else String.format(Locale.getDefault(), "%02d - %s", index, name)
        }.toMutableList()
        months.add(0, getString(R.string.month_all)) // Tambahkan "Semua Bulan" di awal

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonthFilter.adapter = adapter

        // Set pilihan default ke bulan saat ini
        val currentMonth = SimpleDateFormat("MM", Locale.getDefault()).format(Calendar.getInstance().time)
        val monthIndex = currentMonth.toInt()
        spinnerMonthFilter.setSelection(monthIndex) // Set ke bulan saat ini (index 1 = Jan, 12 = Des)


        spinnerMonthFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedMonthText = parent.getItemAtPosition(position).toString()
                val monthValue = if (position == 0) "all" else String.format(Locale.getDefault(), "%02d", position) // "all" atau "01", "02", dst.
                transactionViewModel.setFilterMonth(monthValue)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                transactionViewModel.setFilterMonth("all")
            }
        }
    }

    // --- Fungsi untuk Spinner Tahun (sama seperti di MainActivity) ---
    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).map { it.toString() }.toMutableList() // Rentang tahun

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYearFilter.adapter = adapter

        // Set pilihan default ke tahun saat ini
        val yearIndex = years.indexOf(currentYear.toString())
        if (yearIndex != -1) {
            spinnerYearFilter.setSelection(yearIndex)
        }

        spinnerYearFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = parent.getItemAtPosition(position).toString()
                transactionViewModel.setFilterYear(selectedYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                transactionViewModel.setFilterYear(Calendar.getInstance().get(Calendar.YEAR).toString())
            }
        }
    }

    private fun setupPieChartBasic() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawCenterText(true)
        pieChart.centerText = getString(R.string.total_expense_center_text)
        pieChart.setCenterTextSize(16f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(10f)

        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 0f
        legend.yOffset = 0f
    }

    private fun setupPieChartData(data: Map<String, Double>) {
        val pieEntries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val chartColors = intArrayOf(
            Color.parseColor("#FFC107"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#673AB7"),
            Color.parseColor("#3F51B5")
        )
        var colorIndex = 0

        for ((category, amount) in data) {
            if (amount > 0) {
                pieEntries.add(PieEntry(amount.toFloat(), category))
                colors.add(chartColors[colorIndex % chartColors.size])
                colorIndex++
            }
        }

        if (pieEntries.isEmpty()) {
            tvNoChartData.visibility = View.VISIBLE
            pieChart.visibility = View.GONE
            return
        } else {
            tvNoChartData.visibility = View.GONE
            pieChart.visibility = View.VISIBLE
        }

        val dataSet = PieDataSet(pieEntries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(Color.BLACK)

        pieChart.data = pieData
        pieChart.highlightValues(null)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
