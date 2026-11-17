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

> The screenshots below are from the original app and will be replaced with ones of this fork.

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
`2.0.0-ci35+0f146ab`). Install the new APK **over** the previous one, always the same flavor
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

- **Configurable LLM settings**: Custom API keys, base URLs, prompt customization, and summary lengths

- **Text to speech for the summaries with multilingual and speed adjustment support**

- **History search**

- **Private by design**: no analytics or tracking; your API keys and BiliBili login are encrypted on
  your device. Content is only sent to the LLM provider you configure. Two optional third-party
  requests exist: if a site blocks direct access, the article URL goes to the
  [Jina Reader](https://jina.ai/reader/) service, and History thumbnails come from YouTube's image
  server (can be switched off). See [PRIVACY.md](PRIVACY.md).

## 🍴 What's different in this fork

**New features**

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

**Fixes**

- PDF text extraction crashed on Android 13/14 for scanned pages
- Providers without an API key (e.g. Ollama) failed with "you have to provide an API key"
- OpenRouter requests failed with "Not Found" when the URL contained `/api/v1`; a bare "Not Found"
  now shows a helpful hint
- Custom prompt fields lost typed text and the cursor got stuck
- The app closed after the first-run tutorial when the saved settings could not be read: settings
  fields now have fixed numbers and an unreadable settings file resets to defaults instead of crashing
- Only one instance of the app runs (sharing a link reuses it instead of opening another)

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
- Privacy policy rewritten to match what the app actually does (no analytics, third-party requests
  listed); the contact route is now
  [GitHub issues](https://github.com/GeorgiosStamatis/UTubeSummary/issues)

**Project**

- Renamed to UTubeSummary with a new app id (`io.github.utubesummary`): it installs next to the
  original app and does not share its data
- Storage identifiers renamed: Room database `utubesummary_db` and Keystore alias
  `utubesummary_api_key_alias`. Existing history and API keys are not carried over
- In-app "GitHub" links open this fork, and the upstream author's footer credit was removed
- Google Play badges removed, because this fork has no Play Store listing
- CI builds installable debug APKs on every push: each has its own increasing version number (shown
  in Settings), all share one debug signing key so they update in place, and the unit test suite
  covers the new features
- README and project pages use the new logo (`.github/social-preview.png` is ready for the
  repository's social preview)

## 🏗️ Architecture

The app follows Google's [official Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations) and [Modern Android Development (MAD)](https://developer.android.com/courses/pathways/android-architecture) best practices:

- **Clean Layered Architecture**:
  - **UI Layer (`ui/`, `vm/`)**: Built with Jetpack Compose and Material 3 Expressive, powered by ViewModels exposing reactive `StateFlow` and following Unidirectional Data Flow (UDF).
  - **Domain Layer (`domain/`, `model/`, `exception/`)**: Pure Kotlin business operations (`domain/usecase/`), data contracts (`domain/repository/`), system providers (`domain/provider/`), pure domain models (`model/`), and centralized exceptions (`exception/`).
  - **Data Layer (`data/`)**: Repository implementations acting as Single Source of Truth (SSOT), encapsulating Room database with dedicated entities and mappers, ProtoBuf DataStore, and Ktor network client.
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
