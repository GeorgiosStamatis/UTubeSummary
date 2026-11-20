# UTubeSummary user guide

UTubeSummary summarizes YouTube and BiliBili videos, web articles, documents, images and plain text with an
AI model **you** choose, using **your own** API key ("bring your own key"). This guide walks through every
screen and every setting, step by step. For what is stored and sent where, see the
[privacy policy](../PRIVACY.md).

**How to read this guide.** Menu paths are written with arrows: *Settings → AI Provider* means "open **Settings**
(the gear icon at the top left of Home), then tap **AI Provider**". Names of buttons and labels appear in **bold**
exactly as the app shows them in English. Pictures are in [`docs/images`](images); each step says what you should
see afterwards.

## Contents

1. [Install and first start](#1-install-and-first-start)
2. [Set up an AI provider](#2-set-up-an-ai-provider)
3. [The Home screen](#3-the-home-screen)
4. [Summarize something](#4-summarize-something)
5. [Profiles, language and length](#5-profiles-language-and-length)
6. [Work with the result](#6-work-with-the-result)
7. [Share sheet, text selection and the instant dialog](#7-share-sheet-text-selection-and-the-instant-dialog)
8. [History](#8-history)
9. [Tablets and landscape](#9-tablets-and-landscape)
10. [Every setting, one by one](#10-every-setting-one-by-one)
11. [Privacy and your data](#11-privacy-and-your-data)
12. [Troubleshooting](#12-troubleshooting)
13. [Quick reference](#13-quick-reference)

---

## 1. Install and first start

### 1.1 Choose a build

| Build | Good for | Notes |
|---|---|---|
| **gms** | Most phones | Smaller. Reads text from images with Google Play Services. |
| **standalone** | Phones without Google Play Services | Bigger. The text recognition model is inside the app and works offline. |

Both need **Android 13 or newer**. A new version installs over the old one if it is the same build type; your
provider, key, profiles and History are kept.

### 1.2 First start: the tutorial

The first time you open the app, a five-page tutorial explains the main ideas. Use **Next** to move on, **Previous**
to go back (on the first page the left button is **Skip**), or tap a dot of the page indicator to jump.

<img src="images/01-onboarding-welcome.png" width="260" alt="Tutorial page 1: Welcome to UTubeSummary">

| Page | What it tells you |
|---|---|
| **Welcome to UTubeSummary!** | The app summarizes websites, articles and YouTube videos. |
| **AI Providers & Quick Switcher** | You bring your own API key. Tap the switcher to select providers and models, or long-press and drag (↔ provider, ↕ model) to switch instantly. |
| **Paste & Summarize** | Paste a link, choose a profile and the language below the input, then ask follow-up questions. |
| **Instant Share Overlay** | Share a link to the app to get a summary in an overlay. |
| **History & Offline Search** | Your past summaries are kept in History. |

<img src="images/02-onboarding-providers.png" width="260" alt="Tutorial page 2: AI Providers and Quick Switcher with the demo switcher">

On the **last** page two buttons appear: **Setup AI** (opens the provider setup right away, see
[section 2](#2-set-up-an-ai-provider)) and **Finish**.

You can watch the tutorial again at any time: *Settings → Tutorial*.

---

## 2. Set up an AI provider

The app cannot summarize anything until one provider is configured. You need an account with the provider and an
**API key** (a long secret text). The key is stored **encrypted on your device** and is only sent to the provider
you chose.

### 2.1 Which providers are available

| Provider | Needs | Notes |
|---|---|---|
| OpenAI, Gemini, Claude, DeepSeek, Mistral, OpenRouter, Qwen, Kimi, MiniMax, Zhipu | An API key | The official address is filled in for you. |
| Ollama | A server address | A key is not asked for. Enter the address of your own server, for example `192.168.1.10:11434`. |
| LM Studio | A server address | A key (token) is optional. Example address: `http://192.168.1.10:1234/v1`. |
| Custom Provider | A server address | Any OpenAI-compatible server. A key is optional. Example: `https://example.com/v1`. |

Local servers (Ollama, LM Studio) must be reachable from the phone, usually the same Wi-Fi network.

### 2.2 Add a provider with an API key (OpenAI, Gemini, Claude and so on)

1. On Home tap the **gear icon** (top left) to open **Settings**.
2. In the group with the AI items tap **AI Provider**. The dialog **AI Provider** opens with a list of providers.

   <img src="images/03-provider-dialog-key.png" width="260" alt="The AI Provider dialog with the provider list, the address field and the API key field">

3. Tap the provider you want. It gets a highlighted frame.
4. The field **Custom URL** shows the provider's official address as a faint hint. Leave it empty unless you use a
   proxy or a regional endpoint.
5. In the field **\* Set API Key** paste your key. Tap the clipboard icon at the end of the field to paste, and the
   same icon clears it. The key is hidden as dots. A star means the field is required.
6. Tap **Test**. The app asks the server whether the address and the key work and shows one of:
   - **Connection successful. The API key was accepted.** and the number of models found (for example
     **16 models loaded.**). The model list of the provider is updated.
   - **Authentication failed (HTTP 401). Check the API key.**
   - **Endpoint not found (HTTP 404). Check the URL.**
   - **Couldn't reach the server. Check the URL and your network connection.**

   <img src="images/04-provider-dialog-test-success.png" width="260" alt="Provider dialog after a successful test showing the connection successful message">

7. Tap **Next** (you changed the provider) to go on and choose a model (see [2.4](#24-choose-and-manage-models)), or
   **OK** if you only changed the key of the provider that is already active.

If the app says the saved key **can't be read anymore** (this can happen after a phone restore), enter the key again.

### 2.3 Add a local or custom provider (Ollama, LM Studio, Custom Provider)

1. *Settings → AI Provider*, tap **Ollama**, **LM Studio** or **Custom Provider**.
2. Fill in **\* Base Url** with the address of your server. The star means it is required. The grey hint shows an
   example.
3. For LM Studio and the Custom Provider the key field is labelled **Set API Key (optional)**: fill it in only if
   your server asks for a token.
4. Tap **Test**, then **Next**.

<img src="images/05-provider-dialog-local.png" width="260" alt="The AI Provider dialog with Ollama selected and the required Base Url field">

> If **Test** says the server cannot be reached, check that the server is running, that the phone and the computer
> are on the same network and that the address includes the port.

### 2.4 Choose and manage models

After **Next**, or any time via *Settings → Select Model*, the dialog **Manage Models (provider name)** opens.

<img src="images/06-model-dialog.png" width="260" alt="Manage Models dialog with a list of models, the selected one highlighted">

- **Select a model:** tap a row. The selected row is highlighted. Tap **OK** to confirm (disabled while no model
  is selected).
- **Add a model:** type or paste the model name into **Add Model** and tap the **+** button (or the keyboard's
  Done key). The new model goes to the top of the list and is selected. Use this when your provider has a model
  that is not in the list, or for self-hosted servers whose list is empty.
- **Rename a model:** tap the pencil on its row, edit, and confirm with **Save edit** (or **Cancel edit**).
- **Delete a model:** tap the bin on its row.
- **Reorder:** drag a row by its handle (**Drag to reorder**).
- **Reset:** puts the list back to how it was when you opened the dialog (only active after a change).
- **Cancel** closes without saving.

If you see **No models configured. Please configure in Settings.** add at least one model name here.

### 2.5 Switch quickly between providers and models

The badge at the top left of Home, next to the gear, shows the **provider icon and model name** in use.

<img src="images/07-home-provider-sheet.png" width="260" alt="Bottom sheet Select AI Provider with provider chips and a model list">

- **Tap the badge:** a bottom sheet **Select AI Provider** opens with the providers you set up and, below, the
  models of the active provider (**Select Model**). Tap to choose. Providers without a key show a message
  **Please configure … in Settings before use**. The button **Setup AI** opens Settings.
- **Long-press the badge and drag, then release:** the **Quick Switch** popup appears. Drag **left or right**
  (↔ **Provider**) to change the provider and **up or down** (↕ **Model**) to change the model. Release to
  switch (**Release to switch**).

<img src="images/08-quick-switch.png" width="260" alt="Quick Switch popup while dragging, showing provider and model">

### 2.6 Change the order of providers

In the **AI Provider** dialog drag a provider by its handle to put the ones you use most at the top. Tap **OK**
to save. This order is used by the bottom sheet and by Quick Switch.

---

## 3. The Home screen

Home is where you summarize.

<img src="images/09-home-empty.png" width="260" alt="Home screen with the title bar, the input field and the floating buttons">

| Area | What it does |
|---|---|
| **Gear icon** (top left) | Opens **Settings**. |
| **Provider and model badge** (next to the gear) | Tap to choose provider and model; long-press and drag for Quick Switch. |
| **Clock icon** (top right) | Opens **History**. |
| **Input field** (**URL / Text**) | Where you paste a link, type text or see the name of a chosen file. |
| **Profile chip** | The style of the summary (only shown when you have more than one profile). |
| **Language chip** | The language of the summary. |
| **Short / Middle / Long** | The length of the summary. |
| **Large round button** (bottom right) | Paste (empty input), **Summarize** (input present) or **Regenerate** (result is up to date). |
| **+ button** (above the large button) | **Image**, **Camera**, **Document**. |

### 3.1 The input field

- A small **badge** at the start shows what the app recognized: YouTube, BiliBili, web article, document, or text.
- The **x** at the end (**Clear**) empties the field.
- For long text (at least 100 characters or more than one line) an arrow (**Expand** / **Collapse**) opens the field
  up to seven lines.
- For plain text, a hint **Approximate tokens: n** shows roughly how much the model will read (about 100 tokens per
  75 words). It is not shown for links and files.
- The keyboard's **Done** key starts the summary, like the round button.

<img src="images/10-home-input-link.png" width="260" alt="Input field with a YouTube link and its badge, and the profile, language and length controls below">

### 3.2 The large round button

| Icon | Meaning |
|---|---|
| Clipboard | The field is empty: tap to paste from the clipboard (**Paste from clipboard**). |
| Check mark | There is input: tap to summarize (**Summarize**). |
| Circular arrow | You already have a result for this input and these settings: tap to make a new one (**Regenerate**). |
| Spinner | The app is working. Tapping shows **The document is still being processed…**. |

### 3.3 The + button (files, images, camera)

Tap **+**. Three entries appear: **Image**, **Camera**, **Document**. Tap **+** again (it turns into **x**) or press the
back button to close the menu.

<img src="images/11-home-attachments-menu.png" width="260" alt="The plus button opened with the Image, Camera and Document entries">

---

## 4. Summarize something

### 4.1 Steps for every kind of content

1. Put the content into the input field (see 4.2 to 4.7).
2. Choose **profile**, **language** and **length** (see [section 5](#5-profiles-language-and-length)). The defaults
   are fine for a first try.
3. Tap the **round button** (check mark).
4. Watch the progress: **Fetching content…**, then **Summarizing…**.

   <img src="images/12-home-progress.png" width="260" alt="Home while working: the message Summarizing and a Cancel button">

   Tap **Cancel** to stop. A cancelled run is not an error and nothing is saved.
5. The result card appears below the controls (see [section 6](#6-work-with-the-result)) and a copy is added to
   [History](#8-history).

Changing the profile, language or length after a result marks the result as out of date; the round button then
offers to summarize again. Switching **Short / Middle / Long** shows a summary you already made for that length
instead of calling the model again.

### 4.2 A YouTube video

1. Copy the link in the YouTube app (**Share → Copy link**) or from the browser.
2. In UTubeSummary tap the round button while the field is empty (it pastes) or paste into the field.
3. Check that the badge shows YouTube, then tap the check button.

The app reads the **subtitles** of the video. Manual subtitles are preferred over automatic ones. If you chose a
specific summary language and the video offers subtitles in that language, those are used.

> Videos without subtitles, private, removed, region-locked and age-restricted videos cannot be summarized
> (see [troubleshooting](#12-troubleshooting)).

### 4.3 A BiliBili video

Paste the BiliBili link and tap summarize, as for YouTube. Some videos need you to be logged in; then the app
opens the login for you. You can also do it beforehand:

1. *Settings → BiliBili Account* opens the sheet **BiliBili Login**.

   <img src="images/13-bilibili-login.png" width="260" alt="The BiliBili Login sheet showing the BiliBili sign-in page">

2. Sign in on the page as you would on the BiliBili website, then close the sheet (**Close**). Back in Settings the
   item shows your login state.
3. To log out, **long-press** *BiliBili Account* (it only reacts to long-press while you are logged in) and confirm
   **Clear BiliBili Login**.

### 4.4 A web article

Paste the link and summarize. The app downloads the page and reads its main text. If a site
blocks the app, the text is fetched through the public reader service `r.jina.ai` instead (see
[privacy](#11-privacy-and-your-data)). Pages behind a paywall or login cannot be read: **Paywall detected,
summarizing not possible.**

If you paste text that **contains** a link ("Look at this: https://…"), the setting
[*Auto Extract Web Link*](#10-every-setting-one-by-one) decides whether the link is picked out and summarized.

### 4.5 A document (PDF, Word, text, Markdown)

1. Tap **+**, then **Document**.
2. Choose a file: `.pdf`, `.docx`, `.txt` or `.md`.
3. The file name appears in the input field (read-only). Tap the check button.

Tables and line breaks of Word files are kept. Text files with a byte-order mark, UTF-16 or Windows line endings
are read correctly. If the file cannot be opened later, pick it again: **The file could not be opened. Pick it
again, or check that it is a valid file.**

### 4.6 An image or a photo

1. Tap **+**, then **Image** (choose a picture) or **Camera** (take one).
2. The text in the picture is recognized on your phone (Latin text) and then summarized.

### 4.7 Plain text

Type or paste the text into the input field and summarize. Very short text gives **Provided medium is too short.**

### 4.8 The clipboard shortcut

Long-press the app icon on your launcher and tap **Summarize from clipboard** (short name **Clipboard**). The app
opens with the clipboard content in the input field. **Nothing is sent until you tap the check button.**

### 4.9 When something goes wrong

Errors appear in red under the input field. Tap **Show details** to see the provider, model and the cause (without
your key); **Hide details** closes it.

<img src="images/14-home-error-details.png" width="260" alt="An error message under the input field with Show details expanded">

The full list of messages and what to do is in [section 12](#12-troubleshooting).

---

## 5. Profiles, language and length

### 5.1 Choose a profile

A **profile** is a base prompt: it decides the style of the answer.

1. Tap the **Profile** chip (it reads **Profile: Default** and so on).
2. Pick one from the menu.

<img src="images/15-home-selectors.png" width="260" alt="The profile menu open with the six built-in profiles">

| Built-in profile | Use it for |
|---|---|
| **Default** | An overview, key points, tags and a body text. |
| **Technical Deep Dive** | Technical talks and tutorials. |
| **Critical Analysis** | A critical reading of arguments and claims. |
| **Learning Mode** | Explanations for studying. |
| **Technical Content Quality Evaluator** | Judging the quality of technical content. |
| **Quick Brief** | A very short answer. |

Except for **Default**, the answer follows the format given by the profile's prompt and is shown as formatted text.

### 5.2 Choose the language of the summary

1. Tap the **Language** chip.
2. Pick **Video language** (the language of the content), **App language**, or one of: English, French, German,
   Spanish, Italian, Portuguese, Dutch, Greek, Russian, Ukrainian, Polish, Turkish, Arabic, Hindi, Chinese,
   Japanese, Korean, Czech, Swedish.

<img src="images/16-home-language-menu.png" width="260" alt="The language menu with Video language, App language and the list of languages">

For a video, choosing a specific language also picks the subtitles in that language when the video has them. If a
summary comes out in an unexpected language, choose the language explicitly.

### 5.3 Choose the length

If the **Short / Middle / Long** buttons are shown, tap one. If you do not see them, the length options are
switched off: *Settings → Advanced Summary Setup → Use Length Options*.

### 5.4 Edit, create and share profiles

1. *Settings → Advanced Summary Setup*, then tap **Summarization profiles**.
2. The screen **Summarization profiles** lists all profiles with a radio button. Tapping a row **selects** it as
   the profile in use. A profile you changed shows **Edited**.

   <img src="images/17-profiles-list.png" width="260" alt="Summarization profiles list with radio buttons and edit pencils">

3. **Edit:** tap the pencil. The **Default** profile says "The Default profile is edited in Advanced Summary
   Setup" and is not edited here.
4. **Create:** tap the **+** button (**New profile**).
5. In the editor fill in **Name** and **Base prompt**. Write `[Language instructions]` and `[Length instructions]`
   where the rules for language and length should go; they are added automatically if you leave them out.
6. The switch **Overview, key points and tags** shows the answer as a card with an overview, key points and tags
   instead of the free-form text of your prompt.
7. Tap **Save**.

   <img src="images/18-profile-editor.png" width="260" alt="The profile editor with Name, Base prompt and the structured-output switch">

8. **Share** sends the profile as text. Someone else copies that text and taps the clipboard icon at the top
   right of **Summarization profiles** (**Import profile from clipboard**); the app then says **Profile imported:
   name**, or **The clipboard does not contain a shared profile.**
9. **Delete** removes your own profile; for a built-in profile the button is **Reset to preset** and restores the
   original prompt.

### 5.5 Advanced Summary Setup

*Settings → Advanced Summary Setup* (**Configure base prompt and language constraints**):

<img src="images/19-advanced-setup.png" width="260" alt="Advanced Summary Setup screen with the base prompt, the append switch and the language item">

1. **Base Prompt** shows the prompt of the **Default** profile.
2. **Append Custom Prompt** (on by default) keeps the app's prompt as it is and **adds** your own text in the field
   **Additional Prompt** ("Enter additional instructions…") to every profile. Switch it **off** to write your own
   base prompt: the field becomes editable, with the buttons **Clear** and **Reset to Default**.
3. **Dynamic Constraints (Read-only)** lists the language and length rules the app adds itself.
4. **Language** chooses the default summary language (same list as on Home).
5. **Use Length Options** switches the **Short / Middle / Long** buttons on Home on or off.
6. **Final Prompt Preview** shows the complete prompt that would be sent for the selected profile, so you can check
   your changes.

<img src="images/20-advanced-final-prompt.png" width="260" alt="The Final Prompt Preview card at the bottom of Advanced Summary Setup">

---

## 6. Work with the result

<img src="images/21-home-result-card.png" width="260" alt="A summary card with title, author, tags, overview, key points and text">

The **result card** shows (depending on the profile): the **title** and **author** (shown only when known), the
**tags**, the **Overview**, the **key points** and the summary text. Long text can be unfolded with **Show More** and
folded with **Show Less**. The small badge shows the kind of source (YouTube, BiliBili, article, document, text).

### 6.1 The action buttons

<img src="images/22-home-result-actions.png" width="260" alt="The row of action buttons under a summary: read aloud, link, copy, share, save">

| Button | What it does |
|---|---|
| Speaker (**Read**) | Reads the summary aloud with the phone's text-to-speech. Tap again to stop (**Stop**). While reading, **Pause** / **Continue** and the speed button appear; tap the speed button to cycle through the speeds. |
| Link (**Original link**) | Opens the video or article. |
| Copy (**Copy**) | Copies the summary as plain text (**Copied**). |
| Share (**Share**) | Shares the summary as Markdown: title, link, key points, text, tags. |
| Save (**Save as Markdown file**) | Saves a `.md` file where you choose (**Saved**). |

If the speech engine is missing or does not know the language you may see **Text-to-speech engine failed to
initialize.** or **Text-to-speech language not supported.** Install or update a speech engine in the Android
settings.

### 6.2 Ask about this content

Right after a summary, the box **Ask about this content** appears under the card.

<img src="images/23-home-question-box.png" width="260" alt="Ask about this content box with a question field and an answer">

1. Type your question in **Ask a question…**.
2. Tap **Send question**.
3. The answer appears above the field; **Copy answer** copies it.

The text of the video, article or document stays in memory only and is sent with your question to your provider.
Earlier questions and answers count as context. A new input starts a new conversation.

---

## 7. Share sheet, text selection and the instant dialog

You do not have to open the app first.

### 7.1 Share a link or a file

1. In YouTube, a browser or any other app, tap **Share**.
2. In the share sheet choose one of the two targets of the app:
   - **UTubeSummary** opens the app's Home screen with the link in the input field, so you can pick the profile and
     the language before you summarize.
   - **Instant Summarize** shows a floating dialog on top of the current app (see 7.3).

   <img src="images/24-share-sheet.png" width="260" alt="Android share sheet with the UTubeSummary and Instant Summarize targets">

Documents (PDF, Word, ...) and images can be shared the same way.

**A shared link always lands on Home.** Even if History or Settings is open when you share, the app comes back to
the Home screen with the link in the input field.

### 7.2 Summarize selected text

1. Select text in any app.
2. In the selection menu tap **Instant Summarize** (on some phones it is under **⋮ More**).

<img src="images/25-text-selection-menu.png" width="260" alt="Text selection menu with the Summarize entry">

### 7.3 The instant dialog

<img src="images/26-instant-dialog.png" width="260" alt="The instant dialog with profile and language chips and a finished summary">

- The dialog shows the title, the **Profile** and **Language** chips, and the progress.
- Changing profile or language summarizes the same text again.
- After the summary you can copy it (**Copied**) or share it (**Share**).
- **Open in App** continues on the Home screen with the same input. The close icon closes the dialog.
- If something fails, an error with **Open in App** appears.

---

## 8. History

Every summary is saved on your device. Open History with the **clock icon** at the top right of Home.

<img src="images/27-history-list.png" width="260" alt="History list with entries grouped by date, thumbnails and badges">

### 8.1 Find an entry

- **Search** by title, author or text with the search bar.
- **Filter chips:** **Pinned** (only pinned entries), **Profile** (all or one profile) and the content types
  **Video**, **Article**, **Document**, **Text**.
- **Sort chip:** **Newest first**, **Oldest first** or **Title A-Z**. Pinned entries always stay on top.

<img src="images/28-history-filters.png" width="260" alt="The History filter chips and an active search">

If nothing matches you see **Nothing found**; with an empty History you see **You haven't summarized anything yet.**

### 8.2 Open an entry

Tap a row. A sheet opens with the summary and its actions (on a tablet the entry opens on the right). The sheet
has **Pin** / **Unpin**, **Summarize again with other settings**, **Update** (videos and web pages) and one card
per summary.

<img src="images/29-history-detail.png" width="260" alt="History entry sheet with several summaries of the same video">

### 8.3 One entry, many summaries

Summarizing the **same** link or file again with another profile, language or length adds a card to the **same
entry** instead of a second entry. The cards are ordered oldest first and the sheet scrolls to the newest one.
Each card has a header line with the profile, language, length and date, and older cards start with a shortened
text (tap **Show More** to read all). The bin next to the header (**Remove this summary**) removes only that summary
(**Summary removed**).

### 8.4 Pin, delete and select several

- **Pin:** open the entry and tap **Pin**. A pin shows on the row.
- **Delete one:** swipe the row **to the left**. The message **Summary deleted** has **Undo**.
- **Select several:** **long-press** a row. A check mark appears; tap more rows. The top bar shows **n selected**
  with **Pin** and **Delete** icons and a close icon to leave the selection.

<img src="images/30-history-selection.png" width="260" alt="History in selection mode with three entries checked and the count in the top bar">

### 8.5 Summarize again with other settings

Open an entry and tap **Summarize again with other settings**. Home opens with the original source in the input
field so you can change profile, language, length or model.

### 8.6 Update an entry

*Update* is for **videos and web pages** (text and documents have no button). It fetches the source **again** and
makes **every summary of the entry** again from the new content with the profile, language and length each had and
with your **current provider and model**.

1. Open the entry and tap **Update**.
2. The dialog **Update this entry?** says how many summaries will be made again. Every summary is one model call,
   so check this number. Tap **Update** to continue or **Cancel**.

   <img src="images/31-history-update-confirm.png" width="260" alt="The Update this entry dialog with the number of summaries">

3. The app shows **Fetching the source…**, then **Updating 1/3…** and so on. **Cancel** stops it
   (**Update cancelled**).

   <img src="images/32-history-update-progress.png" width="260" alt="Update in progress with the counter and a Cancel button">

4. When it is done you see one of:
   - **Updated n summaries** — the new summaries replace the old ones.
   - **Already up to date** — the content has not changed; no model call was made.
   - **Updated n summaries, m skipped (profile no longer exists)** — summaries of a deleted custom profile are kept as
     they are.
   - **Updated n of m summaries; the others failed**

The app compares a short fingerprint (a hash, not the text) of the new content with the one stored with each
summary, which is how it knows nothing changed.

### 8.7 Reuse of the downloaded text

If you summarize the **same** link or file again in the same session (for example with another profile), the app
reuses the text it already downloaded instead of fetching it again. Use **Update** when you want fresh content;
closing and reopening the app also starts fresh.

### 8.8 Export and import

- **Export:** *Settings → Export History* saves your YouTube video summaries to a file that can be imported again
  (**History exported**). Without any YouTube summaries the app says **There are no YouTube video summaries to
  export.**
- **Import:** *Settings → Import video summaries* reads a JSON backup (also from another app). Choose the file; the
  app shows **Imported n. Skipped m already in your history.** Videos already in History are skipped and original
  dates are kept. Files over 25 MB are refused (**This file is too large to import (25 MB maximum).**).

---

## 9. Tablets and landscape

On a tablet, and on a phone in landscape (at least 600 dp wide), the app uses two columns:

- **History:** the list on the left; the selected entry on the right (no bottom sheet). With nothing selected the
  right side says **Select a summary to read it here**.
- **Home:** input and options on the left; the result and the question box on the right. Each side scrolls on its
  own.
- The instant dialog uses more of the height in landscape.
- If you rotate the phone, the open History entry stays open.

<img src="images/33-tablet-home.png" width="480" alt="Home on a tablet in landscape with the input on the left and the result on the right">

<img src="images/34-tablet-history.png" width="480" alt="History on a tablet in landscape with the list on the left and the open entry on the right">

On a phone in portrait nothing changes.

---

## 10. Every setting, one by one

Open **Settings** with the gear icon on Home. Use the **Search settings** bar at the top to find a setting by
name.

<img src="images/35-settings-top.png" width="260" alt="Settings: the search bar and the Language, Theme, Dynamic Color and Video thumbnails items, then AI items">

### 10.1 Display

| Setting | What it does |
|---|---|
| **Language** — *Choose a language* | Opens the **Android per-app language settings** where you choose the language of the app. This is the "App language". (The language of a *summary* is chosen on Home or in Advanced Summary Setup.) |
| **Theme** | Opens a dialog with **System**, **Light** and **Dark**. Tap one, then **OK**. |
| **Dynamic Color** | A switch: *Use colors from your wallpaper*. |
| **Video thumbnails** | A switch: *Show thumbnails in History, loaded from YouTube (about 10 KB each).* Off means no request to YouTube's picture server. |

<img src="images/36-theme-dialog.png" width="260" alt="Theme dialog with System, Light and Dark options">

### 10.2 AI

| Setting | What it does |
|---|---|
| **AI Provider** | Provider, address and key, see [2.2](#22-add-a-provider-with-an-api-key-openai-gemini-claude-and-so-on). The subtitle shows the active provider. |
| **Select Model** | The model of the active provider, see [2.4](#24-choose-and-manage-models). The subtitle shows the active model. |
| **Advanced Summary Setup** | Base prompt, extra instruction, default language, length options, profiles, see [5.5](#55-advanced-summary-setup). |

### 10.3 Services

| Setting | What it does |
|---|---|
| **BiliBili Account** | Tap to log in (see [4.3](#43-a-bilibili-video)); long-press while logged in to log out (**Clear BiliBili Login**). |
| **Auto Extract Web Link** | A switch (on by default): *Automatically extract web link from shared content.* On: a link inside shared or pasted text ("Look at this: https://…") is found and that page is summarized. Off: only text that starts with the link counts as a link. |

### 10.4 Data

| Setting | What it does |
|---|---|
| **Import video summaries** | See [8.8](#88-export-and-import). |
| **Export History** | See [8.8](#88-export-and-import). |

### 10.5 Help and about

| Setting | What it does |
|---|---|
| **Tutorial** | *Rewatch the Tutorial.* |
| **Open Source Code** | *Our GitHub repository.* |
| **Version** (bottom) | Shows the app version. Tap it to copy the app information, handy when you report a problem. |

<img src="images/37-settings-bottom.png" width="260" alt="Bottom of Settings: BiliBili Account, Auto Extract Web Link, Import, Export and the help items">

### 10.6 Search settings

Type in the **Search settings** bar. Matching settings appear in one list and work as normal (tap, switch). The
cross clears the search. **Nothing found** means no setting matches.

---

## 11. Privacy and your data

- There is **no analytics or tracking**.
- The text you summarize and the questions you ask go **only to the AI provider you configured**.
- Two optional requests go elsewhere: the reader service `r.jina.ai` (only when a website blocks the app) and
  YouTube's picture server for History thumbnails (switch them off with **Video thumbnails**).
- Your **History, profiles and settings stay on your device**. API keys and the BiliBili login are stored
  encrypted.
- With each summary of a video or web page the app stores a short **fingerprint** (a hash, not the text) so
  **Update** can tell whether the source changed.
- Sharing, saving as a file and exporting only happen when you choose them.
- To remove your data, delete entries in History, remove the keys in *Settings → AI Provider*, log out of BiliBili,
  or clear the app's storage in the Android settings (this removes everything).

Details: [PRIVACY.md](../PRIVACY.md).

---

## 12. Troubleshooting

| What you see | Cause and what to do |
|---|---|
| **Invalid link: Summary not possible** | The text looks like a link but is not a usable address. Check the link or paste the text itself. |
| **No Transcript found** | The video has no subtitles. Try another video. |
| **YouTube does not give access to this video. It may be private, age-restricted, region-locked or removed.** | The video cannot be read without a YouTube login. |
| **Could not read this YouTube video. YouTube may have changed something…** | The app could not get the subtitles. Try again later or update the app. Tap **Show details** and include the text when you report it. |
| **Paywall detected, summarizing not possible.** | The page is behind a paywall or login. Paste the text instead. |
| **No Content** / **Provided medium is too short.** | There is nothing to summarize or the text is too short. |
| **Provided medium is too long.** / **This content is too long for the selected model. Choose a model with a larger context window and try again.** | Pick a model with a larger context window in *Select Model* (long videos and books need one). Nothing is cut off silently. |
| **No Internet Connection** | Check your connection. Local servers (Ollama, LM Studio) must be reachable from the phone. |
| **Incorrect API key** / **Error with server, you have to provide an API key.** | Check the key in *Settings → AI Provider* and use **Test**. |
| **Access denied: The model or service cannot be accessed.** | Your key has no access to that model. Choose another model or check the plan of your provider. |
| **Model or endpoint not found. Please check the model name and the URL in Settings.** | A typo in the model name or the address. |
| **Server overloaded, try again later.** | Rate limit. Wait a moment or switch to another model or provider. |
| **AI service is currently unavailable. Please try again later.** | The provider has a problem. |
| **The saved API key can't be read anymore…** | Secure storage was reset (for example after a restore). Enter the key again. |
| **Couldn't store this securely on your device, so it was not saved.** | The phone's secure storage failed. Try again or restart the phone. |
| **The file could not be opened. Pick it again, or check that it is a valid file.** | Pick the file again; access may have ended. |
| **This file is not a valid video summaries backup.** / **The file could not be read.** | Wrong or damaged import file. |
| **The summary is in an unexpected language** | Choose the **Language** on Home explicitly instead of **Video language**. |
| **The summary is outdated** | Open the entry in History and tap **Update**. |
| Text-to-speech messages | Install or update a speech engine in the Android settings. |

**Report a problem or an idea:** [GitHub issues](https://github.com/GeorgiosStamatis/UTubeSummary/issues). Tip:
**Show details** on an error gives the provider, model and cause without your API key.

---

## 13. Quick reference

### 13.1 I want to…

| I want to… | Do this |
|---|---|
| Summarize a link | Paste it on Home, tap the check button. |
| Summarize from YouTube without opening the app | Share → **Instant Summarize**. |
| Choose style and language before summarizing a shared link | Share → **UTubeSummary**, then change the chips on Home. |
| Use another model | Tap the badge at the top of Home, or long-press and drag. |
| Add my own prompt | *Settings → Advanced Summary Setup → Append Custom Prompt*. |
| Make a new style | *Settings → Advanced Summary Setup → Summarization profiles → +*. |
| Hide the length buttons | *Settings → Advanced Summary Setup → Use Length Options*. |
| Ask a follow-up question | The box **Ask about this content** under the summary. |
| Refresh an old summary | History → entry → **Update**. |
| Remove several entries | Long-press an entry in History, tick more, tap the bin. |
| Move my videos to another phone | *Export History* on one phone, *Import video summaries* on the other. |
| Change the app language | *Settings → Language* (opens the Android setting). |
| See the tutorial again | *Settings → Tutorial*. |

### 13.2 Glossary

| Term | Meaning |
|---|---|
| **API key** | A secret text from your AI provider that lets the app use your account. |
| **Provider** | The company or server that runs the AI model (OpenAI, Gemini, Ollama, ...). |
| **Model** | The specific AI model, for example a "flash" or "mini" variant. |
| **Profile** | A base prompt that decides the style of a summary. |
| **Context window** | How much text a model can read at once. |
| **Transcript / subtitles** | The text of a video's speech that the app reads. |
| **Fingerprint** | A short hash of the downloaded content, used to detect changes. |
