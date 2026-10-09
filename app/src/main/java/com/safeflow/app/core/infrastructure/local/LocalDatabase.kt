package com.safeflow.app.core.infrastructure.local

import android.content.Context
import androidx.room3.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.Gson
import com.safeflow.app.core.domain.Records
import com.safeflow.app.core.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers

// Typed adapters own their namespaces. JSON is confined to local mock persistence.
@Entity(tableName = "records", primaryKeys = ["namespace", "id"])
data class LocalRecord(val namespace: String, val id: String, val payload: String)
@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE namespace = :namespace ORDER BY rowid")
    fun observe(namespace: String): Flow<List<LocalRecord>>
    @Query("SELECT * FROM records WHERE namespace = :namespace ORDER BY rowid")
    suspend fun all(namespace: String): List<LocalRecord>
    @Query("SELECT * FROM records WHERE namespace = :namespace AND id = :id")
    suspend fun find(namespace: String, id: String): LocalRecord?
    @Upsert suspend fun save(record: LocalRecord)
}
@Database(entities = [LocalRecord::class], version = 1, exportSchema = true)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun records(): RecordDao
    companion object {
        fun open(context: Context, name: String = "safeflow.db"): LocalDatabase =
            Room.databaseBuilder<LocalDatabase>(context, name)
                .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    }
}
class RoomTransaction(private val db: LocalDatabase) : Transaction {
    override suspend fun <T> run(block: suspend () -> T): T = db.withWriteTransaction { block() }
}
abstract class JsonRecords<T>(private val dao: RecordDao, private val namespace: String,
    private val type: Class<T>) : Records<T> {
    private val gson = Gson()
    override fun observe() = dao.observe(namespace).map { rows -> rows.map { gson.fromJson(it.payload, type) } }
    override suspend fun all() = dao.all(namespace).map { gson.fromJson(it.payload, type) }
    override suspend fun find(id: String): T? = dao.find(namespace, id)?.let { gson.fromJson(it.payload, type) }
    override suspend fun save(id: String, value: T) { dao.save(LocalRecord(namespace, id, gson.toJson(value, type))) }
}
