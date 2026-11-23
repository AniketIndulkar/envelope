package dev.novanest.envelope.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import dev.novanest.envelope.ai.ModelJson
import dev.novanest.envelope.privacy.TokenEntry
import kotlinx.serialization.builtins.ListSerializer

@Entity(tableName = "token_maps")
data class TokenMapEntity(@PrimaryKey val letterId: String, val json: String)

@Dao
interface TokenMapDao {
    @Upsert suspend fun upsert(entity: TokenMapEntity)
    @Query("SELECT * FROM token_maps WHERE letterId = :id") suspend fun get(id: String): TokenMapEntity?
}

@Database(entities = [TokenMapEntity::class], version = 1, exportSchema = false)
abstract class EnvelopeDb : RoomDatabase() {
    abstract fun tokenMaps(): TokenMapDao

    companion object {
        fun create(context: Context): EnvelopeDb =
            Room.databaseBuilder(context, EnvelopeDb::class.java, "envelope.db").build()
    }
}

/** Token maps in app-private Room storage; excluded from backup/transfer in res/xml. */
class RoomTokenStore(private val dao: TokenMapDao) : TokenStore {
    private val serializer = ListSerializer(TokenEntry.serializer())
    override suspend fun put(letterId: String, tokens: List<TokenEntry>) =
        dao.upsert(TokenMapEntity(letterId, ModelJson.json.encodeToString(serializer, tokens)))
    override suspend fun get(letterId: String): List<TokenEntry> =
        dao.get(letterId)?.let { ModelJson.json.decodeFromString(serializer, it.json) }.orEmpty()
}
