package com.petshop.pos.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.domain.PeriodBounds
import com.petshop.pos.domain.PeriodMath
import com.petshop.pos.domain.PeriodUnit
import com.petshop.pos.domain.TrendBucket
import com.petshop.pos.ui.components.formatCurrency
import com.petshop.pos.ui.components.formatDate
import com.petshop.pos.ui.theme.AppStrings
import com.petshop.pos.ui.theme.CoralAccent
import com.petshop.pos.ui.theme.LocalAppStrings
import com.petshop.pos.ui.theme.OneUiAmber
import com.petshop.pos.ui.theme.OneUiGreen
import com.petshop.pos.ui.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecapTimeframe(val icon: String, val unit: PeriodUnit) {
    DAY("📅", PeriodUnit.DAY),
    WEEK("🗓️", PeriodUnit.WEEK),
    MONTH("📈", PeriodUnit.MONTH),
    YEAR("🏛️", PeriodUnit.YEAR);

    fun localizedLabel(strings: AppStrings): String = when (this) {
        DAY -> strings.tfDay
        WEEK -> strings.tfWeek
        MONTH -> strings.tfMonth
        YEAR -> strings.tfYear
    }

    fun localizedSubtitle(strings: AppStrings): String = when (this) {
        DAY -> strings.tfSubDay
        WEEK -> strings.tfSubWeek
        MONTH -> strings.tfSubMonth
        YEAR -> strings.tfSubYear
    }
}

