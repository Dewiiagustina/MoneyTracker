package com.programer.moneytracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.programer.moneytracker.data.Transaction
import com.programer.moneytracker.ui.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val viewModel: TransactionViewModel // <--- Tambahkan ViewModel untuk akses getNamaKategori
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_item_description)
        private val tvAmountType: TextView = itemView.findViewById(R.id.tv_item_amount_type) // Ubah ID
        private val tvKategoriTanggal: TextView = itemView.findViewById(R.id.tv_item_kategori_tanggal) // Tambah ID baru
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(transaction: Transaction) {
            tvDescription.text = transaction.description

            val amountText = "Rp ${"%,.0f".format(transaction.amount)}"
            if (transaction.type == "income") {
                tvAmountType.text = "$amountText (Pemasukan)"
                tvAmountType.setTextColor(Color.parseColor("#4CAF50")) // Hijau
            } else {
                tvAmountType.text = "$amountText (Pengeluaran)"
                tvAmountType.setTextColor(Color.parseColor("#F44336")) // Merah
            }

            // Dapatkan nama kategori secara asinkron
            CoroutineScope(Dispatchers.Main).launch { // Gunakan Main dispatcher untuk update UI
                val kategoriName = viewModel.getNamaKategori(transaction.kategoriId)
                val kategoriText = kategoriName ?: "Tidak Diketahui" // Jika null, tampilkan "Tidak Diketahui"
                tvKategoriTanggal.text = "Kategori: $kategoriText - Tanggal: ${transaction.tanggal}"
            }


            btnEdit.setOnClickListener { onEditClick(transaction) }
            btnDelete.setOnClickListener { onDeleteClick(transaction) }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}