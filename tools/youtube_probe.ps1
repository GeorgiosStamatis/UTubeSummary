<#
.SYNOPSIS
  Checks which YouTube "client identities" can still fetch subtitles, so the app's fallback list can be chosen.

.DESCRIPTION
  For every video id and every candidate client this script asks YouTube's player API (the same request the
  UTubeSummary app makes), reads the subtitle tracks and downloads the subtitle text of the first suitable
  track. Each client is tried with and without the public API key that YouTube puts in its watch page.

  It talks only to youtube.com. It sends nothing except normal requests from your computer (so YouTube sees
  your IP address). The result contains public video ids, client names, HTTP codes, language codes and the
  first 60 characters of a subtitle line. It never prints the API key or any cookie.

  Works with Windows PowerShell 5.1 (built into Windows 10/11) and PowerShell 7.

.PARAMETER VideoIds
  Videos to test. Add one that only has translated subtitles, one that is age-restricted and one that is
  private to cover those cases.

.PARAMETER OutFile
  Text file that receives the result (default: youtube_probe_result.txt in the current folder).

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\youtube_probe.ps1

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\youtube_probe.ps1 -VideoIds 9UMpD5QMU-c,dQw4w9WgXcQ
#>
param(
    [string[]]$VideoIds = @('9UMpD5QMU-c', 'dQw4w9WgXcQ', 'jNQXAC9IJRY'),
    [string]$OutFile = 'youtube_probe_result.txt',
    [int]$TimeoutSec = 30
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 does not use TLS 1.2 by default.
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
} catch {
    # PowerShell 7 on other platforms: nothing to do.
}

# The candidate clients. ANDROID_LEGACY is what the app sends today (no User-Agent, no extra fields).
$Clients = @(
    @{ Label = 'ANDROID_LEGACY'; Name = 'ANDROID'; Version = '20.10.38'; UserAgent = $null; NameId = $null
       Client = @{}; Context = @{} },
    @{ Label = 'ANDROID_FULL'; Name = 'ANDROID'; Version = '20.10.38'
       UserAgent = 'com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip'; NameId = 3
       Client = [ordered]@{ androidSdkVersion = 30; osName = 'Android'; osVersion = '11'; hl = 'en'; gl = 'US' }; Context = @{} },
    @{ Label = 'IOS'; Name = 'IOS'; Version = '20.10.4'
       UserAgent = 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)'; NameId = 5
       Client = [ordered]@{ deviceMake = 'Apple'; deviceModel = 'iPhone16,2'; osName = 'iPhone'; osVersion = '18.3.2.22D82'; hl = 'en'; gl = 'US' }; Context = @{} },
    @{ Label = 'TV_EMBEDDED'; Name = 'TVHTML5_SIMPLY_EMBEDDED_PLAYER'; Version = '2.0'
       UserAgent = 'Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15'; NameId = 85
       Client = [ordered]@{ hl = 'en'; gl = 'US' }; Context = @{ thirdParty = @{ embedUrl = 'https://www.youtube.com/' } } },
    @{ Label = 'WEB'; Name = 'WEB'; Version = '2.20250312.04.00'
       UserAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36'; NameId = 1
       Client = [ordered]@{ hl = 'en'; gl = 'US' }; Context = @{} }
)

