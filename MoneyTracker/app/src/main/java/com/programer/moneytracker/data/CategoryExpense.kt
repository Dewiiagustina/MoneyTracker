package com.programer.moneytracker.data
data class CategoryExpense(
    val kategoriId: Int?,     // Harus sesuai dengan 'kategoriId' dari SELECT
    val totalAmount: Double   // Harus sesuai dengan 'SUM(amount) as totalAmount'
)
