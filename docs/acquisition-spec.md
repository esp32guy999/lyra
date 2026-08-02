# Lyra — Acquisition (Manage tab) Spec

Discovery + acquisition layer over the existing stack. Lyra is Command & Control; the
arr stack + qBittorrent do the heavy lifting. Lives in the **Manage** tab.

## Flow
1. **Artist search** → pick an artist.
2. **Discography** — list the artist's release-groups (albums/EPs/singles/live/comp),
   each flagged **[✓] OWNED** or **[ ] MISSING** (see Ownership Diff).
3. **Select a missing album → dual-source search**, results merged into one list:
   - **Lidarr** (managed): parsed quality, size, seeders, indexer; grab = whole album,
     auto-imported by Lidarr. *Requires the album added to Lidarr first.*
   - **Prowlarr** (raw): all indexer hits; torrents are **track-selectable**; manual import.
4. **Each result row** shows: **quality/format** (FLAC/320/V0), **seeders**, and a
   **source badge** — 🟩 Lidarr (managed) vs 🟦 Prowlarr (raw). Sort by seeders/quality.
5. **Grab:**
   - Lidarr row → `POST /api/v1/release` (grab) → Lidarr downloads + imports.
   - Prowlarr torrent row → add to qBit; **optional track-select** (expander, torrents
     only) via qBit `filePrio`; then **place & tag** the file(s) into `/music` so
     Navidrome picks them up (Lidarr's import is bypassed for track grabs).

## Data sources & endpoints
- **Discography + album metadata:** Lidarr `GET /api/v1/artist`, `/api/v1/album?artistId=`
  (Lidarr mirrors MusicBrainz — avoids MB's 1 req/sec limit). Fuzzy add via
  `/api/v1/artist/lookup`.
- **Ownership diff:** Navidrome (Subsonic) library — match by **MBID** where files are
  tagged, else fuzzy artist+album+year. Album is OWNED if present in Navidrome.
- **Lidarr search:** `GET /api/v1/release?albumId=X` (parsed releases). Grab: `POST /api/v1/release`.
- **Prowlarr search:** `GET /api/v1/search?query=&categories=3000` (music). Grab: hand to qBit.
- **Track-select (torrent only):** qBit `POST /torrents/add` (paused) →
  `GET /torrents/files?hash=` → `POST /torrents/filePrio` (0=skip, 1=want) → start.
- **Place & tag:** move grabbed loose track(s) into the Navidrome music root with correct
  path/tags; trigger a Navidrome rescan.

## Ownership diff
- Pull owned set from Navidrome once per session (cache). Compare discography album MBIDs
  (from Lidarr) to owned MBIDs; fall back to normalized `artist – album (year)` match.
- Track-level OWNED needs per-track MBID/title match; album-level first (simpler).

## Config (BuildConfig, from local.properties — gitignored)
- Lidarr: `lidarr.url` / `lidarr.key` (done).
- Prowlarr: `prowlarr.url` / `prowlarr.key`.
- qBittorrent: `qbit.url` / `qbit.user` / `qbit.pass`.
- Navidrome: reuse the app's existing Subsonic login.

## Build phases
- **P1 — data layer:** ✅ Lidarr discography + release-search + grab; Prowlarr raw search;
  DI/config. (compile-verified, committed)
- **P3 — UI:** ✅ stage drill-down (SEARCH→DISCOG→RESULTS): artist search → discography →
  merged Lidarr+Prowlarr results (quality + seeders + source badge) → grab. Material 3.
- **P4 — track-select (torrents):** ✅ Prowlarr grab → qBit add-paused → locate hash →
  list files → track-select dialog → filePrio (0 skip / 1 want) → start. Only chosen
  tracks download. (committed)
- **P2 — ownership diff:** ✅ on opening an artist, query Navidrome (search3 → getArtist) for
  owned albums; each discography row shows OWNED (green) / MISSING (amber) via normalized-title
  match. (committed)
- **P5 — library placement:** ⬜ Prowlarr/qBit downloads land in qBit's dir, not the music
  library. Need import: point qBit save-path at a Lidarr-watched folder (or manual move +
  tag) + Navidrome rescan. Lidarr grabs already auto-import. Not yet built.

## Caveats (designed-around)
- Lidarr search needs the album added first; Prowlarr raw doesn't → offer both paths.
- Torrent titles parse quality unreliably → Lidarr rows are authoritative, Prowlarr best-effort.
- filePrio grabbing one file pulls partial adjacent pieces (minor overhead).
