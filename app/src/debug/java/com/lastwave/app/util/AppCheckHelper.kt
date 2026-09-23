package com.lastwave.app.util

import android.content.Context
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckHelper {
    fun createProviderFactory(context: Context): AppCheckProviderFactory {
        return DebugAppCheckProviderFactory.getInstance()
    }
}
