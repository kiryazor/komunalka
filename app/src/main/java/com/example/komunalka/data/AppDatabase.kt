package com.example.komunalka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.komunalka.data.dao.ApartmentDao
import com.example.komunalka.data.dao.BillDao
import com.example.komunalka.data.dao.PaymentDao
import com.example.komunalka.data.dao.UtilityTypeDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Apartment::class, UtilityType::class, Bill::class, Payment::class, PaymentBillCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apartmentDao(): ApartmentDao
    abstract fun utilityTypeDao(): UtilityTypeDao
    abstract fun billDao(): BillDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "komunalka.db"
                )
                    .addCallback(SeedCallback(context))
                    .build().also { INSTANCE = it }
            }
        }
    }

    /**
     * При первом запуске приложения база пустая — заполняем её парой
     * стандартных типов услуг и одной квартирой "по умолчанию",
     * чтобы пользователь сразу видел рабочий интерфейс, а не пустой экран.
     */
    private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                val apartmentId = database.apartmentDao().insert(
                    Apartment(name = "Моя квартира", address = "Укажите адрес в настройках", areaSqm = null)
                )
                database.utilityTypeDao().insertAll(
                    listOf(
                        UtilityType(name = "Электричество", unit = "кВт·ч", tariff = 5.5, iconKey = "electricity"),
                        UtilityType(name = "Холодная вода", unit = "м³", tariff = 45.0, iconKey = "water_cold"),
                        UtilityType(name = "Горячая вода", unit = "м³", tariff = 210.0, iconKey = "water_hot"),
                        UtilityType(name = "Газ", unit = "м³", tariff = 7.2, iconKey = "gas"),
                        UtilityType(name = "Отопление", unit = "Гкал", tariff = 2300.0, iconKey = "heating"),
                        UtilityType(name = "Интернет и ТВ", unit = "мес.", tariff = 600.0, iconKey = "internet")
                    )
                )
                // apartmentId используется только для того, чтобы гарантировать создание записи выше
                if (apartmentId <= 0) return@launch
            }
        }
    }
}
