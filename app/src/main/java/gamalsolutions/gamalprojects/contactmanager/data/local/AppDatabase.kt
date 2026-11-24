package gamalsolutions.gamalprojects.contactmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * قاعدة بيانات Room المحلية لتخزين جهات الاتصال مع دعم الترقية الآمنة من الإصدار 1 إلى 2.
 */
@Database(
    entities = [ContactEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        private const val DATABASE_NAME = "contact_manager.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN category TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE contacts ADD COLUMN preferredPlatform TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL("ALTER TABLE contacts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
