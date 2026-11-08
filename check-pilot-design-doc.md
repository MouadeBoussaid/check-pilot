# Check Pilot — Design Document (v0.3, Android-first)

> Status: Draft, reviewed with owner feedback · Date: 2026-11-06 · Stack and scope decisions: 2026-11-08 (see Section 14, D18–D27)
> Scope of this version: concept, domain research, functional spec, data model, UX flow, Android tech options, risks, and suggestions. Open questions are collected at the end (Section 14) with options, to be answered later.

---

## 1. Concept in one paragraph

**Check Pilot** is a note-taking and debriefing app for a pilot assessor (check airman / instructor / line-check captain / observer). During a flight (or sim session) the assessor identifies the two crew members (CM1, CM2), the route (departure/destination), and then captures short free-text observations, filed under one of **9 competencies**. Each observation can be tagged to **CM1, CM2 or Both**, and optionally linked to one **Observable Behaviour (OB)** from a fixed, competency-specific list. After the flight the assessor generates a **per-crew-member debriefing**: competencies are re-sorted by evidence volume, a **grading** is assigned per competency, and the underlying notes can be reviewed via an "eye" pop-up.

The core value: **fast, low-friction capture in the cockpit** → **structured, evidence-based debrief** with no re-typing.

---

## 2. Domain research summary

### 2.1 Where the 9 competencies come from
- The structure matches the **EBT / CBTA competency framework**: ICAO Doc 9995 (Manual of Evidence-based Training) defined 8 core competencies; EASA added a ninth, **Application of Knowledge**, for its EBT regulation (AMC/GM to ORO.FC.231 / ORO.FC.232).
- Each competency is made observable through a catalogue of **Observable Behaviours (OBs)**; instructors grade against competencies, using OBs as evidence — which is exactly the note → OB → grade flow in this concept.
- Industry guidance stresses that OB catalogues must be usable in practice (too many OBs overwhelms instructors, too few loses resolution) — relevant for the dropdown UX (Section 7.3).

### 2.2 The 9 competencies (EASA set; codes as used by EASA)

| # | Code | Competency |
|---|------|------------|
| 1 | PRO | Application of Procedures and Compliance with Regulations |
| 2 | COM | Communication |
| 3 | FPA | Flight Path Management — Automation |
| 4 | FPM | Flight Path Management — Manual Control |
| 5 | LTW | Leadership and Teamwork |
| 6 | PSD | Problem Solving and Decision Making |
| 7 | SAW | Situation Awareness (and Management of Information) |
| 8 | WLM | Workload Management |
| 9 | KNO | Application of Knowledge |

This table is a **reference only**. The order used in Check Pilot will be **defined by the owner and differs from the EASA order above**. For now the app uses placeholder Competencies 1–9 (see 9.2b); the real names and order are filled in later and stay fixed for the user at runtime.

### 2.3 Observable Behaviours
- OBs are published per competency in ICAO Doc 9995 and in the EASA AMC/GM. Operators frequently adapt them into their own competency system.
- The example in the concept ("Gives feedback to acknowledge understanding" under Communication) is consistent with this style of OB wording.
- **Important:** the app must ship with an OB catalogue that is **correct and approved for the intended user's operator**. This design doc does **not** reproduce the official lists. The catalogue should be loaded from a versioned data file (Section 9.3) that the owner fills from the authoritative source (EASA AMC/GM, ICAO Doc 9995, or the operator's own OM-D/training manual).

### 2.4 Grading scale — note the difference
The concept uses a 6-step scale:

`Not Observed → Exemplary → Effective → Adequate → Minimum Acceptable Level → Not Yet Competent`

This is **not** the plain 1–5 numeric scale commonly seen in ICAO/IATA EBT material, so it appears to be an **operator-specific** (or personal) scale. That is fine — but it means the scale should be **configurable data**, not hard-coded (see 9.3 and D3).

