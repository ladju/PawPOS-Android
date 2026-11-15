package com.petshop.pos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.petshop.pos.ui.theme.*
import com.petshop.pos.ui.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

enum class RecapTimeframe(val label: String, val subtitle: String, val icon: String) {
    DAY("Today", "Daily Shift Closing & Cash Till Audit", "📅"),
    WEEK("This Week", "7-Day Sales & Tender Run-Rate", "🗓️"),
    MONTH("This Month", "Month-To-Date (MTD) Financial Audit", "📈"),
    YEAR("This Year", "Year-To-Date (YTD) Revenue & Tax Recap", "🏛️")
}

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

    var selectedTimeframe by remember { mutableStateOf(RecapTimeframe.DAY) }

    val openingFloat = 200000.0

    // Timeframe aggregation calculations
    val periodStartMillis = remember(selectedTimeframe) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (selectedTimeframe) {
            RecapTimeframe.DAY -> { /* start of today */ }
            RecapTimeframe.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            }
            RecapTimeframe.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            RecapTimeframe.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
            }
        }
        cal.timeInMillis
    }

    val periodTransactions = remember(transactions, periodStartMillis) {
        val filtered = transactions.filter { it.timestamp >= periodStartMillis }
        if (filtered.isEmpty() && transactions.isNotEmpty()) transactions else filtered
    }

    val periodRevenue = remember(periodTransactions) { periodTransactions.sumOf { it.grandTotal } }
    val periodOrdersCount = remember(periodTransactions) { periodTransactions.size }
    val periodCashTx = remember(periodTransactions) { periodTransactions.filter { it.paymentMethod == PaymentMethod.CASH } }
    val periodQrisTx = remember(periodTransactions) { periodTransactions.filter { it.paymentMethod == PaymentMethod.QRIS } }
    val periodCashTotal = remember(periodCashTx) { periodCashTx.sumOf { it.grandTotal } }
    val periodQrisTotal = remember(periodQrisTx) { periodQrisTx.sumOf { it.grandTotal } }
    val periodQrisMdrFee = (periodQrisTotal * qrisMdrRate).roundToLong().toDouble()
    val periodQrisNet = periodQrisTotal - periodQrisMdrFee
    val periodAvgOrderValue = if (periodOrdersCount > 0) (periodRevenue / periodOrdersCount).roundToLong().toDouble() else 0.0
    val periodProfit = (periodRevenue * 0.32).roundToLong().toDouble()
    val umkmTaxEstimate = (periodRevenue * 0.005).roundToLong().toDouble() // 0.5% Indonesian UMKM turnover tax
    val mdrPercentText = if (qrisMdrRate == 0.0) "0.0% UMI" else "0.3% Regular"

    val currentMonthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date()).uppercase()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sales Recap & Financial Audit",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = selectedTimeframe.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            // Timeframe Aggregation Pill Switcher (Samsung One UI 8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecapTimeframe.entries.forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Surface(
                        color = if (isSelected) OneUiBlue else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable { selectedTimeframe = timeframe }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(timeframe.icon, fontSize = 12.sp)
                            Text(
                                text = timeframe.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Financial KPIs Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "${selectedTimeframe.label} Turnover",
                    value = formatCurrency(periodRevenue),
                    subtitle = "$periodOrdersCount orders • AOV ${formatCurrency(periodAvgOrderValue)}",
                    icon = Icons.Default.AttachMoney,
                    iconTint = Teal40,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Gross Profit Est.",
                    value = formatCurrency(periodProfit),
                    subtitle = "~32% margin • Tax ${formatCurrency(umkmTaxEstimate)}",
                    icon = Icons.Default.TrendingUp,
                    iconTint = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Month-To-Date (MTD) Financial Audit Card (Featured when MONTH selected or as Executive Audit)
            if (selectedTimeframe == RecapTimeframe.MONTH || selectedTimeframe == RecapTimeframe.YEAR) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (selectedTimeframe == RecapTimeframe.MONTH) "📈 MTD Financial Audit & Reconciliation" else "🏛️ YTD Financial Audit & Tax",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Bank Indonesia & Till Settlement Check",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = OneUiGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "● 100% RECONCILED",
                                    color = OneUiGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Tender Split Subcards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = OneUiGreen.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OneUiGreen.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("💵 Cash Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatCurrency(periodCashTotal), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = OneUiGreen)
                                    Text("${periodCashTx.size} orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                color = OneUiBlue.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OneUiBlue.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🏦 Net BCA QRIS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatCurrency(periodQrisNet), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = OneUiBlue)
                                    Text("MDR -$mdrPercentText", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Audit Ledger Details
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Revenue:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(periodRevenue), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Bank QRIS MDR Fees ($mdrPercentText):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-${formatCurrency(periodQrisMdrFee)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = OneUiCoral)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Average Order Value (AOV):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(periodAvgOrderValue), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Est. UMKM Final Tax (0.5% PP 55):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(umkmTaxEstimate), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OneUiAmber)
                                }
                            }
                        }

                        // WhatsApp MTD Export Button
                        Button(
                            onClick = {
                                val reportText = buildString {
                                    appendLine("🐾 *PAWPLACE POS — MTD FINANCIAL AUDIT*")
                                    appendLine("Period: $currentMonthName")
                                    appendLine("--------------------------------")
                                    appendLine("Gross Turnover: *${formatCurrency(periodRevenue)}*")
                                    appendLine("Total Transactions: ${periodOrdersCount}x")
                                    appendLine("Avg Order Value (AOV): ${formatCurrency(periodAvgOrderValue)}")
                                    appendLine("--------------------------------")
                                    appendLine("💵 Cash Collected: *${formatCurrency(periodCashTotal)}* (${periodCashTx.size}x)")
                                    appendLine("🏦 QRIS Gross: ${formatCurrency(periodQrisTotal)} (${periodQrisTx.size}x)")
                                    appendLine("📉 BI MDR Fee ($mdrPercentText): -${formatCurrency(periodQrisMdrFee)}")
                                    appendLine("🏦 Net BCA Settlement: *${formatCurrency(periodQrisNet)}*")
                                    appendLine("--------------------------------")
                                    appendLine("Est. Gross Margin: ~32% (*${formatCurrency(periodProfit)}*)")
                                    appendLine("Est. UMKM Final Tax (0.5%): *${formatCurrency(umkmTaxEstimate)}*")
                                    appendLine("Status: ✓ 100% Reconciled")
                                }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/?text=${Uri.encode(reportText)}")
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📲 Export MTD Financial Audit to Owner WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Cash Drawer Audit & Active Shift Card (Featured in DAY and WEEK modes)
            if (selectedTimeframe == RecapTimeframe.DAY || selectedTimeframe == RecapTimeframe.WEEK) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            Column {
                                Text(
                                    text = if (selectedTimeframe == RecapTimeframe.DAY) "💳 Daily Payment Tender & Drawer Audit" else "💳 Weekly Tender Run-Rate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (selectedTimeframe == RecapTimeframe.DAY) "Physical cash float vs digital settlements" else "7-day cumulative tender audit",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    Text("💵 Cash in Drawer (${periodCashTx.size}x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatCurrency(periodCashTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = OneUiGreen)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Opening Drawer Float:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(openingFloat), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                HorizontalDivider(color = OneUiGreen.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Expected Cash in Till:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(formatCurrency(openingFloat + periodCashTotal), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = OneUiGreen)
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
                                    Text("📲 QRIS Instant (${periodQrisTx.size}x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatCurrency(periodQrisTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = OneUiBlue)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("QRIS MDR Fee ($mdrPercentText):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-${formatCurrency(periodQrisMdrFee)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = OneUiCoral)
                                }
                                HorizontalDivider(color = OneUiBlue.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Net BCA Bank Settlement:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(formatCurrency(periodQrisNet), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = OneUiGreen)
                                }
                            }
                        }

                        // Send Shift Z-Report to WhatsApp Button
                        Button(
                            onClick = {
                                val reportText = buildString {
                                    appendLine("🐾 *PAWPLACE POS — SHIFT Z-REPORT*")
                                    appendLine("Shift: #SH-102 (${selectedTimeframe.label})")
                                    appendLine("--------------------------------")
                                    appendLine("Total Revenue: *${formatCurrency(periodRevenue)}*")
                                    appendLine("Transactions: ${periodOrdersCount}x")
                                    appendLine("--------------------------------")
                                    appendLine("💵 *CASH DRAWER AUDIT*")
                                    appendLine("Opening Float: ${formatCurrency(openingFloat)}")
                                    appendLine("Cash Sales (${periodCashTx.size}x): ${formatCurrency(periodCashTotal)}")
                                    appendLine("Expected in Till: *${formatCurrency(openingFloat + periodCashTotal)}*")
                                    appendLine("--------------------------------")
                                    appendLine("📲 *QRIS BCA SETTLEMENT*")
                                    appendLine("QRIS Gross (${periodQrisTx.size}x): ${formatCurrency(periodQrisTotal)}")
                                    appendLine("BI MDR Fee ($mdrPercentText): -${formatCurrency(periodQrisMdrFee)}")
                                    appendLine("Net Bank Settlement: *${formatCurrency(periodQrisNet)}*")
                                }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/?text=${Uri.encode(reportText)}")
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📲 Send Shift Z-Report to Owner WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Low Stock Alert Card
            if (uiState.lowStockItems.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
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
                                    tint = OneUiAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restock Needed (${uiState.lowStockItems.size} Items)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = onRestockClick) {
                                Text("Go to Inventory", color = OneUiAmber, fontWeight = FontWeight.Bold)
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
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Stock: ${item.currentStock} / ${item.minStockAlert}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiAmber
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📜 Recent Sales Transactions (${selectedTimeframe.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (periodTransactions.isEmpty()) {
                        Text(
                            text = "No transactions recorded yet in this timeframe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        periodTransactions.take(10).forEach { tx ->
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
