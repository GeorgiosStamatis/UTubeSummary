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

> **UTubeSummary is a fork of [SummaryExpressive](https://github.com/kid1412621/SummaryExpressive)**
> by [kid1412621](https://github.com/kid1412621) and contributors, licensed under the
> [GNU GPL v3](LICENSE.txt). Full credit for the original app goes to them. See
> [What's different in this fork](#-whats-different-in-this-fork) for the changes made here.

A modern, BYOK and FOSS android app to summarize videos(YouTube, BiliBili), articles, images and
documents with AI/LLM.

[MAD](https://developer.android.com/courses/pathways/android-architecture): pure Kotlin + Jetpack Compose + M3 Expressive

## 📱 Screenshots

<div style="text-align: center;">
  <img src=".github/screenshots/home.png" alt="Home" width="24%">
  <img src=".github/screenshots/instant.png" alt="Instant Summary" width="24%">
  <img src=".github/screenshots/history.png" alt="History" width="24%">
  <img src=".github/screenshots/dark.png" alt="Dark Mode" width="24%">
</div>

## 🔗 Downloads

> ⚠️ **Coming soon for this fork.** UTubeSummary has no published release and no Play Store listing
> yet. The sections below describe how releases are packaged. Until then you can try the latest
> build: open the **Actions** tab, pick the latest **Build** run and download the
> `UTubeSummary-debug-apks` artifact (debug APKs, GitHub login required).

[![Get it on Github](https://images.weserv.nl/?url=https://s1.ax1x.com/2023/01/12/pSu1a36.png&h=80)](https://github.com/GeorgiosStamatis/UTubeSummary/releases)

Minimal Android version: **13**

### GitHub Release

There are 2 releases available:

- **standalone** which
  includes [ML model](https://developers.google.com/ml-kit/vision/text-recognition/v2) to recognize
  text from image, it got larger package size.

- **gms** which allows user to download the model from Google Play Services, but GMS required.

### Play Store

The Play Store release bundled with gms version. This package is signed by Google managed key for
simplicity, which means it's not compatible to update in Google Play Store if you installed a GitHub
package previously.

### Installing test builds

Each CI build has its own increasing version number (shown in Settings, e.g.
`2.0.0-ci19+abc1234 - 119`). Install the new APK **over** the previous one, always the same flavor
(`gms` or `standalone`): it updates in place and keeps your AI provider, API key and history. You
only need to uninstall first if Android says "App not installed", which happens for builds older
than #17 (signed with a different key).

## 📖 Features

- **Summarize multiple media types**

| media        | supported types                     |
|--------------|-------------------------------------|
| Video (Link) | YouTube, BiliBili                   |
| Document     | MS Word, PDF                        |
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

- **Custom Model Management**: Add, edit, delete, and drag-and-drop reorder models per provider with fallback to Koog defaults

- **Quick Model Switcher**: Easily switch providers and active models on the Home screen following your custom configured order

- **[Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive) UI**: Engaging
  and easier to use, light/dark theme and dynamic color theme

- **Instant summarize via share sheet or text selection toolbar**: Convenient to trigger summarization, show result in overlay

- **Summarization profiles**: choose *how* a summary is written, right next to the Short / Middle /
  Long length selector on Home. Each profile is its own base prompt: **Default**, **Technical Deep
  Dive**, **Critical Analysis**, **Learning Mode**, **Technical Content Quality Evaluator** and
  **Quick Brief**. Edit or reset any preset, or create and delete your own profiles; "Append Custom
  Prompt" is applied on top of any profile.

- **Ask questions about a summary**: follow up on a summarized video or document. The extracted text
  is only kept in memory (never saved) and is sent to your configured LLM together with your question.

- **Import video summaries** from another app's JSON backup (Settings), skipping videos that are
  already in your history.

- **Configurable LLM settings**: Custom API keys, base URLs, prompt customization, and summary lengths

- **Text to speech for the summaries with multilingual and speed adjustment support**

- **History search**, with small, cached video thumbnails (can be switched off in Settings)

- **Private by design**: no analytics or tracking; your keys stay encrypted on your device. Content is
  only sent to the LLM provider you configure (and, if a site blocks direct access, the article URL
  goes to the [Jina Reader](https://jina.ai/reader/) service). See [PRIVACY.md](PRIVACY.md).

## 🍴 What's different in this fork

**New**

- **Summarization profiles**: pick a base prompt per summary (Default, Technical Deep Dive, Critical
  Analysis, Learning Mode, Technical Content Quality Evaluator, Quick Brief) or create your own
- **Ask questions** about a summarized video or document, answered from the in-memory transcript
- **Import video summaries** from another app's JSON backup
- **Video thumbnails in History**: small, cached on disk, and optional
- **LM Studio** and **Custom Provider** (any OpenAI-compatible server) with a URL and an optional token
- **Test** button in the AI provider dialog: checks that the URL is reachable and the API key is valid
- Model list is **filled in automatically** from the provider when you press Test or OK
- The official URL is **prefilled** for built-in providers (still editable)
- Local and LAN addresses typed without a scheme (e.g. `192.168.1.10:1234`) default to `http://`
- Error banners have a **Show details** option (provider, model, URL and the raw cause)

**Fixes**

- Crash after onboarding on updated installs: settings fields were renumbered by mistake, so older
  settings files were read wrongly. Field numbers are now fixed and an unreadable settings file
  resets to defaults instead of crashing
- PDF text extraction crashed on Android 13/14 for scanned pages
- Providers without an API key (e.g. Ollama) failed with "you have to provide an API key"
- OpenRouter **Test** returned 404, and pasting a URL that already ended in `/api/v1` produced a
  doubled version path (the same is handled for OpenAI, Anthropic and Gemini)
- OK/Test buttons in the provider dialog now behave correctly after a successful Test
- Custom prompt fields lost typed text and the cursor got stuck

**Security & privacy**

- Exported entry points no longer read the clipboard or auto-summarize it on behalf of other apps
- The BiliBili cookie is now stored encrypted, like API keys; an unreadable key or cookie gives a
  clear message instead of a crash
- Shares reuse the running app instead of opening duplicate instances
- Privacy policy rewritten to match what the app actually does

**Project**

- Renamed to UTubeSummary with a new app id (`io.github.utubesummary`) and a new app icon: it
  installs next to the original app and does not share its data
- CI uploads installable debug APKs on every build; each one has an increasing version number and
  the same signing key, so it updates in place (see [Installing test builds](#installing-test-builds))

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
