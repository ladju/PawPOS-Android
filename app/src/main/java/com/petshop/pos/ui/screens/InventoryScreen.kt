package com.petshop.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.data.local.entity.StockChangeReason
import com.petshop.pos.ui.components.*
import com.petshop.pos.ui.theme.*
import com.petshop.pos.ui.viewmodel.InventoryViewModel
import com.petshop.pos.ui.viewmodel.StockFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    var quickTypeProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var quickCategoryProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddProductDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search product name, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Product Type Filter Pills (Dry Cat Food, Wet Cat Food, Bird Food, Fish Food, obat cacing, Accesories, Grooming, Cat Litter)
            ProductTypePills(
                selectedType = uiState.selectedType,
                onTypeSelected = { viewModel.selectType(it) }
            )

            // Stock Health filter tabs (All, Low Stock, Out of Stock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.stockFilter == StockFilter.ALL,
                    onClick = { viewModel.selectStockFilter(StockFilter.ALL) },
                    label = { Text("All (${products.size})") }
                )
                FilterChip(
                    selected = uiState.stockFilter == StockFilter.LOW_STOCK,
                    onClick = { viewModel.selectStockFilter(StockFilter.LOW_STOCK) },
                    label = { Text("Low Stock (${lowStockCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = Color(0xFFB45309)
                    )
                )
                FilterChip(
                    selected = uiState.stockFilter == StockFilter.OUT_OF_STOCK,
                    onClick = { viewModel.selectStockFilter(StockFilter.OUT_OF_STOCK) },
                    label = { Text("Out of Stock") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFE4E6),
                        selectedLabelColor = CoralAccent
                    )
                )
            }

            // Low Stock Warning Banner
            if (lowStockCount > 0 && uiState.stockFilter == StockFilter.ALL) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$lowStockCount items are running low on stock! Tap 'Low Stock' to review.",
                            color = Color(0xFFB45309),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No inventory items found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        InventoryItemCard(
                            product = product,
                            onQuickStockStep = { delta -> viewModel.quickStepStock(product.id, delta) },
                            onAdjustStock = { viewModel.openAdjustStockDialog(product) },
                            onEdit = { viewModel.openEditProductDialog(product) },
                            onEditCategory = { quickCategoryProduct = product },
                            onEditType = { quickTypeProduct = product }
                        )
                    }
                }
            }
        }
    }

    // Quick Change Product Type Dialog
    quickTypeProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { quickTypeProduct = null },
            title = {
                Column {
                    Text("Edit Product Type", fontWeight = FontWeight.Bold)
                    Text(product.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select new type for this product:", style = MaterialTheme.typography.labelMedium)
                    ProductType.entries.filter { it != ProductType.ALL }.forEach { type ->
                        val isCurrent = type == product.productType
                        Surface(
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveProduct(
                                        id = product.id,
                                        barcode = product.barcode,
                                        name = product.name,
                                        category = product.category,
                                        productType = type,
                                        costPrice = product.costPrice,
                                        sellingPrice = product.sellingPrice,
                                        currentStock = product.currentStock,
                                        minStockAlert = product.minStockAlert,
                                        unit = product.unit,
                                        expiryDate = product.expiryDate,
                                        description = product.description,
                                        imageUri = product.imageUri
                                    )
                                    quickTypeProduct = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${type.icon} ${type.displayName}",
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (isCurrent) {
                                    Text("Active ✓", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { quickTypeProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Change Pet Category Dialog
    quickCategoryProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { quickCategoryProduct = null },
            title = {
                Column {
                    Text("Edit Pet Category", fontWeight = FontWeight.Bold)
                    Text(product.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select new pet category for this product:", style = MaterialTheme.typography.labelMedium)
                    PetCategory.entries.filter { it != PetCategory.ALL }.forEach { category ->
                        val isCurrent = category == product.category
                        Surface(
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveProduct(
                                        id = product.id,
                                        barcode = product.barcode,
                                        name = product.name,
                                        category = category,
                                        productType = product.productType,
                                        costPrice = product.costPrice,
                                        sellingPrice = product.sellingPrice,
                                        currentStock = product.currentStock,
                                        minStockAlert = product.minStockAlert,
                                        unit = product.unit,
                                        expiryDate = product.expiryDate,
                                        description = product.description,
                                        imageUri = product.imageUri
                                    )
                                    quickCategoryProduct = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${category.icon} ${category.displayName}",
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (isCurrent) {
                                    Text("Active ✓", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { quickCategoryProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Product Sheet
    if (uiState.isAddEditSheetOpen) {
        AddEditProductSheet(
            product = uiState.selectedProductForEdit,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { id, barcode, name, cat, type, cost, price, stock, minStock, unit, exp, desc, imageUri ->
                viewModel.saveProduct(
                    id, barcode, name, cat, type, cost, price, stock, minStock, unit, exp, desc, imageUri
                )
            },
            onDelete = uiState.selectedProductForEdit?.let { prod ->
                { viewModel.deleteProduct(prod) }
            }
        )
    }

    // Quick Stock Adjustment Dialog
    if (uiState.isAdjustStockDialogOpen && uiState.selectedProductForStockAdjust != null) {
        AdjustStockDialog(
            product = uiState.selectedProductForStockAdjust!!,
            onDismiss = { viewModel.closeAdjustStockDialog() },
            onConfirm = { delta, reason, note ->
                viewModel.adjustStock(
                    uiState.selectedProductForStockAdjust!!.id,
                    delta,
                    reason,
                    note
                )
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    product: ProductEntity,
    onQuickStockStep: (Int) -> Unit,
    onAdjustStock: () -> Unit,
    onEdit: () -> Unit,
    onEditCategory: () -> Unit,
    onEditType: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!product.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = product.imageUri,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Text(
                            text = product.category.icon,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }

                StockBadge(
                    currentStock = product.currentStock,
                    minAlert = product.minStockAlert,
                    isService = product.productType.isService || product.category == PetCategory.SERVICES
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barcode & Editable Category / Type Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (product.expiryDate != null) {
                        Text(
                            text = "Exp: ${product.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                // Interactive Editable Pet Category & Product Type Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge (Editable)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onEditCategory() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${product.category.icon} ${product.category.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Product Type Badge (Editable)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onEditType() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${product.productType.icon} ${product.productType.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Type",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Profit margin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cost: ${formatCurrency(product.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Price: ${formatCurrency(product.sellingPrice)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Margin: %.1f%%".format(product.profitMargin),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.profitMargin > 30) Color(0xFF047857) else Color(0xFFB45309)
                    )
                    Text(
                        text = "${product.currentStock} ${product.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Quick Stepper & Safe Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline Quick Stock Stepper
                if (!product.productType.isService && product.category != PetCategory.SERVICES) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        FilledIconButton(
                            onClick = { onQuickStockStep(-1) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            enabled = product.currentStock > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Stock", modifier = Modifier.size(18.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAdjustStock() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${product.currentStock}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Stock",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        FilledIconButton(
                            onClick = { onQuickStockStep(1) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Teal40,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Stock", modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Unlimited Service",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onAdjustStock,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalIconButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductSheet(
    product: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, PetCategory, ProductType, Double, Double, Int, Int, String, String?, String, String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var barcode by remember(product) { mutableStateOf(product?.barcode ?: "") }
    var selectedCategory by remember(product) { mutableStateOf(product?.category ?: PetCategory.DOGS) }
    var selectedType by remember(product) { mutableStateOf(product?.productType ?: ProductType.DRY_CAT_FOOD) }
    var costPriceInput by remember(product) {
        mutableStateOf(if (product != null && product.costPrice > 0) formatCurrency(product.costPrice) else "")
    }
    var sellingPriceInput by remember(product) {
        mutableStateOf(if (product != null && product.sellingPrice > 0) formatCurrency(product.sellingPrice) else "")
    }
    var currentStock by remember(product) { mutableStateOf(product?.currentStock?.toString() ?: "10") }
    var minStockAlert by remember(product) { mutableStateOf(product?.minStockAlert?.toString() ?: "5") }
    var unit by remember(product) { mutableStateOf(product?.unit ?: "pcs") }
    var expiryDate by remember(product) { mutableStateOf(product?.expiryDate ?: "") }
    var description by remember(product) { mutableStateOf(product?.description ?: "") }
    var imageUri by remember(product) { mutableStateOf(product?.imageUri) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (product == null) "Add New Product" else "Edit Product",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProductImagePicker(
                selectedImageUri = imageUri,
                onImageSelected = { imageUri = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode / SKU *") },
                placeholder = { Text("Scan picture or enter SKU") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val sampleBarcodes = listOf("8992761102914", "8850389104021", "8999999195518", "8992745330128")
                            barcode = sampleBarcodes.random()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Barcode from Picture",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                supportingText = {
                    Text(
                        text = "📷 Tap camera icon to scan barcode from photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category selector
            Text(text = "Pet Category", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            PetCategoryPills(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Product Type selector
            Text(text = "Product Type", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            ProductTypePills(
                selectedType = selectedType,
                onTypeSelected = { if (it != ProductType.ALL) selectedType = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing Row (with live auto IDR formatting)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = costPriceInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        costPriceInput = if (digits.isBlank()) "" else formatCurrency(digits.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Cost Price (Rp)") },
                    placeholder = { Text("Rp 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sellingPriceInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        sellingPriceInput = if (digits.isBlank()) "" else formatCurrency(digits.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Selling Price (Rp) *") },
                    placeholder = { Text("Rp 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = currentStock,
                    onValueChange = { currentStock = it },
                    label = { Text("Stock Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minStockAlert,
                    onValueChange = { minStockAlert = it },
                    label = { Text("Min Stock Alert") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (pcs/kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text("Expiry Date (YYYY-MM-DD, optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val cost = costPriceInput.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    val sell = sellingPriceInput.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    val stock = currentStock.toIntOrNull() ?: 0
                    val alert = minStockAlert.toIntOrNull() ?: 5
                    onSave(
                        product?.id ?: 0L,
                        barcode,
                        name,
                        selectedCategory,
                        selectedType,
                        cost,
                        sell,
                        stock,
                        alert,
                        unit,
                        expiryDate,
                        description,
                        imageUri
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Product", fontWeight = FontWeight.Bold)
            }

            // Guarded Delete Button when editing existing product
            if (product != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Product from Catalog", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteConfirmDialog && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to remove '${product.name}' from your catalog? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdjustStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, StockChangeReason, String) -> Unit
) {
    var deltaInput by remember { mutableStateOf("10") }
    var selectedReason by remember { mutableStateOf(StockChangeReason.RESTOCK) }
    var note by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Adjust Stock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Current Stock: ${product.currentStock} ${product.unit}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Add / Deduct
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAddition,
                        onClick = {
                            isAddition = true
                            selectedReason = StockChangeReason.RESTOCK
                        },
                        label = { Text("+ Add Stock (In)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isAddition,
                        onClick = {
                            isAddition = false
                            selectedReason = StockChangeReason.DAMAGED
                        },
                        label = { Text("- Deduct Stock (Out)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deltaInput,
                    onValueChange = { deltaInput = it },
                    label = { Text("Quantity Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Adjustment Reason:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                StockChangeReason.entries
                    .filter { if (isAddition) it == StockChangeReason.RESTOCK || it == StockChangeReason.AUDIT else it != StockChangeReason.RESTOCK }
                    .forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(text = reason.displayName, fontSize = 13.sp)
                        }
                    }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. Supplier invoice #, expiry batch)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val qty = deltaInput.toIntOrNull() ?: 0
                            val signedQty = if (isAddition) qty else -qty
                            onConfirm(signedQty, selectedReason, note)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
