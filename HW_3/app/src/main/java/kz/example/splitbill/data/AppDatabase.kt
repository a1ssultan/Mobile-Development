package kz.example.splitbill.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FriendEntity::class, BillEntity::class, ParticipantEntity::class, ItemEntity::class, ItemShareEntity::class, RepaymentEntity::class, ActivityEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SplitBillDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "splitbill.db")
                .addCallback(SeedCallback)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** HW3: soft delete (trash) for bills and the activity history table. Existing data is kept. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN deletedAt INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `activity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, `billId` INTEGER, `text` TEXT NOT NULL, `amount` INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_createdAt` ON `activity` (`createdAt`)")
    }
}

/** Fills a fresh database with the user and a couple of demo bills so the app is not empty on first launch. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        db.execSQL("INSERT INTO friends (id, name, isMe) VALUES (1, 'Я', 1), (2, 'Али', 0), (3, 'Дана', 0), (4, 'Амир', 0)")

        db.execSQL("INSERT INTO bills (id, title, place, createdAt, servicePercent, payerId) VALUES (1, 'Ужин в кафе', 'Алматы, ул. Панфилова', ${now - 2 * day}, 10, 1)")
        db.execSQL("INSERT INTO bill_participants (billId, friendId) VALUES (1, 1), (1, 2), (1, 3), (1, 4)")
        db.execSQL(
            "INSERT INTO items (id, billId, title, price, quantity) VALUES " +
                "(1, 1, 'Бургер', 3500, 1), (2, 1, 'Паста', 4200, 1), (3, 1, 'Кофе', 800, 2), " +
                "(4, 1, 'Пицца на всех', 6000, 1), (5, 1, 'Лимонад', 1200, 1), (6, 1, 'Стейк', 7900, 1)"
        )
        db.execSQL("INSERT INTO item_shares (itemId, friendId) VALUES (1, 2), (2, 3), (3, 4), (5, 2), (5, 3), (6, 1)")
        db.execSQL("INSERT INTO repayments (billId, friendId) VALUES (1, 3)")

        db.execSQL("INSERT INTO bills (id, title, place, createdAt, servicePercent, payerId) VALUES (2, 'Кино', 'Esentai Mall', ${now - day}, 0, 2)")
        db.execSQL("INSERT INTO bill_participants (billId, friendId) VALUES (2, 1), (2, 2), (2, 4)")
        db.execSQL("INSERT INTO items (id, billId, title, price, quantity) VALUES (7, 2, 'Билеты', 2500, 3), (8, 2, 'Попкорн', 1800, 1)")
        db.execSQL("INSERT INTO item_shares (itemId, friendId) VALUES (8, 1), (8, 4)")

        // A bill that is already in the trash, so the trash screen can be tried right away.
        db.execSQL("INSERT INTO bills (id, title, place, createdAt, servicePercent, payerId, deletedAt) VALUES (3, 'Такси в аэропорт', '', ${now - 5 * day}, 0, 3, ${now - 3 * 60 * 60 * 1000L})")
        db.execSQL("INSERT INTO bill_participants (billId, friendId) VALUES (3, 1), (3, 3)")
        db.execSQL("INSERT INTO items (id, billId, title, price, quantity) VALUES (9, 3, 'Поездка', 4600, 1)")

        // History that matches the demo data above.
        db.execSQL(
            "INSERT INTO activity (createdAt, type, billId, text, amount) VALUES " +
                "(${now - 5 * day}, 'BILL_CREATED', 3, 'Создан счёт «Такси в аэропорт»', NULL), " +
                "(${now - 2 * day}, 'BILL_CREATED', 1, 'Создан счёт «Ужин в кафе»', NULL), " +
                "(${now - 2 * day + 60_000}, 'ITEM_ADDED', 1, 'Добавлено 6 позиций в «Ужин в кафе»', 24400), " +
                "(${now - day}, 'BILL_CREATED', 2, 'Создан счёт «Кино»', NULL), " +
                "(${now - day + 60_000}, 'REPAID', 1, 'Дана вернула долг за «Ужин в кафе»', 6930), " +
                "(${now - 3 * 60 * 60 * 1000L}, 'BILL_TRASHED', 3, 'Счёт «Такси в аэропорт» перемещён в корзину', NULL)"
        )
    }
}
