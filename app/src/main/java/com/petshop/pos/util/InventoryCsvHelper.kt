package com.petshop.pos.util

import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType

data class CsvImportParseResult(
    val products: List<ProductEntity>,
    val skippedRows: Int,
    val errors: List<String>
)

object InventoryCsvHelper {

    private val HEADERS = listOf(
        "barcode",
        "name",
        "category",
        "productType",
        "costPrice",
        "sellingPrice",
        "currentStock",
        "minStockAlert",
        "unit",
        "expiryDate",
        "description"
    )

    fun exportToCsv(products: List<ProductEntity>): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(",")).append("\n")
        for (p in products) {
            val row = listOf(
                escapeCsv(p.barcode),
                escapeCsv(p.name),
                escapeCsv(p.category.name),
                escapeCsv(p.productType.name),
                formatNumber(p.costPrice),
                formatNumber(p.sellingPrice),
                p.currentStock.toString(),
                p.minStockAlert.toString(),
                escapeCsv(p.unit),
                escapeCsv(p.expiryDate.orEmpty()),
                escapeCsv(p.description)
            )
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    fun parseCsv(rawContent: String): CsvImportParseResult {
        val content = rawContent.removePrefix("\uFEFF").trim()
        if (content.isEmpty()) {
            return CsvImportParseResult(emptyList(), 0, listOf("CSV file is empty"))
        }

        val firstLineEnd = content.indexOfAny(charArrayOf('\r', '\n')).let {
            if (it == -1) content.length else it
        }
        val firstLine = content.substring(0, firstLineEnd)
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        val rows = parseCsvRecords(content, delimiter)
        if (rows.isEmpty()) {
            return CsvImportParseResult(emptyList(), 0, listOf("No rows found in CSV"))
        }

        val firstRowLower = rows.first().map { it.trim().lowercase() }
        val hasHeader = firstRowLower.any {
            it in setOf("barcode", "sku", "name", "product_name", "sellingprice", "price", "currentstock", "stock")
        }

        val colMap: Map<String, Int>
        val dataRows: List<List<String>>
        if (hasHeader) {
            colMap = buildColumnMap(firstRowLower)
            dataRows = rows.drop(1)
        } else {
            colMap = HEADERS.mapIndexed { idx, h -> h.lowercase() to idx }.toMap()
            dataRows = rows
        }

        val parsed = mutableListOf<ProductEntity>()
        val errors = mutableListOf<String>()
        var skipped = 0

        dataRows.forEachIndexed { index, row ->
            val lineNum = index + if (hasHeader) 2 else 1
            if (row.all { it.isBlank() }) return@forEachIndexed

            fun col(vararg keys: String): String {
                for (k in keys) {
                    val idx = colMap[k]
                    if (idx != null && idx in row.indices) {
                        return row[idx].trim()
                    }
                }
                return ""
            }

            val barcode = col("barcode", "sku", "kode")
            val name = col("name", "product_name", "nama", "product")

            if (barcode.isBlank() || name.isBlank()) {
                skipped++
                errors.add("Row $lineNum: Missing barcode or product name")
                return@forEachIndexed
            }

            val categoryStr = col("category", "kategori")
            val typeStr = col("producttype", "type", "jenis")
            val costPrice = parsePrice(col("costprice", "cost", "hpp", "harga_beli")) ?: 0.0
            val sellingPrice = parsePrice(col("sellingprice", "price", "harga_jual", "harga")) ?: 0.0
            val currentStock = col("currentstock", "stock", "qty", "stok").toIntOrNull()?.coerceAtLeast(0) ?: 0
            val minStockAlert = col("minstockalert", "minstock", "min_stok").toIntOrNull()?.coerceAtLeast(0) ?: 5
            val unit = col("unit", "satuan").ifBlank { "pcs" }
            val expiryDate = col("expirydate", "expiry", "kadaluarsa").ifBlank { null }
            val description = col("description", "desc", "deskripsi")

            parsed.add(
                ProductEntity(
                    barcode = barcode,
                    name = name,
                    category = PetCategory.fromString(categoryStr),
                    productType = ProductType.fromString(typeStr),
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    currentStock = currentStock,
                    minStockAlert = minStockAlert,
                    unit = unit,
                    expiryDate = expiryDate,
                    description = description
                )
            )
        }

        return CsvImportParseResult(parsed, skipped, errors)
    }

    private fun buildColumnMap(headerRow: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headerRow.forEachIndexed { idx, raw ->
            val normalized = raw.replace(" ", "").replace("-", "_")
            map[normalized] = idx
            map[raw] = idx
        }
        return map
    }

    private fun parsePrice(raw: String): Double? {
        if (raw.isBlank()) return null
        val cleaned = raw
            .replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace(" ", "")
            .trim()
        cleaned.toDoubleOrNull()?.let { return it }
        // Fallback if user used commas as thousands separators (e.g. "125,000")
        return cleaned.replace(",", "").toDoubleOrNull()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvRecords(content: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length && content[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(field.toString())
                        field.clear()
                    }
                    '\r' -> {
                        if (i + 1 < content.length && content[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(field.toString())
                        field.clear()
                        records.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\n' -> {
                        currentRow.add(field.toString())
                        field.clear()
                        records.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }

        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            records.add(currentRow)
        }

        return records
    }
}
