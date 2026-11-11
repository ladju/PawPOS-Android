package com.petshop.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import android.net.Uri
import java.io.File
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.data.local.entity.TransactionEntity
import com.petshop.pos.data.local.entity.TransactionItemEntity
import com.petshop.pos.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double): String {
    val localeID = Locale("in", "ID")
    val format = NumberFormat.getNumberInstance(localeID)
    format.maximumFractionDigits = 0
    return "Rp ${format.format(amount)}"
}

data class CashPrediction(
    val label: String,
    val amount: Double,
    val isExact: Boolean = false
)

/**
 * Smart cash tender predictions based on Indonesian Rupiah currency rounding (banknotes: 1k, 2k, 5k, 10k, 20k, 50k, 100k).
 * For example: if amount due is 771.750, suggests:
 * - Uang Pas (Rp 771.750)
 * - Rp 780.000 (nearest 10k)
 * - Rp 800.000 (nearest 50k)
 * - Rp 1.000.000 (major banknote milestone)
 */
fun calculateCashPredictions(total: Double): List<CashPrediction> {
    if (total <= 0.0) return emptyList()
    val roundedTotal = kotlin.math.round(total).toLong()
    val amounts = linkedSetOf<Long>()

    // 1. Exact amount ("Uang Pas")
    amounts.add(roundedTotal)

    val step1: Long
    val step2: Long
    val step3: Long
    val milestone: Long

    when {
        roundedTotal < 20_000L -> {
            step1 = 1_000L
            step2 = 5_000L
            step3 = 10_000L
            milestone = if (roundedTotal > 10_000L) 50_000L else 20_000L
        }
        roundedTotal < 100_000L -> {
            step1 = 5_000L
            step2 = 10_000L
            step3 = 50_000L
            milestone = 100_000L
        }
        roundedTotal < 500_000L -> {
            step1 = 10_000L
            step2 = 50_000L
            step3 = 100_000L
            milestone = 500_000L
        }
        roundedTotal < 1_000_000L -> {
            // e.g. 771.750 -> 780.000, 800.000, 1.000.000
            step1 = 10_000L
            step2 = 50_000L
            step3 = 100_000L
            milestone = 1_000_000L
        }
        else -> {
            step1 = 50_000L
            step2 = 100_000L
            step3 = 500_000L
            val remainder = roundedTotal % 1_000_000L
            milestone = if (remainder == 0L) roundedTotal else ((roundedTotal / 1_000_000L) + 1) * 1_000_000L
        }
    }

    fun ceilTo(num: Long, step: Long): Long {
        return if (num % step == 0L) num else ((num / step) + 1) * step
    }

    val r1 = ceilTo(roundedTotal, step1)
    if (r1 > roundedTotal) amounts.add(r1)

    val r2 = ceilTo(roundedTotal, step2)
    if (r2 > roundedTotal) amounts.add(r2)

    val r3 = ceilTo(roundedTotal, step3)
    if (r3 > roundedTotal) amounts.add(r3)

    if (milestone > roundedTotal) amounts.add(milestone)

    // Ensure at least 4 options if feasible
    var nextStep = ceilTo(roundedTotal, step3)
    while (amounts.size < 4 && nextStep < roundedTotal + (step3 * 10)) {
        nextStep += step3
        if (nextStep > roundedTotal) amounts.add(nextStep)
    }

    val sortedAmounts = amounts.sorted().take(5)
    return sortedAmounts.map { amt ->
        val isExact = amt == roundedTotal
        val label = if (isExact) "Uang Pas (${formatCurrency(amt.toDouble())})" else formatCurrency(amt.toDouble())
        CashPrediction(
            label = label,
            amount = amt.toDouble(),
            isExact = isExact
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun PetCategoryPills(
    selectedCategory: PetCategory,
    onCategorySelected: (PetCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PetCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = "${category.icon} ${category.displayName}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun ProductTypePills(
    selectedType: ProductType,
    onTypeSelected: (ProductType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProductType.entries.forEach { type ->
            val isSelected = type == selectedType
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = "${type.icon} ${type.displayName}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}


@Composable
fun StockBadge(
    currentStock: Int,
    minAlert: Int,
    isService: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isService) {
        Surface(
            color = TealLight,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Text(
                text = "Service",
                color = TealDark,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        return
    }

    val (bgColor, textColor, label) = when {
        currentStock <= 0 -> Triple(Color(0xFFFFE4E6), CoralAccent, "Out of Stock")
        currentStock <= minAlert -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Low: $currentStock")
        else -> Triple(Color(0xFFD1FAE5), Color(0xFF047857), "Stock: $currentStock")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            if (currentStock <= minAlert) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ThermalReceiptView(
    transaction: TransactionEntity,
    items: List<TransactionItemEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🐾 PAWPLACE PET SHOP 🐾",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = "124 Animal Care Ave, Suite 4",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Tel: (555) 345-7890",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "------------------------------------------",
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Invoice: ${transaction.invoiceNumber}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${formatDate(transaction.timestamp)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cashier: ${transaction.cashierName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                if (!transaction.customerName.isNullOrBlank()) {
                    Text(
                        text = "Cust: ${transaction.customerName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "------------------------------------------",
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Line items
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.productName,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatCurrency(item.subtotal),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "  ${item.quantity} x ${formatCurrency(item.unitPrice)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "------------------------------------------",
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Subtotals & Totals
            ReceiptLine(label = "Subtotal:", value = formatCurrency(transaction.subtotal))
            if (transaction.discountAmount > 0) {
                ReceiptLine(label = "Discount:", value = "-${formatCurrency(transaction.discountAmount)}")
            }
            if (transaction.taxAmount > 0) {
                ReceiptLine(label = "Tax (${(transaction.taxRate * 100).toInt()}%):", value = formatCurrency(transaction.taxAmount))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TOTAL:",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = formatCurrency(transaction.grandTotal),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            ReceiptLine(label = "Payment (${transaction.paymentMethod.displayName}):", value = formatCurrency(transaction.grandTotal))
            if (transaction.paymentMethod == PaymentMethod.CASH) {
                ReceiptLine(label = "Cash Tendered:", value = formatCurrency(transaction.cashTendered))
                ReceiptLine(label = "Change Returned:", value = formatCurrency(transaction.changeGiven))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic "Scan for WhatsApp Receipt" Section (Option 1)
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📲", fontSize = 12.sp)
                        Text(
                            text = "SCAN FOR WHATSAPP RECEIPT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }
                    Text(
                        text = "Customer points camera to receive receipt in chat",
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.size(130.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCode2,
                                    contentDescription = "WhatsApp Receipt QR Code",
                                    tint = Color(0xFF075E54),
                                    modifier = Modifier.size(92.dp)
                                )
                                Text(
                                    text = "wa.me / ${transaction.invoiceNumber}",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Thank You for Visiting PawPlace!",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black
            )
            Text(
                text = "Keep your pets happy & healthy! 🐾",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}

@Composable
fun ProductImagePicker(
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for Gallery Picker (Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri.toString())
        }
    }

    // Launcher for Camera Photo Capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            onImageSelected(tempCameraUri.toString())
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📸 Product Picture (Camera or Gallery)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Image Box Preview
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!selectedImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Product Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onImageSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "No Photo",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Action Buttons: Camera & Gallery
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = {
                            val cacheDir = File(context.cacheDir, "images")
                            cacheDir.mkdirs()
                            val file = File(cacheDir, "product_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose from Gallery", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

