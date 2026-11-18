# Remaining refactoring (after batch 7 phases A–C)

Done so far, for reference: mock AI server, priority tags, pure functions with tests (title/author choice,
stale-result rules, History grouping, speech chunks, text decoding, share intents, author/overview rules),
architecture rules test, profile ids in the model layer, `HomeScreen` / `InstantSummaryDialog` split into
files, open History entry kept on rotation. Room schema export was tried and reverted (the two build
flavors wrote the same file at once).

Risk: **L** = pure move or test-only, **L–M** = constructor/Hilt change only CI can verify,
**M** = behaviour only a device can verify, **H** = live network or data behaviour.
"Manual cases" = how the 125 cases in `MANUAL_TESTS.md` are affected.

## A. Low-risk and still open (recommended next)

| # | Item | Risk | Manual cases | Notes |
|---|------|------|--------------|-------|
| 1 | `Clock` and `IdGenerator` seams for `SummarizeContentUseCaseImpl`, `HistoryRepositoryImpl`, `UpdateSummaryProfilesUseCase` | L–M | none removed; makes time/id asserts possible | Hilt `@Binds`; ~5 test constructors change |
| 2 | `@IoDispatcher` seam for the summarize, ask and import use cases | L–M | none removed; cancellation tests become deterministic | Same CI-only risk as 1; do together |
| 3 | One shared set of test fakes (history, preferences, provider config) instead of seven private copies | L | none | Test-only; do after 1 and 2 |
| 4 | Pure `openAiCompatiblePath(url)` out of `LLMExecutorFactory` (bare host → `v1/chat/completions`, URL with path → `chat/completions`) + tests | L | shortens 3.6–3.8 | The rule is hidden in a private method today |
| 5 | Pure BiliBili helpers (`ensureScheme`, `prependHttps`, BVID from path segments) made `internal` + tests | L | shortens 4.5 slightly | Network part stays untested |
| 6 | Constants for the intent extras `"auto_trigger"` / `"handoff_id"` (used in both activities) | L | none | Prevents a typo breaking the hand-over |
| 7 | Content width constants (`840`, `1200`, `600`, `560` dp are repeated in 7 files) as named values in `WindowLayout.kt` | L | none | Pure rename |
| 8 | Move `SummaryActionButtons` (~300 lines, `SummaryCard.kt` is still 732 lines) to its own file | L | none | `private` → `internal` |
| 9 | Delete the 4 unused imports (`QuestionSection`, `HistoryItemRow`, `ProfileSelector`, `SummarizeContentUseCase`) | L | none | Trivial |
| 10 | Bring `AGENTS.md` / README architecture lists up to date with the new files and tests | L | none | No code |

Expected result of 1–10: about 25 more tests, deterministic use-case tests, no behaviour change.

## B. Need a device or more care (not recommended without a reason)

| # | Item | Risk | Manual cases | Why not now |
|---|------|------|--------------|-------------|
| 11 | Home input state (URL, picked file, camera uri) into a ViewModel with `SavedStateHandle` | M–H | removes 1, shortens ~4 (4.14–4.16, 14.1, 14.2) | Process-death behaviour of the main screen |
| 12 | One shared “summary session” for Home and the instant dialog | M–H | shortens ~3 (13.3, 5.4, 6.5) | Two screens, one logic copy |
| 13 | `SpeechController` out of `SummaryCard`'s text-to-speech code | M | shortens 9.1 | Engine/language fallback is device-only |
| 14 | `SummaryHandoff` as an injected singleton instead of an `object` | L–M | none | Only testability |
| 15 | Split `SettingsViewModel` (378 lines of one-line setters) | M | none | Touches every settings screen for no logic gain |
| 16 | Room schema export done safely (per-flavor folder or the Room Gradle plugin) + migration test | L–M | shortens 1.2 | Needs a build to verify; failed once |
| 17 | Room `@Transaction` History save instead of the in-process mutex | M | none (concurrency is unit-tested with fakes) | Only an instrumented test can prove it |
| 18 | Fake HTTP engine for the YouTube / BiliBili / article tools and the connection checker | L–M | removes ~5, shortens ~3 (3.3–3.6, 4.1–4.5, 4.7, 4.8) | Mid-size, no new library, a live check stays |
| 19 | Compose UI tests on an emulator + a CI job that runs them | none for the app; M for CI | removes ~8, shortens ~5 (section 15, 11.4, 11.7, 11.8) | Largest saving, largest effort and flakiness |
| 20 | One list of supported file types for the picker and `fileKindOf` | L–M | none | A test already guards against drift |

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
