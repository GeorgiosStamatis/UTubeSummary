# Remaining refactoring (after batch 7)

Done so far, for reference: all of section A below, the mock AI server, priority tags, pure functions with tests (title/author choice,
stale-result rules, History grouping, speech chunks, text decoding, share intents, author/overview rules),
architecture rules test, profile ids in the model layer, `HomeScreen` / `InstantSummaryDialog` split into
files, open History entry kept on rotation. Room schema export was tried and reverted (the two build
flavors wrote the same file at once).

Risk: **L** = pure move or test-only, **L–M** = constructor/Hilt change only CI can verify,
**M** = behaviour only a device can verify, **H** = live network or data behaviour.
"Manual cases" = how the 125 cases in `MANUAL_TESTS.md` are affected.

## A. Low-risk items: all done

| # | Item | Result |
|---|------|--------|
| 1 | `Clock` and `IdGenerator` seams | In `domain/provider/Clock.kt` with system defaults; used by the summarize use case, History merge and profile creation; 3 tests |
| 2 | `@IoDispatcher` seam | Summarize, ask and import use cases; slow and cancelled requests are tested in virtual time (2 tests) |
| 3 | Shared test fakes | `FakeHistoryRepository` is now a real in-memory store; six private copies deleted (−190 lines) |
| 4 | Called path of self-hosted servers | `openAiCompatibleEndpoint()` with 5 tests |
| 5 | BiliBili link helpers | `ensureHttpsScheme`, `protocolRelativeToHttps`, `bvidFromPathSegments` with 5 tests |
| 6 | Intent extra names | `EXTRA_AUTO_TRIGGER`, `EXTRA_HANDOFF_ID` in `IncomingShare.kt` |
| 7 | Width constants | `ContentMaxWidth`, `TwoPaneMaxWidth`, `DialogMaxWidth`, `OnboardingMaxWidth` in `WindowLayout.kt` |
| 8 | `SummaryActionButtons` moved out of `SummaryCard.kt` | `SummaryCard.kt` 732 → 379 lines |
| 9 | Unused imports | 4 removed |
| 10 | `AGENTS.md` | Lists the new files, shared fakes, injected seams and the architecture test |

## B. Need a device or more care: gain, risk and recommendation

Gain = what you get; risk = what can go wrong; verdict = **do** (worth it now), **later** (only if the
trigger in the last column happens), **drop** (cost is higher than the gain).

| # | Item | Gain | Risk | Effort | Verdict |
|---|------|------|------|--------|---------|
| 11 | Home input state (URL, picked file, camera uri) into a ViewModel with `SavedStateHandle` | Input rules testable (empty input, paste clears, file kept); `HomeScreen` about 150 lines shorter; removes 6 `rememberSaveable`; shortens ~4 manual cases (4.14–4.16, 14.1, 14.2) and removes 1 | **M–H**: this is the process-death fix of 6c; a mistake loses the picked file or the typed link, visible only on a device; the main screen is touched | M | **later**: only when Home is changed again anyway |
| 12 | One shared “summary session” for Home and the instant dialog | One copy of the profile/language/length → re-run and stale logic instead of two; shortens ~3 cases (13.3, 5.4, 6.5); needed for streaming or side-by-side comparison | **M–H**: two screens change together; instant dialog is exported and security-sensitive | L | **drop** unless a feature that needs it (comparison, streaming) is planned |
| 13 | `SpeechController` out of the text-to-speech code | `SummaryActionButtons` about 385 → 200 lines; engine behind an interface so pause/resume offsets can be tested; shortens 9.1 slightly | **M**: language fallback, listener threads and resume offsets only show on a device with a real engine | M | **drop** (small gain; do it only if a text-to-speech bug appears) |
| 14 | `SummaryHandoff` as an injected singleton instead of an `object` | Removes global mutable state, cleaner tests; no manual change | **L–M**: both activities need to be Hilt entry points (they probably already are); a wrong scope loses the hand-over between the two activities (13.4) | S | **later/optional**: cheap, but the gain is small because the object is already tested |
| 15 | Split `SettingsViewModel` (378 lines of one-line setters) | Smaller classes | **M**: `settingsUiState` is shared by Home, Settings and the instant dialog; a 650-line test file must be rewritten; no logic gain | M | **drop** |
| 16 | Room schema export done safely + migration test | JSON schema per version in the repo; an automated migration test (replaces the data part of case 1.2) and protection for the next migration; DB is at version 3 and will change again | **L for the app, M for the build**: the first try failed because both flavors write one file; the fix (per-variant folder or the Room Gradle plugin) cannot be verified without CI, so expect 1–3 CI runs | M | **later**: do it right before the next database change |
| 17 | Room `@Transaction` History save instead of the in-process mutex | A kill during a merge cannot leave a half-merged entry; the mutex already covers overlapping saves in the running app | **M**: Room 3 API for transactions is unverified; only an instrumented test would prove it | M | **drop** unless duplicate or half-merged entries are ever seen |
| 18 | Fake HTTP engine for the content tools and the connection checker | Removes ~5 and shortens ~3 manual cases (3.3–3.6, 4.4, 4.5, 4.7, 4.8): private/age-restricted/deleted videos, no subtitles, Cloudflare page, 401/404/timeouts become automatic | **L–M**: tools must receive their HTTP client (some already do); canned pages can drift from the real sites and give false confidence; a live check stays necessary | M–L | **do selectively**: article extractor and connection checker (stable formats); skip YouTube |
| 19 | Compose UI tests on an emulator + CI job | Biggest saving: ~8 cases removed and ~5 shortened (section 15, 11.4, 11.7, 11.8); catches layout regressions automatically | **none for the app; M for the project**: 10–15 min emulator job per run, flaky tests, Hilt test module with fake repositories, a lot of boilerplate | L (days) | **drop** unless you release often; otherwise the manual smoke run is cheaper |
| 20 | One list of supported file types for the picker and `fileKindOf` | One place to edit when a type is added | **L–M**: the picker list drives what users can select; a typo hides a type | S | **drop** (`SupportedFileTypesTest` already fails when the two lists drift) |

If you only want the best value for the effort: **18 (selective)**, **16 (just before the next DB change)** and
**14** (only if you want zero global state). Everything else in this section costs more than it returns.

## C. Behaviour changes, not refactoring

| Item | Risk | Note |
|------|------|------|
| Merge `isKnownAuthor` and `isUnknownAuthor` | M | Changes which authors are shown |
| Limit clear-text HTTP to local addresses | M–H | Breaks VPN / hostname setups; needs an app-level check |
| YouTube extraction without the hard-coded client version | M–H | Live behaviour, cannot be tested offline |
| PDF text layer first, API < 35 handling | M | Device-only |
| Highlight the active History row | L | Adds one manual check |

## D. Features (add manual cases, not refactoring)

Auto-suggest a profile (+3 cases), token/cost display (+3), real context sizes from providers (+4),
streaming output (+6), side-by-side comparison (+6), playlists (+8), “summarize later” queue (+6),
persisted transcript cache and chat history (+8; conflicts with the “nothing stored” privacy text).
