# Screenshot checklist for the user guide

The [user guide](../user_guide.md) links to the 37 pictures below. They do **not exist yet**; capture them on a phone
and save them in this folder with **exactly** these file names. Until then the guide still reads correctly because
every picture has a text description.

## Rules

- **Device:** one phone in portrait for all pictures except 33 and 34 (tablet or phone in landscape). Use the same
  phone for all.
- **App language:** English. **Theme:** light (system theme off), except where a row says otherwise. Optionally add
  dark versions later as `NN-name-dark.png`.
- **Format:** PNG, about 1080 px wide (a normal phone screenshot). Plain screenshots, no frames or arrows.
- **Privacy:** never show a real API key, a personal link, an account name or a BiliBili login. Blur or crop if in
  doubt. Use a **public** example: a well-known public YouTube video with subtitles and a Wikipedia article.
- **Status bar:** if possible use the demo mode of the phone (full battery, no notifications).
- **Setup once:** configure a provider with a **test key that you revoke afterwards**, or use a local server, so the
  summaries can run. Summarize the same video three times (profiles **Default**, **Quick Brief** and **Technical
  Deep Dive**) so that History has an entry with several summaries.

## The shots, in the order of the guide

| # | File | Screen and state | What to do | Guide section |
|---|---|---|---|---|
| 1 | `01-onboarding-welcome.png` | Tutorial page 1 | Settings → Tutorial, stay on the first page | 1.2 |
| 2 | `02-onboarding-providers.png` | Tutorial page 2 (AI Providers & Quick Switcher) | Tap **Next** once | 1.2 |
| 3 | `03-provider-dialog-key.png` | **AI Provider** dialog, OpenAI selected, key field filled (dots) | Settings → AI Provider, tap OpenAI, paste a key | 2.2 |
| 4 | `04-provider-dialog-test-success.png` | Same dialog after **Test** succeeded (message with the number of models) | Tap **Test** | 2.2 |
| 5 | `05-provider-dialog-local.png` | **AI Provider** dialog, Ollama or LM Studio selected, address filled | Select the provider, type the address | 2.3 |
| 6 | `06-model-dialog.png` | **Manage Models** dialog with a few models, one selected | Settings → Select Model | 2.4 |
| 7 | `07-home-provider-sheet.png` | Bottom sheet **Select AI Provider** with the models below | On Home, tap the provider/model badge | 2.5 |
| 8 | `08-quick-switch.png` | **Quick Switch** popup while dragging | Long-press the badge and keep the finger down | 2.5 |
| 9 | `09-home-empty.png` | Home, empty input, clipboard button | Fresh start of the app | 3 |
| 10 | `10-home-input-link.png` | Home with a YouTube link in the input (badge visible), profile chip, language chip and Short/Middle/Long | Paste a public YouTube link | 3.1 |
| 11 | `11-home-attachments-menu.png` | The **+** menu open (Image, Camera, Document) | Tap **+** | 3.3 |
| 12 | `12-home-progress.png` | Home while **Summarizing…** with **Cancel** | Tap the check button and capture quickly | 4.1 |
| 13 | `13-bilibili-login.png` | **BiliBili Login** sheet (sign-in page, **do not sign in**) | Settings → BiliBili Account | 4.3 |
| 14 | `14-home-error-details.png` | An error under the input with **Show details** expanded | Use an invalid key or a video without subtitles, tap **Show details** (blur any key) | 4.9 |
| 15 | `15-home-selectors.png` | The **Profile** menu open with the built-in profiles | Tap the profile chip | 5.1 |
| 16 | `16-home-language-menu.png` | The **Language** menu open | Tap the language chip | 5.2 |
| 17 | `17-profiles-list.png` | **Summarization profiles** list | Settings → Advanced Summary Setup → Summarization profiles | 5.4 |
| 18 | `18-profile-editor.png` | Profile editor (**Name**, **Base prompt**, structured-output switch, Save/Share/Delete) | Tap the **+** button | 5.4 |
| 19 | `19-advanced-setup.png` | **Advanced Summary Setup** (base prompt, **Append Custom Prompt** switch) | Settings → Advanced Summary Setup | 5.5 |
| 20 | `20-advanced-final-prompt.png` | Same screen scrolled to **Final Prompt Preview** | Scroll down | 5.5 |
| 21 | `21-home-result-card.png` | Result card with title, tags, overview, key points | After a **Default** summary | 6 |
| 22 | `22-home-result-actions.png` | The action buttons row under a summary (speaker, link, copy, share, save) | Scroll to the buttons | 6.1 |
| 23 | `23-home-question-box.png` | **Ask about this content** with one question and answer | Ask a short question | 6.2 |
| 24 | `24-share-sheet.png` | Android share sheet showing **UTubeSummary** and **Instant Summarize** | In YouTube tap Share | 7.1 |
| 25 | `25-text-selection-menu.png` | Text selection menu with **Instant Summarize** | Select text in a browser | 7.2 |
| 26 | `26-instant-dialog.png` | Instant dialog with the profile/language chips and a finished summary | Share a link to **Instant Summarize** | 7.3 |
| 27 | `27-history-list.png` | **History** with several entries (thumbnails, badges, one pinned) | Open History | 8 |
| 28 | `28-history-filters.png` | History filter chips and a search text typed | Type a word in the search bar | 8.1 |
| 29 | `29-history-detail.png` | Entry sheet with several summaries and the **Update** button | Tap the entry that has three summaries | 8.2 |
| 30 | `30-history-selection.png` | History in selection mode (three rows checked, count in the top bar) | Long-press a row, tick two more | 8.4 |
| 31 | `31-history-update-confirm.png` | Dialog **Update this entry?** | Open a video entry, tap **Update** | 8.6 |
| 32 | `32-history-update-progress.png` | **Updating 1/3…** with **Cancel** | Confirm the update and capture quickly | 8.6 |
| 33 | `33-tablet-home.png` | Home in two columns with a result on the right | Tablet or phone in landscape | 9 |
| 34 | `34-tablet-history.png` | History in two columns with an open entry on the right | Tablet or phone in landscape | 9 |
| 35 | `35-settings-top.png` | **Settings**, top (search bar, Language, Theme, Dynamic Color, Video thumbnails, AI items) | Open Settings | 10 |
| 36 | `36-theme-dialog.png` | **Theme** dialog (System, Light, Dark) | Settings → Theme | 10.1 |
| 37 | `37-settings-bottom.png` | **Settings**, bottom (BiliBili Account, Auto Extract, Import, Export, Help, version) | Scroll down | 10.5 |

## After capturing

1. Copy the 37 files into `docs/images/` (same folder as this file).
2. Check every picture for secrets and personal data.
3. Commit them (`docs: screenshots for the user guide`). The guide needs no other change.
4. Optional: delete this file when you no longer need it.
