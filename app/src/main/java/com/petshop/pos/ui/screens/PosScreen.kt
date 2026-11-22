package com.petshop.pos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.petshop.pos.domain.Money
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.ui.components.*
import com.petshop.pos.ui.theme.CoralAccent
import com.petshop.pos.ui.theme.LocalAppStrings
import com.petshop.pos.ui.theme.OneUiBlue
import com.petshop.pos.ui.theme.OneUiGreen
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
    val strings = LocalAppStrings.current
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
                        placeholder = { Text(strings.posSearchHint) },
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onNavigateToScanner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
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
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("cart_pill")
                                .clickable { isCartSheetOpen = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp)
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
                                    text = strings.posItemsTapReview.format(uiState.totalItemsCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatCurrency(uiState.grandTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                text = strings.posCharge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
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
                            text = strings.posNoProducts,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.posNoProductsHint,
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
                    columns = GridCells.Adaptive(minSize = 150.dp),
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
            onQrisReferenceChanged = { viewModel.setQrisReference(it) },
            onCustomerDetailsChanged = { name, phone -> viewModel.setCustomerDetails(name, phone) },
            onDismissError = { viewModel.dismissError() },
            onComplete = { viewModel.completeCheckout() }
        )
    }

    // Chime only once the sale is really saved, and only for QRIS (cashier's audible cue).
    val savedTransaction = uiState.lastTransaction
    LaunchedEffect(savedTransaction?.id, uiState.isReceiptDialogOpen) {
        if (uiState.isReceiptDialogOpen && savedTransaction?.paymentMethod == PaymentMethod.QRIS) {
            playPosVerificationChime()
        }
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
    val strings = LocalAppStrings.current
    val isInCart = inCartQuantity > 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isInCart) BorderStroke(2.dp, Teal40) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_${product.id}")
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
                            text = "🛒 " + strings.posInCart.format(inCartQuantity),
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
                        .clip(RoundedCornerShape(14.dp))
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
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        FilledIconButton(
                            onClick = onDecrease,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
    val strings = LocalAppStrings.current
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
                    text = strings.posReviewOrder.format(uiState.totalItemsCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearCart) {
                    Text(strings.posClearAll, color = MaterialTheme.colorScheme.error)
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
                                RoundedCornerShape(14.dp)
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
                                text = strings.posEach.format(formatCurrency(item.product.sellingPrice)),
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
                                shape = RoundedCornerShape(14.dp),
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
                                shape = RoundedCornerShape(14.dp),
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
                Text(strings.posSubtotal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(formatCurrency(uiState.subtotal), fontWeight = FontWeight.SemiBold)
            }
            if (uiState.appliedDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.posDiscount, color = MaterialTheme.colorScheme.error)
                    Text("-${formatCurrency(uiState.appliedDiscount)}", color = MaterialTheme.colorScheme.error)
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
                        text = if (uiState.taxRate > 0) strings.taxPercentLabel.format(uiState.taxPercent) else strings.taxApply,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = if (uiState.taxRate > 0) formatCurrency(uiState.taxAmount) else strings.taxNotIncluded,
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
                    text = strings.posGrandTotal,
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
                    .height(52.dp)
                    .testTag("proceed_to_payment"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = strings.posProceed,
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
    onCashTenderedChanged: (Long) -> Unit,
    onQrisReferenceChanged: (String) -> Unit,
    onCustomerDetailsChanged: (String, String) -> Unit,
    onDismissError: () -> Unit,
    onComplete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val busy = uiState.isProcessing
    var custName by remember { mutableStateOf(uiState.customerName) }
    var custPhone by remember { mutableStateOf(uiState.customerPhone) }
    // QRIS cannot be confirmed by the app, so the cashier must explicitly attest they saw the payment.
    var paymentSeen by remember(uiState.paymentMethod) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .heightIn(max = 760.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ---- header (fixed) ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBackToCart, enabled = !busy) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Review Cart")
                        }
                        Text(
                            text = strings.coTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // ---- scrollable body ----
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    uiState.errorMessage?.let { message ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralAccent)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissError) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss message", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Tappable order summary to go back and edit the cart
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) { onBackToCart() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.coItemsInCart.format(uiState.totalItemsCount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strings.coReviewItems,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Amount due
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.coAmountDue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatCurrency(uiState.grandTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    append("Subtotal ").append(formatCurrency(uiState.discountedSubtotal))
                                    if (uiState.taxPercent > 0 && uiState.taxAmount > 0) {
                                        append(" + Tax (").append(uiState.taxPercent).append("%) ")
                                        append(formatCurrency(uiState.taxAmount))
                                    } else {
                                        append(" • No tax")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = strings.coPaymentMethod,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = method == uiState.paymentMethod,
                                onClick = { onPaymentMethodChanged(method) },
                                enabled = !busy,
                                label = {
                                    Text(
                                        text = method.displayName,
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

                    Spacer(Modifier.height(16.dp))

                    if (uiState.paymentMethod == PaymentMethod.CASH) {
                        CashTenderSection(
                            uiState = uiState,
                            enabled = !busy,
                            onCashTenderedChanged = onCashTenderedChanged
                        )
                    } else {
                        QrisManualSection(
                            uiState = uiState,
                            enabled = !busy,
                            paymentSeen = paymentSeen,
                            onPaymentSeenChanged = { paymentSeen = it },
                            onReferenceChanged = onQrisReferenceChanged
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = custName,
                        onValueChange = {
                            custName = it.take(60)
                            onCustomerDetailsChanged(custName, custPhone)
                        },
                        enabled = !busy,
                        label = { Text(strings.coCustomerName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ---- pinned footer: the action is always reachable, even on a 360x640 phone ----
                val isQris = uiState.paymentMethod == PaymentMethod.QRIS
                val cashShort = !isQris && !uiState.isTenderSufficient
                val canSubmit = uiState.canCompleteCheckout && (!isQris || paymentSeen)
                Button(
                    onClick = onComplete,
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                        .height(52.dp)
                        .testTag("complete_sale"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    when {
                        busy -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(strings.coProcessing, fontWeight = FontWeight.Bold)
                        }
                        cashShort -> {
                            Text(
                                text = strings.coShortBy.format(formatCurrency(uiState.grandTotal - uiState.cashTendered)),
                                fontWeight = FontWeight.Bold,
                                color = CoralAccent,
                                maxLines = 1
                            )
                        }
                        isQris && !paymentSeen -> {
                            Text(strings.coConfirmFirst, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        else -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isQris) strings.coCompleteQris else strings.coCompleteCash,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Cash received field + quick-tender chips + change box. The ViewModel's cashTendered is the only stored value. */
@Composable
private fun CashTenderSection(
    uiState: PosUiState,
    enabled: Boolean,
    onCashTenderedChanged: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    fun display(amount: Long) = if (amount > 0) formatCurrency(amount) else ""

    // Text + cursor live here only so the caret stays at the end while "Rp 1.250.000" is being reformatted.
    var field by remember { mutableStateOf(TextFieldValue(display(uiState.cashTendered), TextRange(display(uiState.cashTendered).length))) }
    LaunchedEffect(uiState.cashTendered) {
        val shown = display(uiState.cashTendered)
        if (field.text != shown) field = TextFieldValue(shown, TextRange(shown.length))
    }

    Text(
        text = strings.coCashReceived,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val amount = Money.parseDigits(input.text)
            val shown = display(amount)
            field = TextFieldValue(shown, TextRange(shown.length))
            onCashTenderedChanged(amount)
        },
        enabled = enabled,
        label = { Text(strings.coCashReceivedField) },
        placeholder = { Text("Rp 0") },
        trailingIcon = {
            if (field.text.isNotEmpty()) {
                IconButton(onClick = { onCashTenderedChanged(0L) }, enabled = enabled) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear cash input")
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_input"),
        shape = RoundedCornerShape(14.dp)
    )

    Spacer(Modifier.height(10.dp))
    Text(
        text = strings.coQuickAmounts,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))

    val predictions = remember(uiState.grandTotal) { calculateCashPredictions(uiState.grandTotal) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        predictions.forEach { pred ->
            val isSelected = uiState.cashTendered == pred.amount
            FilterChip(
                selected = isSelected,
                onClick = { onCashTenderedChanged(pred.amount) },
                enabled = enabled,
                label = {
                    Text(
                        text = pred.label,
                        fontWeight = if (isSelected || pred.isExact) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OneUiBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    val enough = uiState.isTenderSufficient
    val accent = if (enough) OneUiGreen else CoralAccent
    Surface(
        color = accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enough) strings.coChangeToReturn else strings.coShortByLabel,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
                if (enough) {
                    Text(text = "Kembalian", fontSize = 11.sp, color = accent.copy(alpha = 0.8f))
                }
            }
            Text(
                text = formatCurrency(if (enough) uiState.changeDue else uiState.grandTotal - uiState.cashTendered),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}

/**
 * QRIS without a merchant-issued payload: the app cannot show a scannable code or verify payment. The cashier
 * shows the shop's own QRIS sticker, watches their bank / e-wallet merchant app, and confirms explicitly.
 * The optional reference is whatever the cashier types; the app never invents one.
 */
@Composable
private fun QrisManualSection(
    uiState: PosUiState,
    enabled: Boolean,
    paymentSeen: Boolean,
    onPaymentSeenChanged: (Boolean) -> Unit,
    onReferenceChanged: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.qrisTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.qrisSteps.format(formatCurrency(uiState.grandTotal)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.qrisReference,
                onValueChange = onReferenceChanged,
                enabled = enabled,
                label = { Text(strings.qrisReference) },
                placeholder = { Text(strings.qrisReferenceHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = enabled) { onPaymentSeenChanged(!paymentSeen) }
            ) {
                Checkbox(checked = paymentSeen, onCheckedChange = null, enabled = enabled)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.qrisSeen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (uiState.qrisMdrRate <= 0.0) strings.qrisNoFee
                else strings.qrisFee.format("%.1f%%".format(uiState.qrisMdrRate * 100)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/** Plays after a QRIS sale is saved (never before). The ToneGenerator is always released. */
private suspend fun playPosVerificationChime() {
    val toneGen = try {
        android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90)
    } catch (_: RuntimeException) {
        return
    }
    try {
        try {
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 350)
        } catch (_: RuntimeException) {
        }
        delay(450)
    } finally {
        toneGen.release()
    }
}

@Composable
fun ReceiptDialog(
    transaction: com.petshop.pos.data.local.entity.TransactionEntity,
    items: List<com.petshop.pos.data.local.entity.TransactionItemEntity>,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
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
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(strings.rcWhatsApp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.rcPrint)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_sale"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(strings.rcNewSale, fontWeight = FontWeight.Bold)
            }
        }
    }
}
