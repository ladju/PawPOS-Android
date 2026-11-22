package com.petshop.pos.data.local.entity

enum class PetCategory(val displayName: String, val icon: String) {
    ALL("All Products", "🐾"),
    DOGS("Dogs", "🐶"),
    CATS("Cats", "🐱"),
    BIRDS("Birds", "🦜"),
    AQUATICS("Aquatics", "🐠"),
    SMALL_PETS("Small Pets", "🐹"),
    SERVICES("Services", "✂️");

    companion object {
        fun fromString(name: String): PetCategory {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: ALL
        }
    }
}

enum class ProductType(
    val displayName: String,
    val icon: String,
    val isService: Boolean = false
) {
    ALL("All Types", "🐾"),
    DRY_CAT_FOOD("Dry Cat Food", "🐱"),
    WET_CAT_FOOD("Wet Cat Food", "🥫"),
    BIRD_FOOD("Bird Food", "🦜"),
    FISH_FOOD("Fish Food", "🐠"),
    OBAT_CACING("Obat Cacing", "💊"),
    ACCESORIES("Accessories", "🎀"),
    GROOMING("Grooming", "✂️", isService = true),
    CAT_LITTER("Cat Litter", "📦");

    companion object {
        fun fromString(name: String): ProductType {
            return entries.find {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: ALL
        }
    }
}

enum class PaymentMethod(val displayName: String) {
    CASH("💵 Cash"),
    QRIS("📲 QRIS")
}

enum class StockChangeReason(val displayName: String) {
    RESTOCK("Restock / Shipment Arrival"),
    SALE("Customer Sale"),
    DAMAGED("Damaged / Defect"),
    EXPIRED("Expired Goods Disposal"),
    AUDIT("Inventory Count Adjustment")
}
