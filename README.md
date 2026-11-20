<div style="text-align: center;">
<img src=".github/logo.png"  width=160 height=160 alt="logo">

# UTubeSummary

![GitHub License](https://img.shields.io/github/license/GeorgiosStamatis/UTubeSummary)
[![GitHub Release](https://img.shields.io/github/v/release/GeorgiosStamatis/UTubeSummary)](https://github.com/GeorgiosStamatis/UTubeSummary/releases)

[![ChatGPT](https://custom-icon-badges.demolab.com/badge/ChatGPT-74aa9c?logo=openai&logoColor=white)](https://platform.openai.com)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-886FBF?logo=googlegemini&logoColor=fff)](https://aistudio.google.com)
[![Claude](https://img.shields.io/badge/Claude-D97757?logo=claude&logoColor=fff)](https://platform.claude.com/)
[![Ollama](https://img.shields.io/badge/Ollama-fff?logo=ollama&logoColor=000)](https://ollama.com/)
[![LM Studio](https://img.shields.io/badge/LM%20Studio-5B4FE9)](https://lmstudio.ai/)
[![OpenRouter](https://img.shields.io/badge/OpenRouter-94A3B8?logo=openrouter&logoColor=fff)](https://openrouter.ai)
[![Mistral AI](https://img.shields.io/badge/Mistral%20AI-FA520F?logo=mistral-ai&logoColor=fff)](https://mistral.ai/pricing/api/)
[![Deepseek](https://custom-icon-badges.demolab.com/badge/Deepseek-4D6BFF?logo=deepseek&logoColor=fff)](https://platform.deepseek.com)
[![Kimi](https://img.shields.io/badge/Kimi-002F5B?logo=kimi)](https://platform.kimi.ai)
[![MiniMax](https://img.shields.io/badge/Minimax-E73562?logo=minimax&color=ffffff)](https://platform.minimax.io)
[![Zhipu](https://img.shields.io/badge/Zhipu-2D2D2D?logo=z.ai)](https://z.ai/model-api)
[![Qwen](https://img.shields.io/badge/Qwen-605CEC?logo=qwen&logoColor=fff)](https://qwen.ai/apiplatform)

</div>

> 🔒 **Personal-use project: never on the Play Store, never monetized.** UTubeSummary is built and installed
> from this repository for personal use only. It will **not** be published on the Google Play Store or any
> other app store, and it is **not** a commercial or monetizable product: no paid version, no in-app
> purchases, no ads, no donations. Reason: reading YouTube and BiliBili subtitles uses the services'
> unofficial interfaces, which their terms of service do not allow for published or commercial apps.

> **UTubeSummary is a fork of [SummaryExpressive](https://github.com/kid1412621/SummaryExpressive)**
> by [kid1412621](https://github.com/kid1412621) and contributors, licensed under the
> [GNU GPL v3](LICENSE.txt). Full credit for the original app goes to them. See
> [What's different in this fork](#-whats-different-in-this-fork) for the changes made here.

A modern, BYOK and FOSS android app to summarize videos(YouTube, BiliBili), articles, images and
documents with AI/LLM.

[MAD](https://developer.android.com/courses/pathways/android-architecture): pure Kotlin + Jetpack Compose + M3 Expressive

## 📱 Screenshots

> The screenshots below are from the original app and will be replaced with ones of this fork.

<div style="text-align: center;">
  <img src=".github/screenshots/home.png" alt="Home" width="24%">
  <img src=".github/screenshots/instant.png" alt="Instant Summary" width="24%">
  <img src=".github/screenshots/history.png" alt="History" width="24%">
  <img src=".github/screenshots/dark.png" alt="Dark Mode" width="24%">
</div>

## 🔗 Downloads

> ℹ️ UTubeSummary is distributed only as APK files from this repository, for personal use; there is no
> Play Store listing and there never will be. Get the latest build from the **Actions** tab: pick the
> latest **Build** run and download the `UTubeSummary-debug-apks` artifact (debug APKs, GitHub login
> required), or a tagged release below if one exists.

[![Get it on Github](https://images.weserv.nl/?url=https://s1.ax1x.com/2023/01/12/pSu1a36.png&h=80)](https://github.com/GeorgiosStamatis/UTubeSummary/releases)

Minimal Android version: **13**

### GitHub Release

There are 2 releases available:

- **standalone** which
  includes [ML model](https://developers.google.com/ml-kit/vision/text-recognition/v2) to recognize
  text from image, it got larger package size.

- **gms** which allows user to download the model from Google Play Services, but GMS required.

### Installing test builds

Each CI build has its own increasing version number (shown in Settings, e.g.
`2.2.0-ci35+0f146ab`). Install the new APK **over** the previous one, always the same flavor
(`gms` or `standalone`): it updates in place and keeps your AI provider, API key and history. You
only need to uninstall first if Android says "App not installed", which happens for builds older
than #17 (signed with a different key).

## 📖 Features

A step-by-step guide for users is in [docs/user_guide.md](docs/user_guide.md).

- **Summarize multiple media types**

| media        | supported types                     |
|--------------|-------------------------------------|
| Video (Link) | YouTube, BiliBili                   |
| Document     | MS Word, PDF, plain text, Markdown  |
| Image        | Jpg, Png, Webp (Latin only for now) |
| Text         | Article link, Plain text            |

- Multiple LLM providers supported

  - OpenAI ChatGPT
  - Google Gemini
  - Anthropic Claude
  - DeepSeek
  - Kimi
  - MiniMax
  - Zhipu
  - Alibaba Qwen
  - Mistral
  - OpenRouter
  - Ollama
  - LM Studio
  - Custom provider (any OpenAI-compatible endpoint: URL + optional token)

- **Ask questions about a summary**: right after summarizing a video, article or document, ask
  follow-up questions. The extracted text (e.g. the transcript) stays in memory only and is sent with
  your question to your LLM provider; nothing extra is stored on the device

- **Import video summaries from another app**: Settings > Import video summaries reads a JSON backup
  and adds the videos to your History, skipping those already there

- **Video thumbnails in History**: small, cached YouTube thumbnails (optional, can be switched off)

- **Custom Model Management**: Add, edit, delete, and drag-and-drop reorder models per provider with fallback to Koog defaults

- **Quick Model Switcher**: Easily switch providers and active models on the Home screen following your custom configured order

- **[Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive) UI**: Engaging
  and easier to use, light/dark theme and dynamic color theme

- **Instant summarize via share sheet or text selection toolbar**: Convenient to trigger summarization, show result in overlay

- **Summarization profiles**: choose *how* a summary is written, right next to the Short / Middle /
  Long length selector on Home. Each profile is its own base prompt: **Default**, **Technical Deep
  Dive**, **Critical Analysis**, **Learning Mode**, **Technical Content Quality Evaluator** and
  **Quick Brief**. Tap a profile to use it, edit its prompt (in Summarization profiles or in the Base
  Prompt box of Advanced Summary Setup), reset a preset, or create and delete your own profiles;
  "Append Custom Prompt" is applied on top of any profile. Default keeps the overview / key points /
  tags card; the other profiles answer in the format defined by their prompt (Markdown is rendered),
  unless a profile's "Overview, key points and tags" switch is on. Share a profile as JSON text and
  import one from the clipboard.

- **Summary language**: choose the language of the summary on Home or in Advanced Summary Setup:
  the video's own language, the app language or one of about 20 other languages. If the content is
  too long for the model's context window, the app says so and suggests a model with a larger one
  (nothing is cut)

- **Tablets and landscape**: on wide screens (tablet, or a landscape window at least 600 dp wide)
  History shows the list on the left and the selected entry on the right, Home puts the input and
  options beside the result, and the instant summary dialog uses up to 90% of the height. Phones in
  portrait are unchanged
- **Files in and out**: summarize `.txt`, `.md` and Word files (tables, tabs and line breaks are
  kept); save any summary as a Markdown file; export your YouTube video summaries from Settings in the
  format the importer reads

- **Share and re-run**: share a summary as Markdown (title, source link, key points, tags), see
  whether the app is fetching content or summarizing, cancel a running summary, and summarize a
  History item again with another profile, language or length. Summarizing the same input again reuses the text that was already downloaded or read (kept in memory only, last three inputs), so a second profile does not download the transcript again (use **Update** to refresh it). The instant summary dialog has the
  same profile and language pickers

- **Configurable LLM settings**: Custom API keys, base URLs, prompt customization, and summary lengths

- **Text to speech for the summaries with multilingual and speed adjustment support**; it reads the
  text without Markdown symbols and handles long summaries

- **History**: search; pin items to the top; filter by pinned, profile or type; sort by newest,
  oldest or title; long-press to select several entries for bulk delete or pin (with Undo).
  Summarizing the same item again (other profile, language or length) keeps every summary in one
  entry, oldest first, with all but the newest collapsed, and a single summary can be removed. **Update** (videos and web pages) fetches the source again and
  makes every summary of the entry again from the new content; if the content did not change, nothing is
  re-summarized

- **Private by design**: no analytics or tracking; your API keys and BiliBili login are encrypted on
  your device. Content is only sent to the LLM provider you configure. Two optional third-party
  requests exist: if a site blocks direct access, the article URL goes to the
  [Jina Reader](https://jina.ai/reader/) service, and History thumbnails come from YouTube's image
  server (can be switched off). See [PRIVACY.md](PRIVACY.md).

## 🍴 What's different in this fork

**New features**

- **Summarization profiles**: pick a base prompt per summary (Default, Technical Deep Dive, Critical
  Analysis, Learning Mode, Technical Content Quality Evaluator, Quick Brief) or create your own. Only
  Default uses the structured overview / key points / tags card (unless a profile switches it on);
  the others answer in the format of their prompt. Profiles can be shared as JSON text
- **History management**: remove a single summary of an entry, select several entries for bulk
  delete or pin, and sort by newest, oldest or title
- **More robust YouTube extraction**: if YouTube refuses the app's normal request, the app tries the
  Android client without the API key and then the iOS client; failures say "YouTube may have changed
  something" with the client and step under *Show details* (instead of "video unavailable"); videos with
  automatic dubbing (many auto-generated subtitle tracks) use the manual or original-language subtitles, not
  the first auto-generated one (this caused summaries in an unrelated language)
- **Files in and out**: `.txt`, `.md` and Word files can be summarized (tables, tabs and line breaks
  are kept), a summary can be saved as a Markdown file, and the YouTube video summaries of the
  History can be exported from Settings in the format the importer reads
- **History keeps every summary of an item**: summarizing again with another profile, language or
  length appends a section to the same entry instead of replacing the old one; pin items and filter
  by pinned or profile
- **Summary language**: Video language, App language or one of about 20 languages, chosen on Home
  (under the profile picker), in Advanced Summary Setup and in the instant summary dialog; the old
  "Original Language" switch is used until you pick one
- **Cancel, progress and re-run**: a Cancel button while summarizing, a "Fetching content…" /
  "Summarizing…" status, and "Summarize again with other settings" in the History detail sheet
- **Share as Markdown**: title, author, source link, overview, key points, summary and tags
- **Instant summary dialog**: profile and language pickers; changing one summarizes the text again
- **Ask questions about a summary**: follow-up questions about the video, article or document you just
  summarized, with earlier answers as context. The text is kept in memory only (last 3 inputs)
- **Import video summaries**: import another app's JSON backup from Settings; videos already in the
  History are skipped (same URL, and for YouTube the same video in any link form), original dates
  are kept
- **History thumbnails**: small (about 10 KB) YouTube thumbnails derived from the video id, so older
  and imported items get them too; capped 50 MB cache, falls back to the icon offline; switch in
  Settings > Display
- **LM Studio** and **Custom Provider** (any OpenAI-compatible server) with a URL and an optional token
- **AI provider dialog**: built-in providers show their official URL (editable), a **Test** button
  checks the URL and API key, the model list is **filled in automatically** from the provider, and
  pasted URLs such as `https://openrouter.ai/api/v1` are normalized
- Local and LAN addresses typed without a scheme (e.g. `192.168.1.10:1234`) default to `http://`
- **New app icon and logo**: a play button above three lines of text, as adaptive and themed
  (monochrome) Android icons
- Error messages have a **Show details** option (provider, model, URL and the raw cause) to make
  problems easy to report
- **Clearer errors**: content that does not fit the model's context window asks you to choose a
  model with a larger one; private, age-restricted or removed YouTube videos are named as such;
  BiliBili access problems ask for a login or report an invalid link instead of "no internet"

**Fixes**

- PDF text extraction crashed on Android 13/14 for scanned pages
- Providers without an API key (e.g. Ollama) failed with "you have to provide an API key"
- OpenRouter requests failed with "Not Found" when the URL contained `/api/v1`; a bare "Not Found"
  now shows a helpful hint
- Custom prompt fields lost typed text and the cursor got stuck
- The app closed after the first-run tutorial when the saved settings could not be read: settings
  fields now have fixed numbers and an unreadable settings file resets to defaults instead of crashing
- Only one instance of the app runs (sharing a link reuses it instead of opening another)
- A second summarize request, or clearing the input, no longer lets an older result appear later;
  cancelling is no longer reported as an error
- History: overlapping saves cannot lose a result, Undo restores an entry exactly as it was (same
  date, no merging), `%` and `_` in a search match literally, and the profile used is shown
- Long summaries no longer travel through intent extras (about 1 MB limit) when opening the main
  app from the instant dialog
- Error classification: numbers and words in ordinary text (for example "500" or "quota") are no
  longer mistaken for HTTP or rate-limit errors
- YouTube: a missing author no longer fails extraction, and a manual caption track in the spoken
  language is preferred
- Text to speech read Markdown symbols aloud and could fail on long summaries
- PDF and image text recognition now release their bitmaps and recognizer
- Provider and summarizer caches are real bounded LRU caches and are keyed by a hash of the API key
- The question box scrolls above the keyboard, and the reader fallback for blocked articles is decided
  by the error type, not by words in its message
- Accessibility: content descriptions are localized and purely decorative icons are no longer announced
- The new texts (profiles, language, progress, errors, pinning, sharing) are translated into the existing languages
  (machine translations: corrections are welcome); German, Spanish, French, Italian and Portuguese
  now also cover the older Settings, onboarding, Ask and error texts that used to fall back to English
- A picked file or camera photo is kept when Android ends the app's process, and a file that can no
  longer be opened gets a clear "pick it again" message
- The "fetching / summarizing" status can no longer be cleared by an older request finishing
- YouTube videos with automatic dubbing (many auto-generated subtitle tracks) could be summarized from
  an auto-generated track in an unrelated language (for example Arabic for an English video)
- Text files with a byte order mark or Windows line endings are read cleanly, and a text-to-speech chunk
  can no longer exceed the speech engine's limit by one character
- The Settings list no longer stretches across a tablet
- A link shared into the app while History or Settings is open now opens the Home screen (where profile and
  language can be chosen) instead of staying behind the open screen

**Security & privacy**

- The BiliBili login cookie is now stored encrypted (an old plain-text one is migrated)
- API keys: a key that can no longer be decrypted gives a clear message instead of looking like "no
  key", and a failed encryption is reported instead of silently saving an empty key
- Exported entry points no longer read the clipboard or auto-summarize it on behalf of other apps
- The "Summarize from clipboard" shortcut fills the input field only; nothing is sent until you tap
  summarize
- The `utubesummary://clipboard` deep link is no longer browsable, so web pages and links cannot
  trigger it
- Removed the undocumented `INSTANT_SUMMARIZE` intent action; share-sheet and text-selection
  summaries are unchanged
- The privacy policy now also covers video thumbnails, the data that stays on the device and the
  sharing, saving and export features that only run when you start them
- Privacy policy rewritten to match what the app actually does (no analytics, third-party requests
  listed); the contact route is now
  [GitHub issues](https://github.com/GeorgiosStamatis/UTubeSummary/issues)

**Project**

- Version 2.2.0 (since 2.1.0: tablet and landscape layouts, History management, files in and out, more
  robust YouTube extraction, History **Update**, reuse of the downloaded text for the same input; the
  database migration 2 to 3 keeps the existing History)
- Renamed to UTubeSummary with a new app id (`io.github.utubesummary`): it installs next to the
  original app and does not share its data
- Storage identifiers renamed: Room database `utubesummary_db` and Keystore alias
  `utubesummary_api_key_alias`. Existing history and API keys are not carried over
- In-app "GitHub" links open this fork, and the upstream author's footer credit was removed
- Google Play badges, the in-app "Rate us on Google Play" item and the Play upload step of the release
  workflow removed, because this fork is for personal use and will never be on the Play Store
- The in-app Discord link (the upstream project's community) removed
- CI builds installable debug APKs on every push: each has its own increasing version number (shown
  in Settings), all share one debug signing key so they update in place, and the unit test suite
  covers the new features; `ArchitectureRulesTest` fails when a layer imports one it must not know
- Manual test cases are in [docs/MANUAL_TESTS.md](docs/MANUAL_TESTS.md); remaining refactoring ideas
  are in [docs/REFACTORING_BACKLOG.md](docs/REFACTORING_BACKLOG.md)
- README and project pages use the new logo (`.github/social-preview.png` is ready for the
  repository's social preview)

## 🏗️ Architecture

The app follows Google's [official Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations) and [Modern Android Development (MAD)](https://developer.android.com/courses/pathways/android-architecture) best practices:

- **Clean Layered Architecture**:
  - **UI Layer (`ui/`, `vm/`)**: Built with Jetpack Compose and Material 3 Expressive, powered by ViewModels exposing reactive `StateFlow` and following Unidirectional Data Flow (UDF).
  - **Domain Layer (`domain/`, `model/`, `exception/`)**: Pure Kotlin business operations (`domain/usecase/`), data contracts (`domain/repository/`), system providers (`domain/provider/`), pure domain models (`model/`), and centralized exceptions (`exception/`).
  - **Data Layer (`data/`)**: Repository implementations acting as Single Source of Truth (SSOT), encapsulating Room database with dedicated entities and mappers, ProtoBuf DataStore, and Ktor network client.
- **Summarization profiles**: built-in prompts live in `llm/BuiltInProfiles.kt`; the domain layer
  (`resolveProfiles()`, `UpdateSummaryProfilesUseCase`) merges them with the user's edits and custom
  profiles stored in the ProtoBuf DataStore, and the chosen profile's base prompt is passed to the
  LLM handler for each summary.
- **Summary language, progress and handoff**: the language setting (`model/SummaryLanguage.kt`) is
  resolved in `SummarizeContentUseCase` and named in the prompt; `SummarizationProgress` exposes the
  current phase through `ObserveSummarizationPhaseUseCase` (each request has an id, so an older one
  finishing cannot clear a newer one's phase); `SummaryHandoff` passes a finished summary from the
  instant dialog to the main activity in memory.
- **LLM gateway**: the summarize and ask use cases depend on the `LlmGateway` interface (implemented by
  `LLMHandler`), so they are unit-tested with a fake gateway and in-memory repositories.
- **History model**: an entry keeps the current result per length plus `alternatives` (results of other
  profiles or languages), a `pinned` flag and `variantKeys` for the profile filter; the Room database
  is at version 3 with a migration from version 2.
- **Injected seams**: time, ids and the IO dispatcher are injected into the use cases (`Clock`,
  `IdGenerator` and `@IoDispatcher` in `domain/provider/`), so they are tested with fixed values and in
  virtual time; the tests share in-memory fakes in `testutil/`.
- **History Update**: `UpdateHistoryEntryUseCase` fetches the source again (`LlmGateway.extractContent`), plans
  with the pure `planUpdate` which summaries are outdated (each result stores a `sourceFingerprint`, a hash of the
  text it was made from) and makes them again through `SummarizeContentUseCase`, which reuses the text kept in
  `SourceTextStore`.
- **YouTube extraction**: the client identities (`ANDROID_LEGACY`, `ANDROID_FULL`, `IOS`) and their order
  are in `llm/tools/YouTubeClient.kt`; the pure policy `runAttempts` (`YouTubeFallback.kt`) uses the first
  that works and tells a restricted video, a video without subtitles and a failed extraction apart.
  The caption track is chosen by language rather than by position (`selectCaptionTrack`).
- **Dependency Injection**: Powered by Dagger Hilt with `@Binds` for domain repository contracts.
- **Asynchronous Operations**: Kotlin Coroutines + Flow for reactive, main-safe async streams.

## 🌟 Credits

- [SummaryExpressive](https://github.com/kid1412621/SummaryExpressive) by
  [kid1412621](https://github.com/kid1412621): the app this fork is based on

- The [original idea](https://github.com/talosross/SummaryYou)
  from [talosross](https://github.com/talosross)

- [Koog](https://koog.ai) for kotlin-based LLM interactions

## 🤝 Contributing & License

Bug reports and ideas are welcome in the
[issues](https://github.com/GeorgiosStamatis/UTubeSummary/issues).

Licensed under the [GNU General Public License v3.0](LICENSE.txt).
