# UTubeSummary user guide

UTubeSummary summarizes YouTube and BiliBili videos, web articles, documents, images and plain text
with an AI model **you** choose, using **your own** API key. This guide explains how to use the app.
For what is stored and sent where, see the [privacy policy](../PRIVACY.md).

## Contents

1. [Getting started](#1-getting-started)
2. [Summarize something](#2-summarize-something)
3. [Choose how the summary is written](#3-choose-how-the-summary-is-written)
4. [Work with the result](#4-work-with-the-result)
5. [Share sheet and text selection](#5-share-sheet-and-text-selection)
6. [History](#6-history)
7. [Tablets and landscape](#7-tablets-and-landscape)
8. [Settings](#8-settings)
9. [Privacy in practice](#9-privacy-in-practice)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Getting started

**Install.** The app needs Android 13 or newer. There are two builds: **gms** (smaller; reads text from images
with Google Play Services) and **standalone** (bigger; works without Google Play Services). A new build installs
over the old one if it is the same build type; your provider, key and History are kept.

**First start.** A short tutorial explains the main screens. You can open it again later in
*Settings → Tutorial*.

**Add an AI provider** (the app does not work without one):

1. Open *Settings → AI Provider* and choose a provider: OpenAI, Gemini, Claude, DeepSeek, Mistral, OpenRouter,
   Qwen, Kimi, MiniMax, Zhipu, Ollama, LM Studio or a **Custom Provider** (any OpenAI-compatible server).
2. Paste your **API key**. Ollama, LM Studio and the Custom Provider do not need one; for them enter the address
   of your server (for example `192.168.1.10:1234`).
3. Tap **Test**. It checks the address and the key and fills in the list of models.
4. Choose a model in *Settings → Select Model* (or tap the provider/model badge at the top of the Home screen).
   Long-press the badge to switch quickly between the providers you set up.

Your keys are stored **encrypted on your device**. You can reorder providers and add, rename, delete or reset
models per provider.

---

## 2. Summarize something

Everything starts on the **Home** screen. Put the content into the input field and tap **Summarize**:

| You want to summarize | How |
|---|---|
| A **YouTube** or **BiliBili** video | Paste or share the link. The app reads the subtitles (YouTube: manual subtitles are preferred). |
| A **web article** | Paste or share the link. If a site blocks the app, the text is fetched through a reader service. |
| A **document** | Tap the file button: PDF, Word (`.docx`), `.txt` or `.md`. Tables and line breaks of Word files are kept. |
| An **image** | Pick a picture or take a photo with the camera button; the text in it is recognized (Latin text). |
| **Plain text** | Type or paste it into the field. |

Handy buttons: **Paste from clipboard**, and the launcher shortcut **Summarize from clipboard**, which only fills
the field. Nothing is sent until you tap *Summarize*.

While it works you see **Fetching content…**, then **Summarizing…**. Tap **Cancel** to stop; a cancelled run is
not an error and nothing is saved.

**BiliBili:** some videos need you to be logged in. The app then opens *Settings → BiliBili Account*; log in
there once. *Clear BiliBili Login* removes it again.

---

## 3. Choose how the summary is written

Under the input field you choose three things. Changing one after a result marks the result as out of date, and
the button offers to summarize again.

- **Length:** Short, Middle or Long (the length selector can be switched off in Settings).
- **Profile:** the style of the summary. Built in: **Default** (overview, key points, tags and a body text),
  **Technical Deep Dive**, **Critical Analysis**, **Learning Mode**, **Technical Content Quality Evaluator**
  and **Quick Brief**. Except for Default, the answer follows the format of the profile's prompt and is shown
  as formatted text.
- **Language:** *Video language* (the language of the content), *App language*, or about 20 other languages.

**Edit profiles** in *Settings → Summarization profiles*: edit a prompt, reset a built-in profile, create or
delete your own. A profile can show the overview/key points/tags card with its own switch. You can **share a
profile** as text and **import** one from the clipboard. *Settings → Advanced Summary Setup* holds the base
prompt of the Default profile and an extra instruction ("Append Custom Prompt") that is added to every profile.

If the content is too long for the model, the app says so and suggests a model with a larger context window.
Nothing is cut off silently.

---

## 4. Work with the result

On the result card:

- **Read aloud** with text to speech (Markdown symbols are not read out); tap the speed button to change the speed.
- **Copy** the summary, or **share** it as Markdown (title, link, key points, text, tags).
- **Save as Markdown file** keeps it as a `.md` file where you choose.
- **Ask about this content:** right after a summary you can ask follow-up questions about the video, article or
  document. The text stays in memory only and is sent with your question to your provider. Earlier answers count
  as context. A new input starts a new conversation.

Switching between Short, Middle and Long shows a summary you already made; otherwise tap *Summarize*.

---

## 5. Share sheet and text selection

You do not have to open the app first:

- **Share a link or text** from YouTube, a browser or any app to UTubeSummary. The **instant dialog** opens over
  the other app and shows the summary; you can change **profile** and **language** in it (the text is
  summarized again). **Open in app** continues on the Home screen.
- **Select text** in any app and choose *Summarize* from the selection menu.
- If you share a link while the app is already open, for example while **History** or **Settings** is on screen,
  the app **always goes to the Home screen** with the link in the input, so you can choose the profile and the
  language before you summarize.

---

## 6. History

Every summary is saved on your device. Open it with the clock icon on Home.

**Find things.** Search by title, author or text. Filter by type (video, article, ...), by **profile** or by
**pinned**. Sort by newest, oldest or title. **Pin** an entry to keep it at the top.

**One entry, many summaries.** Summarizing the same item again with another profile, language or length adds a
section to the **same entry** (oldest first, older ones collapsed). Remove a single summary with the bin icon.

**Delete.** Swipe an entry away, or **long-press** to select several and delete or pin them together. Every delete
has **Undo**.

**Summarize again.** *Summarize again with other settings* opens the Home screen with the source filled in.

**Update.** *Update* (for videos and web pages) fetches the source **again** and makes **every summary of the
entry** again from the new content, with the profile, language and length each one had and with your current
provider and model. You are asked first, because every summary is one model call. If the content has not
changed the app says **Already up to date** and makes no model call. New summaries replace the old ones; a
summary whose custom profile no longer exists is kept as it is. You can cancel while it runs. Text and document
entries have no Update button.

**Reuse of the downloaded text.** If you summarize the **same** link or file again in the same session (for
example with another profile), the app reuses the text it already downloaded or read instead of fetching it
again. Use **Update** when you want fresh content; closing and reopening the app also starts fresh.

**Export and import.** *Settings → Export History* saves your YouTube video summaries as a file that can be
imported again. *Settings → Import video summaries* adds videos from such a file (also from another app's
backup); videos already in your History are skipped and the original dates are kept.

---

## 7. Tablets and landscape

On a tablet, and on a phone in landscape (at least 600 dp wide), the app uses two columns:

- **History:** the list on the left, the selected entry on the right (no bottom sheet).
- **Home:** the input and options on the left, the result and the question box on the right.

On a phone in portrait nothing changes.

---

## 8. Settings

| Setting | What it does |
|---|---|
| *AI Provider*, *Select Model* | Provider, key, address and models (see Getting started). |
| *Summarization profiles*, *Advanced Summary Setup* | Profiles and prompts (see section 3). |
| *Language* | The default summary language. |
| *Theme*, *Dynamic Color* | Light, dark or system; colors from your wallpaper. |
| *Video thumbnails* | Small YouTube pictures in the History (can be switched off). |
| *Auto Extract Web Link* | Finds the web link inside shared or pasted text (for example "Look at this: https://…") and summarizes that page. When off, only text that starts with the link counts as a link. |
| *Import video summaries*, *Export History* | See section 6. |
| *BiliBili Account*, *Clear BiliBili Login* | Login for videos that need it. |
| *Tutorial* | Shows the first-start tour again. |

Use the search bar at the top of Settings to find a setting.

---

## 9. Privacy in practice

- There is **no analytics or tracking**.
- The text you summarize (and the questions you ask) goes **only to the AI provider you configured**.
- Two optional requests go elsewhere: the reader service (only when a site blocks the app) and YouTube's picture
  server for History thumbnails (switch them off in Settings).
- Your **History, profiles and settings stay on your device**. API keys and the BiliBili login are encrypted.
- With each summary of a video or web page the app stores a short **fingerprint** (a hash, not the text) so
  *Update* can tell whether the source changed.
- Sharing, saving as a file and exporting only happen when you choose them.

Details: [PRIVACY.md](../PRIVACY.md).

---

## 10. Troubleshooting

| What you see | What to do |
|---|---|
| **"Could not read this YouTube video. YouTube may have changed something…"** | Try again later or update the app. *Show details* names what failed; please include it when you report the problem. |
| **"YouTube does not give access to this video…"** | The video is private, age-restricted or removed. |
| **No transcript found** | The video has no subtitles. |
| **Content does not fit the model / "choose a larger-context model"** | Pick a model with a larger context window in *Select Model* (long videos and books need one). |
| **Incorrect key / access denied / model not found** | Check the key and the model name in *AI Provider*; use *Test*. |
| **Rate limit** | Wait a moment or switch to another model or provider. |
| **No internet** | Check your connection. Local servers (Ollama, LM Studio) must be reachable from the phone. |
| **Paywall or blocked article** | The app cannot read the page; paste the text instead. |
| **A file cannot be opened** | Pick it again (access may have ended). |
| **The summary is in an unexpected language** | Choose the **Language** explicitly on Home instead of *Video language*. |
| **The summary is outdated** | Open the entry in History and tap *Update*. |

**Report a problem or an idea:** [GitHub issues](https://github.com/GeorgiosStamatis/UTubeSummary/issues).
Tip: *Show details* on an error message gives the provider, model and cause without your API key.
