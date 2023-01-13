package com.dr.dairyaccounting.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_ADVANCE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_PAID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.DATE
import com.dr.dairyaccounting.utils.AppConstants.DOCUMENT_ID
import com.dr.dairyaccounting.utils.AppConstants.EVENING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.IS_CLIENT_ACTIVE
import com.dr.dairyaccounting.utils.AppConstants.MORNING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.PHONE
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP

@Entity(tableName = "records")
data class RecordsEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
        (name = DOCUMENT_ID) val id : String,
    @ColumnInfo
        (name = CLIENT_ID) val clientId : String,
    @ColumnInfo
        (name = CLIENT_NAME) var clientName : String,
    @ColumnInfo
        (name = RATE) var rate : Float,
    @ColumnInfo
        (name = AMOUNT) var amount : Float,
    @ColumnInfo
        (name = AMOUNT_PAID) var amountPaid : Float,
    @ColumnInfo
        (name = AMOUNT_ADVANCE) val amountAdvance : Float,
    @ColumnInfo
        (name = RECORD_TYPE) val recordType : String,
    @ColumnInfo
        (name = SHIFT) val shift : String,
    @ColumnInfo
        (name = QUANTITY) var quantity : Float,
    @ColumnInfo
        (name = TIMESTAMP) var timestamp : Long,
    @ColumnInfo
        (name = ORDER_TIMESTAMP) var orderTimeStamp : Long,
    @ColumnInfo
        (name = DATE) var date : String
)

@Entity(tableName = "clients")
data class ClientsEntity (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
        (name = CLIENT_ID) val id : String,
    @ColumnInfo
        (name = CLIENT_NAME) val clientName : String,
    @ColumnInfo
        (name = IS_CLIENT_ACTIVE) val isActive : Boolean,
    @ColumnInfo
        (name = PHONE) val phone : String,
    @ColumnInfo
        (name = ACCOUNT_TYPE) val accountType : String,
    @ColumnInfo
        (name = MORNING_QUANTITY) val morningQuantity : Float,
    @ColumnInfo
        (name = EVENING_QUANTITY) val eveningQuantity : Float,
    @ColumnInfo
        (name = RATE) val rate : Float,
    @ColumnInfo
        (name = TIMESTAMP) val timestamp: Long,
    @ColumnInfo
        (name = ORDER_TIMESTAMP) var orderTimeStamp: Long,
    @ColumnInfo
        (name = MORNING_SHIFT) val morningShift: Boolean,
    @ColumnInfo
        (name = EVENING_SHIFT) val eveningShift: Boolean
)


@Entity(tableName = "temp_records")
data class TempRecordEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
        (name = DOCUMENT_ID) val id : String,
    @ColumnInfo
        (name = CLIENT_ID) val clientId : String,
    @ColumnInfo
        (name = CLIENT_NAME) var clientName : String,
    @ColumnInfo
        (name = RATE) var rate : Float,
    @ColumnInfo
        (name = AMOUNT) var amount : Float,
    @ColumnInfo
        (name = AMOUNT_PAID) var amountPaid : Float,
    @ColumnInfo
        (name = AMOUNT_ADVANCE) val amountAdvance : Float,
    @ColumnInfo
        (name = RECORD_TYPE) val recordType : String,
    @ColumnInfo
        (name = SHIFT) val shift : String,
    @ColumnInfo
        (name = QUANTITY) var quantity : Float,
    @ColumnInfo
        (name = TIMESTAMP) var timestamp : Long,
    @ColumnInfo
        (name = ORDER_TIMESTAMP) var orderTimeStamp : Long,
    @ColumnInfo
        (name = DATE) var date : String
)

@Entity(tableName = "old_records")
data class OldRecordEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
        (name = DOCUMENT_ID) val id : String,
    @ColumnInfo
        (name = CLIENT_ID) val clientId : String,
    @ColumnInfo
        (name = CLIENT_NAME) var clientName : String,
    @ColumnInfo
        (name = RATE) var rate : Float,
    @ColumnInfo
        (name = AMOUNT) var amount : Float,
    @ColumnInfo
        (name = AMOUNT_PAID) var amountPaid : Float,
    @ColumnInfo
        (name = AMOUNT_ADVANCE) val amountAdvance : Float,
    @ColumnInfo
        (name = RECORD_TYPE) val recordType : String,
    @ColumnInfo
        (name = SHIFT) val shift : String,
    @ColumnInfo
        (name = QUANTITY) var quantity : Float,
    @ColumnInfo
        (name = TIMESTAMP) var timestamp : Long,
    @ColumnInfo
        (name = ORDER_TIMESTAMP) var orderTimeStamp : Long,
    @ColumnInfo
        (name = DATE) var date : String
)




