package com.dr.dairyaccounting.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE_SALE
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_SALE
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import java.text.SimpleDateFormat

@Dao
interface RecordsDao {

    @Insert
    suspend fun insertRecord(entity: RecordsEntity)

    @Update
    suspend fun updateRecord(entity: RecordsEntity)

    @Query("DELETE FROM records") 
    fun deleteAllRecords()

    @Query("DELETE FROM records WHERE date ==:date")
    fun deleteTodayRecords(date: String = getTodayDate())
    
    @Query("DELETE FROM records WHERE documentId == :id")
    fun deleteById(id : String)

    @Query("Select * From records WHERE documentId == :documentId")
    fun getRecordsById(documentId : String):List<RecordsEntity>

    @Query("Select * From records WHERE clientId == :documentId")
    fun getRecordsByIClientId(documentId : String):List<RecordsEntity>

    @Query("Select * From records WHERE clientId == :documentId")
    fun getRecordsByIClientIdLive(documentId : String): LiveData<List<RecordsEntity>>

    @Query("Select * From records ORDER BY orderTimestamp DESC")
    fun getAllRecords():List<RecordsEntity>

    @Query("Select * From records WHERE recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByRecordType(recordType: String):List<RecordsEntity>

    @Query("Select * From records WHERE recordType ==:recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getRecordsByShiftAndRecordType(recordType: String, shift: String):List<RecordsEntity>

    @Query("Select * From records WHERE recordType ==:recordType AND shift==:shift AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByShiftAndRecordType(recordType: String, shift: String, date: String = getTodayDate()):List<RecordsEntity>

    @Query("Select * From records WHERE clientId ==:clientId AND recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByClientIdAndRecordType(clientId: String, recordType: String):List<RecordsEntity>

    @Query("Select * From records ORDER BY orderTimestamp DESC")
    fun getAllRecordsLive() : LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE clientId ==:clientId AND timestamp <:timeTill ORDER BY orderTimestamp DESC")
    fun getAllRecordsByClientIdAndTimeTill(clientId: String, timeTill: Long) : List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsByClientId(recordType : String = RECORD_TYPE_PURCHASE, clientId: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date ==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date ==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayRecords(recordType : String, date: String = getTodayDate(), shift: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientId(recordType : String, date: String = getTodayDate(), shift: String, clientId: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientIdAndShift(recordType : String, date: String = getTodayDate(), clientId: String, shift: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<RecordsEntity>

     @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT, clientId: String, date: String = getTodayDate()): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEveningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT, clientId: String, date: String = getTodayDate()): List<RecordsEntity>


    @Query("Select * From records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecords(recordType : String = RECORD_TYPE_SALE): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientId(recordType : String = RECORD_TYPE_SALE, clientId: String): List<RecordsEntity>

     @Query("Select * From records WHERE recordType == :recordType AND clientId==:clientId AND timestamp >=:startTime AND timestamp <=:endTime ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientIdAndTimeRange(recordType : String = RECORD_TYPE_SALE, clientId: String, startTime: Long, endTime: Long): List<RecordsEntity>

     @Query("Select * From records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecords(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): List<RecordsEntity>

     @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEveningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLive(recordType : String = RECORD_TYPE_SALE): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsLive(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorningByClientId(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT, clientId: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): LiveData<List<RecordsEntity>>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEveningByUserId(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT, clientId: String): List<RecordsEntity>

    @Query("Select * From records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): LiveData<List<RecordsEntity>>

}

@Dao
interface ClientsDao {

    @Insert
    suspend fun insertClient(entity: ClientsEntity)

    @Update
    suspend fun updateClient(entity: ClientsEntity)

    @Query("DELETE FROM clients")
    fun deleteAllClients()

    @Query("DELETE FROM clients WHERE clientId == :id")
    fun deleteById(id : String)

    @Query("Select * From clients WHERE clientId == :id")
    fun getClientById(id : String):List<ClientsEntity>

    @Query("Select * From clients ORDER BY orderTimestamp ASC")
    fun getAllClients():List<ClientsEntity>

    @Query("Select * From clients WHERE isClientActive == 1 ORDER BY orderTimestamp ASC")
    fun getAllActiveClients():List<ClientsEntity>

     @Query("Select * From clients WHERE isClientActive == 1 AND accountType==:recordType ORDER BY orderTimestamp ASC")
    fun getAllActiveClients(recordType: String):List<ClientsEntity>

    @Query("Select * From clients WHERE accountType ==:clientType AND isClientActive = 1  ORDER BY orderTimestamp ASC")
    fun getAllClientsByClientType(clientType: String):List<ClientsEntity>

    @Query("Select * From clients ORDER BY orderTimestamp ASC")
    fun getAllClientsLive() : LiveData<List<ClientsEntity>>

    @Query("Select * From clients WHERE isClientActive == 0 ORDER BY orderTimestamp ASC")
    fun getAllInActiveClients():List<ClientsEntity>

    @Query("Select * From clients WHERE accountType == :accountType AND isClientActive == 1 ORDER BY orderTimestamp ASC")
    fun getAllSalesClients(accountType: String = ACCOUNT_TYPE_SALE):List<ClientsEntity>

    @Query("Select * From clients WHERE accountType == :accountType AND isClientActive ==:accountStatus ORDER BY orderTimestamp ASC")
    fun getAllSalesClientsLive(accountType: String = ACCOUNT_TYPE_SALE, accountStatus: Boolean = true) : LiveData<List<ClientsEntity>>

    @Query("Select * From clients WHERE accountType == :accountType AND isClientActive ==:accountStatus ORDER BY orderTimestamp ASC")
    fun getAllPurchaseClients(accountType: String = ACCOUNT_TYPE_PURCHASE, accountStatus: Boolean = true):List<ClientsEntity>

    @Query("Select * From clients WHERE accountType == :accountType AND isClientActive ==:accountStatus ORDER BY orderTimestamp ASC")
    fun getAllPurchaseClientsLive(accountType: String = ACCOUNT_TYPE_PURCHASE, accountStatus: Boolean = true) : LiveData<List<ClientsEntity>>


}

@Dao
interface TempRecordsDao {

    @Insert
    suspend fun insertRecord(entity: TempRecordEntity)

    @Update
    suspend fun updateRecord(entity: TempRecordEntity)

    @Query("DELETE FROM temp_records")
    fun deleteAllRecords()


    @Query("DELETE FROM temp_records WHERE documentId == :id")
    fun deleteById(id : String)

    @Query("Select * From temp_records WHERE documentId == :documentId")
    fun getRecordsById(documentId : String):List<TempRecordEntity>

    @Query("Select * From temp_records WHERE clientId == :documentId")
    fun getRecordsByIClientId(documentId : String):List<TempRecordEntity>

    @Query("Select * From temp_records WHERE clientId == :documentId")
    fun getRecordsByIClientIdLive(documentId : String): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records ORDER BY orderTimestamp DESC")
    fun getAllRecords():List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByRecordType(recordType: String):List<TempRecordEntity>

    @Query("Select * From temp_records WHERE clientId ==:clientId AND recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByClientIdAndRecordType(clientId: String, recordType: String):List<TempRecordEntity>

    @Query("Select * From temp_records ORDER BY orderTimestamp DESC")
    fun getAllRecordsLive() : LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE clientId ==:clientId AND timestamp <:timeTill ORDER BY orderTimestamp DESC")
    fun getAllRecordsByClientIdAndTimeTill(clientId: String, timeTill: Long) : List<TempRecordEntity>

    @Query("Select * From temp_records WHERE shift ==:shift AND recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByShiftAndRecordType(shift: String, recordType: String) : List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsByClientId(recordType : String = RECORD_TYPE_PURCHASE, clientId: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date ==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientId(recordType : String, date: String = getTodayDate(), clientId: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientIdAndShift(recordType : String, date: String = getTodayDate(), clientId: String, shift: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT, clientId: String, date: String = getTodayDate()): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEveningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT, clientId: String, date: String = getTodayDate()): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecords(recordType : String = RECORD_TYPE_SALE): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientId(recordType : String = RECORD_TYPE_SALE, clientId: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND clientId==:clientId AND timestamp >=:startTime AND timestamp <=:endTime ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientIdAndTimeRange(recordType : String = RECORD_TYPE_SALE, clientId: String, startTime: Long, endTime: Long): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecords(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEveningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLive(recordType : String = RECORD_TYPE_SALE): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsLive(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorningByClientId(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT, clientId: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): LiveData<List<TempRecordEntity>>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEveningByUserId(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT, clientId: String): List<TempRecordEntity>

    @Query("Select * From temp_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): LiveData<List<TempRecordEntity>>

}

@Dao
interface OldRecordsDao {

    @Insert
    suspend fun insertRecord(entity: OldRecordEntity)

    @Update
    suspend fun updateRecord(entity: OldRecordEntity)

    @Query("DELETE FROM old_records")
    fun deleteAllRecords()


    @Query("DELETE FROM old_records WHERE documentId == :id")
    fun deleteById(id : String)

    @Query("Select * From old_records WHERE documentId == :documentId")
    fun getRecordsById(documentId : String):List<OldRecordEntity>

    @Query("Select * From old_records WHERE clientId == :documentId")
    fun getRecordsByIClientId(documentId : String):List<OldRecordEntity>

    @Query("Select * From old_records WHERE clientId == :documentId")
    fun getRecordsByIClientIdLive(documentId : String): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records ORDER BY orderTimestamp DESC")
    fun getAllRecords():List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByRecordType(recordType: String):List<OldRecordEntity>

    @Query("Select * From old_records WHERE clientId ==:clientId AND recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByClientIdAndRecordType(clientId: String, recordType: String):List<OldRecordEntity>

    @Query("Select * From old_records ORDER BY orderTimestamp DESC")
    fun getAllRecordsLive() : LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE clientId ==:clientId AND timestamp <:timeTill ORDER BY orderTimestamp DESC")
    fun getAllRecordsByClientIdAndTimeTill(clientId: String, timeTill: Long) : List<OldRecordEntity>

    @Query("Select * From old_records WHERE shift ==:shift AND recordType ==:recordType ORDER BY orderTimestamp DESC")
    fun getRecordsByShiftAndRecordType(shift: String, recordType: String) : List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsByClientId(recordType : String = RECORD_TYPE_PURCHASE, clientId: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date ==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecords(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientId(recordType : String, date: String = getTodayDate(), clientId: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date ==:date AND clientId ==:clientId AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getTodayRecordsByClientIdAndShift(recordType : String, date: String = getTodayDate(), clientId: String, shift: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsLive(recordType : String = RECORD_TYPE_PURCHASE, date: String = getTodayDate()): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsMorningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT, clientId: String, date: String = getTodayDate()): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveMorning(recordType : String = RECORD_TYPE_PURCHASE, shift: String = MORNING_SHIFT): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift AND clientId==:clientId AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodayPurchaseRecordsEveningByClientId(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT, clientId: String, date: String = getTodayDate()): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift ==:shift ORDER BY orderTimestamp DESC")
    fun getPurchaseRecordsLiveEvening(recordType : String = RECORD_TYPE_PURCHASE, shift: String = EVENING_SHIFT): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecords(recordType : String = RECORD_TYPE_SALE): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientId(recordType : String = RECORD_TYPE_SALE, clientId: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND clientId==:clientId AND timestamp >=:startTime AND timestamp <=:endTime ORDER BY orderTimestamp DESC")
    fun getSaleRecordsByClientIdAndTimeRange(recordType : String = RECORD_TYPE_SALE, clientId: String, startTime: Long, endTime: Long): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecords(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsMorningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = MORNING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsEveningByClientId(clientId: String,recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate(), shift: String = EVENING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLive(recordType : String = RECORD_TYPE_SALE): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND date==:date ORDER BY orderTimestamp DESC")
    fun getTodaySaleRecordsLive(recordType : String = RECORD_TYPE_SALE, date: String = getTodayDate()): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsMorningByClientId(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT, clientId: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveMorning(recordType : String = RECORD_TYPE_SALE, shift: String = MORNING_SHIFT): LiveData<List<OldRecordEntity>>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift AND clientId==:clientId ORDER BY orderTimestamp DESC")
    fun getSaleRecordsEveningByUserId(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT, clientId: String): List<OldRecordEntity>

    @Query("Select * From old_records WHERE recordType == :recordType AND shift==:shift ORDER BY orderTimestamp DESC")
    fun getSaleRecordsLiveEvening(recordType : String = RECORD_TYPE_SALE, shift: String = EVENING_SHIFT): LiveData<List<OldRecordEntity>>

}

