package dev.novanest.envelope.ui.home

import dev.novanest.envelope.ui.components.DarkStatusBarIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.novanest.envelope.ai.NanoState
import dev.novanest.envelope.data.LetterRecord
import dev.novanest.envelope.data.SampleLetter
import dev.novanest.envelope.ui.components.Dock
import dev.novanest.envelope.ui.components.LineButton
import dev.novanest.envelope.ui.components.LineButtonKind
import dev.novanest.envelope.ui.components.LinePill
import dev.novanest.envelope.ui.components.PillKind
import dev.novanest.envelope.ui.theme.Ink
import dev.novanest.envelope.ui.theme.Mine
import dev.novanest.envelope.ui.theme.Muted
import dev.novanest.envelope.ui.theme.PhoneBg
import dev.novanest.envelope.ui.theme.SoftSurface
import dev.novanest.envelope.ui.theme.StandIn
import dev.novanest.envelope.ui.theme.Surface
import dev.novanest.envelope.ui.theme.UiFamily
import dev.novanest.envelope.ui.theme.WarnInk

/**
 * Wallet home: your saved letters as passes. The soonest deadline is in front; the others are stacked strips above it.
 * Only real saved letters appear here, never samples.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onScan: () -> Unit,
    onSample: (SampleLetter) -> Unit,
    onOpen: (LetterRecord) -> Unit,
    onSettings: () -> Unit,
) {
    DarkStatusBarIcons()
    val nano by vm.nano.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val letters = remember(history) { history.map { WalletLetter.of(it) } }
    val records = remember(history) { history.associate { it.record.id to it.record } }
    var pickSample by remember { mutableStateOf(false) }

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var fanned by rememberSaveable { mutableStateOf(false) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    // Letters already seen, so one saved while we were away can drop in. The first non-empty list is the baseline.
    var known by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var landedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(letters) {
        if (letters.isEmpty()) return@LaunchedEffect
        val ids = letters.map { it.id }
        val before = known
        if (before != null) ids.firstOrNull { it !in before }?.let { new ->
            landedId = new; selectedId = new; flipped = false; fanned = false
        }
        known = ids
    }
    val front = letters.firstOrNull { it.id == selectedId } ?: letters.firstOrNull()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(PhoneBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp + navBottom),
        ) {
            Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Envelope", color = Ink, fontFamily = UiFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.045).em, modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Tune, contentDescription = "Settings", tint = Ink) }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) { NanoPill(nano) }
                if (letters.size > 1) Text(
                    if (fanned) "Stack" else "Show all ${letters.size}",
                    color = StandIn, fontFamily = UiFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { fanned = !fanned }
                        .semantics { stateDescription = if (fanned) "Showing all letters" else "Stacked" }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                )
            }
            if (!vm.firebaseReady) FirebaseNotice()
            if (front == null) EmptyPass()
            else WalletStack(
                letters = letters,
                front = front,
                fanned = fanned,
                flipped = flipped,
                landedId = landedId,
                onPick = { selectedId = it.id; flipped = false; fanned = false; landedId = null },
                onOpen = { l -> records[l.id]?.let(onOpen) },
                onFlip = { flipped = !flipped },
            )
        }

        Dock(Modifier.align(Alignment.BottomCenter)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineButton("Scan a letter", onScan, Modifier.weight(1f), icon = Icons.Rounded.DocumentScanner)
                LineButton("Sample", { pickSample = true }, kind = LineButtonKind.Secondary)
            }
        }
    }

    if (pickSample) {
        ModalBottomSheet(onDismissRequest = { pickSample = false }, containerColor = Surface) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Try a sample letter", color = Ink, fontFamily = UiFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
                )
                Text(
                    "Fictional letters, so you can try Envelope without your own.", color = Muted, fontFamily = UiFamily, fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                vm.samples.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftSurface)
                            .clickable(role = Role.Button) { pickSample = false; onSample(s) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = StandIn, modifier = Modifier.size(20.dp))
                        Text(s.title, color = Ink, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun NanoPill(state: NanoState) = when (state) {
    NanoState.Ready -> LinePill("Gemini Nano on this phone", kind = PillKind.Phone, dot = true)
    is NanoState.Downloading -> LinePill("Getting Gemini Nano… ${state.percent}%", kind = PillKind.Warn, dot = true)
    NanoState.Downloadable -> LinePill("Gemini Nano can be downloaded in Settings", kind = PillKind.Warn, dot = true)
    NanoState.Checking -> LinePill("Checking for Gemini Nano…", kind = PillKind.Phone)
    NanoState.Unavailable -> LinePill("No Nano: numbers still hidden", kind = PillKind.Warn, dot = true)
}

@Composable
private fun FirebaseNotice() {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(14.dp)).background(Mine)
            .semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Settings, contentDescription = null, tint = WarnInk, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Firebase isn’t set up", color = WarnInk, fontFamily = UiFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Add your google-services.json to explain letters. Scanning and hiding details still work.",
                color = WarnInk, fontFamily = UiFamily, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }
    }
}
