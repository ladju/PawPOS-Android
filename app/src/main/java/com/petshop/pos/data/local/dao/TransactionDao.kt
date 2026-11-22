package com.petshop.pos.data.local.dao

import androidx.room.*
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.TransactionEntity
import com.petshop.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

data class TopSellerSummary(
    val productName: String,
    val category: PetCategory,
    val totalQuantity: Int,
    val totalRevenue: Long
)

@Dao
interface TransactionDao {

    // ABORT (not REPLACE): a duplicate invoice number must fail loudly instead of overwriting a sale.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    // ABORT (not REPLACE): a duplicate invoice number must fail loudly instead of overwriting a sale.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getTransactionsSince(sinceTimestamp: Long): List<TransactionEntity>

    @Query("""
        SELECT 
            productName,
            category,
            SUM(quantity) as totalQuantity,
            SUM(subtotal) as totalRevenue
        FROM transaction_items
        GROUP BY productName, category
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    suspend fun getTopSellers(limit: Int = 5): List<TopSellerSummary>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTotalTransactionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE invoiceNumber = :invoiceNumber")
    suspend fun countByInvoice(invoiceNumber: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :since AND timestamp < :until")
    suspend fun countBetween(since: Long, until: Long): Int

    @Query("SELECT * FROM transactions WHERE timestamp >= :since AND timestamp < :until ORDER BY timestamp DESC")
    fun observeBetween(since: Long, until: Long): Flow<List<TransactionEntity>>

    /** Gross item profit (selling - cost) for transactions in the range; discounts are subtracted separately. */
    @Query("""
        SELECT COALESCE(SUM((i.unitPrice - i.costPrice) * i.quantity), 0)
        FROM transaction_items i
        INNER JOIN transactions t ON t.id = i.transactionId
        WHERE t.timestamp >= :since AND t.timestamp < :until
    """)
    fun observeItemProfitBetween(since: Long, until: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM transaction_items i
        INNER JOIN transactions t ON t.id = i.transactionId
        WHERE t.timestamp >= :since AND t.timestamp < :until
    """)
    fun observeItemsSoldBetween(since: Long, until: Long): Flow<Int>

    @Query("""
        SELECT i.productName AS productName, i.category AS category,
               SUM(i.quantity) AS totalQuantity, SUM(i.subtotal) AS totalRevenue
        FROM transaction_items i
        INNER JOIN transactions t ON t.id = i.transactionId
        WHERE t.timestamp >= :since AND t.timestamp < :until
        GROUP BY i.productName, i.category
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    fun observeTopSellersBetween(since: Long, until: Long, limit: Int): Flow<List<TopSellerSummary>>
}
