package com.m3u.features.crash.screen.detail

import android.app.Application
import com.m3u.core.architecture.viewmodel.AndroidPlatformViewModel
import kotlinx.coroutines.flow.update
import java.io.File

class DetailViewModel(
    application: Application
) : AndroidPlatformViewModel<DetailState, DetailEvent>(
    application = application,
    emptyState = DetailState()
) {
    override fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.Init -> init(event.path)
        }
    }

    private fun init(path: String) {
        val file = File(context.cacheDir, path)
        val text = file.readText()
        writable.update {
            it.copy(
                text = text
            )
        }
    }
}