/**
 * Sales recap. Every number on this screen is computed from saved transactions for the selected calendar
 * period. An empty period shows zeros; nothing is estimated, sample-filled or hardcoded.
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    qrisMdrRate: Double = 0.003,
    onRestockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.periodStats.collectAsState()
    val previousStats by viewModel.previousStats.collectAsState()
    val transactions by viewModel.periodTransactions.collectAsState()
    val soldProducts by viewModel.soldProducts.collectAsState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    var timeframeIndex by rememberSaveable { mutableStateOf(0) }
    var offsetBack by rememberSaveable { mutableStateOf(0) }
    val timeframe = RecapTimeframe.entries[timeframeIndex]

    val now = remember(timeframeIndex, offsetBack) { System.currentTimeMillis() }
    val bounds = remember(timeframeIndex, offsetBack, now) { PeriodMath.bounds(timeframe.unit, offsetBack, now) }
    val previousBounds = remember(timeframeIndex, offsetBack, now) { PeriodMath.bounds(timeframe.unit, offsetBack + 1, now) }
    LaunchedEffect(bounds, previousBounds) { viewModel.setPeriod(bounds, previousBounds) }

    val cashTx = remember(transactions) { transactions.filter { it.paymentMethod == PaymentMethod.CASH } }
    val qrisTx = remember(transactions) { transactions.filter { it.paymentMethod == PaymentMethod.QRIS } }
    val cashTotal = cashTx.sumOf { it.grandTotal }
    val qrisTotal = qrisTx.sumOf { it.grandTotal }
    val qrisFeeEstimate = Math.round(qrisTotal * qrisMdrRate)
    val averageOrder = if (stats.transactionCount > 0) stats.revenue / stats.transactionCount else 0L
    val marginPercent: Double? = if (stats.revenue > 0) stats.grossProfit * 100.0 / stats.revenue else null
    val revenueChange = PeriodMath.percentChange(stats.revenue, previousStats.revenue)

    val trend = remember(timeframe, bounds, transactions) {
        PeriodMath.buckets(timeframe.unit, bounds, transactions.map { it.timestamp to it.grandTotal })
    }
    val categoryRevenue = remember(soldProducts) {
        soldProducts.groupBy { it.category }
            .map { (category, rows) -> category to rows.sumOf { it.totalRevenue } }
            .sortedByDescending { it.second }
    }
    val topSellers = remember(soldProducts) { soldProducts.sortedByDescending { it.totalQuantity }.take(5) }
    val recentTransactions = remember(transactions) { transactions.sortedByDescending { it.timestamp }.take(50) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- period selector ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RecapTimeframe.entries.forEachIndexed { index, tf ->
                        FilterChip(
                            selected = index == timeframeIndex,
                            onClick = {
                                timeframeIndex = index
                                offsetBack = 0
                            },
                            label = {
                                Text(
                                    text = tf.localizedLabel(strings),
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { offsetBack += 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous period")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = periodLabel(timeframe.unit, bounds),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (offsetBack == 0) "Current period" else "Tap › to move forward",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { offsetBack -= 1 }, enabled = offsetBack > 0) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next period")
                    }
                }
            }
        }

        // ---- KPI grid ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        label = "Revenue",
                        value = formatCurrency(stats.revenue),
                        sub = revenueChange?.let { (if (it >= 0) "▲ " else "▼ ") + Math.abs(it) + "% vs previous" }
                            ?: "No previous data",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Orders",
                        value = stats.transactionCount.toString(),
                        sub = "${stats.itemsSold} items sold",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        label = "Gross profit",
                        value = formatCurrency(stats.grossProfit),
                        sub = marginPercent?.let { "Margin " + "%.1f".format(it) + "%" } ?: "No sales yet",
                        valueColor = if (stats.grossProfit < 0) CoralAccent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Avg. order",
                        value = formatCurrency(averageOrder),
                        sub = if (stats.discountTotal > 0) "Discounts ${formatCurrency(stats.discountTotal)}" else "No discounts",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- trend ----
        item {
            SectionCard(title = "Sales trend") {
                if (stats.transactionCount == 0) {
                    EmptyHint("No sales in this period yet.")
                } else {
                    TrendBars(trend)
                }
            }
        }

        // ---- payment split ----
        item {
            SectionCard(title = "Payments") {
                PaymentRow("💵 Cash", cashTx.size, cashTotal)
                Spacer(Modifier.height(8.dp))
                PaymentRow("📲 QRIS", qrisTx.size, qrisTotal)
                if (qrisTotal > 0) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Estimated QRIS fee (${"%.1f".format(qrisMdrRate * 100)}% MDR): ${formatCurrency(qrisFeeEstimate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Estimated net to your account: ${formatCurrency(qrisTotal - qrisFeeEstimate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Estimate only. Check your bank or e-wallet statement for the real settlement.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // ---- category split ----
        item {
            SectionCard(title = "Sales by category") {
                if (categoryRevenue.isEmpty()) {
                    EmptyHint("No sales in this period yet.")
                } else {
                    val total = categoryRevenue.sumOf { it.second }.coerceAtLeast(1L)
                    categoryRevenue.forEach { (category, amount) ->
                        val share = (amount.toFloat() / total).coerceIn(0f, 1f)
                        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${category.icon} ${category.displayName}",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${formatCurrency(amount)} · ${Math.round(share * 100)}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(share.coerceAtLeast(0.02f))
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- low stock ----
        if (uiState.lowStockItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Restock needed (${uiState.lowStockItems.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        uiState.lowStockItems.take(5).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.category.icon} ${item.name}",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (item.currentStock <= 0) "Out of stock" else "${item.currentStock} / ${item.minStockAlert}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.currentStock <= 0) CoralAccent else OneUiAmber
                                )
                            }
                        }
                        if (uiState.lowStockItems.size > 5) {
                            Text(
                                text = "+${uiState.lowStockItems.size - 5} more",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRestockClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Open inventory", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ---- top sellers ----
        item {
            SectionCard(title = "Top selling products") {
                if (topSellers.isEmpty()) {
                    EmptyHint("No sales in this period yet.")
                } else {
                    topSellers.forEachIndexed { index, seller ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                            Text(
                                text = "${seller.category.icon} ${seller.productName}",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${seller.totalQuantity} sold", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = formatCurrency(seller.totalRevenue),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- share ----
        item {
            Button(
                onClick = {
                    val report = buildReport(timeframe, bounds, stats.revenue, stats.transactionCount, stats.grossProfit, cashTotal, qrisTotal)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(send, "Share sales report"))
                },
                enabled = stats.transactionCount > 0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share sales report", fontWeight = FontWeight.Bold)
            }
        }

        // ---- transactions ----
        item {
            Text(
                text = "Sales in this period (${transactions.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (recentTransactions.isEmpty()) {
            item { EmptyHint("No transactions in this period yet.") }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.viewTransactionDetails(tx) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.invoiceNumber,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDate(tx.timestamp)} • ${tx.paymentMethod.displayName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatCurrency(tx.grandTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (uiState.isReceiptDetailOpen && uiState.selectedTransaction != null) {
        ReceiptDialog(
            transaction = uiState.selectedTransaction!!,
            items = uiState.selectedTransactionItems,
            onDismiss = { viewModel.closeReceiptDetail() }
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PaymentRow(label: String, count: Int, total: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "$count transaction${if (count == 1) "" else "s"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Text(formatCurrency(total), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun TrendBars(buckets: List<TrendBucket>) {
    val max = (buckets.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    val labelEvery = if (buckets.size > 12) 6 else 1
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            buckets.forEach { bucket ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fraction = if (bucket.amount <= 0L) 0.02f else (bucket.amount.toFloat() / max).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(
                                if (bucket.amount <= 0L) barColor.copy(alpha = 0.2f) else barColor,
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    text = if (index % labelEvery == 0) bucket.label else "",
                    fontSize = 9.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun periodLabel(unit: PeriodUnit, bounds: PeriodBounds): String {
    val locale = Locale.getDefault()
    val start = Date(bounds.start)
    val lastDay = Date(bounds.end - 1)
    return when (unit) {
        PeriodUnit.DAY -> SimpleDateFormat("EEE, d MMM yyyy", locale).format(start)
        PeriodUnit.WEEK -> SimpleDateFormat("d MMM", locale).format(start) + " – " +
            SimpleDateFormat("d MMM yyyy", locale).format(lastDay)
        PeriodUnit.MONTH -> SimpleDateFormat("MMMM yyyy", locale).format(start)
        PeriodUnit.YEAR -> SimpleDateFormat("yyyy", locale).format(start)
    }
}

private fun buildReport(
    timeframe: RecapTimeframe,
    bounds: PeriodBounds,
    revenue: Long,
    orders: Int,
    profit: Long,
    cash: Long,
    qris: Long
): String = buildString {
    appendLine("PawPOS sales report")
    appendLine(periodLabel(timeframe.unit, bounds))
    appendLine()
    appendLine("Revenue: ${formatCurrency(revenue)}")
    appendLine("Orders: $orders")
    appendLine("Gross profit: ${formatCurrency(profit)}")
    appendLine("Cash: ${formatCurrency(cash)}")
    append("QRIS: ${formatCurrency(qris)}")
}
