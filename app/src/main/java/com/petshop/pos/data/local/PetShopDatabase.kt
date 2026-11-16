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
                    barcode = "89970001",
                    name = "Rana Life Cat",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 12500.0,
                    sellingPrice = 14000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "can",
                    expiryDate = "2027-12-31",
                    description = "Pakan Basah Kaleng"
                ),
                ProductEntity(
                    barcode = "89970002",
                    name = "Rana Oricat",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 12500.0,
                    sellingPrice = 14000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "can",
                    expiryDate = "2027-12-31",
                    description = "Pakan Basah Kaleng"
                ),
                ProductEntity(
                    barcode = "89970003",
                    name = "Rana Bolt",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 11500.0,
                    sellingPrice = 13000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "can",
                    expiryDate = "2027-12-31",
                    description = "Pakan Basah Kaleng"
                ),
                ProductEntity(
                    barcode = "89970004",
                    name = "Rana Pocha",
                    category = PetCategory.CATS,
                    productType = ProductType.WET_CAT_FOOD,
                    costPrice = 11500.0,
                    sellingPrice = 13000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "can",
                    expiryDate = "2027-12-31",
                    description = "Pakan Basah Kaleng"
                ),
                ProductEntity(
                    barcode = "89970005",
                    name = "Rana Felibite",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 11500.0,
                    sellingPrice = 13000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970006",
                    name = "Rana Nutristar",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 9500.0,
                    sellingPrice = 11500.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970007",
                    name = "Rana Happy Feline",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 9500.0,
                    sellingPrice = 11500.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970008",
                    name = "Rana Winky",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 10500.0,
                    sellingPrice = 12000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970009",
                    name = "Rana Top Pet",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 14000.0,
                    sellingPrice = 15000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970010",
                    name = "Rana Lezato 1Kg",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 21000.0,
                    sellingPrice = 23000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970011",
                    name = "Rana Lezato Kitten",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 18500.0,
                    sellingPrice = 21000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970012",
                    name = "Rana Exel",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 11000.0,
                    sellingPrice = 12500.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970013",
                    name = "Rana Cat Choize",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 17500.0,
                    sellingPrice = 20000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970014",
                    name = "Rana Bolt",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 16600.0,
                    sellingPrice = 180000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970015",
                    name = "Rana Meo Repack",
                    category = PetCategory.CATS,
                    productType = ProductType.DRY_CAT_FOOD,
                    costPrice = 22600.0,
                    sellingPrice = 24000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Kering"
                ),
                ProductEntity(
                    barcode = "89970016",
                    name = "Rana Takari",
                    category = PetCategory.AQUATICS,
                    productType = ProductType.FISH_FOOD,
                    costPrice = 5000.0,
                    sellingPrice = 6000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Ikan"
                ),
                ProductEntity(
                    barcode = "89970017",
                    name = "Rana Kenari",
                    category = PetCategory.BIRDS,
                    productType = ProductType.BIRD_FOOD,
                    costPrice = 8000.0,
                    sellingPrice = 9000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Burung"
                ),
                ProductEntity(
                    barcode = "89970018",
                    name = "Rana Kroto",
                    category = PetCategory.BIRDS,
                    productType = ProductType.BIRD_FOOD,
                    costPrice = 10000.0,
                    sellingPrice = 11000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Burung"
                ),
                ProductEntity(
                    barcode = "89970019",
                    name = "Rana Ikn Repack",
                    category = PetCategory.AQUATICS,
                    productType = ProductType.FISH_FOOD,
                    costPrice = 11000.0,
                    sellingPrice = 12500.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Pakan Ikan"
                ),
                ProductEntity(
                    barcode = "89970020",
                    name = "Rana Kalung",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 2000.0,
                    sellingPrice = 5000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970021",
                    name = "Rana Mangkok 2 Sekat",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 5300.0,
                    sellingPrice = 9000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Wadah Pakan"
                ),
                ProductEntity(
                    barcode = "89970022",
                    name = "Rana Mangkok 2 Sekat",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 5500.0,
                    sellingPrice = 9000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Wadah Pakan"
                ),
                ProductEntity(
                    barcode = "89970023",
                    name = "Rana Kandang Kecil",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 92000.0,
                    sellingPrice = 120000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "unit",
                    expiryDate = "2027-12-31",
                    description = "Kandang"
                ),
                ProductEntity(
                    barcode = "89970024",
                    name = "Rana Kandang Besar",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 135000.0,
                    sellingPrice = 160000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "unit",
                    expiryDate = "2027-12-31",
                    description = "Kandang"
                ),
                ProductEntity(
                    barcode = "89970025",
                    name = "Rana Kandang Tingkat",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 142000.0,
                    sellingPrice = 225000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "unit",
                    expiryDate = "2027-12-31",
                    description = "Kandang"
                ),
                ProductEntity(
                    barcode = "89970026",
                    name = "Rana Pasir",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 80000.0,
                    sellingPrice = 20000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "bag",
                    expiryDate = "2027-12-31",
                    description = "Pasir Wangi (/5kg 2kg 9rb)"
                ),
                ProductEntity(
                    barcode = "89970027",
                    name = "Rana Pasir",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 14000.0,
                    sellingPrice = 17000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "bag",
                    expiryDate = "2027-12-31",
                    description = "Pasir Halus"
                ),
                ProductEntity(
                    barcode = "89970028",
                    name = "Rana Pasir",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 14000.0,
                    sellingPrice = 17000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "bag",
                    expiryDate = "2027-12-31",
                    description = "Pasir Kasar"
                ),
                ProductEntity(
                    barcode = "89970029",
                    name = "Rana Sisir",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 5800.0,
                    sellingPrice = 9000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970030",
                    name = "Rana Gunting Kuku",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 4900.0,
                    sellingPrice = 8000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970031",
                    name = "Rana Mainan",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 4000.0,
                    sellingPrice = 7000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970032",
                    name = "Rana Obat Cacing Kucing",
                    category = PetCategory.CATS,
                    productType = ProductType.OBAT_CACING,
                    costPrice = 12500.0,
                    sellingPrice = 16000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Obat Cacing"
                ),
                ProductEntity(
                    barcode = "89970033",
                    name = "Rana Obat Cacing Sapi",
                    category = PetCategory.DOGS,
                    productType = ProductType.OBAT_CACING,
                    costPrice = 10800.0,
                    sellingPrice = 15000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Obat Cacing"
                ),
                ProductEntity(
                    barcode = "89970034",
                    name = "Rana Kalung Sleyer",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 12800.0,
                    sellingPrice = 18000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970035",
                    name = "Rana Shampo",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 9000.0,
                    sellingPrice = 11000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970036",
                    name = "Rana Kaung Cahaya",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 2500.0,
                    sellingPrice = 6000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970037",
                    name = "Rana Sisir Besar",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 9000.0,
                    sellingPrice = 13000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970038",
                    name = "Rana Serok Pasir",
                    category = PetCategory.CATS,
                    productType = ProductType.CAT_LITTER,
                    costPrice = 3000.0,
                    sellingPrice = 6000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "bag",
                    expiryDate = "2027-12-31",
                    description = "Accessories"
                ),
                ProductEntity(
                    barcode = "89970039",
                    name = "Rana Mangkok Sigle",
                    category = PetCategory.CATS,
                    productType = ProductType.ACCESORIES,
                    costPrice = 3700.0,
                    sellingPrice = 5000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Wadah Pakan"
                ),
                ProductEntity(
                    barcode = "89970040",
                    name = "Rana Vitamin Sapi",
                    category = PetCategory.DOGS,
                    productType = ProductType.OBAT_CACING,
                    costPrice = 5700.0,
                    sellingPrice = 8000.0,
                    currentStock = 25,
                    minStockAlert = 5,
                    unit = "pcs",
                    expiryDate = "2027-12-31",
                    description = "Obat Cacing"
                ),
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
