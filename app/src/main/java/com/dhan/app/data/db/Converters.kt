package com.dhan.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTxnSource(v: TxnSource): String = v.name

    @TypeConverter
    fun toTxnSource(v: String): TxnSource = TxnSource.valueOf(v)

    @TypeConverter
    fun fromBillStatus(v: BillStatus): String = v.name

    @TypeConverter
    fun toBillStatus(v: String): BillStatus = BillStatus.valueOf(v)
}
