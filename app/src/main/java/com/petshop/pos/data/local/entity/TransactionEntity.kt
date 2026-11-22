package com.petshop.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Long,
    val taxRate: Double = 0.0, // e.g. 0.05 for 5%
    val taxAmount: Long = 0,
    val discountAmount: Long = 0,
    val grandTotal: Long,
    val paymentMethod: PaymentMethod,
    val cashTendered: Long = 0,
    val changeGiven: Long = 0,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val cashierName: String = "Staff",
    val notes: String = ""
)
