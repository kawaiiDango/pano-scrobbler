package com.arn.scrobble.db

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec


@RenameColumn(
    tableName = RegexEditsDao.tableName,
    fromColumnName = "field",
    toColumnName = "fields"
)
class Spec_10_11 : AutoMigrationSpec