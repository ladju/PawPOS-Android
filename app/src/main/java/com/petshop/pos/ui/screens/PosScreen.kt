package com.petshop.pos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.ui.components.*
import com.petshop.pos.ui.theme.CoralAccent
import com.petshop.pos.ui.theme.Teal40
import com.petshop.pos.ui.theme.TealLight
import com.petshop.pos.ui.viewmodel.PosUiState
import com.petshop.pos.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState()
    var isCartSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Search & Quick Scanner Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search product name or barcode...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onNavigateToScanner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = Color.White
                        )
                    }
                }

                // Product Type Filter Pills (Dry Cat Food, Wet Cat Food, Bird Food, Fish Food, obat cacing, Accesories, Grooming, Cat Litter)
                ProductTypePills(
                    selectedType = uiState.selectedType,
                    onTypeSelected = { viewModel.selectType(it) }
                )
            }
        },
        bottomBar = {
            if (uiState.cartItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side: Clickable order summary to review/edit cart
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isCartSheetOpen = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.totalItemsCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${uiState.totalItemsCount} items • Tap to Review",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatCurrency(uiState.grandTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Right side: Samsung One UI Pill-Shaped Primary Charge CTA
                        Button(
                            onClick = { isCartSheetOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal40,
                                contentColor = Color.White
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
                        ) {
                            Text(
                                text = "Charge ${formatCurrency(uiState.grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🐾",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try another category or search keyword",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val cartQuantityMap = remember(uiState.cartItems) {
                    uiState.cartItems.associate { it.product.id to it.quantity }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val inCartQty = cartQuantityMap[product.id] ?: 0
                        ProductCard(
                            product = product,
                            inCartQuantity = inCartQty,
                            onAddToCart = { viewModel.addToCart(product) },
                            onIncrease = { viewModel.addToCart(product) },
                            onDecrease = { viewModel.updateQuantity(product.id, inCartQty - 1) }
                        )
                    }
                }
            }

            // Error banner
            uiState.errorMessage?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }

    // Cart Sheet Dialog
    if (isCartSheetOpen) {
        CartModalSheet(
            uiState = uiState,
            onDismiss = { isCartSheetOpen = false },
            onUpdateQuantity = { id, qty -> viewModel.updateQuantity(id, qty) },
            onRemoveItem = { id -> viewModel.removeFromCart(id) },
            onClearCart = { viewModel.clearCart() },
            onToggleTax = { viewModel.toggleTax(it) },
            onOpenCheckout = {
                isCartSheetOpen = false
                viewModel.openCheckoutDialog()
            }
        )
    }

    // Checkout Dialog
    if (uiState.isCheckoutDialogOpen) {
        CheckoutDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeCheckoutDialog() },
            onBackToCart = {
                viewModel.closeCheckoutDialog()
                isCartSheetOpen = true
            },
            onPaymentMethodChanged = { viewModel.setPaymentMethod(it) },
            onCashTenderedChanged = { viewModel.setCashTendered(it) },
            onCustomerDetailsChanged = { name, phone -> viewModel.setCustomerDetails(name, phone) },
            onComplete = { viewModel.completeCheckout() }
        )
    }

    // Receipt Dialog
    if (uiState.isReceiptDialogOpen && uiState.lastTransaction != null) {
        ReceiptDialog(
            transaction = uiState.lastTransaction!!,
            items = uiState.lastTransactionItems,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    inCartQuantity: Int = 0,
    onAddToCart: () -> Unit,
    onIncrease: () -> Unit = onAddToCart,
    onDecrease: () -> Unit = {}
) {
    val isInCart = inCartQuantity > 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isInCart) BorderStroke(2.dp, Teal40) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.currentStock > 0 || product.productType.isService || product.category == PetCategory.SERVICES) {
                onAddToCart()
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.productType.icon} ${product.productType.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (isInCart) {
                    Surface(
                        color = Teal40,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🛒 $inCartQuantity in cart",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    StockBadge(
                        currentStock = product.currentStock,
                        minAlert = product.minStockAlert,
                        isService = product.productType.isService || product.category == PetCategory.SERVICES
                    )
                }
            }

            if (!product.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Barcode: ${product.barcode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isInCart) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        FilledIconButton(
                            onClick = onDecrease,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "$inCartQuantity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        FilledIconButton(
                            onClick = onIncrease,
                            enabled = inCartQuantity < product.currentStock || product.productType.isService || product.category == PetCategory.SERVICES,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Teal40,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    FilledIconButton(
                        onClick = onAddToCart,
                        enabled = product.currentStock > 0 || product.productType.isService || product.category == PetCategory.SERVICES,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Teal40,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartModalSheet(
    uiState: PosUiState,
    onDismiss: () -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onToggleTax: (Boolean) -> Unit = {},
    onOpenCheckout: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Order & Items (${uiState.totalItemsCount})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearCart) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatCurrency(item.product.sellingPrice)} each",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Stepper: -, qty, +
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(
                                onClick = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            FilledIconButton(
                                onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Teal40,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatCurrency(item.subtotal),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(70.dp)
                        )

                        IconButton(
                            onClick = { onRemoveItem(item.product.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Pricing summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(formatCurrency(uiState.subtotal), fontWeight = FontWeight.SemiBold)
            }
            if (uiState.discountAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount", color = MaterialTheme.colorScheme.error)
                    Text("-${formatCurrency(uiState.discountAmount)}", color = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.taxRate > 0,
                        onCheckedChange = { onToggleTax(it) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.taxRate > 0) "Tax (${(uiState.taxRate * 100).toInt()}%)" else "Apply Tax (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = if (uiState.taxRate > 0) formatCurrency(uiState.taxAmount) else "Rp 0 (Not included)",
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.taxRate > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grand Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatCurrency(uiState.grandTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Proceed to Payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CheckoutDialog(
    uiState: PosUiState,
    onDismiss: () -> Unit,
    onBackToCart: () -> Unit = onDismiss,
    onPaymentMethodChanged: (PaymentMethod) -> Unit,
    onCashTenderedChanged: (Double) -> Unit,
    onCustomerDetailsChanged: (String, String) -> Unit,
    onComplete: () -> Unit
) {
    var custName by remember { mutableStateOf(uiState.customerName) }
    var custPhone by remember { mutableStateOf(uiState.customerPhone) }
    var tenderInput by remember(uiState.grandTotal) {
        mutableStateOf(if (uiState.cashTendered > 0) formatCurrency(uiState.cashTendered) else formatCurrency(uiState.grandTotal))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToCart) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Review Cart")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Checkout Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tappable Order items summary strip to return to cart details
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBackToCart() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛍️ ${uiState.totalItemsCount} items in cart",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Review Items ✎",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total display
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amount Due",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatCurrency(uiState.grandTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.taxRate > 0 && uiState.taxAmount > 0) {
                                "Subtotal ${formatCurrency(uiState.discountedSubtotal)} + Tax (${(uiState.taxRate * 100).toInt()}%) ${formatCurrency(uiState.taxAmount)}"
                            } else {
                                "Subtotal ${formatCurrency(uiState.discountedSubtotal)} • Tax Not Included (Small Business 0%)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector
                Text(
                    text = "Select Payment Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val isSelected = method == uiState.paymentMethod
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPaymentMethodChanged(method) },
                            label = { Text(method.displayName) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // If Cash, show tender and change calculation
                if (uiState.paymentMethod == PaymentMethod.CASH) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cash Received",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = tenderInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.isBlank()) {
                                tenderInput = ""
                                onCashTenderedChanged(0.0)
                            } else {
                                val parsed = digits.toDoubleOrNull() ?: 0.0
                                tenderInput = formatCurrency(parsed)
                                onCashTenderedChanged(parsed)
                            }
                        },
                        label = { Text("Cash Received (IDR)") },
                        placeholder = { Text("Rp 0") },
                        trailingIcon = {
                            if (tenderInput.isNotBlank()) {
                                IconButton(onClick = {
                                    tenderInput = ""
                                    onCashTenderedChanged(0.0)
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear cash input")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Tender Suggestions (Currency Rounding):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val predictions = remember(uiState.grandTotal) {
                        calculateCashPredictions(uiState.grandTotal)
                    }

                    // Dynamic Cash Prediction Pills (IDR Banknotes & Currency Rounding)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predictions.forEach { pred ->
                            val isSelected = kotlin.math.abs(uiState.cashTendered - pred.amount) < 0.5
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    tenderInput = formatCurrency(pred.amount)
                                    onCashTenderedChanged(pred.amount)
                                },
                                label = {
                                    Text(
                                        text = pred.label,
                                        fontWeight = if (isSelected || pred.isExact) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Teal40,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change calculation box
                    Surface(
                        color = if (uiState.cashTendered >= uiState.grandTotal) Color(0xFFD1FAE5) else Color(0xFFFFE4E6),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.cashTendered >= uiState.grandTotal) "Change to Return:" else "Short by:",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (uiState.cashTendered >= uiState.grandTotal) Color(0xFF047857) else CoralAccent
                                )
                                if (uiState.cashTendered >= uiState.grandTotal) {
                                    Text(
                                        text = "Kembalian Uang",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857).copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(Math.abs(uiState.cashTendered - uiState.grandTotal)),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (uiState.cashTendered >= uiState.grandTotal) Color(0xFF047857) else CoralAccent
                            )
                        }
                    }
                } else if (uiState.paymentMethod == PaymentMethod.QRIS) {
                    // Dynamic QRIS Counter Scanning & 1-Tap Customer Fullscreen Flip
                    var countdownSeconds by remember { mutableStateOf(180) }
                    var isCustomerFullscreenOpen by remember { mutableStateOf(false) }
                    val rrnCode = uiState.currentQrisRrn.ifBlank { "261112502600" }

                    LaunchedEffect(uiState.paymentMethod) {
                        countdownSeconds = 180
                        while (countdownSeconds > 0) {
                            kotlinx.coroutines.delay(1000L)
                            countdownSeconds--
                        }
                    }

                    val minStr = String.format("%02d", countdownSeconds / 60)
                    val secStr = String.format("%02d", countdownSeconds % 60)

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header: QRIS Standar Nasional + Timer + Customer View Flip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFDC2626),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "QRIS",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Standar Nasional",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "⏳ $minStr:$secStr",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier.clickable { isCustomerFullscreenOpen = true }
                                    ) {
                                        Text(
                                            text = "🔲 Customer View",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            Text(
                                text = "PawPlace Pet Shop & Grooming",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "NMID: ID1020038192019 • RRN: $rrnCode",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scannable White QR Card (Tap to Enlarge)
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 4.dp,
                                border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.clickable { isCustomerFullscreenOpen = true }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DynamicQrisCodeBox(
                                        amount = uiState.grandTotal,
                                        sizeDp = 168
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🔍 TAP TO ENLARGE FOR CUSTOMER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF4B5563)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Exact QRIS Total Strip
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Exact QRIS Total",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = formatCurrency(uiState.grandTotal),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (uiState.qrisMdrRate == 0.0) {
                                    "No customer surcharge • 0.0% UMI Subsidized MDR"
                                } else {
                                    "No customer surcharge • Store absorbs 0.3% MDR"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "GoPay • OVO • DANA • ShopeePay • BCA • Mandiri • BRI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    playPosVerificationChime()
                                    onComplete()
                                },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚡ Simulate Customer Scanned & Paid",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isCustomerFullscreenOpen) {
                        CustomerFullscreenQrisDialog(
                            amount = uiState.grandTotal,
                            timerText = "⏳ $minStr:$secStr",
                            onDismiss = { isCustomerFullscreenOpen = false },
                            onCompleteSale = {
                                isCustomerFullscreenOpen = false
                                playPosVerificationChime()
                                onComplete()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional Customer Details
                OutlinedTextField(
                    value = custName,
                    onValueChange = {
                        custName = it
                        onCustomerDetailsChanged(custName, custPhone)
                    },
                    label = { Text("Customer Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val isCashShort = uiState.paymentMethod == PaymentMethod.CASH && uiState.cashTendered < uiState.grandTotal
                Button(
                    onClick = {
                        if (uiState.paymentMethod == PaymentMethod.QRIS) {
                            playPosVerificationChime()
                        }
                        onComplete()
                    },
                    enabled = !isCashShort,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (isCashShort) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CoralAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Full Cash (Short ${formatCurrency(uiState.grandTotal - uiState.cashTendered)})",
                            fontWeight = FontWeight.Bold,
                            color = CoralAccent,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.paymentMethod == PaymentMethod.QRIS) {
                                "✓ Customer Has Paid • Complete Sale"
                            } else {
                                "✓ Complete Cash Sale"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun playPosVerificationChime() {
    try {
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90)
        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 350)
    } catch (_: Exception) {
    }
}

@Composable
fun DynamicQrisCodeBox(
    amount: Double,
    sizeDp: Int = 168
) {
    val seed = kotlin.math.abs(amount.toLong()).coerceAtLeast(12345L)
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val scale = w / 200f

            fun drawFinder(x: Float, y: Float) {
                drawRoundRect(
                    color = Color(0xFF1C1C1E),
                    topLeft = androidx.compose.ui.geometry.Offset(x * scale, y * scale),
                    size = androidx.compose.ui.geometry.Size(46f * scale, 46f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset((x + 8f) * scale, (y + 8f) * scale),
                    size = androidx.compose.ui.geometry.Size(30f * scale, 30f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale)
                )
                drawRoundRect(
                    color = Color(0xFF1C1C1E),
                    topLeft = androidx.compose.ui.geometry.Offset((x + 14f) * scale, (y + 14f) * scale),
                    size = androidx.compose.ui.geometry.Size(18f * scale, 18f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale)
                )
            }

            drawFinder(10f, 10f)
            drawFinder(144f, 10f)
            drawFinder(10f, 144f)

            for (r in 0 until 14) {
                for (c in 0 until 14) {
                    val x = 20f + c * 11.5f
                    val y = 20f + r * 11.5f
                    val inTL = x < 65f && y < 65f
                    val inTR = x > 135f && y < 65f
                    val inBL = x < 65f && y > 135f
                    val inCenter = x > 80f && x < 120f && y > 80f && y < 120f
                    if (!inTL && !inTR && !inBL && !inCenter) {
                        val v = kotlin.math.sin(seed * 0.001 + r * 7.3 + c * 13.7 + (r * c)) * 10000.0
                        if (kotlin.math.abs(v - kotlin.math.floor(v)) > 0.45) {
                            drawRoundRect(
                                color = Color(0xFF1C1C1E),
                                topLeft = androidx.compose.ui.geometry.Offset(x * scale, y * scale),
                                size = androidx.compose.ui.geometry.Size(7f * scale, 7f * scale),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * scale, 1.5f * scale)
                            )
                        }
                    }
                }
            }

            drawCircle(
                color = Color(0xFF0381FE),
                radius = 18f * scale,
                center = androidx.compose.ui.geometry.Offset(100f * scale, 100f * scale)
            )
        }
        Text(text = "🐾", fontSize = (sizeDp / 10).sp)
    }
}

@Composable
fun CustomerFullscreenQrisDialog(
    amount: Double,
    timerText: String,
    onDismiss: () -> Unit,
    onCompleteSale: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    Text(
                        text = "✕ Back to Cashier",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = timerText,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Center High-Contrast Customer Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "QRIS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "PEMBAYARAN NASIONAL",
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "GPN",
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFE5E7EB)
                    )

                    Text(
                        text = "PawPlace Pet Shop & Grooming",
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "NMID: ID1020038192019 • Bali, Indonesia",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(4.dp, Color(0xFF111827))
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            DynamicQrisCodeBox(amount = amount, sizeDp = 210)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOTAL PEMBAYARAN",
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatCurrency(amount),
                                color = Color(0xFF0381FE),
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "BCA • Mandiri • BRI • BNI • GoPay • OVO • DANA • ShopeePay",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom Cashier Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCompleteSale,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("⚡ Simulate Paid", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onCompleteSale,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0381FE)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("✓ Complete Sale", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    transaction: com.petshop.pos.data.local.entity.TransactionEntity,
    items: List<com.petshop.pos.data.local.entity.TransactionItemEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ThermalReceiptView(transaction = transaction, items = items)

            Spacer(modifier = Modifier.height(14.dp))

            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val storePhone = "6281234567890"
                        val customerMsg = "Hi PawPlace! Please send my digital receipt for Invoice ${transaction.invoiceNumber} (${transaction.paymentMethod.displayName}, Total: ${formatCurrency(transaction.grandTotal)}) 🐾"
                        val encoded = java.net.URLEncoder.encode(customerMsg, "UTF-8")
                        val uri = Uri.parse("https://wa.me/$storePhone?text=$encoded")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("💬 WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("New Sale ➔", fontWeight = FontWeight.Bold)
            }
        }
    }
}
