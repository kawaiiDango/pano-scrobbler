package com.arn.scrobble.pref

import androidx.datastore.core.DataMigration


class MainPrefsMigration6 : DataMigration<MainPrefs> {

    override suspend fun shouldMigrate(currentData: MainPrefs) =
        currentData.version < 6

    override suspend fun migrate(currentData: MainPrefs): MainPrefs {
        if (currentData.changelogSeenHashcode == null) // first install
            return currentData.copy(
                version = 6,
            )
        // keep spotifyApi = true for existing installations, but show prompt
        else
            return currentData.copy(
                spotifyApi = true,
                version = 6,
            )
    }

    override suspend fun cleanUp() {

    }
}