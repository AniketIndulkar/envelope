# Envelope — design

Date: 2026-11-23 · Entry for #BuildHybridAI 2026 (deadline 30 Nov 2026, 10pm) · Target: Best Android App + Grand Prize contention

## 1. Intent

People in the UK get official letters from HMRC, councils, the NHS, banks and landlords that are hard to understand and often carry a deadline with a penalty attached. The people who struggle most (older people, people with English as a second language, people under financial stress) are the same people who should not paste a letter full of their NI number, account numbers and address into a chatbot.

Envelope explains the letter, says what to do and by when, and drafts a reply, while the cloud model **never sees who you are**. Gemini Nano on the phone finds and strips personal details; the cloud model does the hard reasoning on the anonymised text; the phone puts your details back into the answer. A privacy receipt proves what left the phone.

Why it wins (mapped to judging criteria):

| Criterion | How Envelope scores |
|---|---|
| Technological implementation (hybrid use) | Each step uses the inference mode that fits its privacy needs: `ONLY_ON_DEVICE` for redaction, `PREFER_ON_DEVICE` for triage (safe because input is already redacted), `ONLY_IN_CLOUD` for deep reasoning. The privacy receipt shows which engine handled each step, read from `inferenceSource`. The hybrid choice *is* the product. |
| Design (complete product) | End-to-end flow: scan → review → explain → act (calendar reminder, copy/share reply) → history. Works on phones without Nano via a visible, honest fallback. |
| Potential impact | Specific audience, specific harm (missed deadlines, penalties, scams, privacy exposure). Explanation in the user's own language. |
| Quality of idea | Using the on-device model as a privacy firewall for the cloud model is non-obvious and shows understanding of both models' strengths. |

## 2. Goals, non-goals, anti-goals

**Goals**
- Filmable happy path in under 3 minutes on a Nano-capable phone.
- Raw letter text never leaves the device. Enforced in code, provable on screen.
- Works (with reduced redaction) on any Android 8+ phone.
- Uses App Check, Remote Config, Firestore and Anonymous Auth meaningfully.

**Non-goals**
- iOS or web versions (no time in 7 days; a strong single-platform app can still win).
- Accounts, sync across devices, multi-letter conversations, PDF import.
- Legal or financial advice. App explains and signposts (Citizens Advice, GOV.UK); it does not advise.

**Anti-goals (would be failure even if it works)**
- Any path where unredacted text reaches the cloud, including silent SDK fallback.
- Token map (`[NAME_1]` → real name) leaving the phone.
- Redacting so aggressively the explanation becomes useless (e.g. stripping deadlines or amounts owed).

## 3. Constraints (from docs, 2026-11-23)

- Firebase AI Logic hybrid SDK is **Experimental**. `com.google.firebase:firebase-ai:18.2.0`, `com.google.firebase:firebase-ai-ondevice:16.0.0-beta08` (not in BoM, pin it). Needs `@OptIn(PublicPreviewAPI::class)`.
- **minSdk 26** (project is currently 24 — raise it).
- On-device: single-turn only, ≤4000 input tokens, ~256 output tokens recommended, English validated, foreground only, structured JSON output supported, no tools.
- Model status: `FirebaseAIOnDevice.checkStatus()` → `UNAVAILABLE | DOWNLOADABLE | DOWNLOADING | AVAILABLE`; `FirebaseAIOnDevice.download()` flow. SDK does not auto-download.
- Errors: `ErrorCode.BUSY` (retry with backoff), `PER_APP_BATTERY_USE_QUOTA_EXCEEDED`.
- App Check required; debug provider accepted for submission.
- Hackathon: public repo with Apache-2.0 LICENSE, README with value prop, ≤3 min YouTube video, English.

## 4. Approaches considered

| | Approach | Trade-off | Verdict |
|---|---|---|---|
| A | Firebase AI Logic hybrid SDK for both models | One SDK, mode-per-step story; SDK is experimental | **Chosen** — required by hackathon, best story |
| B | ML Kit GenAI Prompt API for Nano + Firebase AI Logic for cloud | More Nano control; two SDKs, weaker story, may not count as hybrid SDK | Rejected |
| C | Rules-only redaction, Nano for triage only | Runs everywhere; drops the headline, can't find names/addresses | Rejected as primary; kept as **fallback** |

No-Nano fallback options: (1) rules-only redaction + warning + manual tap-to-redact, (2) block the app. **Chose (1)**: app stays usable on most phones, the privacy promise still holds (raw text never goes to cloud), and the visible degraded mode demonstrates real understanding of hybrid.

## 5. Pipeline

```
Photo ──► [1 OCR] ──► [2 Redact] ──► [3 Leak check] ──► [4 Quick check] ──► user reviews ──► [5 Explain] ──► [6 Restore] ──► Result + Receipt
          device       device          device             PREFER_ON_DEVICE                    ONLY_IN_CLOUD    device
                                                          (redacted input)                    (redacted input)
```

