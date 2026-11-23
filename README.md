# Envelope

**Understand any official letter, without handing over who you are.**

Photograph a confusing letter from HMRC, the council, the NHS, a bank or a landlord. Envelope tells you what it means, what to do and by when, warns you if it looks like a scam, and drafts a reply. Gemini Nano strips your personal details **on the phone** before the cloud model ever sees the letter; your details are put back into the answer on the phone. A privacy receipt shows exactly what left the phone.

Built for **#BuildHybridAI 2026** with the Firebase AI Logic hybrid inference SDK.

## Why

The people who struggle most with official letters (older people, people reading English as a second language, people under financial stress) are the same people who should not paste a letter full of their National Insurance number, account numbers and address into a chatbot. Missed deadlines cost real money: a late Self Assessment return is a £100 penalty, then £10 a day.

## How hybrid inference is used

Each step uses the inference mode its privacy needs. The hybrid choice *is* the product.

| Step | Where | Mode | Why |
|---|---|---|---|
| Read the letter | Phone | ML Kit Text Recognition v2 | Works offline |
| Find personal details | Phone | Gemini Nano, `ONLY_ON_DEVICE` + rules + ML Kit Entity Extraction | Raw letter text must never reach the cloud; throws instead of falling back |
| Quick check (sender, urgency, deadline) | Nano if present, else cloud | `PREFER_ON_DEVICE` | Input is already redacted, so a cloud fallback is safe; works offline on Nano phones |
| Explain, next steps, draft reply | Cloud | `ONLY_IN_CLOUD` | Deep reasoning on the redacted text only |
| Put your details back | Phone | Plain code | Real values never leave the phone |

The receipt reads `inferenceSource` from each response to show which engine handled which step.

**No Nano on the phone?** Redaction falls back to on-device rules (never to the cloud). The app says so plainly, highlights words that look like names and addresses, and lets you tap any word to hide it.

## Privacy guarantees and where they are enforced

- **One choke point.** Every prompt that may leave the phone and every Firestore write passes `LeakGuard.assertSafeToSend` (`app/src/main/java/dev/novanest/envelope/privacy/LeakGuard.kt`). It re-runs the UK identifier rules and checks no real value survived. If anything leaks, nothing is sent.
- **Nano returns spans, not text.** Nano lists the personal details it found (keeping within the ~256-token on-device output budget); deterministic code replaces them with tokens like `[NAME_1]`. Spans Nano invents are dropped. A reply that did not come from the device is rejected.
- **Token maps stay local.** The `[NAME_1] → real name` map is stored in Room only, excluded from backup and device transfer.
- **Kept on purpose:** dates, money amounts and the sender's name. The explanation needs them, and they identify the letter, not you.
- **Rules are data.** UK patterns (NI number, sort code, account number, IBAN, card with Luhn, postcode, phone, email, UTR, NHS number with mod-11, date of birth, labelled references) ship in `assets/redaction_patterns.json` and can be updated with Remote Config. A broken remote rule set falls back to the bundled rules.

## Firebase

- **AI Logic** hybrid SDK (`firebase-ai` 18.2.0, `firebase-ai-ondevice` 16.0.0-beta08)
- **App Check** (debug provider in debug builds, Play Integrity in release)
- **Remote Config**: cloud model name, redaction rules, prompts, Nano chunk size
- **Firestore**: history of redacted letters only, `users/{uid}/letters/{id}`, rules in `firestore.rules`
- **Anonymous Auth**: scopes each install to its own history

## Setup

1. Create a Firebase project and add an Android app with package `dev.novanest.envelope`.
2. Download `google-services.json` into `app/`.
3. In the console, enable **Firebase AI Logic** with the Gemini Developer API, **Anonymous** sign-in, **Firestore**, and **Remote Config**.
4. Deploy the Firestore rules: `firebase deploy --only firestore:rules`.
5. Run a debug build, copy the App Check debug token from logcat (`DebugAppCheckProvider`), and register it under App Check › Apps › Manage debug tokens.
6. For on-device redaction, use a phone that supports Gemini Nano through ML Kit GenAI / AICore. Settings › Gemini Nano downloads the model if needed.

Without `google-services.json` the app still builds and runs: scanning and on-device redaction work; explaining is turned off.

Try it without a real letter: **Try a sample letter** offers five fictional letters (tax penalty, council tax, hospital appointment, a bank scam, rent increase).

## Build and test

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug
```

Unit tests cover the privacy core (patterns, merging, leak guard, restoring), the pipeline with fake models (proving only redacted text is sent), the data layer, and golden tests over every sample letter.

## Project layout

```
app/src/main/java/dev/novanest/envelope/
  privacy/   pattern rules, Nano + ML Kit finders, merger, LeakGuard, Restorer
  ai/        hybrid models, prompts, JSON contracts, Nano status
  pipeline/  read → redact → quick check → explain → restore
  ocr/       ML Kit Document Scanner and Text Recognition
  data/      Firestore history, Room token maps, Remote Config, samples
  ui/        Compose screens
```

## Not advice

Envelope explains letters in plain English. It is not legal or financial advice.

## License

Apache-2.0. See [LICENSE](LICENSE).
