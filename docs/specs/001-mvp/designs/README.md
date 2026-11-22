# Designs: Check Pilot MVP

Variant A, "Amber cards", is the design to build. The owner chose it in November 2026 over Variant B (Instrument) and
Variant C (One-handed). The source is the "Check Pilot screens" canvas in Claude Design
(<https://claude.ai/artifact/XgdUoPQpgZf4gMhA6yQNHJ>), top row. The PNGs here are its artboards rendered at 390×844,
2× scale. Where the canvas and a PNG differ, the canvas wins; render it again and replace the PNG.

## Screens

| File | Screen | Stories |
|---|---|---|
| `01-home.png` | Home: Last Session card, New Session, Open Session, Settings gear | US-1, US-5 |
| `02-setup.png` | New Session setup, with an airport-code error | US-1 |
| `03-capture-note-focused.png` | Capture, a Note focused: CM Tag, OBS BEH, move, ✕ | US-1, US-2, US-3, US-6, US-11 |
| `04-capture-writing-note.png` | Capture, writing a Note full screen above the keyboard, Next Note | US-2, US-3 |
| `05-capture-end-undo.png` | Capture, end of the list with Done, undo snackbar | US-2, US-4, US-6 |
| `06-debrief-pick-cm.png` | Generate Debrief: which Crew Member | US-4 |
| `07-debrief-grade-menu.png` | Debrief: CM toggle, Grade progress, Grade menu open | US-4 |
| `08-debrief-notes-popup.png` | Debrief: the eye pop-up listing a Competency's Notes | US-4 |
| `09-pin-wrong-entry.png` | Enter PIN, wrong PIN, Forgot PIN | US-7, US-8 |
| `10-session-list.png` | Session list: search, date filter, Capture and Debrief per Session, ⋮ menu | US-7, US-10 |
| `11-catalogue-editor.png` | Catalogue editor: reorder, rename, add, delete while unused, "used in N Sessions" | US-9 |
| `12-settings.png` | Settings, from the Home gear: Change PIN, Edit the Catalogue | US-8, US-9 |
| `13-pin-create.png` | Set a PIN: typed twice, "The PINs don't match.", 4 to 6 digits, Start over | US-7, US-8 |
| `14-pin-lockout.png` | PIN lockout after 5 wrong PINs: wait 30 seconds, keypad disabled, Forgot PIN | US-8 |
| `15-forgot-pin.png` | Forgot PIN: deletes all saved Sessions, the Catalogue is kept | US-8 |
| `16-session-menu.png` | Session list, ⋮ menu open: Duplicate, Delete | US-7, US-10 |
| `17-delete-session.png` | Delete a Session: confirmation with a summary of it | US-7 |
| `18-move-note.png` | Move a Note: pick the Competency, the OBS BEH link is removed | US-6 |
| `19-edit-staff-number.png` | Edit a Staff Number from its chip, Remove refused while Notes are tagged | US-11 |
| `20-remove-crew-member.png` | Remove CM2: confirmation that CM2's Grades are deleted | US-11 |
| `21-date-picker.png` | Setup date picker: selected date filled, today ringed | US-1 |
| `22-leave-guard.png` | Leaving a Debrief with ungraded Competencies: Stay, Leave | US-4 |

## Look

- Dark only, warm: background `#1A1712`, cards `#25211A`, raised `#2F2A21`, borders `#3E372C` / `#4A4236`.
- Text `#F2EADB`, secondary `#B9AE98`, hint `#968A74`.
- Accent amber `#F2B544`, with `#231803` text on it. Error `#FF8A80`.
- CM1 is blue `#8CCBFF` with a round dot; CM2 is orange `#E8873F` with a square dot. Shape as well as colour tells
  them apart.
- Type: Atkinson Hyperlegible Next for text, Atkinson Hyperlegible Mono for Staff Numbers, airport codes and routes.
- Primary buttons 64 dp high, other controls at least 48 dp. Corners 8 dp on Notes, 12 dp on fields and buttons.
- Icons are Material Symbols.

## Not drawn

Every screen the stories need has a picture now. These states don't, and reuse a drawn one:

- the undo snackbar after moving a Note (US-6): as in `05-capture-end-undo`
- the "At least one Staff Number is required." refusal (US-11): as in `19-edit-staff-number`

## What the pictures don't show

- The status bar, gesture bar and keyboard are the system's, drawn for context only.
- The capture screen scrolls as one column. Scrolling must never take focus away from a Note (technical plan,
  Decisions).
- Session list rows and the Grade progress use the demo data in the canvas; layout, not content, is the design.
