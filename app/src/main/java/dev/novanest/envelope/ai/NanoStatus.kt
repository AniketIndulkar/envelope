package dev.novanest.envelope.ai

import com.google.firebase.ai.DownloadStatus
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface NanoState {
    data object Checking : NanoState
    data object Ready : NanoState
    data class Downloading(val percent: Int) : NanoState
    data object Downloadable : NanoState
    data object Unavailable : NanoState
}

/** Tracks whether Gemini Nano can run on this phone. The SDK never downloads on its own, so we do it up front. */
@OptIn(PublicPreviewAPI::class)
class NanoStatus(private val model: GenerativeModel?) {

    private val _state = MutableStateFlow<NanoState>(if (model == null) NanoState.Unavailable else NanoState.Checking)
    val state: StateFlow<NanoState> = _state.asStateFlow()

    val isReady: Boolean get() = _state.value == NanoState.Ready

    suspend fun refresh() {
        val ext = model?.onDeviceExtension ?: return run { _state.value = NanoState.Unavailable }
        _state.value = try {
            when (ext.checkStatus()) {
                OnDeviceModelStatus.AVAILABLE -> NanoState.Ready.also { runCatching { ext.warmUp() } }
                OnDeviceModelStatus.DOWNLOADABLE -> NanoState.Downloadable
                OnDeviceModelStatus.DOWNLOADING -> NanoState.Downloading(0)
                else -> NanoState.Unavailable
            }
        } catch (e: Exception) {
            NanoState.Unavailable
        }
    }

    suspend fun download() {
        val ext = model?.onDeviceExtension ?: return
        var total = 0L
        ext.download().collect { s ->
            when (s) {
                is DownloadStatus.DownloadStarted -> { total = s.bytesToDownload; _state.value = NanoState.Downloading(0) }
                is DownloadStatus.DownloadInProgress -> _state.value =
                    NanoState.Downloading(if (total > 0) (s.totalBytesDownloaded * 100 / total).toInt() else 0)
                is DownloadStatus.DownloadCompleted -> refresh()
                is DownloadStatus.DownloadFailed -> _state.value = NanoState.Downloadable
                else -> Unit
            }
        }
    }
}
