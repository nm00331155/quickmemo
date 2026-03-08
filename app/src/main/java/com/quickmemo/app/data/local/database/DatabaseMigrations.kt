package com.quickmemo.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memos ADD COLUMN isChecklist INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE memos ADD COLUMN deletedAt INTEGER")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `memos_fts` USING fts4(`title`, `contentPlainText`, content=`memos`)"
            )
            db.execSQL(
                "INSERT INTO memos_fts(rowid, title, contentPlainText) SELECT id, title, contentPlainText FROM memos"
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memos ADD COLUMN blocks TEXT NOT NULL DEFAULT '[]'")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todo_items` (
                    `id` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `isDone` INTEGER NOT NULL,
                    `priority` TEXT NOT NULL,
                    `dueDate` INTEGER,
                    `orderIndex` INTEGER NOT NULL,
                    `tabIndex` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_todo_items_tab_done_order` ON `todo_items` (`tabIndex`, `isDone`, `orderIndex`)"
            )
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todo_items_new` (
                    `id` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `checked` INTEGER NOT NULL,
                    `priority` TEXT NOT NULL,
                    `dueDate` INTEGER,
                    `sortOrder` INTEGER NOT NULL,
                    `tabId` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `checkedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `todo_items_new`(
                    `id`,
                    `text`,
                    `checked`,
                    `priority`,
                    `dueDate`,
                    `sortOrder`,
                    `tabId`,
                    `createdAt`,
                    `checkedAt`
                )
                SELECT
                    `id`,
                    `text`,
                    `isDone`,
                    `priority`,
                    `dueDate`,
                    `orderIndex`,
                    `tabIndex`,
                    `createdAt`,
                    CASE WHEN `isDone` = 1 THEN `updatedAt` ELSE NULL END
                FROM `todo_items`
                """.trimIndent(),
            )

            db.execSQL("DROP TABLE `todo_items`")
            db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_todo_items_tab_checked_sort` ON `todo_items` (`tabId`, `checked`, `sortOrder`)"
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `memos_new`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memos_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `contentHtml` TEXT NOT NULL,
                    `contentPlainText` TEXT NOT NULL,
                    `colorLabel` INTEGER NOT NULL,
                    `isPinned` INTEGER NOT NULL,
                    `isLocked` INTEGER NOT NULL,
                    `isChecklist` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `memos_new`(
                    `id`,
                    `title`,
                    `contentHtml`,
                    `contentPlainText`,
                    `colorLabel`,
                    `isPinned`,
                    `isLocked`,
                    `isChecklist`,
                    `isDeleted`,
                    `createdAt`,
                    `updatedAt`,
                    `deletedAt`
                )
                SELECT
                    `id`,
                    COALESCE(`title`, ''),
                    COALESCE(`contentHtml`, ''),
                    COALESCE(`contentPlainText`, ''),
                    COALESCE(`colorLabel`, 0),
                    COALESCE(`isPinned`, 0),
                    COALESCE(`isLocked`, 0),
                    COALESCE(`isChecklist`, 0),
                    COALESCE(`isDeleted`, 0),
                    COALESCE(`createdAt`, CAST(strftime('%s','now') AS INTEGER) * 1000),
                    COALESCE(`updatedAt`, CAST(strftime('%s','now') AS INTEGER) * 1000),
                    `deletedAt`
                FROM `memos`
                """.trimIndent(),
            )

            db.execSQL("DROP TABLE `memos`")
            db.execSQL("ALTER TABLE `memos_new` RENAME TO `memos`")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `memos_fts` USING FTS4(`title`, `contentPlainText`, content=`memos`)"
            )
            db.execSQL("INSERT INTO `memos_fts`(`memos_fts`) VALUES('rebuild')")

            db.execSQL("DROP TABLE IF EXISTS `todo_items_new`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todo_items_new` (
                    `id` TEXT NOT NULL,
                    `tabId` INTEGER NOT NULL,
                    `text` TEXT NOT NULL,
                    `checked` INTEGER NOT NULL,
                    `dueDate` INTEGER,
                    `sortOrder` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `checkedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `todo_items_new`(
                    `id`,
                    `tabId`,
                    `text`,
                    `checked`,
                    `dueDate`,
                    `sortOrder`,
                    `createdAt`,
                    `checkedAt`
                )
                SELECT
                    `id`,
                    `tabId`,
                    `text`,
                    `checked`,
                    `dueDate`,
                    `sortOrder`,
                    `createdAt`,
                    `checkedAt`
                FROM `todo_items`
                """.trimIndent(),
            )

            db.execSQL("DROP TABLE `todo_items`")
            db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_todo_items_tab_checked_sort` ON `todo_items` (`tabId`, `checked`, `sortOrder`)"
            )
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `memos` ADD COLUMN `blocks` TEXT NOT NULL DEFAULT '[]'")

            db.execSQL(
                """
                UPDATE `memos`
                SET `blocks` =
                    '[{"type":"rich_text","id":"legacy_' || `id` || '","html":"' ||
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(COALESCE(`contentHtml`, ''), '\\', '\\\\'),
                                '"',
                                '\\"'
                            ),
                            CHAR(13),
                            ''
                        ),
                        CHAR(10),
                        '\\n'
                    )
                    || '"}]'
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dictionary_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `label` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }
}
