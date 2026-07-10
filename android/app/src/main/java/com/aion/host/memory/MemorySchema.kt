package com.aion.host.memory

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

/** DOC-019 §1 — v1 schema, T-060: conversations/turns/memories/episodes/skills/element_maps/plugins. */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val summary: String?,
)

@Entity(
    tableName = "turns",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["convId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("convId")],
)
data class TurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val convId: Long,
    val role: String,
    val text: String,
    val lang: String,
    val ts: Long,
)

/** kind: fact|pref|profile. piiTags: comma-joined. */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val text: String,
    val confidence: Double,
    val provenance: String,
    val piiTags: String,
    val created: Long,
    val accessed: Long,
    val decayScore: Double,
    val deletedSoft: Boolean,
)

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val planJson: String,
    val outcome: String,
    val failureClass: String?,
    val latencyMs: Long,
    val costUsd: Double,
    val appPkg: String?,
    val ts: Long,
)

/** status: proposed|approved|active|retired. */
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yaml: String,
    val version: Int,
    val status: String,
    val successCount: Int,
    val failCount: Int,
    val approvedAt: Long?,
)

@Entity(tableName = "element_maps", primaryKeys = ["appPkg", "appVersion", "screenHash"])
data class ElementMapEntity(
    val appPkg: String,
    val appVersion: String,
    val screenHash: String,
    val selectorJson: String,
    val confidence: Double,
    val ts: Long,
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val version: String,
    val status: String,
    val permissionsJson: String,
    val installedAt: Long,
)

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY startedAt DESC")
    suspend fun getAll(): List<ConversationEntity>
}

@Dao
interface TurnDao {
    @Insert
    suspend fun insert(turn: TurnEntity)

    @Query("SELECT * FROM turns WHERE convId = :convId ORDER BY ts ASC")
    suspend fun getForConversation(convId: Long): List<TurnEntity>
}

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    @Query("SELECT * FROM memories WHERE deletedSoft = 0 ORDER BY created DESC")
    suspend fun getAllActive(): List<MemoryEntity>

    /** T-111 — consolidation's merge/decay updates go through this, not a raw DAO write elsewhere. */
    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("UPDATE memories SET deletedSoft = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface EpisodeDao {
    @Insert
    suspend fun insert(episode: EpisodeEntity)

    @Query("SELECT * FROM episodes ORDER BY ts DESC")
    suspend fun getAll(): List<EpisodeEntity>
}

@Dao
interface SkillDao {
    @Insert
    suspend fun insert(skill: SkillEntity): Long

    @Query("SELECT * FROM skills WHERE status = :status")
    suspend fun getByStatus(status: String): List<SkillEntity>
}

@Dao
interface ElementMapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(elementMap: ElementMapEntity)

    @Query("SELECT * FROM element_maps WHERE appPkg = :appPkg AND appVersion = :appVersion")
    suspend fun getForAppVersion(
        appPkg: String,
        appVersion: String,
    ): List<ElementMapEntity>

    @Query(
        "SELECT * FROM element_maps WHERE appPkg = :appPkg AND appVersion = :appVersion AND screenHash = :screenHash",
    )
    suspend fun getOne(
        appPkg: String,
        appVersion: String,
        screenHash: String,
    ): ElementMapEntity?
}

@Dao
interface PluginDao {
    @Insert
    suspend fun insert(plugin: PluginEntity)

    @Query("SELECT * FROM plugins")
    suspend fun getAll(): List<PluginEntity>
}
