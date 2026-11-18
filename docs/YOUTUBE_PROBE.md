# YouTube probe (PowerShell)

`tools/youtube_probe.ps1` checks which YouTube "client identities" can still fetch subtitles. The
result decides which fallback clients the app tries when YouTube refuses its normal request. You run it
**once** on a Windows computer and paste the result into the chat.

What it does: for every video you name it downloads the watch page (to see whether the public API
key is found), then sends the same player request the app sends, as five different clients, with and
without that key, and downloads the subtitle text of the chosen track. It contacts only `youtube.com`.
It sends nothing except normal requests from your computer, so YouTube sees your IP address like any
visit. The result contains public video ids, client names, HTTP codes, language codes and the first 60
characters of a subtitle line. It never prints the API key or any cookie. It changes nothing on your
computer.

## 1. Get the script
- If you have the repository: `git pull` the branch `ccr-cad0612b-nqwuv3`; the file is `tools\youtube_probe.ps1`.
- Otherwise open the file on GitHub, click **Raw**, and save it as `youtube_probe.ps1` (keep the
  `.ps1` ending; choose "All files" in the save dialog).
- If Windows says the file came from another computer, run this once in PowerShell
  (adjust the path): `Unblock-File .\tools\youtube_probe.ps1`

## 2. Open PowerShell
Start menu → type **PowerShell** → open **Windows PowerShell** (or PowerShell 7 if you have it).
You do not need administrator rights. Go to the folder that holds the file, for example:

```powershell
cd C:\Users\<you>\source\UTubeSummary
```

## 3. Run it
This form works even when Windows blocks scripts, and it does not change any setting:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\youtube_probe.ps1
```

Add your own videos for the cases the three defaults do not cover (they are
`9UMpD5QMU-c`, `dQw4w9WgXcQ` and `jNQXAC9IJRY`). The video id is the 11 characters after `v=` or
after `youtu.be/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\youtube_probe.ps1 `
  -VideoIds 9UMpD5QMU-c,dQw4w9WgXcQ,<only-translated-subtitles-id>,<age-restricted-id>,<private-id>
```

It takes one to three minutes (it waits a little between requests on purpose) and prints progress.
At the end it prints a table and saves everything to `youtube_probe_result.txt` in the folder where
you ran it.

## 4. Read the table
One row per video × client × "with key / no key":

| Column | Meaning |
|---|---|
| `Status` | HTTP code of the player request (200 = accepted, 0 = no answer) |
| `Playability` | `OK` = playable; `LOGIN_REQUIRED` / `UNPLAYABLE` / `ERROR` = YouTube refuses the video |
| `Tracks` / `Languages` | number of subtitle tracks and their languages, `(auto)` = auto-generated |
| `Subtitles` | `OK <n> chars: …` = the subtitle text was downloaded; `EMPTY (HTTP …)` = the address was refused or empty |

The lines above the table tell whether the API key was found in the watch page (`present` /
`absent`) for the two page variants.

## 5. What to do with the result
1. **Paste the whole output (or the contents of `youtube_probe_result.txt`) into the chat.**
2. If most rows fail, run it once more from another network (for example your phone's hotspot) and
   paste that too. A block by your IP address looks different from a block of one client.
3. Delete `youtube_probe_result.txt` afterwards (it is listed in `.gitignore`, so it is never committed).

What I do with it:

| You see | Decision |
|---|---|
| `ANDROID_LEGACY` has `OK … chars` for the normal videos | it stays the first client |
| `ANDROID_LEGACY` fails but `ANDROID_FULL` works | `ANDROID_FULL` becomes first, legacy second |
| a client works for the translated-only or age-restricted video where Android does not | that client becomes the second fallback |
| the "no key" rows work as well as the "with key" rows | the app may try without the key when the page has none |
| every client fails for every video | not a client problem (IP or region block, or a YouTube-wide change); no client list fixes it, and I say so instead of guessing |

## Troubleshooting
- *"running scripts is disabled on this system"*: use the command from step 3 (`-ExecutionPolicy Bypass`).
- *"cannot be loaded because it is not digitally signed"* after downloading: `Unblock-File` (step 1).
- *Many rows with `Status 0` and `no answer`*: no internet, a proxy or firewall that blocks the request, or
  YouTube blocking your IP; try another network.
- The script was written for Windows PowerShell 5.1 and PowerShell 7; if it stops with a red error, paste
  the error text into the chat.
