package com.postaltools.tr25.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One row per bill. The searchable columns are copied out of the bill; the full bill
 * (all items and tables) is stored as JSON in [json].
 */
@Entity(tableName = "bills", indices = [Index("subBillNo"), Index("updatedAt")])
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subBillNo: String,
    val claimDate: String,
    val officeName: String,
    val officialName: String,
    val designation: String,
    val status: String,
    val netAmount: String,
    val pdfPath: String?,
    val json: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY updatedAt DESC")
    suspend fun getAll(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Query("SELECT COUNT(*) FROM bills WHERE subBillNo = :no AND id != :excludeId AND status = 'FINAL'")
    suspend fun countFinalWithSubBillNo(no: String, excludeId: Long): Int

    @Insert
    suspend fun insert(e: BillEntity): Long

    @Update
    suspend fun update(e: BillEntity)

    @Query("UPDATE bills SET pdfPath = :path WHERE id = :id")
    suspend fun setPdfPath(id: Long, path: String?)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [BillEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "tr25.db").build()
    }
}
