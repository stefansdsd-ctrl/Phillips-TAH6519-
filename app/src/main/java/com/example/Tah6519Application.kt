package com.example

import android.app.Application
import android.content.Context
import android.os.Build

class Tah6519Application : Application() {
    override fun attachBaseContext(base: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attributionContext = base.createAttributionContext("default")
            super.attachBaseContext(attributionContext)
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun getAttributionTag(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "default"
        } else {
            super.getAttributionTag()
        }
    }
}