### 5.1 Capture + OCR (device)
- **ML Kit Document Scanner** (`play-services-mlkit-document-scanner`): edge detection, crop, multi-page, no camera permission. Also gallery import, and **bundled sample letters** for demo/judges.
- **ML Kit Text Recognition v2** (Latin, bundled model) on each page. Join pages with blank line.

### 5.2 Redact (device) — three layers, merged
1. **Pattern layer** (deterministic, regex; patterns in Remote Config with bundled defaults): NI number, sort code, UK account number (8 digits near "account"), IBAN, card number (Luhn), UK postcode, phone, email, HMRC UTR (10 digits near "UTR"/"reference"), NHS number (10 digits, mod-11 check), labelled date of birth.
2. **ML Kit Entity Extraction** (English model): addresses, phone, email, IBAN, payment card — catches formats regex misses.
3. **Nano** (`ONLY_ON_DEVICE`, structured JSON): returns spans only, never rewritten text:
   ```json
   {"entities":[{"text":"Margaret O'Neill","type":"NAME"},{"text":"14 Elm Road","type":"ADDRESS"}]}
   ```
   Finds what rules can't: personal names, street addresses, free-text references. Letters longer than ~2500 words are split into paragraph chunks and each chunk is sent separately.

**Merge:** union all spans; each distinct value gets one token, numbered per type in order of first appearance: `[NAME_1]`, `[ADDRESS_1]`, `[NI_NUMBER_1]`, `[ACCOUNT_1]`, `[REFERENCE_1]`, `[PHONE_1]`, `[EMAIL_1]`, `[POSTCODE_1]`, `[DOB_1]`. Replace every occurrence of each value (case-insensitive, longest first so "Margaret O'Neill" wins over "Margaret").

**Deliberately kept, not redacted:** dates (except labelled DOB), money amounts, organisation names (HMRC, Leeds City Council), tax years. These are what the explanation depends on and identify the letter, not the person. Shown on the receipt as "kept on purpose".

**Not trusted:** Nano spans that do not appear verbatim in the text are dropped (hallucination guard).

### 5.3 Leak check (device)
Before any cloud call, re-run pattern layer on the redacted text. Any hit blocks sending and highlights the match. Also checks no token-map value appears in outgoing text. Single function `assertSafeToSend(text, tokenMap)`; every cloud call goes through it (one choke point, unit tested).

### 5.4 Quick check (`PREFER_ON_DEVICE`, redacted input)
Nano if present (works offline), cloud otherwise. Structured JSON, small:
```json
{"sender":"HMRC","category":"TAX","urgency":"HIGH","deadline":"2026-12-12","oneLine":"You owe £412 in Self Assessment tax; pay by 12 Dec to avoid a £100 penalty."}
```
Shown on the review screen within seconds, while the user checks the redaction.

### 5.5 Review screen (the trust moment)
Letter text with redacted spans as coloured chips (`NAME_1`). User can **tap any word to redact it** or tap a chip to un-redact. Banner states redaction strength: "Nano + rules" or "Rules only — names may remain, check before sending". Button: "Explain (sends only highlighted version)".

### 5.6 Explain (`ONLY_IN_CLOUD`, redacted input)
Cloud model (name from Remote Config, default `gemini-3.8-flash`). System instruction: keep tokens exactly as written; plain English at reading age ~11; answer in chosen language; no legal advice; signpost. Structured JSON:
```json
{
  "summary": "...",
  "whatItMeans": "...",
  "actions": [{"step":"Pay £412 online at GOV.UK","by":"2026-12-12"}],
  "deadline": "2026-12-12",
  "consequences": "If you miss it: £100 penalty, then interest.",
  "redFlags": ["none"],
  "draftReply": "Dear Sir or Madam,\n... [NAME_1]\n[ADDRESS_1]",
  "helpLinks": [{"label":"Citizens Advice","url":"https://www.citizensadvice.org.uk"}]
}
```
`redFlags` = scam signals (pressure, unusual payment method, mismatched sender). Real-world value, cheap to add.
**Explanation language** setting (English default; e.g. Polish, Urdu, Punjabi, Romanian, Bengali). Draft reply stays in English (it goes to the sender).

### 5.7 Restore (device)
Replace tokens in every output field with real values. Tolerant match: `\[\s*(TYPE)_(\d+)\s*\]` case-insensitive. Tokens the model invented (no map entry) are left visible and flagged. Tokens the model dropped are fine.

### 5.8 Result screen
Summary card with deadline countdown; actions checklist; consequences; red flags (if any, prominent); draft reply with Copy / Share; **Add deadline to calendar** (`Intent.ACTION_INSERT`, no permission); help links; "Privacy receipt" button.

