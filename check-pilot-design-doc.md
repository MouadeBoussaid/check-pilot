# Check Pilot — Design Document (v0.4, Android-first)

> Status: Draft, reviewed with owner feedback · Date: 2026-11-06 · Stack and scope decisions: 2026-11-08 (D18–D27) ·
> Grilling decisions: 2026-11-08 (D28–D42; earlier decisions updated in place)
> Scope of this version: concept, domain research, functional spec, data model, UX flow, Android tech options, risks,
> and suggestions. Decisions are collected at the end (Section 14).
> Vocabulary: see `CONTEXT.md` (Session, Debrief, Crew Member, CM1/CM2, Note, CM Tag, Catalogue, …). Recorded
> decisions: `docs/adr/`.

---

## 1. Concept in one paragraph

**Check Pilot** is a note-taking and debriefing app for a pilot **Assessor** (check airman / instructor / line-check
captain / observer). During a flight (or sim session) the Assessor identifies the one or two **Crew Members** (CM1,
CM2) by Staff Number, the route (departure/destination), and then captures short free-text **Notes**, each filed under
one of the **Competencies** in the Catalogue. Each Note can be tagged to **CM1, CM2 or Both**, and optionally linked to
one **Observable Behaviour (OBS BEH)** from that Competency's list. After the flight the Assessor opens a
**per-Crew-Member Debrief**: Competencies are re-sorted by evidence volume, a **Grade** is set per Competency, and the
underlying Notes can be reviewed via an "eye" pop-up.

The core value: **fast, low-friction capture in the cockpit** → **structured, evidence-based Debrief** with no
re-typing.

---

## 2. Domain research summary

### 2.1 Where the 9 competencies come from
- The structure matches the **EBT / CBTA competency framework**: ICAO Doc 9995 (Manual of Evidence-based Training)
  defined 8 core competencies; EASA added a ninth, **Application of Knowledge**, for its EBT regulation (AMC/GM to
  ORO.FC.231 / ORO.FC.232).
- Each competency is made observable through a catalogue of **Observable Behaviours**; instructors grade against
  competencies, using Observable Behaviours as evidence — which is exactly the Note → OBS BEH → Grade flow in this
  concept.
- Industry guidance stresses that Observable Behaviour catalogues must be usable in practice (too many overwhelms
  instructors, too few loses resolution) — relevant for the dropdown UX (Section 7.3).

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

This table is a **reference only**. The order used in Check Pilot is **defined by the owner and differs from the EASA
order above**. The app starts with placeholder Competencies 1–9 (see 9.2b); the Assessor fills in the real names and
order in the app's Catalogue editor (5.6, ADR 0002).

### 2.3 Observable Behaviours
- Observable Behaviours are published per competency in ICAO Doc 9995 and in the EASA AMC/GM. Operators frequently
  adapt them into their own competency system.
- The example in the concept ("Gives feedback to acknowledge understanding" under Communication) is consistent with
  this style of wording.
