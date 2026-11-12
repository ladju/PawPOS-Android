package com.petshop.pos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.ui.components.*
import com.petshop.pos.ui.theme.AmberPrimary
import com.petshop.pos.ui.theme.CoralAccent
import com.petshop.pos.ui.theme.GreenSuccess
import com.petshop.pos.ui.theme.OneUiBlue
import com.petshop.pos.ui.theme.OneUiGreen
import com.petshop.pos.ui.theme.Teal40
import com.petshop.pos.ui.viewmodel.AnalyticsViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    qrisMdrRate: Double = 0.003,
    onRestockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val context = LocalContext.current

    val openingFloat = 200000.0
    val cashTransactions = remember(transactions) { transactions.filter { it.paymentMethod == PaymentMethod.CASH } }
    val qrisTransactions = remember(transactions) { transactions.filter { it.paymentMethod == PaymentMethod.QRIS } }
    val cashSalesTotal = remember(cashTransactions) { cashTransactions.sumOf { it.grandTotal } }
    val qrisSalesTotal = remember(qrisTransactions) { qrisTransactions.sumOf { it.grandTotal } }
    val expectedCashInTill = openingFloat + cashSalesTotal
    val qrisMdrFee = (qrisSalesTotal * qrisMdrRate).roundToLong().toDouble()
    val qrisNetSettlement = qrisSalesTotal - qrisMdrFee
    val mdrPercentText = if (qrisMdrRate == 0.0) "0.0% UMI" else "0.3% Regular"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Recap & Shift Z-Report", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshAnalytics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Today's Revenue",
                    value = formatCurrency(uiState.dailySummary.totalRevenue),
                    subtitle = "${uiState.dailySummary.transactionCount} transactions",
                    icon = Icons.Default.AttachMoney,
                    iconTint = Teal40,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Gross Profit",
                    value = formatCurrency(uiState.dailySummary.totalProfit),
                    subtitle = "${uiState.dailySummary.itemsSold} items sold",
                    icon = Icons.Default.TrendingUp,
                    iconTint = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Payment Tender Breakdown & Cash Drawer Audit (One UI 8 Focus Block)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💳 Payment Tender & Drawer Audit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            color = OneUiGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "SHIFT #SH-102 ACTIVE",
                                color = OneUiGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Cash in Drawer Subcard
                    Surface(
                        color = OneUiGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OneUiGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("💵 Cash in Drawer (${cashTransactions.size}x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(formatCurrency(cashSalesTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = OneUiGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Opening Drawer Float:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                Text(formatCurrency(openingFloat), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            HorizontalDivider(color = OneUiGreen.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected Cash in Till:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(formatCurrency(expectedCashInTill), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = OneUiGreen)
                            }
                        }
                    }

                    // QRIS Instant Settlement Subcard
                    Surface(
                        color = OneUiBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OneUiBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📲 QRIS Instant (${qrisTransactions.size}x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(formatCurrency(qrisSalesTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = OneUiBlue)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("QRIS MDR Fee ($mdrPercentText):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                Text("-${formatCurrency(qrisMdrFee)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            HorizontalDivider(color = OneUiBlue.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net BCA Bank Settlement:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(formatCurrency(qrisNetSettlement), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = OneUiGreen)
                            }
                        }
                    }

                    // Send Shift Z-Report to WhatsApp Button
                    Button(
                        onClick = {
                            val reportText = buildString {
                                appendLine("🐾 *PAWPLACE POS — SHIFT Z-REPORT*")
                                appendLine("Shift: #SH-102")
                                appendLine("--------------------------------")
                                appendLine("Total Revenue: *${formatCurrency(uiState.dailySummary.totalRevenue)}*")
                                appendLine("Transactions: ${uiState.dailySummary.transactionCount}x")
                                appendLine("--------------------------------")
                                appendLine("💵 *CASH DRAWER AUDIT*")
                                appendLine("Opening Float: ${formatCurrency(openingFloat)}")
                                appendLine("Cash Sales (${cashTransactions.size}x): ${formatCurrency(cashSalesTotal)}")
                                appendLine("Expected in Till: *${formatCurrency(expectedCashInTill)}*")
                                appendLine("--------------------------------")
                                appendLine("📲 *QRIS BCA SETTLEMENT*")
                                appendLine("QRIS Gross (${qrisTransactions.size}x): ${formatCurrency(qrisSalesTotal)}")
                                appendLine("BI MDR Fee ($mdrPercentText): -${formatCurrency(qrisMdrFee)}")
                                appendLine("Net Bank Settlement: *${formatCurrency(qrisNetSettlement)}*")
                            }
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/?text=${Uri.encode(reportText)}")
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📲 Send Shift Z-Report to Owner WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Low Stock Alert Card
            if (uiState.lowStockItems.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restock Needed (${uiState.lowStockItems.size} Items)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            TextButton(onClick = onRestockClick) {
                                Text("Go to Inventory", color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.lowStockItems.take(4).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.category.icon} ${item.name}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF78350F),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Stock: ${item.currentStock} / ${item.minStockAlert}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            // Top Sellers Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏆 Top Selling Products",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.topSellers.isEmpty()) {
                        Text(
                            text = "No sales data recorded yet. Checkout orders in POS to view best sellers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        uiState.topSellers.forEachIndexed { index, seller ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${seller.category.icon} ${seller.productName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${seller.totalQuantity} sold",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatCurrency(seller.totalRevenue),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (index < uiState.topSellers.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions History
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📜 Recent Sales Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            text = "No transactions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        transactions.take(10).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.viewTransactionDetails(tx) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.invoiceNumber,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${formatDate(tx.timestamp)} • ${tx.paymentMethod.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatCurrency(tx.grandTotal),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // Receipt Detail Dialog
    if (uiState.isReceiptDetailOpen && uiState.selectedTransaction != null) {
        ReceiptDialog(
            transaction = uiState.selectedTransaction!!,
            items = uiState.selectedTransactionItems,
            onDismiss = { viewModel.closeReceiptDetail() }
        )
    }
}
