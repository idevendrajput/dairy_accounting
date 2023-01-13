package com.dr.dairyaccounting.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordsEntity::class, ClientsEntity::class, TempRecordEntity::class, OldRecordEntity::class], version = 6)
abstract class MyDatabase : RoomDatabase() {

    abstract fun recordDao() : RecordsDao
    abstract fun clientDao() : ClientsDao
    abstract fun tempRecordDao() : TempRecordsDao
    abstract fun oldRecordDao() : OldRecordsDao

    companion object {
        private var INSTANCE: MyDatabase? = null
        fun getDatabase(context: Context): MyDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, MyDatabase::class.java, "dairy_accounting_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return INSTANCE!!
        }
    }
}