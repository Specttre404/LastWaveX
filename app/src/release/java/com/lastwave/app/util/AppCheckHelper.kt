package com.lastwave.app.util

import android.content.Context
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckHelper {
    fun createProviderFactory(context: Context): AppCheckProviderFactory {
        return PlayIntegrityAppCheckProviderFactory.getInstance()
    }
}
