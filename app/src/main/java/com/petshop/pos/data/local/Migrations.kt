package com.petshop.pos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2
 *  - every money column changes from REAL (Double) to INTEGER (Long, whole rupiah)
 *  - transactions.invoiceNumber becomes UNIQUE (existing duplicates are made unique by suffixing the row id)
 *
 * SQLite cannot change column types in place, so each table is renamed to *_old, recreated, copied
 * and the old tables are dropped child-first (so foreign-key cascades never touch the copied rows).
 * No sales history or stock ledger is lost.
 */
object Migrations {

    // MIGRATION_1_2_SQL_BEGIN
    val MIGRATION_1_2_SQL: List<String> = listOf(
        "DROP INDEX IF EXISTS `index_products_barcode`",
        "DROP INDEX IF EXISTS `index_products_category`",
        "DROP INDEX IF EXISTS `index_transaction_items_transactionId`",
        "DROP INDEX IF EXISTS `index_transaction_items_productId`",
        "DROP INDEX IF EXISTS `index_stock_movements_productId`",

        "ALTER TABLE `products` RENAME TO `products_old`",
        "ALTER TABLE `transactions` RENAME TO `transactions_old`",
        "ALTER TABLE `transaction_items` RENAME TO `transaction_items_old`",
        "ALTER TABLE `stock_movements` RENAME TO `stock_movements_old`",

        "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `barcode` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `productType` TEXT NOT NULL, `costPrice` INTEGER NOT NULL, `sellingPrice` INTEGER NOT NULL, `currentStock` INTEGER NOT NULL, `minStockAlert` INTEGER NOT NULL, `unit` TEXT NOT NULL, `expiryDate` TEXT, `description` TEXT NOT NULL, `imageUri` TEXT, `isActive` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)",
        "CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)",
        "INSERT INTO `products` (`id`,`barcode`,`name`,`category`,`productType`,`costPrice`,`sellingPrice`,`currentStock`,`minStockAlert`,`unit`,`expiryDate`,`description`,`imageUri`,`isActive`) SELECT `id`,`barcode`,`name`,`category`,`productType`,CAST(ROUND(`costPrice`) AS INTEGER),CAST(ROUND(`sellingPrice`) AS INTEGER),`currentStock`,`minStockAlert`,`unit`,`expiryDate`,`description`,`imageUri`,`isActive` FROM `products_old`",

        "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceNumber` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `subtotal` INTEGER NOT NULL, `taxRate` REAL NOT NULL, `taxAmount` INTEGER NOT NULL, `discountAmount` INTEGER NOT NULL, `grandTotal` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `cashTendered` INTEGER NOT NULL, `changeGiven` INTEGER NOT NULL, `customerName` TEXT, `customerPhone` TEXT, `cashierName` TEXT NOT NULL, `notes` TEXT NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_invoiceNumber` ON `transactions` (`invoiceNumber`)",
        "INSERT INTO `transactions` (`id`,`invoiceNumber`,`timestamp`,`subtotal`,`taxRate`,`taxAmount`,`discountAmount`,`grandTotal`,`paymentMethod`,`cashTendered`,`changeGiven`,`customerName`,`customerPhone`,`cashierName`,`notes`) SELECT t.`id`, CASE WHEN t.`id` = (SELECT MIN(t2.`id`) FROM `transactions_old` t2 WHERE t2.`invoiceNumber` = t.`invoiceNumber`) THEN t.`invoiceNumber` ELSE t.`invoiceNumber` || '-' || t.`id` END, t.`timestamp`, CAST(ROUND(t.`subtotal`) AS INTEGER), t.`taxRate`, CAST(ROUND(t.`taxAmount`) AS INTEGER), CAST(ROUND(t.`discountAmount`) AS INTEGER), CAST(ROUND(t.`grandTotal`) AS INTEGER), t.`paymentMethod`, CAST(ROUND(t.`cashTendered`) AS INTEGER), CAST(ROUND(t.`changeGiven`) AS INTEGER), t.`customerName`, t.`customerPhone`, t.`cashierName`, t.`notes` FROM `transactions_old` t",

        "CREATE TABLE IF NOT EXISTS `transaction_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `productBarcode` TEXT NOT NULL, `category` TEXT NOT NULL, `unitPrice` INTEGER NOT NULL, `costPrice` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `subtotal` INTEGER NOT NULL, FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_transaction_items_transactionId` ON `transaction_items` (`transactionId`)",
        "CREATE INDEX IF NOT EXISTS `index_transaction_items_productId` ON `transaction_items` (`productId`)",
        "INSERT INTO `transaction_items` (`id`,`transactionId`,`productId`,`productName`,`productBarcode`,`category`,`unitPrice`,`costPrice`,`quantity`,`subtotal`) SELECT `id`,`transactionId`,`productId`,`productName`,`productBarcode`,`category`,CAST(ROUND(`unitPrice`) AS INTEGER),CAST(ROUND(`costPrice`) AS INTEGER),`quantity`,CAST(ROUND(`subtotal`) AS INTEGER) FROM `transaction_items_old`",

        "CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantityChange` INTEGER NOT NULL, `previousStock` INTEGER NOT NULL, `resultingStock` INTEGER NOT NULL, `reason` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `referenceInvoice` TEXT, `note` TEXT NOT NULL, FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)",
        "INSERT INTO `stock_movements` (`id`,`productId`,`productName`,`quantityChange`,`previousStock`,`resultingStock`,`reason`,`timestamp`,`referenceInvoice`,`note`) SELECT `id`,`productId`,`productName`,`quantityChange`,`previousStock`,`resultingStock`,`reason`,`timestamp`,`referenceInvoice`,`note` FROM `stock_movements_old`",

        "DROP TABLE `transaction_items_old`",
        "DROP TABLE `stock_movements_old`",
        "DROP TABLE `transactions_old`",
        "DROP TABLE `products_old`"
    )
    // MIGRATION_1_2_SQL_END

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2_SQL.forEach { db.execSQL(it) }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
