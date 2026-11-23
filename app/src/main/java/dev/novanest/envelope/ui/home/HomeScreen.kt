package dev.novanest.envelope.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CurrencyPound
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.novanest.envelope.ai.NanoState
import dev.novanest.envelope.data.LetterRecord
import dev.novanest.envelope.data.SampleLetter
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.components.ButtonKind
import dev.novanest.envelope.ui.components.CardShape
import dev.novanest.envelope.ui.components.DeadlineTile
import dev.novanest.envelope.ui.components.EnvelopeButton
import dev.novanest.envelope.ui.components.EnvelopeCard
import dev.novanest.envelope.ui.components.HeroCard
import dev.novanest.envelope.ui.components.IconTile
import dev.novanest.envelope.ui.components.Notice
import dev.novanest.envelope.ui.components.Pill
import dev.novanest.envelope.ui.components.SectionTitle
import dev.novanest.envelope.ui.components.SquareIconButton
import dev.novanest.envelope.ui.review.categoryIcon
import dev.novanest.envelope.ui.theme.CardWhite
import dev.novanest.envelope.ui.theme.Deep
import dev.novanest.envelope.ui.theme.InkFaint
import dev.novanest.envelope.ui.theme.InkSoft
import dev.novanest.envelope.ui.theme.OnDeepSoft
import dev.novanest.envelope.ui.theme.Ready
import dev.novanest.envelope.ui.theme.SoonTile
import dev.novanest.envelope.ui.theme.Tint
import dev.novanest.envelope.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onScan: () -> Unit,
    onSample: (SampleLetter) -> Unit,
    onOpen: (LetterRecord) -> Unit,
    onSettings: () -> Unit,
) {
    val nano by vm.nano.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var pickSample by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(Icons.Rounded.MailOutline, Deep, CardWhite, size = 40.dp, corner = 12.dp)
                Text("Envelope", style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), modifier = Modifier.weight(1f))
                SquareIconButton(Icons.Rounded.Settings, "Settings", onSettings)
            }
        }
        item {
            HeroCard(Modifier.padding(top = 4.dp), flap = true) {
                Spacer(Modifier.height(60.dp))
                NanoBadge(nano)
                Text("Understand any official letter", style = MaterialTheme.typography.displaySmall, color = CardWhite)
                Text(
                    "Your name, address and numbers are hidden on this phone before anything goes online.",
                    style = MaterialTheme.typography.bodyLarge, color = OnDeepSoft,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EnvelopeButton("Scan a letter", onScan, kind = ButtonKind.Light, icon = Icons.Rounded.DocumentScanner)
                    EnvelopeButton("Try a sample letter", { pickSample = true }, kind = ButtonKind.Ghost, height = 50.dp)
                }
            }
        }
        if (!vm.firebaseReady) item {
            Notice(Icons.Rounded.Settings, "Firebase isn’t set up", "Add your google-services.json to explain letters. Scanning and hiding details still work.", Tint, Deep, CardWhite, Primary)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.Bottom) {
                SectionTitle(if (history.isEmpty()) "Your letters" else "Coming up", Modifier.weight(1f).padding(horizontal = 0.dp))
                if (history.isNotEmpty()) Text("${history.size} ${if (history.size == 1) "letter" else "letters"}", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            }
        }
        if (history.isEmpty()) item {
            Notice(
                Icons.Rounded.VerifiedUser, null,
                "Letters you scan show up here, soonest deadline first. Only the hidden version is saved online.",
                Tint, Deep, CardWhite, Primary,
            )
        }
        items(history, key = { it.record.id }) { LetterRow(it) { onOpen(it.record) } }
    }

    if (pickSample) {
        ModalBottomSheet(onDismissRequest = { pickSample = false }, containerColor = CardWhite) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Try a sample letter", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 4.dp))
                Text("Fictional letters, so you can try Envelope without your own.", style = MaterialTheme.typography.bodyMedium, color = InkSoft, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                vm.samples.forEach { s ->
                    Surface(onClick = { pickSample = false; onSample(s) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile(Icons.Rounded.Description, Tint, Primary, size = 40.dp, iconSize = 20.dp, corner = 12.dp)
                            Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = InkFaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NanoBadge(state: NanoState) {
    val (text, dot) = when (state) {
        NanoState.Ready -> "Gemini Nano ready on this phone" to Ready
        is NanoState.Downloading -> "Getting Gemini Nano… ${state.percent}%" to SoonTile
        NanoState.Downloadable -> "Gemini Nano can be downloaded in Settings" to SoonTile
        NanoState.Checking -> "Checking for Gemini Nano…" to OnDeepSoft
        NanoState.Unavailable -> "Private mode: rules on this phone" to SoonTile
    }
    Pill(text, CardWhite.copy(alpha = 0.14f), CardWhite, dot = dot)
}

@Composable
private fun LetterRow(item: HomeItem, onClick: () -> Unit) {
    val record = item.record
    Surface(onClick = onClick, shape = CardShape, color = CardWhite, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DeadlineTile(Dates.daysLeft(record.deadline))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(categoryIcon(record.quickCheck?.category), null, tint = InkSoft, modifier = Modifier.size(15.dp))
                    Text(record.quickCheck?.sender ?: "Letter", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
                Text(item.line, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = InkFaint)
        }
    }
}
