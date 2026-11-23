package dev.novanest.envelope.ui.history

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.novanest.envelope.EnvelopeApp
import dev.novanest.envelope.privacy.Restorer
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.components.LoadingCard
import dev.novanest.envelope.ui.components.Notice
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.Primary
import dev.novanest.envelope.ui.theme.Tint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import dev.novanest.envelope.ui.result.ResultModel
import dev.novanest.envelope.ui.result.ResultScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Reopens a saved letter: tokenised record from Firestore + real details from this phone's token map. */
class HistoryDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as EnvelopeApp).container.letters
    private val _model = MutableStateFlow<ResultModel?>(null)
    val model: StateFlow<ResultModel?> = _model
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed

    fun load(id: String) {
        if (_model.value != null) return
        if (repo == null) { _failed.value = true; return }
        viewModelScope.launch {
            val record = repo.find(id)
            val explanation = record?.explanation ?: run { _failed.value = true; return@launch }
            val tokens = repo.tokens(id)
            val unknown = linkedSetOf<String>()
            fun r(s: String) = Restorer.restore(s, tokens).also { unknown += it.unknownTokens }.text
            val restored = explanation.copy(
                summary = r(explanation.summary), whatItMeans = r(explanation.whatItMeans),
                actions = explanation.actions.map { it.copy(step = r(it.step)) }, consequences = r(explanation.consequences),
                redFlags = explanation.redFlags.map(::r), draftReply = r(explanation.draftReply),
                helpLinks = explanation.helpLinks.map { it.copy(label = r(it.label)) },
            )
            _model.value = ResultModel(record.quickCheck?.sender ?: "Letter", restored, tokens, unknown.toList(), tokens.size)
        }
    }
}

@Composable
fun HistoryDetailScreen(vm: HistoryDetailViewModel, onBack: () -> Unit) {
    val model by vm.model.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    model?.let { ResultScreen(it, onBack, onReceipt = null) } ?: Column(Modifier.fillMaxSize().statusBarsPadding()) {
        EnvelopeTopBar("What it means", onBack)
        Column(Modifier.padding(16.dp)) {
            if (failed) Notice(
                Icons.Rounded.CloudOff, "Couldn’t open this letter",
                "Check your internet connection and try again. Your details are still safe on this phone.",
                Tint, Deep, CardWhite, Primary,
            ) else LoadingCard("Opening your letter…")
        }
    }
}
