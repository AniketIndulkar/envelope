package dev.novanest.envelope.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.novanest.envelope.ocr.rememberScannerLauncher
import dev.novanest.envelope.ui.history.HistoryDetailScreen
import dev.novanest.envelope.ui.history.HistoryDetailViewModel
import dev.novanest.envelope.ui.home.HomeScreen
import dev.novanest.envelope.ui.receipt.ReceiptScreen
import dev.novanest.envelope.ui.result.ResultModel
import dev.novanest.envelope.ui.result.ResultScreen
import dev.novanest.envelope.ui.review.ReviewScreen
import dev.novanest.envelope.ui.settings.SettingsScreen

@Composable
fun EnvelopeNav() {
    val nav = rememberNavController()
    val session: LetterSessionViewModel = viewModel()
    val ui by session.ui.collectAsStateWithLifecycle()
    val scan = rememberScannerLauncher { uris -> session.startFromUris(uris); nav.navigate("review") }

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = viewModel(),
                onScan = scan,
                onSample = { session.startFromSample(it); nav.navigate("review") },
                onOpen = { nav.navigate("history/${it.id}") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("review") {
            ReviewScreen(ui, { nav.popBackStack() }, session::hideWord, session::showValue, session::setLanguage, onStartAgain = { nav.popBackStack("home", false) }) {
                session.explain { nav.navigate("result") }
            }
        }
        composable("result") {
            val explained = ui.explained
            val prepared = ui.prepared
            if (explained != null && prepared != null) {
                ResultScreen(
                    ResultModel(
                        sender = ui.quick?.sender ?: "Your letter",
                        explanation = explained.restored,
                        tokens = prepared.outcome.redaction.tokens,
                        unknownTokens = explained.unknownTokens,
                        hiddenCount = prepared.outcome.redaction.tokens.size,
                    ),
                    onBack = { nav.popBackStack() },
                    onReceipt = { nav.navigate("receipt") },
                )
            }
        }
        composable("receipt") {
            ui.explained?.let { ReceiptScreen(it.sentRedaction, ui.steps) { nav.popBackStack() } }
        }
        composable("settings") { SettingsScreen { nav.popBackStack() } }
        composable("history/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val vm: HistoryDetailViewModel = viewModel()
            val id = entry.arguments?.getString("id").orEmpty()
            LaunchedEffect(id) { vm.load(id) }
            HistoryDetailScreen(vm) { nav.popBackStack() }
        }
    }
}
