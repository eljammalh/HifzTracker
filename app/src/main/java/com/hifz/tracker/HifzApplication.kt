package com.hifz.tracker

import android.app.Application
import com.hifz.tracker.notify.Reminders

class HifzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
    }
}
