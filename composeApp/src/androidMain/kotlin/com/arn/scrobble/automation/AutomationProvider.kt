package com.arn.scrobble.automation

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.arn.scrobble.utils.AndroidStuff.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


class AutomationProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(
        p0: Uri,
        p1: String?,
        p2: Array<out String?>?
    ): Int = 0

    override fun getType(p0: Uri): String? = null

    override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? {
        val callingPackage = callingPackage ?: return createCursor(false)
        val command = uri.pathSegments?.first() ?: return createCursor(false)
        val arg = uri.pathSegments.getOrNull(1)

        val wasSuccessful = Automation.executeAction(
            command,
            arg,
            callingPackage
        )

        if (wasSuccessful) {
            runBlocking(Dispatchers.Main) {
                context?.toast(command)
            }
        }

        return createCursor(wasSuccessful)
    }

    private fun createCursor(successful: Boolean): Cursor {
        val cursor = MatrixCursor(arrayOf("result"))
        cursor.addRow(
            arrayOf(
                if (successful)
                    "Ok"
                else
                    "Failed"
            )
        )

        return cursor
    }

    override fun update(
        p0: Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String?>?
    ): Int = 0
}