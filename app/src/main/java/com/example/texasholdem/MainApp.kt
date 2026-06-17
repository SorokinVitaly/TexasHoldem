package com.example.texasholdem

import android.app.Application
import coil.ImageLoader
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MainApp : Application() {
    val imageLoader by lazy {
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}