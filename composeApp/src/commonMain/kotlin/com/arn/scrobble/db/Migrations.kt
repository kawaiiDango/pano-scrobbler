package com.arn.scrobble.db

import androidx.room3.DeleteColumn
import androidx.room3.DeleteTable
import androidx.room3.RenameColumn
import androidx.room3.migration.AutoMigrationSpec


@RenameColumn(
    tableName = RegexEditsDao.tableName,
    fromColumnName = "field",
    toColumnName = "fields"
)
class Spec_10_11 : AutoMigrationSpec

@DeleteColumn(
    tableName = "simpleEdits",
    columnName = "legacyHash"
)
class Spec_16_17 : AutoMigrationSpec

@DeleteTable(tableName = "cachedTracks")
@DeleteTable(tableName = "cachedAlbums")
@DeleteTable(tableName = "cachedArtists")
@DeleteTable(tableName = "cachedTracksWithAlbums")
class Spec_18_19 : AutoMigrationSpec