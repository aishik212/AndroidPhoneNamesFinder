package com.texts.devicenamefinder

import android.app.Activity
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.database.getStringOrNull
import java.io.File
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

        fun getDeviceDataInPref(
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
            customPhoneName: String = Build.MODEL,
        ) =
            Thread {
                tries = 0
                val phoneName = "like '%${customPhoneName}'"
//                val phoneName = "like '%JKM-LX1'"
                val fileName = "MySQLiteDB.sqlite"
                val file = activity.getDatabasePath(fileName)
                if (file.exists()) {
                    if (BuildConfig.DEBUG) {
                        getFinalDetails(file, phoneName, deviceDetailsListener, true, activity)
                    } else {
                        getFinalDetails(file, phoneName, deviceDetailsListener, forced, activity)
                    }
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
                    getFinalDetails(file, phoneName, deviceDetailsListener, forced, activity)
                }
            }.start()

        var tries = 0
        private fun getFinalDetails(
            file: File,
            phoneName: String,
            deviceDetailsListener: DeviceDetailsListener,
            forced: Boolean,
            activity: Activity,
        ) {
            val openOrCreateDatabase = SQLiteDatabase.openOrCreateDatabase(file, null)
            if (!forced && !hasDeviceDataInPref(activity)) {
                val doQuery = doQuery(openOrCreateDatabase, phoneName, phoneName)
                setDeviceDateToPref(activity, doQuery)
                deviceDetailsListener.details(doQuery)
            } else {
                deviceDetailsListener.details(getDeviceDataInPref(activity))
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

    }


}