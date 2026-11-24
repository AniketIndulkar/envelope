# Envelope UI redesign brief: "The Line"

Port the agreed redesign into the existing Jetpack Compose app. The design source is the Design canvas
https://claude.ai/artifact/LFMrSJsw6snPhpDzw5u73j (page "Redesign"). Its file `project/Envelope.dc.html`
is the full interactive prototype: read it for exact layout, copy, spacing and animation timings.

## Rules
- Change UI only. Keep ViewModels, repositories, AI calls, redaction logic and navigation routes working as they are.
- Work in phases, building and running unit tests (`./gradlew testDebugUnitTest`) after each one. Commit nothing unless asked.
- Real data only: the wallet shows the user's saved letters from history, never the prototype's sample letters.
- Accessibility: touch targets at least 48dp, text contrast at least 4.5:1, TalkBack labels on icon buttons and stand-in chips,
  every animation shortened or skipped when the system animator scale is 0 or "Remove animations" is on.
- Keep the "not legal or financial advice" line and the existing help links.

## Concept
One horizontal glowing line is the edge of the phone. Above it is the cloud (Gemini), below it is the phone (Gemini Nano).
Personal details never cross; only stand-ins such as "Name 1" do. The line moves between stages of the flow.

## Tokens (put in ui/theme/Color.kt, Theme.kt, Type.kt)
| Role | Hex |
|---|---|
| cloud (sky) | #2B1A3A (strips #22132F, #362248) |
| line / accent | #FFB088 (glow = same at 35–90% alpha), highlight #FFE6D8, ring #FFC9AD |
| phone background | #FBF8F6, surface #FFFFFF, soft surface #F2ECEE |
| ink | #1E1524 (pressed #33263B), muted text #675C66 |
| hairlines | #E8E0E2, #EFE8EA, #DDD3D6, #C4B8BD |
| stand-in chip | #6B4A8E (pressed #573A76, on-cloud #7D5CA2, soft #EFE7F6) |
| your real details | #FFE8DA background with #FFB088 2dp underline |
| deadline label | #A2461A; urgent red #C8372D |
| risk card | #FBEAE8 background, #7A1A12 text |
| timeline rail | #F3D3C2 |
| scan camera | #140C1B |

Type: Instrument Sans (Google Fonts, downloadable font or bundled) for all UI. Big numbers 116sp Bold with letter spacing -0.05em;
titles Bold -0.025em. Source Serif 4 only for letter text. Radii: cards 24dp, chips full pill, buttons 14dp, timeline cards 16dp.

## Screens
1. **Wallet home (HomeScreen)**: header "Envelope" and a Nano status pill. Letters are passes: the front pass shows sender,
   days left (huge number that warms from white towards the accent colour as days run low), unit text, due date, a 3dp accent line,
   then next step, Open / Receipt buttons and an "N details kept on this phone" chip. The other letters sit as stacked strips
   above it (tap one to bring it to front; "Show all" fans them out with a spring). Receipt flips the pass (rotateY) to show what
   went to Gemini (stand-in chips) and what stayed (ledger with dotted leaders). Subtle 3D tilt on drag. A newly saved letter
   drops in with a spring and the others shuffle down. Bottom dock: "Scan a letter" plus "Sample".
2. **Scan**: dark camera background, accent corner brackets, a scan bar sweeping up and down, "Use this scan".
3. **Check (ReviewScreen)**: the line sits at 150dp, with the cloud strip above saying "Nothing has left your phone yet".
   A beam sweeps the letter; each personal detail scrambles (monospace, random glyphs) and resolves into its stand-in pill,
   and the big counter counts up as each one locks. Tap a pill to un-hide it. Without Nano: the line is dashed, with an amber
   banner and underlined words to tap and hide. Dock: "N stand-ins will go up" plus "Explain this letter".
4. **Explain**: the line slides down to about 520dp. Stand-ins rise up through it; at each crossing the line ripples and throws a
   spark and ring, and the cloud glow brightens. Below: "Your details stay here" with gold chips. Auto-advances when the answer is ready.
5. **Answer (ResultScreen)**: the line at 300dp. The cloud shows the sender, the days-left number counting down from 30 to the
   real value, the unit and the due date. Below the line: a "Put back on this phone only" row where stand-ins decode back into
   gold real details, the summary, Add to calendar, then **What to do** as a dated timeline: "Today", step cards (date label,
   title, note, an action button and a "Mark done" toggle; the deadline step gets an accent dot and outline), ending in a red
   diamond "If you do nothing" card. Then the reply-letter card (header with "Gold = added on this phone", serif letter,
   Copy / Share) and the help links. Dock: "Save to my letters".
6. **Privacy receipt (ReceiptScreen / history detail)**: same content as the pass back.

## Phases
1. Theme tokens and type, plus shared components in ui/components (Line with ripple and sparks, StandInChip, RealDetailChip,
   Pass, TimelineStep, Dock).
2. Answer screen timeline and reply card.
3. Check screen decode animation and no-Nano state.
4. Explain transition with the moving line.
5. Wallet home with passes, flip, fan and tilt.
6. Polish: reduced motion, TalkBack pass, frame timing check (`adb shell dumpsys gfxinfo dev.novanest.envelope`).
