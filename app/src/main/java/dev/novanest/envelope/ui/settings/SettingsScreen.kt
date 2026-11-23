package dev.novanest.envelope.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.novanest.envelope.BuildConfig
import dev.novanest.envelope.EnvelopeApp
import dev.novanest.envelope.ai.NanoState
import dev.novanest.envelope.data.Prefs
import dev.novanest.envelope.ui.components.EnvelopeButton
import dev.novanest.envelope.ui.components.EnvelopeCard
import dev.novanest.envelope.ui.components.EnvelopeTopBar
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as EnvelopeApp).container
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    val nano by container.nanoStatus.state.collectAsStateWithLifecycle()
    var language by remember { mutableStateOf(prefs.language) }
    var forceRules by remember { mutableStateOf(prefs.forceRulesOnly) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        EnvelopeTopBar("Settings", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EnvelopeCard {
                Text("Gemini Nano", style = MaterialTheme.typography.titleLarge)
                Text(
                    when (nano) {
                        NanoState.Ready -> "Ready. Personal details are found on this phone."
                        is NanoState.Downloading -> "Downloading… ${(nano as NanoState.Downloading).percent}%"
                        NanoState.Downloadable -> "Available for this phone. Download it to find names and addresses privately."
                        NanoState.Checking -> "Checking this phone…"
                        NanoState.Unavailable -> "Not available on this phone. Envelope hides numbers with rules, and you can tap names to hide them."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (nano == NanoState.Downloadable) {
                    EnvelopeButton("Download Gemini Nano", { scope.launch { container.nanoStatus.download() } }, icon = Icons.Rounded.Download)
                }
            }
            EnvelopeCard {
                Text("Explain letters in", style = MaterialTheme.typography.titleLarge)
                Text("Replies to the sender are always written in English.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                Prefs.LANGUAGES.forEach { lang ->
                    Row(
                        Modifier.fillMaxWidth().selectable(language == lang, role = Role.RadioButton) { language = lang; prefs.language = lang }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == lang, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                        Text("  $lang", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (BuildConfig.DEBUG) EnvelopeCard {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Switch) { forceRules = !forceRules; prefs.forceRulesOnly = forceRules },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo: act like a phone without Nano", style = MaterialTheme.typography.titleMedium)
                        Text("Debug builds only.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    }
                    Switch(checked = forceRules, onCheckedChange = null)
                }
            }
        }
    }
}

