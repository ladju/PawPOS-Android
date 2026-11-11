package com.petshop.pos.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.petshop.pos.data.local.dao.ProductDao
import com.petshop.pos.data.local.dao.StockMovementDao
import com.petshop.pos.data.local.dao.TransactionDao
import com.petshop.pos.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromPetCategory(value: PetCategory): String = value.name

    @TypeConverter
    fun toPetCategory(value: String): PetCategory = PetCategory.fromString(value)

    @TypeConverter
    fun fromProductType(value: ProductType): String = value.name

    @TypeConverter
    fun toProductType(value: String): ProductType = try {
        ProductType.valueOf(value)
    } catch (e: Exception) {
        when (value) {
            "FOOD" -> ProductType.DRY_CAT_FOOD
            "HEALTH" -> ProductType.OBAT_CACING
            "TOY", "ACCESSORY" -> ProductType.ACCESORIES
            "GROOMING", "SERVICE" -> ProductType.GROOMING
            else -> ProductType.ALL
        }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromStockChangeReason(value: StockChangeReason): String = value.name

    @TypeConverter
    fun toStockChangeReason(value: String): StockChangeReason = try {
        StockChangeReason.valueOf(value)
    } catch (e: Exception) {
        StockChangeReason.RESTOCK
    }
}

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockMovementEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PetShopDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: PetShopDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PetShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PetShopDatabase::class.java,
                    "petshop_database.db"
                )
                    .addCallback(PetShopDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PetShopDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.productDao())
                }
            }
        }

        private suspend fun populateInitialData(productDao: ProductDao) {
            val initialProducts = listOf(
                ProductEntity(
                    barcode = "8992001",
                    name = "Whiskas Ocean Fish Adult Dry Cat Food 1.2kg",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 52000.0,
                    sellingPrice = 78000.0,
                    currentStock = 22,
                    minStockAlert = 6,
                    unit = "bag",
                    expiryDate = "2027-09-01",
                    description = "100% complete and balanced dry cat food with real ocean fish."
                ),
                ProductEntity(
                    barcode = "8992006",
                    name = "Royal Canin Kitten Dry Cat Food 2kg",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 180000.0,
                    sellingPrice = 265000.0,
                    currentStock = 15,
                    minStockAlert = 5,
                    unit = "bag",
                    expiryDate = "2028-01-10",
                    description = "Formulated with precise nutrients for growing kittens up to 12 months."
                ),
                ProductEntity(
                    barcode = "8992002",
                    name = "Sheba Premium Wet Cat Food Tuna & Salmon 85g",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 8500.0,
                    sellingPrice = 14000.0,
                    currentStock = 45,
                    minStockAlert = 10,
                    unit = "pouch",
                    expiryDate = "2027-12-15",
                    description = "Luxurious tender flakes in culinary gourmet gravy."
                ),
                ProductEntity(
                    barcode = "8992007",
                    name = "Whiskas Pouch Mackerel Wet Cat Food 80g",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 6000.0,
                    sellingPrice = 9000.0,
                    currentStock = 60,
                    minStockAlert = 15,
                    unit = "pouch",
                    expiryDate = "2027-10-25",
                    description = "Delicious mackerel meat with essential vitamins for adult cats."
                ),
                ProductEntity(
                    barcode = "8993001",
                    name = "Kaytee Forti-Diet Pro Health Canary & Finch Bird Food 1kg",
                    category = PetCategory.BIRDS,
                    productType = ProductType.BIRD_FOOD,
                    costPrice = 38000.0,
                    sellingPrice = 62000.0,
                    currentStock = 14,
                    minStockAlert = 4,
                    unit = "bag",
                    expiryDate = "2027-10-30",
                    description = "Enriched seed blend with DHA Omega-3 and probiotics for vibrant feathers."
                ),
                ProductEntity(
                    barcode = "8994001",
                    name = "TetraMin Tropical Flakes Aquarium Fish Food 100g",
                    category = PetCategory.AQUATICS,
                    productType = ProductType.FISH_FOOD,
                    costPrice = 32000.0,
                    sellingPrice = 55000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "can",
                    expiryDate = "2028-03-01",
                    description = "Nutritionally balanced staple diet for all tropical aquarium fish."
                ),
                ProductEntity(
                    barcode = "8992005",
                    name = "Drontal Cat Obat Cacing Tablet (Dewormer)",
                    category = PetCategory.CATS,
                    productType = ProductType.OBAT_CACING,
                    costPrice = 18000.0,
                    sellingPrice = 28000.0,
                    currentStock = 35,
                    minStockAlert = 5,
                    unit = "tablet",
                    expiryDate = "2028-06-30",
                    description = "Veterinary anthelmintic tablet for roundworms, tapeworms, and hookworms."
                ),
                ProductEntity(
                    barcode = "8992008",
                    name = "Wormzol-K Obat Cacing Kucing 10ml",
                    category = PetCategory.CATS,
                    productType = ProductType.OBAT_CACING,
                    costPrice = 11000.0,
                    sellingPrice = 18000.0,
                    currentStock = 24,
                    minStockAlert = 5,
                    unit = "bottle",
                    expiryDate = "2027-12-31",
                    description = "Fast acting suspension anthelmintic for kittens and adult cats."
                ),
                ProductEntity(
                    barcode = "8993002",
                    name = "Prevue Hanging Bird Cage with Feeders",
                    category = PetCategory.BIRDS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 210000.0,
                    sellingPrice = 350000.0,
                    currentStock = 3,
                    minStockAlert = 2,
                    unit = "unit",
                    description = "Powder-coated bird cage with perches and easy-clean removable tray."
                ),
                ProductEntity(
                    barcode = "8992004",
                    name = "Cat Feather Wand & Bell Interactive Toy",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 12000.0,
                    sellingPrice = 25000.0,
                    currentStock = 30,
                    minStockAlert = 5,
                    unit = "pcs",
                    description = "Flexible wand with natural feathers and jingle bell."
                ),
                ProductEntity(
                    barcode = "8994002",
                    name = "Sunsun Submersible Aquarium Filter 500L/H",
                    category = PetCategory.AQUATICS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 65000.0,
                    sellingPrice = 115000.0,
                    currentStock = 8,
                    minStockAlert = 3,
                    unit = "unit",
                    description = "Quiet multi-stage filtration for fresh and saltwater aquariums."
                ),
                ProductEntity(
                    barcode = "8991004",
                    name = "KONG Classic Durable Rubber Chew Toy",
                    category = PetCategory.DOGS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 65000.0,
                    sellingPrice = 110000.0,
                    currentStock = 15,
                    minStockAlert = 3,
                    unit = "pcs",
                    description = "Ultra-durable natural rubber toy that fulfills instinctual chewing needs."
                ),
                ProductEntity(
                    barcode = "8996001",
                    name = "Full Pet Spa & Grooming (Bath & Cut)",
                    category = PetCategory.SERVICES,
                    productType = ProductType.GROOMING,
                    costPrice = 35000.0,
                    sellingPrice = 150000.0,
                    currentStock = 999,
                    minStockAlert = 0,
                    unit = "session",
                    description = "Complete shampoo, blow-dry, nail trim, ear clean, and custom styling."
                ),
                ProductEntity(
                    barcode = "8996002",
                    name = "Nail Trimming & Ear Cleaning Service",
                    category = PetCategory.SERVICES,
                    productType = ProductType.GROOMING,
                    costPrice = 10000.0,
                    sellingPrice = 45000.0,
                    currentStock = 999,
                    minStockAlert = 0,
                    unit = "session",
                    description = "Quick and gentle nail clip and hygienic ear canal cleaning."
                ),
                ProductEntity(
                    barcode = "8992003",
                    name = "Tidy Cats Clumping Bentonite Litter 10kg",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 60000.0,
                    sellingPrice = 95000.0,
                    currentStock = 2,
                    minStockAlert = 5,
                    unit = "bag",
                    description = "Superior odor control with quick clumping natural bentonite clay."
                ),
                ProductEntity(
                    barcode = "8992009",
                    name = "Pasir Kucing Gumpal Tofu Scented 7L",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 42000.0,
                    sellingPrice = 65000.0,
                    currentStock = 16,
                    minStockAlert = 4,
                    unit = "bag",
                    description = "Eco-friendly flushable tofu cat litter with fast clumping and fresh lavender aroma."
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
