# UTubeSummary 2.2.0 — manual test cases

> **Status (branch `ccr-cad0612b-nqwuv3`, after batch 7 and the follow-up cleanups):** CI compiles both flavors and the
> unit tests pass. Since the first version of this document the app gained: the tablet/landscape layouts
> (section 15), the open History entry surviving rotation (15.4), a text-to-speech chunk fix (9.1), text
> files with a byte order mark or Windows line endings (4.12, 12.2), and code moves that touched the
> share-intent handling (section 13) and the Home screen. The list of cases that deserve extra attention
> after those code moves is in [Regression focus](#regression-focus-after-the-code-moves). Room schema
> export is **not** enabled (it broke the two-flavor build and was reverted).

Everything here needs a person, a device and (for most cases) a real AI provider key. The pure logic
behind these features is covered by the unit tests (`./gradlew testGmsDebugUnitTest`); see
[What the unit tests already cover](#what-the-unit-tests-already-cover) at the end, so you do not
repeat those checks by hand.

How to use: install the build, work through the sections in order (later ones reuse the History you
create), and mark each case **P** (pass), **F** (fail, add a note) or **S** (skipped). Report a
failure with the case id, what you did and what you saw; for crashes add the Logcat stack trace.

---

## 0. Preparation

**Devices / builds**

| Id | What | Needed for |
|----|------|------------|
| D1 | Phone, Android 13+, portrait (also rotate it) | everything |
| D2 | Tablet or emulator ≥ 840 dp wide (e.g. Pixel Tablet) | section 15 |
| D3 | Android 13 or 14 device/emulator | PDF case 4.6 |
| B1 | `gms` debug APK | all |
| B2 | `standalone` debug APK | image/OCR cases (4.9, 4.10) |

**Accounts and keys** (at least one cloud provider; more is better)

- K1: a working key for one cloud provider (OpenAI, Gemini, Claude, …).
- K2: a second working provider/key, to test switching.
- K3: a deliberately wrong key (e.g. `sk-wrong`).
- K4 (optional): Ollama or LM Studio on the same Wi-Fi, for the local-server cases.
- K5 (optional): a model with a small context window, for case 8.6.

**Test content** (keep these handy, ideally in a note on the phone)

| Id | Content |
|----|---------|
| C1 | A YouTube video with captions, 5–15 min, in a language other than your app language |
| C2 | A YouTube video with manual (not auto) captions in its spoken language |
| C3 | A very long YouTube video (2 h+) or a book-length text |
| C4 | A private, an age-restricted and a deleted YouTube video link |
| C5 | A BiliBili video link with subtitles; one without subtitles |
| C6 | A normal article URL; a URL that blocks bots (Cloudflare/“Just a moment…”); a URL with a typo |
| C7 | A text PDF; a scanned PDF (pictures of pages) |
| C8 | A Word `.docx` with a table, tabs and several line breaks |
| C9 | A `.txt` file and a `.md` file (headings, list, code block) |
| C10 | A photo/screenshot of English text (jpg/png/webp) |
| C11 | A paragraph of plain text (about 500 words) |
| C12 | A video-summaries JSON backup of the original app (for import) |
| C13 | A YouTube video whose manual subtitles exist in several languages but not in English (and has no auto-generated captions) |

**Priority tags.** Every case carries a tag: `P0` smoke (27 cases, about 45 minutes, run on every
build), `P1` normal (64, run before a release), `P2` rare or cosmetic (37, run when the area
changed). See the [smoke run](#smoke-run-p0-only) below.

**Test providers.** Cases that only need *a* model answer (sections 5–11, 13, 15) can use any provider or a
local OpenAI-compatible test server added as a **Custom Provider**. Error cases are made repeatable with a
wrong key (8.1), a non-existing model id (8.2), and a very long text on a small-context model (8.6). Real
providers are needed for 3.1, 3.3–3.6, 4.x (the real YouTube/BiliBili/article/file extraction) and one final run.

Start with a clean install unless a case says otherwise (Settings > App info > Storage > Clear data).

## Smoke run (P0 only)

Install over the previous build, then: 1.1 (fresh) or 1.2 (update) → 3.1, 3.3 → 4.1, 4.7, 4.12, 4.13 →
5.2, 5.3 → 6.3 → 7.2 → 8.1 → 9.1 → 10.1 → 11.1, 11.2, 11.4, 11.5, 11.7 → 12.3, 12.4 → 13.1, 13.4 →
14.1 → 15.2, 15.6 (tablet or landscape). Everything passes, no crash, no stuck spinner.

## Regression focus after the code moves

The batch-7 refactoring moved code without intending any change. The unit tests cover the moved logic,
but only a device shows wiring mistakes. Run these once (they are mostly `P0`/`P1` already):

| Moved code | Cases to run |
|-----------|--------------|
| Share/intent handling (`IncomingShare.kt`, both activities) | 13.1, 13.2, 13.4, 13.6, 13.7, 16.5, plus sharing a PDF and an image to the app (4.6, 4.9) |
| Home screen split into several files | 4.1, 4.14, 4.15, 5.3, 6.5, 7.2, 15.6, 15.7 |
| Instant dialog in its own file | 13.1, 13.3, 13.4, 13.5, 13.8 |
| Summary card rules (author and overview display) | 4.1 (author shown, overview not repeated), 4.13 (no author for pasted text), 5.3 |
| History grouping (`HistorySections.kt`), open entry kept on rotation | 11.3, 11.6, 11.8, 11.12, 15.2, 15.4, 15.5 |
| Text to speech chunking | 9.1 (very long summary) |
| Text file decoding | 4.12 (file with BOM, file with Windows line endings), 12.1, 12.2 |
| Width constants (`ContentMaxWidth` etc. replace repeated dp values) | 2.4, 13.8, 15.8, 15.9 (nothing should look different) |
| Self-hosted server path (`openAiCompatibleEndpoint`) | 3.7, 3.8 with LM Studio / Custom Provider (bare host and host with `/v1`) |
| BiliBili link helpers (moved out of the tool class) | 4.5 with a `bilibili.com/video/BV…` link and a `b23.tv` short link |
| Summary card action buttons (copy, share, save, speak) moved to `SummaryActionButtons.kt` | 9.1–9.4 and the buttons in the History detail (11.3) and the instant dialog (13.1) |
| Time and id sources (`Clock`, `IdGenerator`) injected into the summarize use case, History merge and profile creation | 5.6 (create a custom profile), 11.1, 11.2 (date of the merged entry is the newest summary), 11.12 |
| Injected IO dispatcher in the summarize, ask and import use cases | 4.1 (one summary end to end), 7.2 (cancel), 10.1 (ask), 12.1 (import) |
| YouTube client profiles, fallback chain and error handling (`YouTubeClient.kt`, `YouTubeFallback.kt`) | 4.1, 4.3, 4.4 (private/removed/no subtitles keep their texts), 8.4 (airplane mode says no internet), 4.17, 4.18 |
| Subtitle ranking, wanted subtitle language from the summary language, transcript language in `ExtractedContent` | 4.1, 4.3, 4.18, 6.3 |
| Shared link while History/Settings is open goes to Home (`backStackForStartAction`) | 13.9, 13.1, 13.4, 11.11 (Summarize again) |
| Profile ids moved to the model layer | 5.1, 5.5, 5.6, 11.9, 11.14 |

---

## 1. Install, upgrade and data migration

**1.1 `P0` Fresh install shows the tutorial.** Install, open. *Expected:* onboarding appears; finishing it
lands on Home; reopening the app does not show it again.

**1.2 `P0` Update keeps data (migration 2→3).** Install the *previous* build (2.0.0 or an older CI
build), add a provider, create 3 History entries (one with Short and Long of the same video), then
install this build **over** it. *Expected:* no crash on first start; provider, key, model and all
History entries are still there; old entries open in the detail view, show their summary, and can be
pinned/deleted; no profile label is wrong (older entries count as “Default”).

**1.3 `P2` Unreadable settings do not crash.** (Developer-only, optional) corrupt the settings file via
`adb run-as … ` and start the app. *Expected:* the app starts with default settings instead of
closing after the tutorial.

**1.4 `P2` Version label.** Settings footer shows `2.2.0` (CI builds: `2.2.0-ci<N>+<commit>`) and each new
CI build installs over the previous one.

---

## 2. Settings basics

**2.1 `P2` Theme.** Settings > Theme: light, dark, system, dynamic colour on/off. *Expected:* applies at
once; survives restart.

**2.2 `P2` Settings search.** Type “prompt”, “thumb”, “language” in the Settings search bar. *Expected:*
matching entries are listed; clearing the field restores the full list.

**2.3 `P2` Display > thumbnails.** Toggle “show thumbnails” off/on, then open History (see 11.15).

**2.4 `P1` Tablet width.** On D2 or in landscape, open Settings. *Expected:* the list is centred and not
wider than about 840 dp (it does not stretch edge to edge).

---

## 3. AI providers

**3.1 `P0` Add a provider with a key.** Settings > AI provider > choose OpenAI (or another), paste K1,
Save. *Expected:* Home top bar shows the provider/model indicator; the key is hidden after saving.

**3.2 `P2` Official URL is shown and editable.** Open a built-in provider dialog. *Expected:* its official
URL is displayed (editable); saving the unchanged URL keeps using the official endpoint.

**3.3 `P0` Test connection — good key.** In the dialog tap **Test**. *Expected:* success message; the model
list is filled in automatically from the provider.

**3.4 `P1` Test connection — wrong key (K3).** *Expected:* “credentials rejected” style message, not a
crash and not “no internet”.

**3.5 `P2` Test connection — wrong URL.** Use a typo path (e.g. `https://api.openai.com/xx`). *Expected:*
“endpoint not found” hint. With airplane mode on: “unreachable” message.

**3.6 `P1` Pasted version path.** For OpenRouter paste `https://openrouter.ai/api/v1`; for OpenAI paste
`https://api.openai.com/v1`. *Expected:* test and summaries work (no HTTP 404 “Not Found”).

**3.7 `P2` URL without a scheme.** For LM Studio/Ollama/Custom type `192.168.1.10:1234`, then
`mypc.local:11434`, then `api.example.com`. *Expected:* the first two are treated as `http://…`, the
third as `https://…`. *Unit-tested:* the url rules (`ProviderConfigUseCaseTest`); only the real server connection needs a device. *Unit-tested:* how the server address maps to the called path (`OpenAiCompatibleEndpointTest`); the device check is the real connection.

**3.8 `P1` Local server without key (K4).** Ollama: leave the key empty, test, summarize. *Expected:* works;
no “you have to provide an API key” error. LM Studio / Custom Provider: optional token is accepted
and not sent when blank. *Unit-tested:* how the server address maps to the called path (`OpenAiCompatibleEndpointTest`); the device check is the real connection.

**3.9 `P2` Models: add, edit, delete, reorder.** In the model dialog add a custom model id, rename it,
delete another, drag to reorder, then use **Reset to default**. *Expected:* each change is shown
immediately, survives restart; reset restores the built-in list and first model.

**3.10 `P1` Provider ordering and quick switcher.** Reorder providers in Settings. Long-press the indicator
on Home. *Expected:* quick switcher lists providers in your order; choosing one changes provider and
model on Home; the confirmation switch works.

**3.11 `P2` Key that cannot be decrypted.** (Hard to force; optional) restore the app data to another device.
*Expected:* a clear “saved key can no longer be read, enter it again” message, not “no key”.

**3.12 `P1` No provider selected.** Fresh install, skip provider setup, try to summarize. *Expected:* a
message asking you to select a provider; nothing is sent.

---

## 4. Summarizing every kind of content

For each case: paste/select the input on Home and tap **Summarize**. Expected for all: progress
shows “Fetching content…” then “Summarizing…”, a result card appears with title, author (when
known), provider/model, and the entry appears at the top of History.

**4.1 `P0` YouTube (C1), full URL.** *Expected:* card with overview, key points, summary, tags (Default
profile); title and channel name filled in.

**4.2 `P2` YouTube link forms.** `youtu.be/ID`, `m.youtube.com/watch?v=ID`, `youtube.com/shorts/ID`,
a link with `&t=30s`. *Expected:* all summarize the same video; in History they count as the same video
(see 11.2).

**4.3 `P2` Manual captions preferred (C2).** *Expected:* summary is based on the manual track (no obvious
auto-caption errors).

**4.4 `P1` YouTube special errors (C4).** Private / age-restricted / removed video. *Expected:* each gives
its own specific message (private, age-restricted, unavailable), not a generic failure.

**4.5 `P1` BiliBili (C5).** With subtitles: works. Without: a clear “no subtitles” style message. When
BiliBili asks for a login, the app opens Settings > third-party service. After login, retry works;
**Clear BiliBili Login** asks for confirmation and clears it. *Unit-tested:* link forms and video-id extraction (`BiliBiliLinkTest`), error mapping (`BiliBiliHttpFailureTest`); the live subtitles and login need the real site.

**4.6 `P1` PDF text (C7, D3).** Text PDF and scanned PDF on Android 13/14. *Expected:* both summarize; no
crash on scanned pages.

**4.7 `P0` Article URL (C6).** *Expected:* title and author from the page (or from the URL when the page
gives none). The title is never “Medium”, “Just a moment…” or “Text Input”. *Unit-tested:* which title and author are chosen (model, page, link, placeholders such as “Text Input”, “Medium”, “Just a moment…”) in `TitleAndAuthorTest`; the device check is that a real page gives sensible values.

**4.8 `P1` Blocked article.** *Expected:* the app falls back to the reader service and still summarizes;
airplane mode gives a network error, not a wrong-fallback message.

**4.9 `P1` Image (C10).** Pick from the gallery. *Expected:* text recognized and summarized (standalone
works offline; gms needs Play Services). An image with no text gives a clear message.

**4.10 `P2` Camera photo.** Use the camera button. *Expected:* same as 4.9.

**4.11 `P1` Word file (C8).** *Expected:* table cells, tabs and line breaks are in the summarized text (ask a
question about a table cell to confirm, see 10.x).

**4.12 `P0` `.txt` and `.md` files (C9).** Pick each. *Expected:* both are accepted by the picker and
summarized; `.md` headings/lists are not lost. Also try a `.txt` saved with a byte order mark (Notepad “UTF-8 with BOM”) and one with Windows line endings. *Expected:* no stray character at the start, lines separate normally. *Unit-tested:* `TextDecodingTest`, `FileKindTest`, `SupportedFileTypesTest` (every type the picker offers is readable).

**4.13 `P0` Plain text (C11).** Paste text. *Expected:* summary; title is generated by the model or derived,
never empty or “Text Input”. *Unit-tested:* title rules (`TitleAndAuthorTest`).

**4.14 `P1` Clipboard paste.** Tap the paste icon. *Expected:* input is filled; nothing is sent until you
tap Summarize.

**4.15 `P1` Empty input.** Tap Summarize with an empty field. *Expected:* nothing is sent, no crash.

**4.16 `P1` Unreadable file.** Pick a file, remove or lose access to it (move it/revoke permission), then
Summarize. *Expected:* “pick it again” message.

**4.17 `P2` YouTube extraction failure and fallback.** The app asks YouTube as the Android client first (exactly
as before), then as the full Android client without the API key, then as the iOS client; the first that works
wins. You cannot force a failure on demand, but every run logs it: in Logcat (tag `YouTubeExtractor`) a normal
video shows `Attempt ANDROID 20.10.38 +key for …: Success`. *Expected when YouTube refuses all of them:* “Could
not read this YouTube video. YouTube may have changed something…”, and **Show details** lists every attempt, for
example `ANDROID 20.10.38 +key · player API: HTTP 400 | ANDROID 20.10.38 no key · … | IOS 20.10.4 no key · …`. It
must **not** say “private / age-restricted / removed” unless YouTube really says so (a real private or removed
video: 4.4), a video without any subtitles still says “no transcript” (4.4), and without internet it says “no
internet” (8.4). *Unit-tested:* the whole policy (`YouTubeFallbackTest`, `YouTubeClientTest`); the device check is
that the first attempt succeeds for a normal video. If a YouTube case starts failing, check the client
constants in `YouTubeClient.kt` and report the Logcat lines (tag `YouTubeExtractor`).

**4.18 `P1` Subtitle choice with many auto-generated tracks.** Summarize `https://youtu.be/9UMpD5QMU-c`
(English video with automatic dubbing: 20 auto-generated subtitle tracks in other languages and one manual
`en-US`) with **Video language**. *Expected:* the summary is in **English** (before the fix it was Arabic,
because the first auto track is Arabic); Logcat (`YouTubeExtractor`) shows “Caption tracks for …; chosen
en-US”. Then choose **German** as summary language: *Expected:* still the English transcript (no manual
German track), summarized in German. Also try a video with only translated manual subtitles (C13): the track
YouTube marks as default is used. *Unit-tested:* the whole ranking (`YouTubeTrackChoiceTest`,
`YouTubeTranscriptToolTest`); the device check is that real YouTube answers contain the fields the code reads.

---

## 5. Summarization profiles

**5.1 `P1` List.** Home shows a profile selector (once more than one profile exists): Default, Technical
Deep Dive, Critical Analysis, Learning Mode, Technical Content Quality Evaluator, Quick Brief.

**5.2 `P0` Default = card.** Summarize C1 with Default. *Expected:* structured card (overview, key
points, tags).

**5.3 `P0` Other profile = free-form.** Same video with Quick Brief and with Critical Analysis.
*Expected:* answer follows each profile’s own format, Markdown is rendered (headings, bold, lists,
tables), no empty “overview/key points” sections.

**5.4 `P1` Selector is disabled while running**, and the selection survives restart. *Unit-tested:* when a result counts as out of date (`ResultStalenessTest`).

**5.5 `P1` Edit a preset.** Settings > Summarization profiles > Quick Brief > edit the prompt, save.
*Expected:* labelled **Edited**; Home uses the new prompt (visible in the answer style); **Reset to
preset** restores the original.

**5.6 `P1` Create / rename / delete a custom profile.** Name it, prompt “Answer in exactly 3 bullet
points.”, save, use it, then delete it. *Expected:* works; deleting the selected profile falls back
to Default; presets cannot be deleted.

**5.7 `P1` Default profile prompt.** Advanced Summary Setup > Base Prompt box follows the selected
profile; for Default it edits the Default prompt. “Append Custom Prompt” on/off changes whether the
extra text is added on top of any profile.

**5.8 `P2` Prompt tokens.** In a custom prompt put `[Language instructions]` and `[Length instructions]`.
*Expected:* language and length rules are placed where you put them (not duplicated at the end).

**5.9 `P1` Overview/key points/tags switch.** In a non-default profile switch **Overview, key points and
tags** on. *Expected:* the card layout appears for that profile; off returns to free-form.

**5.10 `P2` Share a profile.** Profile > Share. *Expected:* JSON text is shared. Copy it, then on the
profiles screen choose **Import profile from clipboard**. *Expected:* “Profile imported: name”; the
profile appears. Importing random text shows “clipboard does not contain a shared profile”. Importing
the same profile twice does not overwrite a built-in by accident.

---

## 6. Summary length and language

**6.1 `P1` Lengths.** Short / Middle / Long on a long article. *Expected:* clearly different lengths;
switching length after a result re-uses cached results where it has them and otherwise asks you to
summarize again.

**6.2 `P1` Length switch off.** Settings: turn “show length” off. *Expected:* the selector disappears and
the summary has no length constraint.

**6.3 `P0` Language choices.** On Home choose **Video language**, **App language**, then Japanese,
Spanish, Arabic. Summarize C1 each time. *Expected:* the summary is in the chosen language each time;
the same choice appears in Advanced setup and the instant dialog.

**6.4 `P2` Old setting is honoured.** (Upgrade only) with the old “Original language” switch on and no new
choice made, the summary language is the video language; after choosing, the new choice wins.

**6.5 `P1` Changing language marks the result stale.** After a result, change the language. *Expected:*
the app offers to summarize again (button changes), and the old result is not silently relabelled. *Unit-tested:* the stale rules for length, profile and language, including results restored from History (`ResultStalenessTest`); the device check is the button label changing.

---

## 7. Progress, cancel and concurrency

**7.1 `P1` Status text.** Watch “Fetching content…” → “Summarizing…”.

**7.2 `P0` Cancel.** Tap **Cancel** while running (once while fetching, once while summarizing).
*Expected:* stops at once; no error banner; nothing is added to History; you can start again.

**7.3 `P1` Replace a running request.** Start a long summary, change the link, tap Summarize again.
*Expected:* only the newer result appears; the old one never shows up later.

**7.4 `P1` Clear while running.** Tap clear during a request. *Expected:* result does not reappear.

**7.5 `P2` Status after an older request finishes.** (From instant dialog + app) start a summary in the
instant dialog, open the app and start another; *Expected:* the second keeps its own “Summarizing…”
status.

---

## 8. Errors

**8.1 `P0` Wrong key (K3).** *Expected:* “incorrect key” style banner; **Show details** shows provider,
model, URL and raw cause (no key in it).

**8.2 `P1` Unknown model.** Set a non-existing model id. *Expected:* “model not found”.

**8.3 `P2` Rate limit / quota.** (If you can trigger one.) *Expected:* rate-limit message.

**8.4 `P1` No network.** Airplane mode. *Expected:* a network message, not an invalid-key message.

**8.5 `P1` Text containing error-looking words.** Summarize a text that contains “500”, “quota”,
“unauthorized”. *Expected:* it summarizes normally (words in ordinary text are not mistaken for
errors).

**8.6 `P1` Context too long (C3 / K5).** *Expected:* a message that the content does not fit the model and
to choose a model with a larger context; the content is **not** truncated silently.

**8.7 `P2` Unknown/blocked errors keep a readable message.** No raw `Error from client:` or `Status code:
-1` prefixes anywhere.

---

## 9. Result card: speech, copy, share, save

**9.1 `P0` Text to speech.** Play on a long summary with Markdown (bold, bullets). *Expected:* reads the text
without saying “asterisk/hash”; long summaries do not fail; pause/stop works; changing speed works
(Settings); speech stops when a new result arrives. *Fixed:* a chunk sent to the speech engine could be one character over its limit; now it never is. Use a very long summary: it must read to the end and then stop. *Unit-tested:* `SpeechChunksTest`, `MarkdownTextTest`.

**9.2 `P2` Copy.** Copy summary and copy answer. *Expected:* clipboard holds the text.

**9.3 `P1` Share as Markdown.** Share a Default card and a free-form result. *Expected:* title, author,
source link, overview, key points, summary and tags (for the card); readable Markdown in the target app.

**9.4 `P1` Save as Markdown file.** Choose a location. *Expected:* `.md` file is created with the same text;
cancelling the file dialog does nothing.

---

## 10. Ask about a summary

**10.1 `P0` Ask.** After summarizing, ask “What is the main claim?”. *Expected:* answer appears; the box
scrolls above the keyboard.

**10.2 `P1` Follow-up uses history.** Ask a second question that depends on the first answer. *Expected:*
consistent with the earlier answer.

**10.3 `P1` Failed question.** Airplane mode, ask. *Expected:* the question stays with an error line; asking
again later works.

**10.4 `P2` Blank question** is ignored.

**10.5 `P2` Content gone.** After the app is restarted (content no longer in memory), the ask box is not
offered for old results.

**10.6 `P2` New input clears the chat.** Summarize something else. *Expected:* the old chat is gone.

**10.7 `P1` Table question (C8).** Ask about a specific table cell of the Word document. *Expected:* answered
correctly.

---

## 11. History

**11.1 `P0` Save.** Every successful summary appears at top (“Today”) with type icon and profile name.

**11.2 `P0` Same item merges.** Summarize C1 again with another profile, another language and another
length. *Expected:* **one** entry with several sections in the detail view, oldest first, the
newest expanded and the older collapsed; same result twice (same profile, language and length)
replaces itself instead of duplicating; the youtube link in another form (4.2) joins the same entry.

**11.3 `P1` Detail view.** Open an entry. *Expected:* header per section “Profile · Language · Length ·
date”; summary card with copy/share/speak; pin button; “Summarize again with other settings”.

**11.4 `P0` Remove one summary.** In a multi-section entry tap the delete icon on one section. *Expected:*
snackbar “Summary removed”; **Undo** brings it back in place. Removing the newest promotes the next
newest. Removing the only summary removes the whole entry (and closes the detail).

**11.5 `P0` Delete an entry by swipe.** *Expected:* entry removed with **Undo**; Undo restores it exactly (same
date, no merge with another entry of the same link).

**11.6 `P1` Pin / unpin.** In the detail view and from selection mode. *Expected:* pinned items show in a
“Pinned” section on top regardless of sort; pin survives summarizing the same item again.

**11.7 `P0` Multi-select.** Long-press an item, tap more. *Expected:* title shows “N selected”; back
button / back gesture / close icon leaves selection; **Pin**, **Unpin** and **Delete** apply to all;
bulk delete offers **Undo** that restores all of them.

**11.8 `P1` Sort.** Newest first, Oldest first, Title A–Z. *Expected:* order changes; pinned stay on top;
Title sort shows no date headers; list jumps to the top when sort/filter changes. *Unit-tested:* section keys and corner shapes for pinned / date / title sorting (`HistorySectionsTest`).

**11.9 `P1` Filters.** Type chips (video/article/…), pinned only, profile filter (All profiles + each
profile used). *Expected:* an entry with *any* result from the chosen profile matches; combining
filters narrows; the empty state explains an empty result.

**11.10 `P1` Search.** Search a word from a title, from a summary, an author. Search `%` and `_`.
*Expected:* normal words match; `%` and `_` only match those characters (not everything).

**11.11 `P1` Summarize again.** From the detail view. *Expected:* detail closes, Home opens with the source
filled in so you can change profile/language/length and run it.

**11.12 `P2` Date grouping.** Entries of today/yesterday/last week/older appear under the right headers. *Unit-tested:* date titles (`HistoryDateSectionTest`) and grouping (`HistorySectionsTest`).

**11.13 `P2` Large History.** (50+ entries, e.g. via import) scroll smoothly; pagination spinner at the end.

**11.14 `P2` Profile name shown.** The detail header of an entry made by a custom profile shows its name,
even after that profile was renamed or deleted (the stored name remains).

**11.15 `P2` Thumbnails.** YouTube entries show a small thumbnail (also imported/older ones); with the
switch off or offline, an icon is shown instead; Settings shows no excessive cache (cap 50 MB).

---

## 12. Import and export

**12.1 `P1` Import (C12).** Settings > Import video summaries > choose the backup. *Expected:* “Imported N.
Skipped M already in your history. K entries could not be read.”; imported items keep their original
date; repeating the import imports 0 and skips all.

**12.2 `P2` Import — bad file.** Choose a random JSON/text/image file and a file > 25 MB. *Expected:*
“not a valid backup” / “too large”, no crash. Also import a backup saved with a byte order mark (many Windows editors add one). *Expected:* it imports instead of “not a valid backup”. *Unit-tested:* `TextDecodingTest`, `VideoSummaryBackupParserTest`.

**12.3 `P0` Export.** Settings > Export History (with YouTube summaries). *Expected:* “History exported”; the
file opens as JSON with `video_id`, `video_url`, `video_title`, summary text.

**12.4 `P0` Export round trip.** Delete all History, import the file you exported. *Expected:* the YouTube
entries return with titles, authors, dates, summaries.

**12.5 `P2` Export with nothing to export.** Only article entries. *Expected:* “no YouTube video summaries to
export”, no file written.

**12.6 `P2` Export write failure.** Pick a read-only/full location. *Expected:* “could not be exported”.

---

## 13. Instant summary dialog and entry points

**13.1 `P0` Share sheet.** In YouTube/Chrome share a link to UTubeSummary (instant). *Expected:* dialog
opens over the other app, shows progress, then the result with provider/model. *Unit-tested:* which text or file link a share carries and when the app starts summarizing by itself (`IncomingShareTest`); the device check is the real hand-over.

**13.2 `P1` Text selection toolbar.** Select text in any app > “Summarize” (if offered). *Expected:* same as
13.1.

**13.3 `P1` Pickers.** In the dialog change profile and language. *Expected:* changing one summarizes the
text again.

**13.4 `P0` Open in app.** Tap open-in-app after a long summary. *Expected:* the main app opens with the same
result (no 1 MB intent error, no empty screen), including the other lengths.

**13.5 `P2` Dismiss.** Close the dialog. *Expected:* it does not stay in recents; the next instant summary
never shows the previous result.

**13.6 `P2` Single instance.** Share a link while the app is already open. *Expected:* the existing app
instance is reused, no second app window.

**13.7 `P1` Clipboard shortcut.** Use “Summarize from clipboard”. *Expected:* the field is filled; nothing is
sent until you tap Summarize. A web page link `utubesummary://clipboard` in a browser does nothing. *Unit-tested:* only shares and process-text are read by the dialog, never the clipboard (`IncomingShareTest`, `ArchitectureRulesTest` guards the layers).

**13.8 `P1` Landscape dialog.** Rotate to landscape during an instant summary (or open it in landscape).
*Expected:* the dialog uses up to about 90% of the height, content is scrollable; in portrait about
55%.

**13.9 `P1` Share while another screen is open.** Open **History**, then share a YouTube link to the app (not the
instant dialog). *Expected:* the app shows **Home** with the link in the input and the current Profile, Language
and Length selectors, so they can be changed before summarizing; pressing Back on Home leaves the app (History is
not left behind). Repeat with **Settings** and **Advanced Summary Setup** open, with the instant dialog's “Open in
app” while History is open, and while Home is already open (unchanged). During the first-run tutorial the link
appears on Home after the tutorial. *Unit-tested:* the back-stack rule (`NavBackStackTest`); the device check is the
real hand-over.

---

## 14. Process death and rotation

**14.1 `P0` File survives process death.** Pick a document, then in Developer options enable “Don’t keep
activities” (or `adb shell am kill io.github.utubesummary`), return. *Expected:* the file name and
source are still in the input; Summarize works. Same for a camera photo.

**14.2 `P1` Rotation keeps input.** Type a URL, rotate twice. *Expected:* text and chosen profile/length/
language are kept; a running summary continues.

**14.3 `P1` Background during a summary.** Press Home while summarizing, come back. *Expected:* result appears
or the status is correct; no stuck spinner.

**14.4 `P1` Back navigation.** From Settings sub-pages back to Home; from History detail back; no unexpected
exits.

---

## 15. Tablet and landscape layouts (D2 and a phone in landscape)

**15.1 `P1` Wide rule.** Phone portrait: single column everywhere. Phone landscape (≥ 600 dp wide) and
tablet: two-pane. Tablet portrait ≥ 840 dp wide: two-pane; a small tablet (600 dp) in portrait:
single column.

**15.2 `P0` History two-pane.** List on the left (about 40%), detail on the right. *Expected:* with nothing
selected the right pane says “Select a summary to read it here”; tapping an item shows it on the
right with no bottom sheet; the pin, remove-with-undo, text-to-speech, copy/share and “Summarize
again” buttons all work in the pane; search, filter and sort keep working; multi-select long-press
still works (tapping items while selecting toggles selection and does not open them). *Unit-tested:* the wide-layout rule (`WindowLayoutTest`); everything else here is visual.

**15.3 `P1` Remove last summary in two-pane.** Remove the only section of the selected entry. *Expected:* the
entry disappears and the right pane returns to the hint; Undo restores and re-selects it.

**15.4 `P1` Rotate while an entry is selected.** Open an entry (sheet on a phone, right pane on a wide
screen), rotate. *Expected:* the same entry is shown again after the rotation (also when the rotation
switches between sheet and pane); if the entry was deleted in the meantime nothing is selected and no
error appears. Closing the sheet or pane and rotating keeps it closed.

**15.5 `P1` Switch layouts.** Phone: open an entry (sheet), rotate to wide layout: sheet is replaced by the
pane. Rotate back: the sheet appears again for the selected entry. *Expected:* no crash.

**15.6 `P0` Home two-column.** Input, selectors (profile, language, length) on the left; result + question box
on the right; each side scrolls on its own; the floating buttons stay visible; the right side
leaves room at the bottom so the floating button does not cover the last lines.

**15.7 `P1` Home flow.** Paste a link, Summarize, watch progress and result appear on the right, ask a
question, change profile (result marked stale), run again, clear input. Keyboard and the
question box remain usable in landscape.

**15.8 `P2` Large screen.** On a 12–13” tablet the content is centred and does not stretch beyond about
1200 dp (Home) / uses the 40/60 split (History).

**15.9 `P1` Settings, Advanced setup and profiles screens** on a tablet: content is centred with a
maximum width (about 840 dp); dialogs are not clipped.

---

## 16. Privacy and security

**16.1 `P1` No analytics.** Use the app with a traffic monitor (e.g. PCAPdroid or a proxy). *Expected:*
requests only go to your provider, YouTube/BiliBili for content and thumbnails, the article site,
and (only when a site blocks the app) the reader service.

**16.2 `P1` Keys hidden.** The key never shows in clear text in the UI, Show details or share text.

**16.3 `P2` BiliBili cookie.** After logging in, (rooted/adb) the stored value is not plain text; upgrading
from an older build migrates it and you stay logged in.

**16.4 `P1` Nothing stored for chat.** After force-stopping the app, previous questions and the transcript
are gone (only History summaries remain).

**16.5 `P1` No auto-send from other apps.** Another app (or `adb shell am start`) cannot make the app read the
clipboard and summarize it. The removed `INSTANT_SUMMARIZE` action does nothing.

---

## 17. Languages, accessibility, appearance

**17.1 `P1` Languages.** Switch the phone language to German, Spanish, French, Italian, Portuguese, Arabic
(RTL), Japanese, Chinese, Czech, Turkish, Ukrainian. *Expected:* new texts (profiles, language,
progress, pin, export, “select a summary…”) are translated or at least readable English; no layout
breaks; Arabic mirrors correctly in both layouts.

**17.2 `P1` TalkBack.** Home, History and detail are navigable; icons have meaningful descriptions
(delete, pin, share, play); decorative icons are not read aloud; selected rows are announced as
selected.

**17.3 `P2` Font scale and display size.** Largest font: no clipped buttons in profile selector, History
chips, detail header.

**17.4 `P2` Dark and light, dynamic colour** on every screen listed in this plan.

---

## 18. Final regression sweep (about 15 minutes)

Run once on D1 in portrait and once on D2: install over previous → open → summarize C1 (Default) →
summarize C1 again with Quick Brief → open History → check 2 sections → pin → filter by Quick Brief →
export → delete all → import → instant summary from the share sheet → rotate → Settings tour →
no crash, no stuck spinner.

| Area | Result | Notes |
|------|--------|-------|
| 1 Install/migration | | |
| 2 Settings | | |
| 3 Providers | | |
| 4 Content types | | |
| 5 Profiles | | |
| 6 Length/language | | |
| 7 Progress/cancel | | |
| 8 Errors | | |
| 9 Speech/share/save | | |
| 10 Ask | | |
| 11 History | | |
| 12 Import/export | | |
| 13 Instant dialog | | |
| 14 Process death | | |
| 15 Tablet/landscape | | |
| 16 Privacy | | |
| 17 Accessibility/i18n | | |

---

## What the unit tests already cover

Run: `./gradlew testGmsDebugUnitTest` (CI does it on every push).

| Area | Test classes |
|------|--------------|
| Summarize use case (also: result stamped with the injected clock, slow and cancelled requests in virtual time): provider choice, profile prompt, structured vs free-form, language, length, no secret in errors, cancellation, phase | `SummarizeContentUseCaseImplTest`, `SummarizeContentUseCaseTest`, `ActiveProviderTest`, `PromptsTest`, `SummaryLanguageTest`, `SummarizationProgressTest` |
| Summary view model: cancel, replace, stale results, caching, length switch, chat | `SummaryViewModelTest`, `AskAboutContentUseCaseImplTest`, `QuestionPromptTest` |
| Profiles: built-ins, editing/resetting, custom, sharing/import | `BuiltInProfilesTest`, `ResolveProfilesTest`, `UpdateSummaryProfilesUseCaseTest` (also generated ids), `ProfileShareTest` |
| History model and storage: merge by variant, alternatives, remove, pin, escape search, undo restore | `HistoryResultsTest`, `HistoryRepositoryImplTest` (also merge timestamp), `HistoryMapperTest`, `HistoryLengthResultsConverterTest`, `RemoveHistoryResultUseCaseTest`, `HistoryUseCasesTest`, `HistoryFilterTest` |
| History view model: search, filters, sort, selection, bulk delete/pin, reopening an entry by id | `HistoryViewModelTest` |
| Import/export | `VideoSummaryBackupParserTest`, `VideoSummaryBackupWriterTest`, `ImportVideoSummariesUseCaseTest`, `HistoryUseCasesTest` (export) |
| Providers: URL normalization, called path of self-hosted servers, config use case, connection test requests, model parsing | `ProviderConfigUseCaseTest`, `OpenAiCompatibleEndpointTest`, `ProviderConfigTest`, `ProviderConnectionRequestTest`, `ProviderDialogButtonsTest`, `SettingsViewModelTest` |
| Errors | `LlmErrorParserTest`, `SummaryExceptionTest`, `BiliBiliHttpFailureTest` |
| Content tools | `YouTubeTranscriptToolTest`, `YouTubeTrackChoiceTest`, `YouTubeClientTest`, `YouTubeFallbackTest`, `BiliBiliLinkTest`, `ArticleExtractorToolTest`, `DocxTextBuilderTest`, `FileKindTest`, `YouTubeVideoIdTest`, `YouTubeThumbnailTest` |
| Sharing, Markdown, handoff | `ShareTextTest`, `MarkdownTextTest`, `SummaryHandoffTest`, `InMemorySourceTextStoreTest` |
| Settings storage / security | `UserPreferencesCompatibilityTest`, `SecureValueCodecTest`, `StringListConverterTest` |
| Caches, executor | `BoundedLruCacheTest`, `GeminiRequestTest` |
| Layout and display rules | `WindowLayoutTest`, `HistorySectionsTest`, `HistoryDateSectionTest`, `ResultStalenessTest`, `SummaryCardRulesTest`, `ReorderableListStateTest`, `LlmSwitcherStateTest` |
| Title and author choice | `TitleAndAuthorTest`, `ArticleExtractorToolTest` |
| Text to speech text, text files, picker types | `SpeechChunksTest`, `MarkdownTextTest`, `TextDecodingTest`, `SupportedFileTypesTest`, `FileKindTest` |
| Share intents (what is summarized, auto start) and the back stack after a share | `IncomingShareTest`, `NavBackStackTest` |
| Search state | `SettingsSearchStateTest`, `HistorySearchStateTest` |
| Layering of the code (ui/vm/domain/model imports) | `ArchitectureRulesTest` |

**Not unit-testable today (so only the manual cases above cover them):** the Koog agent graph itself in
`SummarizationStrategy` (the title/author choice is now tested separately), the real network behaviour of
`YouTubeTranscriptTool`, `BiliBiliSubtitleTool` and `ArticleExtractorTool` (their parsing is tested, the
live sites are not), `FileExtractorTool` (PDF/OCR need Android), the Compose screens (History two-pane, Home
two-column, instant dialog), Room migration 2→3, Keystore encryption on a real device, text to speech,
the share sheet and the file/photo pickers.

## Known limitations (not defects of this test plan)

- The language of the chosen subtitles is carried inside the app (`ExtractedContent.languageCode`) but not yet shown
  to the user and not yet given to the model; a wrong-language summary for “Video language” can still
  happen with a model that ignores the language rule (parked on purpose).
- The active row of the History list is not highlighted in the two-pane layout (the row style is the
  multi-select check mark).
- Room schema files are not exported, so there is no automated migration test; case 1.2 is the check.
- The summary-card author rule and the article-extractor author rule are two similar but separate lists;
  an author shown in one place may be hidden in another.
- Real YouTube/BiliBili/article extraction can break when those sites change; keep one live check in every
  release run (4.1, 4.5, 4.7).
