package dev.novanest.envelope.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.novanest.envelope.EnvelopeApp
import dev.novanest.envelope.ai.NanoState
import dev.novanest.envelope.data.LetterRecord
import dev.novanest.envelope.data.SampleLetter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import dev.novanest.envelope.privacy.Restorer
import kotlinx.coroutines.launch

data class HomeItem(val record: LetterRecord, val line: String)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as EnvelopeApp).container

    val nano: StateFlow<NanoState> = container.nanoStatus.state
    /** History from Firestore is tokenised; real details are put back here, on the phone, from the local token map. */
    val history: StateFlow<List<HomeItem>> = (container.letters?.let { repo ->
        repo.history().map { list ->
            list.map { r ->
                val line = r.quickCheck?.oneLine ?: r.explanation?.summary.orEmpty()
                HomeItem(r, Restorer.restore(line, repo.tokens(r.id)).text)
            }
        }
    } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val samples: List<SampleLetter> get() = container.samples.all
    val firebaseReady = container.firebaseReady

    /**
     * The SDK never downloads Nano by itself. On a supported phone without the model we start the download straight
     * away (in an app-wide scope), so redaction is private by the time the first letter is scanned.
     */
    init {
        viewModelScope.launch {
            container.nanoStatus.refresh()
            if (container.nanoStatus.state.value == NanoState.Downloadable) container.downloadNano()
        }
    }

    fun downloadNano() = container.downloadNano()
}
