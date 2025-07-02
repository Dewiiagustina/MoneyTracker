package com.programer.moneytracker.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = Kategori::class,
        parentColumns = ["idKategori"],
        childColumns = ["kategoriId"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val kategoriId: Int? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(tanggal)
        parcel.writeString(description)
        parcel.writeDouble(amount)
        parcel.writeString(type)
        parcel.writeValue(kategoriId)
    }

    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<Transaction> {
        override fun createFromParcel(parcel: Parcel): Transaction {
            return Transaction(parcel)
        }
        override fun newArray(size: Int): Array<Transaction?> {
            return arrayOfNulls(size)
        }
    }
}