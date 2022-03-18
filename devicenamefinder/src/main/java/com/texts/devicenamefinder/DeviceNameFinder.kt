package com.texts.devicenamefinder

import android.app.Activity
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DeviceNameFinder : Activity() {
    companion object {
        fun getPhoneValues(
            activity: Activity,
            deviceDetailsListener: DeviceDetailsListener,
            forced: Boolean = false,
        ) =
            Thread {
                tries = 0
//                val phoneName = "like '%${Build.MODEL}'"
                val phoneName = "like '%Redmi note 7'"
                val fileName = "MySQLiteDB.sqlite"
                val file = activity.getDatabasePath(fileName)
                if (file.exists()) {
                    getFinalDetails(file, phoneName, deviceDetailsListener, forced)
                } else {
                    val inputStream: InputStream = activity.assets.open("data.sqlite")
                    val outputStream: OutputStream = FileOutputStream(file)
                    val buffer = ByteArray(1024 * 8)
                    var numOfBytesToRead: Int
                    while (inputStream.read(buffer).also {
                            numOfBytesToRead = it
                        } > 0) outputStream.write(
                        buffer,
                        0,
                        numOfBytesToRead
                    )
                    inputStream.close()
                    outputStream.close()
                    getFinalDetails(file, phoneName, deviceDetailsListener, forced)
                }
            }.start()

        var tries = 0
        private fun getFinalDetails(
            file: File,
            phoneName: String,
            deviceDetailsListener: DeviceDetailsListener,
            forced: Boolean,
        ) {
            val openOrCreateDatabase = SQLiteDatabase.openOrCreateDatabase(file, null)
            val doQuery = doQuery(openOrCreateDatabase, phoneName, phoneName)
            deviceDetailsListener.details(doQuery)
        }

        private fun doQuery(
            openOrCreateDatabase: SQLiteDatabase,
            queryParams: String,
            phoneName: String,
            replace: Boolean = false,
        ): DeviceDetailsModel? {
            var s =
                "SELECT * FROM supported_devices_supported_devices where Model $queryParams or " +
                        "Device $queryParams or " +
                        "\"Marketing Name\" $queryParams"
            if (replace) {
                s = queryParams
            }
            val cursor: Cursor = openOrCreateDatabase.rawQuery(
                s,
                null
            )
            val count = cursor.count
            if (count > 1) {
                var commonName: String? = null
                var brand: String? = null
                var curCount = 0
                var modelName: String? = null
                var codeName: String? = null
                while (cursor.moveToNext()) {
                    if (commonName == null) {
                        brand = cursor.getStringOrNull(0)
                        commonName = cursor.getStringOrNull(1)
                        modelName = cursor.getStringOrNull(3)
                        codeName = cursor.getStringOrNull(2)
                        curCount++
                    } else {
                        if (commonName == cursor.getStringOrNull(1)?.lowercase()) {
                            curCount++
                        }
                    }
                }
                if (curCount == count) {
                    return DeviceDetailsModel(
                        brand,
                        commonName,
                        codeName,
                        modelName,
                        "$brand $commonName"
                    )
                }

                val queryParams1 = phoneName.replace("%", "").replace("like", "")
                cursor.close()
                var replace1 =
                    "SELECT * FROM supported_devices_supported_devices where \"Marketing Name\" like$queryParams1 or Model like$queryParams1"

                when (tries) {
                    0 -> {
                        replace1 =
                            "SELECT * FROM supported_devices_supported_devices where \"Marketing Name\" like$queryParams1 or Model like$queryParams1"
                    }
                    1 -> {
                        replace1 =
                            "SELECT * FROM supported_devices_supported_devices where \"Marketing Name\" like$queryParams1"
                    }
                }
                tries++
                return doQuery(openOrCreateDatabase, replace1, phoneName, true)
            } else if (count == 1) {
                while (cursor.moveToNext()) {
                    val brand = cursor.getStringOrNull(0)
                    val modelName = cursor.getStringOrNull(3)
                    val commonName = cursor.getStringOrNull(1)
                    val codeName = cursor.getStringOrNull(2)
                    cursor.close()
                    return if ("$commonName".contains("$brand ")) {
                        val s1 = "$brand ${"$commonName".replace("$brand ", "")}"
                        DeviceDetailsModel(brand, commonName, codeName, modelName, s1)
                    } else {
                        DeviceDetailsModel(
                            brand,
                            commonName,
                            codeName,
                            modelName,
                            "$brand $commonName"
                        )
                    }
                }
            }
            cursor.close()
            return null
        }

    }


}