### 2.5 Market scan (high level)
- Established players are mostly **airline/ATO-scale CBTA/EBT platforms** (web, instructor workstations, tablets, LMS integration, analytics). They are heavy, B2B, and not designed for fast personal note capture.
- The gap this concept targets: a **lightweight, offline, one-hand-friendly personal tool** for the assessor. Differentiators: auto-growing note fields, CM1/CM2/Both tagging, evidence-sorted debrief.
- **Positioning risk:** if the tool is used in an official capacity, the operator/authority may require approved systems and record-keeping (see Section 12).

---

## 3. Users and context of use

| Persona | Description | Needs |
|---------|-------------|-------|
| Primary: **Assessor** | Check airman / instructor / examiner observing from a jump seat or sim instructor station | Capture fast, minimal taps, readable in low light, works offline |
| Secondary: **Crew members (CM1/CM2)** | Receive the debrief | Clear, fair, evidence-linked feedback |

**Context constraints (drive UX):**
- Used in a dim, noisy, vibrating environment → large tap targets, dark theme, high contrast.
- Notes are short, often one-handed, sometimes between phases of flight → keep the input path to **tap field → type → (optional) CM tag/OB**.
- Mostly **offline** (airplane mode, no signal in sim bays).
- Phone **and** tablet usage likely → adaptive layout.
- Used **in the presence of the trainees** while they are being observed → fully silent, discreet and quick; no voice input, no sounds, nothing that draws attention.

---

## 4. Goals / Non-goals

**Goals (MVP)**
1. Create a session (CM1/CM2 staff numbers, departure/destination, date).
2. Capture notes under 9 fixed-order competencies with auto-growing note fields.
3. Tag notes CM1 / CM2 / Both (default Both).
4. Optionally link an OB (competency-specific list).
5. Move a note to another competency (OB cleared).
6. Generate per-CM debriefing with evidence-based sorting, grading, and notes pop-up.
7. Save/resume sessions; fully offline.

**Non-goals (MVP)**
- Cloud sync, multi-user accounts, airline back-end integration.
- Automatic grading or AI-written feedback.
- iOS/web (considered later, see Section 10).

---

## 5. Functional requirements

### 5.1 Session setup screen
| ID | Requirement |
|----|-------------|
| S1 | Field: CM1 staff number. Field: CM2 staff number. |
| S2 | Field: Departure airport code. Field: Destination airport code. **IATA 3-letter codes only** (see 8.2). |
| S3 | Field: Date, auto-populated with today; tap to edit via date picker. |
| S4 | "Start" creates the session and opens the competency list. |

Suggestions: validate staff number format (configurable), airport autocomplete (Section 8.2), remember last-used values as quick-pick chips.

### 5.2 Competency list (capture screen)
| ID | Requirement |
|----|-------------|
| C1 | Show all 9 competencies in a **fixed order**, vertically, always. |
| C2 | Under each competency, show a list of note fields. |
| C3 | **Auto-grow rule:** whenever the last note field of a competency contains text, a new empty note field appears below it. Invariant: each competency always has **exactly one empty trailing field**. |
| C4 | If a note is cleared (empty) and it isn't the trailing one, it is removed (or highlighted for delete) so there are no empty gaps; the invariant holds. |
| C5 | Under each competency title, show the **selected OBs in small italic** (deduplicated, from the notes in that competency). |
| C6 | No competency is mandatory; a competency may have zero notes. |
| C7 | At the bottom of the list (after scrolling past the last competency) two buttons appear: **Generate debriefing** and **Save report**. |

### 5.3 Note field behaviour
| ID | Requirement |
|----|-------------|
| N1 | Tapping/focusing a note field reveals the **target selector: CM1 / CM2 / BOTH**. Exactly one is active (radio/segmented control). **Default: BOTH.** |
| N2 | The user can change to CM1 or CM2 and change back to BOTH at any time. |
| N3 | After note text is entered, an **optional OB dropdown** is available. It lists only the OBs belonging to the competency the note currently sits under. |
| N4 | A selected OB is shown (small italic) **under the note** and **under the competency title** (see C5). |
| N5 | The user can clear the OB (set to "none"). |
| N6 | **Move note:** an action on the note (menu) lets the user choose another competency. The note is **moved, not copied**. Its CM tag and text are retained; its **OB is cleared** (because the OB list differs per competency). If an OB would be dropped, show a brief confirm or an undo snackbar. |
| N7 | Note order inside a competency: creation order; a moved note is appended at the end of the target competency's filled notes (before the trailing empty field). |
| N8 | Autosave on every change (debounced). |

