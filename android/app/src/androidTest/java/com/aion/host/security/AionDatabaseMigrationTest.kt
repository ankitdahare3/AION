package com.aion.host.security

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.Rule
import org.junit.Test

/** T-060 AC — schema tests: every migration actually applies against the real exported schemas. */
class AionDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AionDatabase::class.java,
            emptyList(),
            testOpenFactory(),
        )

    @Test
    fun migrateAllTheWayFrom1To4() {
        helper.createDatabase(TEST_DB, 1).close()

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DB,
                4,
                true,
                AionDatabase.MIGRATION_1_2,
                AionDatabase.MIGRATION_2_3,
                AionDatabase.MIGRATION_3_4,
            )
        migrated.close()
    }

    @Test
    fun migrate3To4AddsTheFullMemorySchema() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AionDatabase.MIGRATION_3_4)
        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        db.close()

        listOf("conversations", "turns", "memories", "episodes", "skills", "element_maps", "plugins")
            .forEach { table -> assert(table in tables) { "expected table `$table` after migration, got: $tables" } }
    }

    private fun testOpenFactory(): SupportFactory {
        SQLiteDatabase.loadLibs(InstrumentationRegistry.getInstrumentation().targetContext)
        // MigrationTestHelper reopens the DB across multiple createDatabase()/runMigrationsAndValidate()
        // calls with this same factory instance; SupportFactory's default clears the passphrase from
        // memory after the first open (a real security feature, kept for the production factory in
        // DatabaseModule), which breaks re-opening here — clearPassphrase=false opts out for this test only.
        return SupportFactory(SQLiteDatabase.getBytes("test-passphrase".toCharArray()), null, false)
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