- **Important:** the Catalogue the Assessor works with must be **correct and approved for the intended user's
  operator**. This design doc does **not** reproduce the official lists. The app ships with placeholders only; the
  Assessor enters the real Competencies and Observable Behaviours from the authoritative source (EASA AMC/GM, ICAO Doc
  9995, or the operator's own OM-D/training manual) in the Catalogue editor.

### 2.4 Grading scale — note the difference
The concept uses a 6-step scale:

`Not Observed → Exemplary → Effective → Adequate → Minimum Acceptable Level → Not Yet Competent`

This is **not** the plain 1–5 numeric scale commonly seen in ICAO/IATA EBT material, so it appears to be an
**operator-specific** (or personal) scale. For now the scale is **fixed in the app** and not editable (D3).

### 2.5 Market scan (high level)
- Established players are mostly **airline/ATO-scale CBTA/EBT platforms** (web, instructor workstations, tablets, LMS
  integration, analytics). They are heavy, B2B, and not designed for fast personal note capture.
- The gap this concept targets: a **lightweight, offline, one-hand-friendly personal tool** for the Assessor.
  Differentiators: auto-growing Note fields, CM1/CM2/Both tagging, evidence-sorted Debrief.
- **Positioning risk:** if the tool is used in an official capacity, the operator/authority may require approved
  systems and record-keeping (see Section 12).

---

## 3. Users and context of use

| Persona | Description | Needs |
|---------|-------------|-------|
| Primary: **Assessor** | Check airman / instructor / examiner observing from a jump seat or sim instructor station | Capture fast, minimal taps, readable in low light, works offline |
| Secondary: **Crew Members (CM1/CM2)** | Receive the Debrief | Clear, fair, evidence-linked feedback |

**Context constraints (drive UX):**
- Used in a dim, noisy, vibrating environment → large tap targets, dark theme, high contrast.
- Notes are short, often one-handed, sometimes between phases of flight → keep the input path to **tap field → type →
  (optional) CM Tag/OBS BEH**.
- Mostly **offline** (airplane mode, no signal in sim bays).
- Phone **and** tablet usage likely → adaptive layout.
- Used **in the presence of the trainees** while they are being observed → fully silent, discreet and quick; no voice
  input, no sounds, nothing that draws attention.
- The Assessor holds the phone throughout the assessment → no lock on opening the app (ADR 0001).

**How Sessions map to real assessments (D28):**
- **Training:** one Session per Sector; observations go in the Session of the Sector where they were seen.
- **Line Check:** one Session for both Sectors, recorded under the outbound Sector; observations from the return
  Sector go in the same Session, so one combined Debrief comes out.
- A Line Check may assess both pilots or only one of them, so a Session has one or two Crew Members (D29).
- A sim instructor may train two First Officers at once; CM1/CM2 are only slots for telling them apart, not seats or
  roles.

---

## 4. Goals / Non-goals

**Goals (MVP)**
1. Create a Session (one or two Staff Numbers, departure/destination, date).
2. Capture Notes under the Catalogue's Competencies, in Catalogue order, with auto-growing Note fields.
3. Tag Notes CM1 / CM2 / Both (default Both) when the Session has two Crew Members.
4. Optionally link one OBS BEH (from the Note's Competency).
5. Move a Note to another Competency (OBS BEH cleared).
6. Open a per-Crew-Member Debrief with evidence-based sorting, Grades, and a Notes pop-up.
7. Autosave every change; find and reopen Sessions from a PIN-protected list; fully offline.
8. Edit the Catalogue in the app: add, rename, reorder (and delete unused) Competencies and Observable Behaviours.

**Non-goals (MVP)**
- Cloud sync, multi-user accounts, airline back-end integration.
- Backup, restore or export of Sessions (D26).
- A lock on opening the app (ADR 0001).
- Photos, video or any attachments: Sessions are text only.
- Automatic grading or AI-written feedback.
- iOS/web (considered later, see Section 10).

---

## 5. Functional requirements

### 5.1 Session setup screen
| ID | Requirement |
|----|-------------|
| S1 | Fields: CM1 Staff Number, CM2 Staff Number. **At least one is required**; either slot may stay empty (D29). |
| S2 | A Staff Number is **4–6 digits, no letters**; numeric keypad. Stored and shown **exactly as typed** (no padding or stripping of leading zeros) (D15). |
| S3 | Field: Departure airport code. Field: Destination airport code. **IATA 3-letter codes only** (see 8.2). |
| S4 | Field: Date, auto-populated with today; tap to edit via date picker. |
| S5 | "Start" creates the Session and opens the capture screen. |
| S6 | Staff Numbers can be **added or corrected later**, from the capture screen. Removing a Staff Number is only allowed when no Notes are tagged to that Crew Member alone; otherwise the app asks to retag those Notes first (D38). |

Suggestions: airport autocomplete (Section 8.2), remember last-used values as quick-pick chips.

### 5.2 Competency list (capture screen)
| ID | Requirement |
|----|-------------|
| C1 | Show all Competencies of the Catalogue, **in Catalogue order**, vertically, always. The capture screen never offers Catalogue editing (D40). |
| C2 | Under each Competency, show a list of Note fields. |
| C3 | **Auto-grow rule:** whenever the last Note field of a Competency contains text, a new empty Note field appears below it. Invariant: each Competency always has **exactly one empty trailing field**. |
| C4 | A Note that is emptied (e.g. by backspacing) and isn't the trailing one is removed **when it loses focus**, never while typing, with an undo snackbar; the invariant holds (D9). |
| C5 | Under each Competency title, show the **selected OBS BEH in small italic** (deduplicated, from the Notes in that Competency). |
| C6 | No Competency is mandatory; a Competency may have zero Notes. |
| C7 | At the bottom of the list (after scrolling past the last Competency) two buttons appear: **Generate debriefing** and **Done**. Done returns to the home screen; there is no separate save (D27). |

### 5.3 Note field behaviour
| ID | Requirement |
|----|-------------|
| N1 | Tapping/focusing a Note field reveals the **CM Tag selector: CM1 / CM2 / BOTH**. Exactly one is active (radio/segmented control). **Default: BOTH.** The selector is shown **only when the Session has two Crew Members**; with one, every Note is about that Crew Member (D30). |
| N2 | The Assessor can change to CM1 or CM2 and change back to BOTH at any time. |
| N3 | After Note text is entered, an **optional OBS BEH dropdown** is available. It lists only the Observable Behaviours belonging to the Competency the Note currently sits under. |
| N4 | A selected OBS BEH is shown (small italic) **under the Note** and **under the Competency title** (see C5). |
| N5 | The Assessor can clear the OBS BEH (set to "none"). |
| N6 | **Move Note:** an action on the Note (menu) lets the Assessor choose another Competency. The Note is **moved, not copied**. Its CM Tag and text are retained; its **OBS BEH is cleared** (because the list differs per Competency). If an OBS BEH would be dropped, show an undo snackbar (D10). |
| N7 | Note order inside a Competency: creation order; a moved Note is appended at the end of the target Competency's filled Notes (before the trailing empty field). |
| N8 | Autosave on every change (debounced). This is the only save; there are no snapshots (D11). |
| N9 | **Erase:** the focused Note shows a ✕. One tap deletes **only that Note** (never the other Notes of the Competency), with an undo snackbar that restores its text, CM Tag and OBS BEH (D35). |

Example (from the concept):

```
Communication
  *Gives feedback to acknowledge understanding            <- OBS BEH summary under title

  Note 1) Missed ATC radio calls                          [CM1] [CM2] [BOTH*]
  Note 2) Misunderstood pilot - pilot instructions        [CM1] [CM2] [BOTH*]
          *OBS BEH: Gives feedback to acknowledge understanding
  Note 3) Did not respect sterile flight deck environment [CM1] [CM2] [BOTH*]
  Note 4) ______________                                  <- auto-opened empty field
```

### 5.4 Debrief
| ID | Requirement |
|----|-------------|
| G1 | Pressing **Generate debriefing** opens the Debrief. It creates no copy or snapshot: the Session is already autosaved (D11). |
| G2 | With two Crew Members, a pop-up asks: **CM 1 debriefing** or **CM 2 debriefing**. With one, the Debrief opens directly for that Crew Member (D30). |
| G3 | The Debrief shows Competencies for the selected Crew Member only (see filtering rule below). |
| G4 | **Filtering rule:** a Note appears in CM1's Debrief if tagged CM1 or BOTH; in CM2's Debrief if tagged CM2 or BOTH. Example: all Communication Notes tagged CM1 → shown in the CM1 Debrief, absent from the CM2 Debrief. |
| G5 | **Sorting rule** (computed on the selected Crew Member's filtered data): (1) most Notes first; (2) tie → most distinct OBS BEH first; (3) tie → Catalogue order. |
| G6 | Per Competency show: the OBS BEH selected (from the filtered Notes) and a **Grade selector**. |
| G7 | Grade values (in order, fixed in the app): Not Observed, Exemplary, Effective, Adequate, Minimum Acceptable Level, Not Yet Competent. |
| G8 | Each Competency row has an **eye icon**; tapping opens a pop-up/bottom sheet listing the filtered Notes (with their OBS BEH). Dismiss to return. |
| G9 | The date field is shown/edited on the Debrief; auto-populated, editable. It is the Session's date. |
| G10 | Switching between the CM1 and CM2 Debrief is possible without losing data. Grades are stored **per Crew Member per Competency**. |
| G11 | **No automatic grading:** every Competency starts without a Grade. Any Grade can be set on any Competency, including one with zero Notes (the Assessor also grades on overall impression). **Not Observed** is a deliberate Grade, distinct from no Grade (D33). |

**G5 "OBS BEH count" (confirmed):** the tie-break counts **distinct OBS BEH** (not the number of Notes carrying one).
See D4.

**Competencies with zero filtered Notes** still appear (at the bottom, in Catalogue order) so a Grade can be set.

A Session stays editable after its Debrief has been opened: the Assessor can go back to capture, add Notes, and reopen
the Debrief, which re-sorts.

### 5.5 Home screen and Session list
- **Home screen:** **New session**, **Continue last Session**, **Open session**, **Settings**.
- **Continue last Session** opens the **Last Session** (the most recently *created* Session, new or duplicated,
  whatever was edited since) **without the PIN** (D36, ADR 0001).
- **Open session** asks for the **app PIN** and then shows the Session list. One PIN entry unlocks the list until the
  Assessor goes back to the home screen or leaves the app (D37).
- **Session list:** newest first. Each row shows route, date, Staff Numbers and Grade progress, e.g.
  `DOH → SLL · 08 Nov 2026 · 79125 / 4512 · Grades 7/9 (CM1)`. One search field matches a Staff Number, an airport
  code or a date (D32). Sessions have no status (D27).
- Per Session: **open** (capture or Debrief), **duplicate**, **delete**.
- **Duplicate** copies the Staff Numbers, swaps the route (DOH–SLL becomes SLL–DOH) and sets today's date; Notes and
  Grades are not copied (D31). Typical use: the return Sector of a training flight.

### 5.6 Catalogue editor (Settings)
- Reachable only from the home screen (Settings), never from the capture or Debrief screen (D40). No PIN: it holds no
  crew data.
- The Assessor can **add**, **rename** and **reorder** Competencies, and Observable Behaviours within a Competency
  (D39).
- **Delete** is allowed only for an item that no Note or Grade refers to yet.
- Changes apply to every Session, old and new: renames show everywhere, and an added Competency appears in old
  Sessions without a Grade (so their progress becomes e.g. "Grades 9/10") (D16).
- The grading scale is not editable (D3).

### 5.7 App PIN (Settings)
- A **4–6 digit app PIN**, separate from the phone's screen lock, set the first time the Assessor taps **Open session**
  (D14, ADR 0001).
- Changeable in Settings by entering the old PIN.
- **Forgot PIN** deletes all saved Sessions (after a clear warning), then lets the Assessor choose a new PIN. There is
  no other recovery.

---

## 6. Screen map

```
Home ──► [New session] ──► Setup ──► Capture ──► [Move Note] [OBS BEH dropdown] [Erase]
  │                                     │
  ├──► [Continue last Session] ─────────┤ (no PIN)
  │                                     ▼
  │                    Generate debriefing ─► Pop-up: CM1 | CM2 (only with two Crew Members)
  │                                     │
  ├──► [Open session] ─► PIN ─► Session list ──┴──► Debrief (sorted, graded)
  │                             (search, open,          │
  │                              duplicate, delete)     └─► Eye pop-up: Notes for Competency
  │
  └──► [Settings] ──► Catalogue editor · Change PIN
```

### Wireframes (low-fidelity)

**Home**
```
┌─────────────────────────────┐
│ Check Pilot                 │
│                             │
│      [ New session ]        │
│  [ Continue last Session ]  │
│      [ Open session ]       │
│                       ⚙     │
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
│ │ note text…            ✕ │ │
│ │ ( CM1 )( CM2 )(●BOTH)   │ │  ← appears on focus, two Crew Members only
│ │ OBS BEH: [ Select… ▾ ]  │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │                         │ │  ← auto-opened
│ └─────────────────────────┘ │
│ 2 Communication  …          │
│ …                           │
│ [Generate debriefing][Done] │
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
- Keep the keyboard open between Notes: **IME action "Next"** moves to the next/trailing Note.
- **No voice input:** the app is used in the presence of the trainees, so Notes cannot be dictated. Typing only; no
  dedicated mic button planned, and no sounds or haptics by default.
- Don't make the Assessor scroll for tags: show the CM Tag row inline on focus, not in a modal.
- Provide **undo snackbar** for erase/delete/move.

### 7.2 Cockpit-friendly design
- Dark theme default (night-cockpit friendly), optional red-shift/low-brightness theme, big touch targets (≥48 dp),
  minimal animation.
- Colour-code CM Tags (e.g., distinct shapes/labels as well as colour — don't rely on colour alone).

### 7.3 OBS BEH dropdown
- Dropdown content depends on the Competency the Note is under.
- Each Competency has at most ~10 OBS BEH, so a **plain dropdown** is sufficient (no search needed).
- **Single OBS BEH per Note** (D5).

### 7.4 Debrief
- Show a **summary header**: counts of Notes and OBS BEH, quick view of Grades set vs. missing.
- Warn (non-blocking) if some Competencies have no Grade when leaving the Debrief.
- No "evidence vs. Grade" fairness hint: grading a Competency without Notes is normal practice (D34).

### 7.5 Accessibility
- Support font scaling, TalkBack labels for the eye icon and tag controls, sufficient contrast.

---

## 8. Data and domain logic

### 8.1 Data model (conceptual)

```
Session
  id, createdAt, updatedAt, date (editable, defaults today),
  cm1StaffNo (nullable), cm2StaffNo (nullable)   -- at least one set; digits as typed
  departureCode, destinationCode

Note
  id, sessionId, competencyId, text,
  cmTag: CM1 | CM2 | BOTH   (default BOTH),
  observableBehaviourId (nullable), sortIndex, createdAt, updatedAt

Grade
  sessionId, competencyId, crewMember: CM1 | CM2, value (nullable → no Grade)

Competency            (Catalogue; stored on the device, seeded on first install — ADR 0002)
  id, name, sortIndex

ObservableBehaviour   (Catalogue)
  id, competencyId, name, sortIndex

Grading scale: fixed in the app (6 values, see G7).
```

Key rules:
- `Note.observableBehaviourId` must belong to `Note.competencyId`; on move → `observableBehaviourId = null`.
- Trailing empty field is **UI state only** (not persisted as a row) — avoids junk rows.
- Sessions, Notes, Grades and the Catalogue live in Room; Sessions reference Catalogue entries by `id` only.
- Catalogue ids are **never removed or reused**; an item can only be deleted while nothing refers to it. An unknown id
  still renders, as its raw id.
- Sessions store **no catalogue version**: every Session shows the current Catalogue.
- Grades are per Crew Member (the Debrief is per Crew Member).
- The **Last Session** is the Session with the latest `createdAt`.
- No status field: a Session's progress is derived from its Grades.

### 8.2 Airport codes
- **IATA 3-letter codes only** (e.g., AMS, MAD). 4-letter ICAO codes are not needed for now and add nothing; they can
  be added later if ever required.
- Store the code exactly as entered (uppercased); validate as 3 letters A–Z.
- Optional: bundle an offline IATA airport list (e.g., OurAirports, public domain) for autocomplete/validation; allow
  manual override for unknown codes.

### 8.3 Sorting algorithm (Debrief)
```
for each competency c (within selected Crew Member filter):
    notes(c)  = count of notes where cmTag ∈ {CM, BOTH}
    obs(c)    = count of DISTINCT observableBehaviourIds among those notes
sort by: notes desc, obs desc, catalogueOrder asc   // stable sort
```

---

## 9. Content management

### 9.1 Competency & OBS BEH Catalogue
- Stored in the app's database (ADR 0002). On **first install** it is seeded from a bundled placeholder file; an app
  update never overwrites the Catalogue on the device (D41).
- The Assessor corrects wording, fills in the real names and reorders items in the Catalogue editor (5.6), without a
  new app build.

### 9.2 Example (shape only — illustrative, **not** an official list)
```json
{
  "competencies": [
    {
      "id": "COM", "name": "Communication",
      "observableBehaviours": [
        { "id": "COM-01", "name": "Gives feedback to acknowledge understanding" }
      ]
    }
  ]
}
```

### 9.2b Placeholder Catalogue (Competencies 1–9, Observable Behaviours a–g)

The app uses the terms **Competency** and **Observable Behaviour (OBS BEH)** directly, but treats them as plain
**two-level data**:

- **Competencies = Categories 1 to 9**
- **Observable Behaviours = Subcategories with letters a to g** under each Competency

The app has no built-in aviation knowledge; all names are data and can be renamed in the app without code changes.

**Initial placeholder structure (seed):**

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

**Design rules that make renaming painless:**
1. **Stable IDs, editable names.** Each Competency and OBS BEH has an immutable `id` (e.g. `"3"`, `"3c"`; generated for
   items the Assessor adds) and a separate `name`. Notes and Grades reference the `id` only, so renaming never breaks
   saved Sessions.
2. **Don't hard-code "9 Competencies" or "7 OBS BEH per Competency".** These are just the placeholders; the Assessor
   may add more. The capture screen and dropdowns render whatever the Catalogue holds.
3. **UI terms are labels.** Strings such as "Competency", "Observable Behaviour" and "OBS BEH" come from a small
   `labels` block in the seed file; they are not editable in the app.
4. **Order.** The seed's array order sets the initial order; after that the stored order (changed in the editor) is
   used everywhere.
5. All app logic (auto-growing Notes, CM1/CM2/Both, move Note, sorting, grading, eye pop-up) works identically on
   placeholder or real names. Sorting by "most OBS BEH" counts distinct OBS BEH ids.

**Placeholder seed (JSON):**
```json
{
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
  ]
}
```

### 9.3 Editable vs fixed
- Editable by the Assessor, in the app: Competency and OBS BEH names, their order, adding them, and deleting unused
  ones (5.6).
- Not editable during a Session: the capture screen always shows the Catalogue as it was when the screen opened.
- Fixed in the app: the grading scale (G7) and the UI labels.

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
- **Architecture:** MVVM / unidirectional data flow; single activity, Navigation Compose; screens collect ViewModel
  state with `collectAsStateWithLifecycle()`.
- **Persistence:** Room (SQLite) under `data/local` for Sessions, Notes, Grades and the Catalogue, encrypted at rest
  with SQLCipher (MVP; see Section 12). The encryption key must not depend on the phone's screen lock (ADR 0001).
- **Settings:** Jetpack DataStore (Preferences).
- **DI:** Koin (Kotlin DSL, no code generation), modules in `di/MainModules.kt`; a `verify()` unit test checks the
  dependency graph in CI.
- **Date/time:** `java.time`; Material 3 DatePicker.
- **Export (post-MVP):** Android `PdfDocument` API (or render HTML → print to PDF), shared through the Android share
  sheet / `FileProvider`.
- **Min SDK:** API 26.
- **Testing:** unit tests for sorting/filtering/move rules (these are the logic-heavy parts); Compose UI tests for the
  auto-grow field behaviour run on the JVM with Robolectric, so they are part of `testDebugUnitTest` and the
  acceptance tests.

### 10.3 Implementation notes for tricky behaviour
- **Auto-grow fields:** model per Competency as `List<NoteDraft>` + derived UI list = filled Notes + one empty trailing
  item. Never persist the empty trailing item.
- **Focus-driven tag row:** keep `focusedNoteId` in the ViewModel; show the tag row only for that Note, and only when
  the Session has two Crew Members.
- **Emptied Notes:** remove on focus loss, not on the keystroke that empties the field.
- **Move Note:** single DB transaction (update `competencyId`, `observableBehaviourId = null`, set new `sortIndex`).
- **Generate debriefing:** navigate only; the Debrief is a pure function of (Catalogue, Notes, Grades, selected Crew
  Member).
- **PIN unlock:** an in-memory flag, cleared when the Assessor returns to the home screen or the app goes to the
  background.

---

## 11. Export and sharing (options)

| Option | Description | Complexity |
|--------|-------------|------------|
| X1 | PDF of the Debrief per Crew Member (Grades, OBS BEH, optionally Notes) | Medium |
| X2 | Plain-text/Markdown share to messaging/email | Low |
| X3 | ~~JSON backup/restore of all Sessions~~ — dropped (D26) | — |
| X4 | Print via Android print framework | Low once PDF exists |

Decided: **no PDF export in the MVP** (X1 moves to v0.2); **no backup/restore at all** (D26). X2 as a cheap extra
later. Think about whether the Crew-Member-specific export should include Notes or only OBS BEH + Grades (privacy and
tone; see D7).

---

## 12. Privacy, security, compliance

This app stores **assessment data about identifiable individuals** (Staff Numbers + performance Notes), so treat it as
sensitive.

- **GDPR/AVG** applies if used in the EU/NL: data minimisation, purpose limitation, retention limits, right of
  access/erasure. Staff Numbers are personal data when linkable to a person.
- **Employer policy:** the operator may prohibit storing crew assessment data on personal devices or require approved
  systems. Confirm before real-world use.
- **Regulatory context:** if used for official EBT/CBTA assessment, the operator's approved assessment and grading
  system and record-keeping requirements apply; this app should be positioned as a **personal aide/notebook** unless
  adopted by the operator.
- **Technical controls in the MVP:** local-only storage; encrypted DB (SQLCipher); excluded from cloud auto-backup and
  device transfer (`allowBackup=false`, backup rules); **no lock on opening the app**, but the Session list sits
  behind a 4–6 digit app PIN, and a forgotten PIN wipes all saved Sessions (ADR 0001). The Last Session opens without
  the PIN.
- **Accepted consequence:** Sessions live on one device only and are lost with a lost or replaced phone (D26).
- **Technical controls (suggestions, later):** screenshot blocking (`FLAG_SECURE`) as optional setting; auto-delete
  after N days (configurable); no analytics SDKs/trackers.
- **Branding/name:** "Check Pilot" is a generic term; check Play Store name availability and trademarks before
  publishing.

---

## 13. Roadmap suggestion

| Phase | Content |
|-------|---------|
| **MVP (v0.1)** | Setup (one or two Crew Members), capture list with auto-grow Notes, CM Tags, OBS BEH dropdown, move and erase Note, autosave, home screen with Continue last Session, PIN-protected Session list (search, reopen, duplicate, delete), Debrief per Crew Member with sorting/Grades/eye pop-up, Catalogue editor, local storage encrypted with SQLCipher, no cloud backup |
| v0.2 | PDF export, airport autocomplete |
| v0.3 | Tablet layout polish, search across Session contents |
| v1.0 | Hardening, accessibility audit, Play Store release (private/internal testing first) |
| Later | iOS (if Flutter/KMP chosen), operator-specific Catalogue packs, analytics per pilot over time (only with governance) |

**Risks**
1. Catalogue accuracy/approval (content risk, not code risk) — now in the Assessor's hands via the editor.
2. Data-protection / employer-policy constraints on storing crew assessments.
3. Cockpit usability — needs real-world testing (sim sessions) early.
4. Scope creep towards a full CBTA platform.
5. Data loss: no backup, so a lost phone loses every Session (accepted, D26).

---

## 14. Decisions

> Owner feedback (2026-11-06): all suggestions below were accepted, with these exceptions/confirmations: **D1** order
> is defined by the owner (differs from EASA); **D4** confirmed = distinct OBS BEH; **D12** = IATA 3-letter only. D2 is
> moot (placeholder Catalogue, filled in by the Assessor). Rows updated on 2026-11-08 after the grilling are marked
> **(updated)**.

| # | Topic | Options | Decision |
|---|-------|---------|----------|
| D1 | Competency order | (a) EASA order (b) Owner-defined order | **(b)** owner-defined; set in the Catalogue editor |
| D2 | Which OBS BEH set | (a) ICAO Doc 9995 (b) EASA AMC/GM (c) Operator's own OM-D list | Whichever the intended user is formally assessed against; entered in the Catalogue editor |
| D3 | Grading scale **(updated)** | (a) Fixed 6-step scale as specified (b) Configurable scale | **(a)** fixed in the app for now; not editable |
| D4 | "Most OBS BEH" tie-break meaning | (a) Distinct OBS BEH (b) Number of Notes that have one | **(a)** distinct OBS BEH |
| D5 | OBS BEH per Note | (a) Exactly one (b) Multiple | **(a)** for MVP; (b) later if needed |
| D6 | Sorting basis when the Crew Member filter applies | (a) Per-Crew-Member filtered counts (b) Global counts | **(a)** |
| D7 | What a Crew Member's Debrief shows | (a) OBS BEH + Grades only (b) plus Notes via eye pop-up (c) export includes Notes optionally | **(b)** in-app, (c) toggle on export |
| D8 | Grade storage | (a) Per Crew Member per Competency (b) One Grade per Competency | **(a)** |
| D9 | Empty-Note cleanup **(updated)** | (a) Auto-remove emptied Notes (b) Keep until leaving the screen | **(a)**, on focus loss (not while typing), with undo |
| D10 | Moving a Note with an OBS BEH | (a) Clear silently (b) Confirm first (c) Clear + undo snackbar | **(c)** |
| D11 | Snapshot on "Generate debriefing" **(updated)** | (a) Overwrite one save (b) Keep versions | **Neither:** no snapshots; one living Session, autosaved |
| D12 | Airport codes | (a) IATA (b) ICAO (c) both | **(a)** IATA 3-letter only |
| D13 | Platform strategy | (a) Android-only native (b) Cross-platform from start | **(a)**; revisit if iOS demand is real |
| D14 | Device security **(updated)** | (a) None (b) App lock (c) Encrypted DB + app lock | **Encrypted DB, no app lock, app PIN on the Session list; forgotten PIN wipes saved Sessions** (ADR 0001) |
| D15 | Staff Numbers **(updated)** | (a) Free text (b) Validated format (c) Lookup from a roster | **(b)** 4–6 digits, no letters, stored and shown as typed; no roster |
| D16 | After Catalogue renames, how do old Sessions display? **(updated)** | (a) Original names (b) Always latest names | **(b)** latest names; no catalogue version stored |
| D17 | How to edit names later **(updated)** | (a) Edit the file and rebuild (b) Import JSON from device (c) In-app Catalogue editor | **(c)**, in the MVP (ADR 0002) |

### Stack and scope decisions (2026-11-08)

| # | Topic | Decision |
|---|-------|----------|
| D18 | Dependency injection | Koin (not Hilt); modules in `di/MainModules.kt`, graph checked by a `verify()` unit test |
| D19 | Navigation | Single activity + Navigation Compose |
| D20 | Persistence | Room under `data/local` for Sessions, Notes, Grades **and the Catalogue (updated, ADR 0002)** |
| D21 | Settings | Jetpack DataStore (Preferences) |
| D22 | Min SDK | API 26 |
| D23 | Cloud backup | Off in the MVP (`allowBackup=false`, backup rules exclude all data) |
| D24 | UI tests | Compose UI tests on the JVM with Robolectric, inside `testDebugUnitTest` |
| D25 | Catalogue **(superseded by ADR 0002)** | ~~JSON asset read into memory, not stored in Room~~ → stored in Room, seeded from the bundled placeholder file on first install, edited in the app |
| D26 | Export and backup scope **(updated)** | No PDF in the MVP; **no backup/restore ever** — data lives on one device |
| D27 | Save semantics **(updated)** | No "Save report", no snapshots, no `DRAFT / DEBRIEFED / SAVED` status. Autosave is the only save; "Done" returns home; the list shows Grade progress |

### Grilling decisions (2026-11-08)

| # | Topic | Decision |
|---|-------|----------|
| D28 | What a Session covers | One Sector or sim session. A Line Check is one Session under the outbound Sector; in training, Notes go in the Session of the Sector where they were observed |
| D29 | Crew Members per Session | One or two; at least one Staff Number required, in either slot. CM1/CM2 are slots, not seats or roles |
| D30 | Session with one Crew Member | No CM Tag selector; "Generate debriefing" opens that Crew Member's Debrief directly |
| D31 | Duplicate | Copies Staff Numbers, swaps the route, sets today's date; no Notes or Grades |
| D32 | Finding a Session | List shows route, date, Staff Numbers, Grade progress; one search field for Staff Number, airport code or date |
| D33 | Grading | Never automatic; every Competency starts without a Grade; any Grade allowed without Notes; Not Observed is deliberate |
| D34 | Fairness hint | Dropped (grading without Notes is normal) |
| D35 | Erasing a Note | ✕ on the focused Note deletes only that Note, with undo |
| D36 | Last Session | The most recently *created* Session; opens without the PIN |
| D37 | PIN unlock scope | One PIN entry unlocks the Session list until the Assessor returns home or leaves the app |
| D38 | Changing Staff Numbers | Add or correct any time; remove only when no Notes are tagged to that Crew Member alone |
| D39 | Catalogue editing | Add, rename, reorder; delete only unused items |
| D40 | Where the Catalogue is edited | Settings, from the home screen only; no PIN |
| D41 | App updates and the Catalogue | An update never overwrites the Catalogue on the device |
| D42 | Attachments | None; Sessions are text only |

---

## 15. Sources consulted

- ICAO Doc 9995 — Manual of Evidence-based Training (2013) and EASA EBT regulation material (AMC/GM to ORO.FC.231 /
  ORO.FC.232; Opinion 08/2019).
- EASA — Appendix to Opinion No 08/2019 (competency framework incl. Application of Knowledge):
  https://www.easa.europa.eu/sites/default/files/dfu/update_-_appendix_to_opinion_no_08-2019_a_rmt.0599.pdf
- EASA — EBT abbreviations/competency codes (PRO, KNO, COM, FPA, FPM, LTW, PSD …):
  https://www.easa.europa.eu/de/downloads/125288/en
- Aviatize glossary — core competencies, OBs, CBTA/EBT overview: https://www.aviatize.com/glossary/core-competencies ·
  https://www.aviatize.com/glossary/observable-behaviours
- Aviationexam — the nine core competencies and OBs: https://www.aviationexam.com/en/l/atpl-ksa/
- Airbus Safety First — "Learning from the evidence" (nine core competencies):
  https://safetyfirst.airbus.com/learning-from-the-evidence/amp
- Malta CAA — EBT implementation checklist (assessment and grading system expectations):
  https://www.transport.gov.mt/CAD424-EBT-Implementation_Checklist_TMCAD_Issue-2-April-2022.docx-f7553

> Note: official Observable Behaviour wording and grading scales must be taken from the authoritative document
> applicable to the user's operator; this document intentionally does not reproduce them.