### 5.9 Privacy receipt (demo hero)
- Side by side (swipe/toggle on phone): **On your phone** (original) vs **Sent to cloud** (redacted, exact bytes sent).
- Counts by type: "3 names, 1 address, 1 NI number, 2 account numbers removed".
- "Kept on purpose": dates, amounts, organisations.
- **Engine per step** from `inferenceSource`: Read letter — on device; Find personal details — on device (Nano) / on device (rules); Quick check — on device / cloud; Explain — cloud.
- Firestore copy note: "Saved to cloud: redacted version only. Your details stay on this phone."

### 5.10 Home + history
- Home: Nano status chip (Ready / Downloading x% / Not supported → rules mode), "Scan a letter", "Try a sample letter", history list sorted by deadline with urgency badge.
- History item opens the stored result, restored on the fly from the local token map.

## 6. Firebase

- **App Check**: debug provider in debug builds, Play Integrity in release. Enforced for AI Logic.
- **Anonymous Auth**: gives each install a `uid` for Firestore rules.
- **Remote Config** keys (all with bundled defaults so the app works offline/first run):
  `cloud_model_name`, `redaction_patterns_json`, `prompt_redact`, `prompt_quickcheck`, `prompt_explain`, `nano_chunk_words`.
- **Firestore**: `users/{uid}/letters/{letterId}` = `{redactedText, quickCheck, explanation (tokenised), redactionMode, engines, createdAt}`. Security rules: only `request.auth.uid == uid`. Writes go through the same `assertSafeToSend` check. Token maps are **never** written.
- **Local**: Room DB `token_maps(letterId, json)` in app-private storage; excluded from backup and device transfer (`backup_rules.xml`, `data_extraction_rules.xml`).

## 7. App structure

Single `:app` module, Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModels + `StateFlow`, kotlinx.serialization, coroutines. Manual DI (one `AppContainer`) — no Hilt, less setup for 7 days.

```
dev.novanest.envelope
├─ data/        FirestoreRepo, RemoteConfigRepo, LocalStore (Room), SampleLetters
├─ ai/          Models (3 GenerativeModel instances), NanoStatus, prompts, JSON schemas
├─ privacy/     PatternRedactor, EntityRedactor, NanoRedactor, RedactionMerger,
│               LeakGuard (assertSafeToSend), Restorer, TokenMap
├─ ocr/         Scanner, TextReader
├─ pipeline/    LetterPipeline (orchestrates steps, records engine per step)
└─ ui/          home, review, result, receipt, settings, theme
```

`privacy/` is pure Kotlin with no Android deps where possible, so it is unit tested on the JVM (TDD). This is where correctness matters most.

## 8. Testing

- JVM unit tests: PatternRedactor (each pattern, false positives such as dates vs sort codes), RedactionMerger (overlaps, longest-first, numbering), LeakGuard, Restorer (mangled tokens, invented tokens), NHS mod-11, Luhn.
- Golden tests: each sample letter → expected redacted text.
- Manual on device: Nano path, no-Nano path (force rules mode via debug toggle), airplane mode (quick check still works on Nano phone; explain shows "needs internet").

## 9. Demo assets

5 bundled **fictional** sample letters (images + text), clearly fictional names/addresses, realistic structure: HMRC Self Assessment penalty, council tax arrears, NHS appointment/referral, bank account closure/fraud check, landlord Section 21-style notice. One contains scam signals for the red-flag demo.

Video arc (≤3 min): problem (15s) → scan real-looking letter (20s) → review with chips + quick check offline (30s) → explanation + draft reply in Polish/English (40s) → privacy receipt side by side with engine per step (40s) → no-Nano phone fallback (15s) → close (20s).

## 10. Settled by me (builder's calls)

- Fallback = rules + warning + manual tap-to-redact (not block).
- Dates and amounts kept, not redacted (explanation needs them).
- Document Scanner over CameraX (better UX, no permission).
- Manual DI over Hilt; Room for token maps; single module.
- Default cloud model `gemini-3.8-flash` (stable, no billing), overridable in Remote Config.
- Explanation-language setting added for impact; draft reply always English.
- Scam red-flags field added.
- Android only; multi-platform is out of scope for the deadline.
- Apache-2.0 license; public GitHub repo.

## 11. Needs the user (outside the code)

- Firebase project, `google-services.json`, AI Logic enabled with Gemini Developer API, App Check debug token registered.
- A Gemini Nano 4–capable phone for filming and testing.
- GitHub repo creation, YouTube upload, Devpost submission.

## 12. Schedule (7 days)

| Day | Work |
|---|---|
| 1 (23 Nov) | Git, Gradle deps, minSdk 26, Firebase wiring, App Check, Anonymous Auth, Remote Config defaults |
| 2 | `privacy/` pattern layer, merger, leak guard, restorer — TDD |
| 3 | OCR + Document Scanner, Entity Extraction, Nano redaction + status/download |
| 4 | Quick check, cloud explain, pipeline + engine tracking |
| 5 | UI: home, review (chips, tap-to-redact), result, calendar/share |
| 6 | Privacy receipt, Firestore history, sample letters, polish, no-Nano path |
| 7 (30 Nov) | README, LICENSE, video, submit by evening |