Example (from the concept):

```
Communication
  *Gives feedback to acknowledge understanding            <- OB summary under title

  Note 1) Missed ATC radio calls                          [CM1] [CM2] [BOTH*]
  Note 2) Misunderstood pilot - pilot instructions        [CM1] [CM2] [BOTH*]
          *OBS BEH: Gives feedback to acknowledge understanding
  Note 3) Did not respect sterile flight deck environment [CM1] [CM2] [BOTH*]
  Note 4) ______________                                  <- auto-opened empty field
```

### 5.4 Generate debriefing
| ID | Requirement |
|----|-------------|
| G1 | Pressing **Generate debriefing** **automatically creates a save file** (snapshot of the session). |
| G2 | A pop-up asks: **CM 1 debriefing** or **CM 2 debriefing**. |
| G3 | The debriefing screen shows competencies for the selected CM only (see filtering rule below). |
| G4 | **Filtering rule:** a note appears in CM1's debrief if tagged CM1 or BOTH; in CM2's debrief if tagged CM2 or BOTH. Example: all Communication notes tagged CM1 → shown in the CM1 debrief, absent from the CM2 debrief. |
| G5 | **Sorting rule** (computed on the selected CM's filtered data): (1) most notes first; (2) tie → most OBs first; (3) tie → original fixed order. |
| G6 | Per competency show: the OBs selected (from the filtered notes) and a **grading selector**. |
| G7 | Grading values (in order): Not Observed, Exemplary, Effective, Adequate, Minimum Acceptable Level, Not Yet Competent. |
| G8 | Each competency row has an **eye icon**; tapping opens a pop-up/bottom sheet listing the filtered notes (with their OBs). Dismiss to return. |
| G9 | The date field is shown/edited on the report; auto-populated, editable. |
| G10 | Switching between CM1 and CM2 debrief is possible without losing data. Grades are stored **per CM per competency**. |

**G5 "OB count" (confirmed):** the tie-break counts **distinct OBs** (not the number of notes carrying an OB). See D4.

**Competencies with zero filtered notes** still appear (at the bottom, in original order) so a grade (typically Not Observed) can be set.

### 5.5 Save report
- Explicit save of the current state (separate from the automatic save in G1).
- Saved sessions are listed on a home screen (date, route, CM1/CM2) and can be reopened/edited, duplicated, or deleted. Export comes after the MVP (see Section 11).
- The home screen has a **Load report** button to retrieve a previous report (opens the saved-reports list; selecting one reopens it, either in the capture view for editing or directly in the debrief view).

---

## 6. Screen map

```
Home ──► [New session] ──► Setup ──► Competency list / capture ──► [Move note] [OB dropdown]
  │                                          │
  │                                          ▼
  │                         Generate debriefing ─► Pop-up: CM1 | CM2
  │                                          │
  └──► [Load report] ──► Saved reports ──────┴──► Debrief report (sorted, graded)
                          (select one)                  │
                                                        ├─► Eye pop-up: notes for competency
                                                        └─► Export / Share (post-MVP or MVP-lite)
```

### Wireframes (low-fidelity)

**Home**
```
┌─────────────────────────────┐
│ Check Pilot                 │
│                             │
│      [ New session ]        │
│      [ Load report ]        │
└─────────────────────────────┘
```

**Setup**
```
┌─────────────────────────────┐
│ Check Pilot                 │
│ CM1 staff no. [__________]  │
│ CM2 staff no. [__________]  │
│ From [___]    To [___]      │
│ Date  [ 01 Nov 2026  ✎ ]    │
│          [ Start ]          │
└─────────────────────────────┘
```

**Capture**
```
┌─────────────────────────────┐
│ EHAM → LEMD   01 Nov 2026   │
├─────────────────────────────┤
│ 1 Procedures                │
│ ┌─────────────────────────┐ │
│ │ note text…              │ │
│ │ ( CM1 )( CM2 )(●BOTH)   │ │  ← appears on focus
│ │ OB: [ Select… ▾ ]       │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │                         │ │  ← auto-opened
│ └─────────────────────────┘ │
│ 2 Communication  …          │
│ …                           │
│ [Generate debriefing][Save] │
└─────────────────────────────┘
```

**Debrief (CM1)**
```
┌─────────────────────────────┐
│ CM1 debriefing  01 Nov 2026 │
├─────────────────────────────┤
│ Communication (3 notes) 👁   │
│  *Gives feedback to ack…    │
│  Grade: [ Adequate      ▾ ] │
│ Procedures (2 notes)    👁   │
│  Grade: [ Select…       ▾ ] │
│ …                           │
└─────────────────────────────┘
```

---

## 7. UX details and suggestions

### 7.1 Capture speed
- Keep the keyboard open between notes: **IME action "Next"** moves to the next/trailing note.
- **No voice input:** the app is used in the presence of the trainees, so notes cannot be dictated. Typing only; no dedicated mic button planned, and no sounds or haptics by default.
- Don't make the user scroll for tags: show the CM tag row inline on focus, not in a modal.
- Provide **undo snackbar** for delete/move.

### 7.2 Cockpit-friendly design
- Dark theme default (night-cockpit friendly), optional red-shift/low-brightness theme, big touch targets (≥48 dp), minimal animation.
- Colour-code CM tags (e.g., distinct shapes/labels as well as colour — don't rely on colour alone).

### 7.3 OB dropdown
- Dropdown content depends on the competency the note is under (fixed lists).
- Each competency has at most ~10 OBS BEH, so a **plain dropdown** is sufficient (no search needed).
- **Single OB per note** is assumed by the concept. Option: allow multiple OBs per note (see D5).

### 7.4 Debrief
- Show a **summary header**: counts of notes and OBs, quick view of grades set vs. missing.
- Warn (non-blocking) if some competencies have no grade when leaving the debrief.
- Fairness/consistency prompt (optional): flag competencies with a low grade but no notes (evidence-grade mismatch).

### 7.5 Accessibility
- Support font scaling, TalkBack labels for the eye icon and tag controls, sufficient contrast.

---

## 8. Data and domain logic

### 8.1 Data model (conceptual)

```
Session
  id, createdAt, updatedAt, date (editable, defaults today),
  cm1StaffNo, cm2StaffNo, departureCode, destinationCode,
  catalogueVersion,
  status: DRAFT | DEBRIEFED | SAVED   (meaning still open, see D27)

Note
  id, sessionId, competencyId, text,
  target: CM1 | CM2 | BOTH   (default BOTH),
  obId (nullable), sortIndex, createdAt, updatedAt

Grade
  sessionId, competencyId, crewMember: CM1 | CM2, value (nullable → unset)

Catalogue (bundled JSON asset, read into memory, not stored in Room — see 9.2b):
  version, labels, competencies[ id, name, observableBehaviours[ id, name ] ], gradingScale[ label ]
  Order = array order; no separate code or orderIndex fields.
```

Key rules:
- `Note.obId` must belong to `Note.competencyId`; on move → `obId = null`.
- Trailing empty field is **UI state only** (not persisted as a row) — avoids junk rows.
- Store the **catalogue version** in each session so old sessions render correctly after OB updates.
- Sessions, notes and grades live in Room; they reference catalogue entries by `id` only.
- Grades are per CM (the concept's debrief is per CM).

### 8.2 Airport codes
- **IATA 3-letter codes only** (e.g., AMS, MAD). 4-letter ICAO codes are not needed for now and add nothing; they can be added later if ever required.
- Store the code exactly as entered (uppercased); validate as 3 letters A–Z.
- Optional: bundle an offline IATA airport list (e.g., OurAirports, public domain) for autocomplete/validation; allow manual override for unknown codes.

### 8.3 Sorting algorithm (debrief)
```
for each competency c (within selected CM filter):
    notes(c)  = count of notes where target ∈ {CM, BOTH}
    obs(c)    = count of DISTINCT obIds among those notes
sort by: notes desc, obs desc, originalOrder asc   // stable sort
```

---

## 9. Content management

### 9.1 Competency & OB catalogue
- Ship as a **versioned JSON asset** bundled in the app; read into memory at startup (not copied into the database).
- Lets the owner correct wording or switch to the operator's own OB set without touching code.

### 9.2 Example (shape only — illustrative, **not** an official list)
```json
{
  "version": "0.1-draft",
  "competencies": [
    {
      "id": "COM", "name": "Communication", "order": 2,
      "observableBehaviours": [
        { "id": "COM-01", "text": "Gives feedback to acknowledge understanding" }
      ]
    }
  ],
  "gradingScale": [
    "Not Observed", "Exemplary", "Effective",
    "Adequate", "Minimum Acceptable Level", "Not Yet Competent"
  ]
}
```

### 9.2b Placeholder catalogue for now (Competencies 1–9, Observable Behaviours a–g)

The app uses the terms **Competency** and **Observable Behaviour (OBS BEH)** directly, but treats them as plain **two-level data**:

- **Competencies = Categories 1 to 9**
- **Observable Behaviours = Subcategories with letters a to g** under each competency

The app has no built-in aviation knowledge; all names are data and can be renamed later without code changes.

**Initial placeholder structure (v0.1):**

| Competency | Placeholder name | Observable Behaviours (placeholder names) |
|------------|------------------|-------------------------------------------|
| 1 | Competency 1 | 1a, 1b, 1c, 1d, 1e, 1f, 1g |
| 2 | Competency 2 | 2a, 2b, 2c, 2d, 2e, 2f, 2g |
| 3 | Competency 3 | 3a, 3b, 3c, 3d, 3e, 3f, 3g |
| 4 | Competency 4 | 4a, 4b, 4c, 4d, 4e, 4f, 4g |
| 5 | Competency 5 | 5a, 5b, 5c, 5d, 5e, 5f, 5g |
| 6 | Competency 6 | 6a, 6b, 6c, 6d, 6e, 6f, 6g |
| 7 | Competency 7 | 7a, 7b, 7c, 7d, 7e, 7f, 7g |
| 8 | Competency 8 | 8a, 8b, 8c, 8d, 8e, 8f, 8g |
| 9 | Competency 9 | 9a, 9b, 9c, 9d, 9e, 9f, 9g |

**Design rules that make later renaming painless:**
1. **Stable IDs, editable labels.** Each competency and OBS BEH has an immutable `id` (e.g. `"3"`, `"3c"`) and a separate `name`. Notes and grades reference the `id` only, so renaming never breaks saved sessions.
2. **Don't hard-code "7 OBS BEH per competency".** 7 (a–g) is just the placeholder; later a competency may have more or fewer. The dropdown renders whatever list the competency has.
3. **UI terms are labels too.** Strings such as "Competency", "Observable Behaviour" and "OBS BEH" come from a small `labels` block in the same file and can be changed without touching code.
4. **Order = array order.** Competency order (1–9) and OBS BEH order (a–g) come from the file and are fixed for the user at runtime, as required.
5. **Versioning.** Each session stores the catalogue version it was created with. If names change later, old sessions can show their original wording or the latest labels (see D16).
6. All app logic (auto-growing notes, CM1/CM2/Both, move note, sorting, grading, eye pop-up) works identically on placeholder or real names. Sorting by "most OBS BEH" counts distinct OBS BEH ids.

**Placeholder JSON (v0.1):**
```json
{
  "version": "0.1-placeholder",
  "labels": {
    "competency": "Competency",
    "observableBehaviour": "Observable Behaviour",
    "observableBehaviourShort": "OBS BEH"
  },
  "competencies": [
    {
      "id": "1",
      "name": "Competency 1",
      "observableBehaviours": [
        { "id": "1a", "name": "OBS BEH 1a" },
        { "id": "1b", "name": "OBS BEH 1b" },
        { "id": "1c", "name": "OBS BEH 1c" },
        { "id": "1d", "name": "OBS BEH 1d" },
        { "id": "1e", "name": "OBS BEH 1e" },
        { "id": "1f", "name": "OBS BEH 1f" },
        { "id": "1g", "name": "OBS BEH 1g" }
      ]
    }
    /* ... competencies 2 to 9 follow the same pattern ... */
  ],
  "gradingScale": [
    "Not Observed", "Exemplary", "Effective",
    "Adequate", "Minimum Acceptable Level", "Not Yet Competent"
  ]
}
```

**Optional later:** an in-app **catalogue editor** (rename, reorder, add/remove competencies and OBS BEH) or an **import of a JSON file** from device storage, so real names can be filled in without rebuilding the app (see D17).

### 9.3 Configurable vs fixed
- Fixed at runtime for the user: competency order and OBS BEH lists (matches the concept: "fixed lijstje"). For now the catalogue is the placeholder set in 9.2b (Competencies 1–9, OBS BEH a–g).
- Configurable at build/data level: wording, scale labels, order — see D1–D3.

---

## 10. Android technical approach

### 10.1 Options

| Option | Pros | Cons | Fit |
|--------|------|------|-----|
| **A. Native Kotlin + Jetpack Compose (Material 3)** | Best Android UX/perf, first-class offline/db, adaptive layouts, easy PDF/share intents, smallest runtime | Android only (iOS would be a rewrite) | **Recommended** for Android-first |
| B. Flutter | One codebase for Android+iOS, good UI | Slightly heavier, native integrations via plugins | Good if iOS is likely within ~12 months |
| C. React Native / Expo | JS ecosystem, shared logic with web | More bridge/perf considerations; storage/export via libs | OK if you already live in JS/TS |
| D. PWA (installable web app) | Fastest to build, cross-platform, no store | Weaker offline/filesystem guarantees, less native feel, no Play Store presence by default | Prototype only |

### 10.2 Recommended stack (Option A)
- **Language/UI:** Kotlin, Jetpack Compose, Material 3, adaptive layouts (phone + tablet).
- **Architecture:** MVVM / unidirectional data flow; single activity, Navigation Compose; screens collect ViewModel state with `collectAsStateWithLifecycle()`.
- **Persistence:** Room (SQLite) under `data/local`, encrypted at rest with SQLCipher (MVP; see Section 12).
- **Settings:** Jetpack DataStore (Preferences).
- **DI:** Koin (Kotlin DSL, no code generation), modules in `di/MainModules.kt`; a `verify()` unit test checks the dependency graph in CI.
- **Date/time:** `java.time`; Material 3 DatePicker.
- **Export (post-MVP):** Android `PdfDocument` API (or render HTML → print to PDF), shared through the Android share sheet / `FileProvider`.
- **Min SDK:** API 26.
- **Testing:** unit tests for sorting/filtering/move rules (these are the logic-heavy parts); Compose UI tests for the auto-grow field behaviour run on the JVM with Robolectric, so they are part of `testDebugUnitTest` and the acceptance tests.

### 10.3 Implementation notes for tricky behaviour
- **Auto-grow fields:** model per competency as `List<NoteDraft>` + derived UI list = filled notes + one empty trailing item. Never persist the empty trailing item.
- **Focus-driven tag row:** keep `focusedNoteId` in the ViewModel; show the tag row only for that note.
- **Move note:** single DB transaction (update `competencyId`, `obId = null`, set new `sortIndex`).
- **Generate debriefing:** write a snapshot/version row, then navigate; the debrief is a pure function of (notes, grades, selected CM).

---

## 11. Export and sharing (options)

| Option | Description | Complexity |
|--------|-------------|------------|
| X1 | PDF of the debrief per CM (grades, OBs, optionally notes) | Medium |
| X2 | Plain-text/Markdown share to messaging/email | Low |
| X3 | JSON backup/restore of all sessions | Low–Medium |
| X4 | Print via Android print framework | Low once PDF exists |

Decided: **no PDF export in the MVP** (X1 moves to v0.2); whether X3 (JSON backup) is in the MVP is open (D26). X2 as a cheap extra later. Think about whether the CM-specific report should include notes or only OBs + grades (privacy and tone; see D7).

---

## 12. Privacy, security, compliance

This app stores **assessment data about identifiable individuals** (staff numbers + performance notes), so treat it as sensitive.

- **GDPR/AVG** applies if used in the EU/NL: data minimisation, purpose limitation, retention limits, right of access/erasure. Staff numbers are personal data when linkable to a person.
- **Employer policy:** the operator may prohibit storing crew assessment data on personal devices or require approved systems. Confirm before real-world use.
- **Regulatory context:** if used for official EBT/CBTA assessment, the operator's approved assessment and grading system and record-keeping requirements apply; this app should be positioned as a **personal aide/notebook** unless adopted by the operator.
- **Technical controls in the MVP:** local-only storage; device-credential lock (biometric/PIN) on app open; encrypted DB (SQLCipher); excluded from cloud auto-backup and device transfer (`allowBackup=false`, backup rules).
- **Technical controls (suggestions, later):** screenshot blocking (`FLAG_SECURE`) as optional setting; auto-delete after N days (configurable); no analytics SDKs/trackers.
- **Branding/name:** "Check Pilot" is a generic term; check Play Store name availability and trademarks before publishing.

---

## 13. Roadmap suggestion

| Phase | Content |
|-------|---------|
| **MVP (v0.1)** | Setup, capture list with auto-grow notes, CM tags, OB dropdown, move note, session list (reopen, duplicate, delete), autosave, debrief per CM with sorting/grading/eye pop-up, local storage encrypted with SQLCipher, app lock, no cloud backup |
| v0.2 | PDF export, backup/restore (unless pulled into the MVP, D26), airport autocomplete |
| v0.3 | Tablet layout polish, search across sessions, fairness hints |
| v1.0 | Hardening, accessibility audit, Play Store release (private/internal testing first) |
| Later | iOS (if Flutter/KMP chosen), optional encrypted sync, operator-specific catalogue packs, analytics per pilot over time (only with governance) |

**Risks**
1. OB catalogue accuracy/approval (content risk, not code risk).
2. Data-protection / employer-policy constraints on storing crew assessments.
3. Cockpit usability — needs real-world testing (sim sessions) early.
4. Scope creep towards a full CBTA platform.

---

## 14. Decisions needed (options + my suggestion) — for later

> Owner feedback (2026-11-06): all suggestions below are accepted, with these exceptions/confirmations: **D1** order is defined by the owner (differs from EASA); **D4** confirmed = distinct OBs; **D12** = IATA 3-letter only. D2 is currently moot (placeholder catalogue).

| # | Topic | Options | Suggestion |
|---|-------|---------|------------|
| D1 | Competency order | (a) EASA order (b) Owner-defined order | **Decided: (b)** owner-defined; kept as config data |
| D2 | Which OB set | (a) ICAO Doc 9995 (b) EASA AMC/GM (c) Operator's own OM-D list | Use whichever the intended user is formally assessed against; load via JSON |
| D3 | Grading scale | (a) Fixed 6-step scale as specified (b) Configurable scale | (b) with the 6-step scale as default |
| D4 | "Most OBs" tie-break meaning | (a) Distinct OBs (b) Number of notes that have an OB | **Decided: (a)** distinct OBs |
| D5 | OBs per note | (a) Exactly one (b) Multiple | (a) for MVP, as in the concept; (b) later if needed |
| D6 | Sorting basis when CM filter applies | (a) Per-CM filtered counts (b) Global counts | (a) — keeps each CM's report consistent with its content |
| D7 | What a CM's report shows | (a) OBs + grades only (b) plus notes via eye pop-up (as concept) (c) export includes notes optionally | (b) in-app, (c) toggle on export |
| D8 | Grade storage | (a) Per CM per competency (b) One grade per competency | (a) |
| D9 | Empty-note cleanup | (a) Auto-remove emptied notes (b) Keep until leaving the screen | (a) with undo |
| D10 | Moving a note with an OB | (a) Clear OB silently (b) Confirm first (c) Clear + undo snackbar | (c) |
| D11 | Persistence of "Generate debriefing" snapshot | (a) Overwrite one save (b) Keep versions | (a) with option to keep last N snapshots |
| D12 | Airport codes | (a) IATA (b) ICAO (c) both | **Decided: (a)** IATA 3-letter only |
| D13 | Platform strategy | (a) Android-only native (b) Cross-platform from start | (a); revisit if iOS demand is real |
| D14 | Device security | (a) None (b) App lock (c) Encrypted DB + app lock | **Decided: (c), in the MVP** (2026-11-08) |
| D15 | Staff numbers | (a) Free text (b) Validated format (c) Lookup from a roster | (b) with configurable pattern; no roster in MVP |
| D16 | After renaming competencies/OBS BEH later, how do old sessions display? | (a) Original names (catalogue version stored) (b) Always latest names | (b) if renames are only corrections; (a) if the structure changes |
| D17 | How to edit names later | (a) Edit the JSON file and rebuild (b) Import JSON from device (c) In-app catalogue editor | (b) first, (c) later if needed |

### Stack and scope decisions (2026-11-08)

| # | Topic | Decision |
|---|-------|----------|
| D18 | Dependency injection | Koin (not Hilt); modules in `di/MainModules.kt`, graph checked by a `verify()` unit test |
| D19 | Navigation | Single activity + Navigation Compose |
| D20 | Persistence | Room under `data/local` for sessions, notes, grades |
| D21 | Settings | Jetpack DataStore (Preferences) |
| D22 | Min SDK | API 26 |
| D23 | Cloud backup | Off in the MVP (`allowBackup=false`, backup rules exclude all data) |
| D24 | UI tests | Compose UI tests on the JVM with Robolectric, inside `testDebugUnitTest` |
| D25 | Catalogue | Follows 9.2b: JSON asset read into memory, array order, `"1"`/`"1a"` ids, `labels` block; not stored in Room |
| D26 | MVP export scope | No PDF in the MVP; session list offers reopen, duplicate, delete. **Open:** JSON backup/restore in the MVP or v0.2 |
| D27 | Save semantics | **Open:** what "Save report" adds over autosave (N8) and the snapshot on Generate debriefing (G1); meaning of `DRAFT / DEBRIEFED / SAVED`; D11 "keep last N snapshots" |

---

## 15. Sources consulted

- ICAO Doc 9995 — Manual of Evidence-based Training (2013) and EASA EBT regulation material (AMC/GM to ORO.FC.231 / ORO.FC.232; Opinion 08/2019).
- EASA — Appendix to Opinion No 08/2019 (competency framework incl. Application of Knowledge): https://www.easa.europa.eu/sites/default/files/dfu/update_-_appendix_to_opinion_no_08-2019_a_rmt.0599.pdf
- EASA — EBT abbreviations/competency codes (PRO, KNO, COM, FPA, FPM, LTW, PSD …): https://www.easa.europa.eu/de/downloads/125288/en
- Aviatize glossary — core competencies, OBs, CBTA/EBT overview: https://www.aviatize.com/glossary/core-competencies · https://www.aviatize.com/glossary/observable-behaviours
- Aviationexam — the nine core competencies and OBs: https://www.aviationexam.com/en/l/atpl-ksa/
- Airbus Safety First — "Learning from the evidence" (nine core competencies): https://safetyfirst.airbus.com/learning-from-the-evidence/amp
- Malta CAA — EBT implementation checklist (assessment and grading system expectations): https://www.transport.gov.mt/CAD424-EBT-Implementation_Checklist_TMCAD_Issue-2-April-2022.docx-f7553

> Note: official OB wording and grading scales must be taken from the authoritative document applicable to the user's operator; this document intentionally does not reproduce them.
