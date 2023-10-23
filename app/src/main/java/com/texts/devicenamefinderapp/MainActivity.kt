package com.texts.devicenamefinderapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.texts.devicenamefinder.DeviceDetailsListener
import com.texts.devicenamefinder.DeviceDetailsModel
import com.texts.devicenamefinder.DeviceNameFinder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (DeviceNameFinder.init(this)) {
            DeviceNameFinder.getPhoneValues(this, object : DeviceDetailsListener {
                override fun details(doQuery: DeviceDetailsModel?) {
                    super.details(doQuery)
                    runOnUiThread {
                        findViewById<TextView>(R.id.tv).text = doQuery?.calculatedName
                    }
                }
            })
        }
    }
}