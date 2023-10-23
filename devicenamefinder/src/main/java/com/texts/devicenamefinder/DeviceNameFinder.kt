package com.texts.devicenamefinder

import android.app.Activity
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.database.getStringOrNull
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DeviceNameFinder : Activity() {
    companion object {
        private fun setDeviceDateToPref(
            activity: Activity,
            deviceDetailsModel: DeviceDetailsModel?,
        ): Boolean {
            if (deviceDetailsModel != null) {
                val sharedPreferences = activity.getSharedPreferences(activity.packageName, 0)
                val edit = sharedPreferences.edit()
                if (edit.putString("calculatedName", deviceDetailsModel.calculatedName).commit()
                    and
                    edit.putString("codeName", deviceDetailsModel.codeName).commit()
                    and
                    edit.putString("commonName", deviceDetailsModel.commonName).commit()
                    and
                    edit.putString("brand", deviceDetailsModel.brand).commit()
                    and
                    edit.putString("modelName", deviceDetailsModel.modelName).commit()
                ) {
                    return true
                }
            }
            return false
        }

        private fun hasDeviceDataInPref(
            activity: Activity,
        ): Boolean {
            if (BuildConfig.DEBUG) {
                return false
            }
            val sharedPreferences = activity.getSharedPreferences(activity.packageName, 0)
            if (sharedPreferences.contains("calculatedName")
                and
                sharedPreferences.contains("codeName")
                and
                sharedPreferences.contains("commonName")
                and
                sharedPreferences.contains("brand")
                and
                sharedPreferences.contains("modelName")
            ) {
                return true
            }
            return false
        }

        fun GetDeviceDataInPref(
            activity: Activity,
        ): DeviceDetailsModel? {
            return if (hasDeviceDataInPref(activity)) {
                val sharedPreferences = activity.getSharedPreferences(activity.packageName, 0)
                DeviceDetailsModel(
                    sharedPreferences.getString("brand", null),
                    sharedPreferences.getString("commonName", null),
                    sharedPreferences.getString("codeName", null),
                    sharedPreferences.getString("modelName", null),
                    sharedPreferences.getString("calculatedName", null)
                )
            } else {
                null
            }
        }

        fun getPhoneValues(
            activity: Activity,
            deviceDetailsListener: DeviceDetailsListener,
            forced: Boolean = false,
            customPhoneName: String? = null,
        ) =
            Thread {
                tries = 0
                val phoneName = if (customPhoneName != null) {
                    val split = customPhoneName.split(" ")
                    if (split.size > 1) {
                        "like '%${split.subList(1, split.size).joinToString(" ")}'"
                    } else {
                        "like '%${customPhoneName}'"
                    }
                } else {
                    "like '%${Build.MODEL}'"
                }
//              val phoneName = "like 'rubyplus'"
                if (phoneName.contains("sdk_gphone")) {
                    deviceDetailsListener.details(
                        DeviceDetailsModel(
                            "Emulator",
                            "Emulator",
                            "Emulator",
                            "Emulator",
                            "Emulator"
                        )
                    )
                } else {
                    getFinalDetails(
                        phoneName,
                        deviceDetailsListener,
                        forced,
                        activity
                    )
                }
            }.start()

        var tries = 0
        var openOrCreateDatabase: SQLiteDatabase? = null
        private fun getFinalDetails(
            phoneName: String,
            deviceDetailsListener: DeviceDetailsListener,
            forced: Boolean,
            activity: Activity,
        ) {
            openOrCreateDatabase?.let {
                if (forced) {
                    val doQuery = doQuery(it, phoneName, phoneName)
                    setDeviceDateToPref(activity, doQuery)
                    deviceDetailsListener.details(doQuery)
                } else {
                    if (hasDeviceDataInPref(activity)) {
                        deviceDetailsListener.details(GetDeviceDataInPref(activity))
                    } else {
                        val doQuery = doQuery(it, phoneName, phoneName)
                        setDeviceDateToPref(activity, doQuery)
                        deviceDetailsListener.details(doQuery)
                    }
                }
            }
        }

        var tries1: Int = 0
        private fun doQuery(
            openOrCreateDatabase: SQLiteDatabase,
            queryParams: String,
            phoneName: String,
            replace: Boolean = false,
        ): DeviceDetailsModel? {
            tries1++
            /*
                        if (tries1 == 10) {
                            return null
                        }
            */
            val tableName = "supported_devices_supported_devices"
            var s =
                "SELECT * FROM $tableName where Model $queryParams or " +
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
                        if (commonName.lowercase().replace(" ", "") == cursor.getStringOrNull(1)
                                ?.lowercase()?.replace(" ", "")
                        ) {
                            curCount++
                        } else {
                            var common = 0
                            var length = commonName.length
                            val toString = cursor.getStringOrNull(1)
                            if (toString != null && toString.length == length) {
                                while (length != 0) {
                                    if (commonName[length - 1] == toString[length - 1]
                                    ) {
                                        common++
                                    }
                                    length--
                                }
                            }
                            if (common - commonName.length < 3) {
                                curCount++
                            }
                        }
                    }
                }
                if (curCount == count) {
                    val s1 =
                        "$brand ${
                            "${commonName?.lowercase()}".replace(
                                "${brand?.lowercase()} ",
                                ""
                            )
                        }"
                    return DeviceDetailsModel(
                        brand,
                        commonName,
                        codeName,
                        modelName,
                        s1
                    )
                }

                val queryParams1 = phoneName.replace("%", "").replace("like", "")
                closeCursor(cursor)
                var replace1 =
                    "SELECT * FROM $tableName where \"Marketing Name\" like$queryParams1 or Model like$queryParams1"

                when (tries) {
                    0 -> {
                        replace1 =
                            "SELECT * FROM $tableName where \"Marketing Name\" like$queryParams1 or Model like$queryParams1"
                    }

                    1 -> {
                        replace1 =
                            "SELECT * FROM $tableName where \"Marketing Name\" like$queryParams1"
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
                    closeCursor(cursor)
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
            closeCursor(cursor)
            return null
        }

        private fun closeCursor(cursor: Cursor) {
            cursor.close()
        }

        fun init(activity: Activity): Boolean {
            if (openOrCreateDatabase != null) {
                return true
            } else {
                try {
                    val fileName = "MySQLiteDB.sqlite"
                    val file = activity.getDatabasePath(fileName)
                    file.delete()
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
                    if (openOrCreateDatabase == null) {
                        openOrCreateDatabase = SQLiteDatabase.openOrCreateDatabase(file, null)
                    }
                    return true
                } catch (e: Exception) {
                    return false
                }
            }
        }

    }


}