# What the app's HTTP client sends for the watch page (no browser identity) and a browser-like variant
# with the consent cookie, to see whether the API key can be found in both.
$PageVariants = @(
    @{ Label = 'app-like'; UserAgent = 'Dalvik/2.1.0 (Linux; U; Android 14; Pixel 8 Build/AP1A.240305.019.A1)'; Cookie = $null },
    @{ Label = 'browser+consent'; UserAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36'; Cookie = 'SOCS=CAI' }
)

function Get-ErrorStatus {
    param($ErrorRecord)
    try {
        $response = $ErrorRecord.Exception.Response
        if ($null -ne $response -and $null -ne $response.StatusCode) {
            return [int]$response.StatusCode
        }
    } catch { }
    return 0
}

# Returns @{ Status = <int>; Body = <string or $null>; Error = <string or $null> }. Never throws.
function Send-Request {
    param(
        [string]$Uri,
        [string]$Method = 'GET',
        [hashtable]$Headers = @{},
        [string]$UserAgent = $null,
        [string]$Body = $null
    )
    $params = @{ Uri = $Uri; Method = $Method; UseBasicParsing = $true; TimeoutSec = $TimeoutSec; Headers = $Headers }
    if ($UserAgent) { $params['UserAgent'] = $UserAgent }
    if (-not [string]::IsNullOrEmpty($Body)) {
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($Body)
        $params['ContentType'] = 'application/json'
    }
    try {
        $response = Invoke-WebRequest @params
        return @{ Status = [int]$response.StatusCode; Body = [string]$response.Content; Error = $null }
    } catch {
        return @{ Status = (Get-ErrorStatus $_); Body = $null; Error = $_.Exception.Message }
    }
}

function Get-ApiKey {
    param([string]$VideoId, $Variant)
    $headers = @{ 'Accept-Language' = 'en-US,en;q=0.9' }
    if ($Variant.Cookie) { $headers['Cookie'] = $Variant.Cookie }
    $result = Send-Request -Uri "https://www.youtube.com/watch?v=$VideoId" -Headers $headers -UserAgent $Variant.UserAgent
    $key = $null
    if ($result.Body) {
        $match = [regex]::Match($result.Body, '"INNERTUBE_API_KEY"\s*:\s*"([^"]+)"')
        if ($match.Success) { $key = $match.Groups[1].Value }
    }
    return @{ Status = $result.Status; Key = $key }
}

function New-PlayerBody {
    param($Client, [string]$VideoId)
    $clientObject = [ordered]@{ clientName = $Client.Name; clientVersion = $Client.Version }
    foreach ($entry in $Client.Client.GetEnumerator()) { $clientObject[$entry.Key] = $entry.Value }
    $context = [ordered]@{ client = $clientObject }
    foreach ($entry in $Client.Context.GetEnumerator()) { $context[$entry.Key] = $entry.Value }
    $body = [ordered]@{ context = $context; videoId = $VideoId }
    return ($body | ConvertTo-Json -Depth 10 -Compress)
}

function Get-ClientHeaders {
    param($Client)
    $headers = @{}
    if ($null -ne $Client.NameId) {
        $headers['X-YouTube-Client-Name'] = [string]$Client.NameId
        $headers['X-YouTube-Client-Version'] = $Client.Version
    }
    return $headers
}

function Get-TrackText {
    param($Track, $Client)
    $url = [string]$Track.baseUrl
    if ($url.Contains('fmt=srv3')) { $url = $url.Replace('fmt=srv3', 'fmt=json3') }
    elseif ($url.Contains('?')) { $url = "$url&fmt=json3" }
    else { $url = "$url?fmt=json3" }
    $headers = Get-ClientHeaders $Client
    $headers['Accept-Language'] = 'en,en;q=0.9'
    $result = Send-Request -Uri $url -Headers $headers -UserAgent $Client.UserAgent
    $text = ''
    if ($result.Body) {
        try {
            $json = $result.Body | ConvertFrom-Json
            $parts = New-Object System.Collections.Generic.List[string]
            foreach ($item in $json.events) {
                if ($null -ne $item.segs) {
                    foreach ($seg in $item.segs) { if ($null -ne $seg.utf8) { $parts.Add([string]$seg.utf8) } }
                }
            }
            $text = ($parts -join '').Replace("`n", ' ').Trim()
        } catch { $text = '' }
    }
    return @{ Status = $result.Status; Text = $text }
}

# One probe: a client asks for a video, optionally with the API key.
function Test-Client {
    param($Client, [string]$VideoId, [string]$Key, [string]$KeyMode)
    $row = [ordered]@{
        Video = $VideoId; Client = $Client.Label; Key = $KeyMode; Status = 0
        Playability = '-'; Tracks = 0; Languages = '-'; Subtitles = '-'
    }
    $uri = 'https://www.youtube.com/youtubei/v1/player'
    if ($Key) { $uri = $uri + '?key=' + $Key }
    $headers = Get-ClientHeaders $Client
    $result = Send-Request -Uri $uri -Method 'POST' -Headers $headers -UserAgent $Client.UserAgent -Body (New-PlayerBody $Client $VideoId)
    $row.Status = $result.Status
    if (-not $result.Body) {
        $row.Playability = 'no answer'
        return [pscustomobject]$row
    }
    try {
        $json = $result.Body | ConvertFrom-Json
    } catch {
        $row.Playability = 'not JSON'
        return [pscustomobject]$row
    }
    $status = [string]$json.playabilityStatus.status
    if (-not $status) { $status = '?' }
    $row.Playability = $status
    $tracks = @()
    if ($null -ne $json.captions -and $null -ne $json.captions.playerCaptionsTracklistRenderer) {
        $tracks = @($json.captions.playerCaptionsTracklistRenderer.captionTracks)
    }
    $tracks = @($tracks | Where-Object { $null -ne $_ })
    $row.Tracks = $tracks.Count
    if ($tracks.Count -gt 0) {
        $labels = foreach ($track in $tracks) {
            if ($track.kind -eq 'asr') { "$($track.languageCode)(auto)" } else { [string]$track.languageCode }
        }
        $row.Languages = ($labels -join ',')
        # The same simple choice the app makes first: an auto-generated track, else English, else the first.
        $chosen = $tracks | Where-Object { $_.kind -eq 'asr' } | Select-Object -First 1
        if ($null -eq $chosen) { $chosen = $tracks | Where-Object { $_.languageCode -eq 'en' } | Select-Object -First 1 }
        if ($null -eq $chosen) { $chosen = $tracks | Select-Object -First 1 }
        $download = Get-TrackText $chosen $Client
        if ($download.Text.Length -gt 0) {
            $preview = $download.Text
            if ($preview.Length -gt 60) { $preview = $preview.Substring(0, 60) }
            $row.Subtitles = "OK $($download.Text.Length) chars: $preview"
        } else {
            $row.Subtitles = "EMPTY (HTTP $($download.Status))"
        }
    }
    return [pscustomobject]$row
}

$lines = New-Object System.Collections.Generic.List[string]
function Write-Both {
    param([string]$Text)
    Write-Host $Text
    $lines.Add($Text)
}

# Prints how YouTube describes the subtitle tracks and the audio tracks of a video. This shows which
# marker tells the original spoken language from automatic dubbing/translation tracks.
function Show-TrackDetail {
    param($Client, [string]$VideoId)
    Write-Both ("Track detail for {0} (asked as {1}):" -f $VideoId, $Client.Label)
    $result = Send-Request -Uri 'https://www.youtube.com/youtubei/v1/player' -Method 'POST' -Headers (Get-ClientHeaders $Client) -UserAgent $Client.UserAgent -Body (New-PlayerBody $Client $VideoId)
    if (-not $result.Body) { Write-Both '  (no answer)'; return }
    try { $json = $result.Body | ConvertFrom-Json } catch { Write-Both '  (answer is not JSON)'; return }
    $renderer = $null
    if ($null -ne $json.captions) { $renderer = $json.captions.playerCaptionsTracklistRenderer }
    $tracks = @()
    if ($null -eq $renderer) {
        Write-Both '  (no captions block)'
    } else {
        $tracks = @($renderer.captionTracks | Where-Object { $null -ne $_ })
        $index = 0
        foreach ($track in $tracks) {
            $name = ''
            if ($null -ne $track.name) { $name = [string]$track.name.simpleText }
            Write-Both ("  track {0}: lang={1} kind={2} vssId={3} translatable={4} name='{5}'" -f $index, $track.languageCode, $track.kind, $track.vssId, $track.isTranslatable, $name)
            $index++
        }
        Write-Both ("  defaultAudioTrackIndex = {0}" -f $renderer.defaultAudioTrackIndex)
        $audioIndex = 0
        foreach ($audio in @($renderer.audioTracks)) {
            if ($null -ne $audio) {
                Write-Both ("  audioTracks[{0}]: defaultCaptionTrackIndex={1} captionTrackIndices={2} hasDefaultTrack={3}" -f $audioIndex, $audio.defaultCaptionTrackIndex, (@($audio.captionTrackIndices) -join ','), $audio.hasDefaultTrack)
            }
            $audioIndex++
        }
    }
    $seen = @{}
    if ($null -ne $json.streamingData) {
        foreach ($format in @($json.streamingData.adaptiveFormats)) {
            if ($null -ne $format -and $null -ne $format.audioTrack) {
                $id = [string]$format.audioTrack.id
                if (-not $seen.ContainsKey($id)) {
                    $seen[$id] = $true
                    Write-Both ("  audio format track: id={0} name='{1}' isDefault={2}" -f $id, $format.audioTrack.displayName, $format.audioTrack.audioIsDefault)
                }
            }
        }
    }
    if ($seen.Count -eq 0) { Write-Both '  (no audio track info in streamingData)' }
    # The beginning of the text of the first manual track and the first auto-generated track.
    $manual = $tracks | Where-Object { $_.kind -ne 'asr' } | Select-Object -First 1
    $auto = $tracks | Where-Object { $_.kind -eq 'asr' } | Select-Object -First 1
    foreach ($pair in @(@('first manual', $manual), @('first auto', $auto))) {
        $track = $pair[1]
        if ($null -eq $track) { continue }
        $download = Get-TrackText $track $Client
        $preview = $download.Text
        if ($preview.Length -gt 50) { $preview = $preview.Substring(0, 50) }
        Write-Both ("  text of {0} ({1}): HTTP {2}, {3}" -f $pair[0], $track.languageCode, $download.Status, $preview)
    }
    Write-Both ''
}

Write-Both "YouTube probe - $(Get-Date -Format 'yyyy-MM-dd HH:mm') - PowerShell $($PSVersionTable.PSVersion)"
Write-Both ''

$rows = New-Object System.Collections.Generic.List[object]
$detailVideos = @()
foreach ($videoId in $VideoIds) {
    Write-Host "Video $videoId ..."
    $keys = @{}
    foreach ($variant in $PageVariants) {
        $page = Get-ApiKey $videoId $variant
        $keyState = 'absent'
        if ($page.Key) { $keyState = 'present'; if (-not $keys.ContainsKey('any')) { $keys['any'] = $page.Key } }
        Write-Both ("  watch page ({0}): HTTP {1}, API key {2}" -f $variant.Label, $page.Status, $keyState)
        Start-Sleep -Milliseconds 300
    }
    foreach ($client in $Clients) {
        if ($keys.ContainsKey('any')) {
            $rows.Add((Test-Client $client $videoId $keys['any'] 'with key'))
            Start-Sleep -Milliseconds 300
        }
        $rows.Add((Test-Client $client $videoId $null 'no key'))
        Start-Sleep -Milliseconds 300
    }
    $detailVideos += $videoId
}

Write-Both ''
Write-Both 'Result (Status = HTTP code of the player request, 0 = no answer; Subtitles = text downloaded for the chosen track):'
$table = ($rows | Format-Table Video, Client, Key, Status, Playability, Tracks, Languages, Subtitles -AutoSize | Out-String -Width 4096)
Write-Both $table
Write-Both 'Legend: ANDROID_LEGACY is what the app sends today.'
Write-Both ''
Write-Both 'Track details (how YouTube describes the subtitle and audio tracks):'
Write-Both ''
foreach ($videoId in $detailVideos) {
    Show-TrackDetail $Clients[0] $videoId
    Start-Sleep -Milliseconds 300
}
Write-Both 'Please paste everything above into the chat.'

$lines | Out-File -FilePath $OutFile -Encoding utf8
Write-Host ''
Write-Host "Saved to $((Resolve-Path $OutFile).Path)"
