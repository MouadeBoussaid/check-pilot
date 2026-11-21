# Design doc grilling — open questions

Each question has a recommendation. Fill in **Answer** (or just "agree").

Background: the app starts with the placeholder list (Competency 1–9, each with OBS BEH a–g), and you can add
Competencies and OBS BEH names in the app. That replaces D25 (catalogue read-only from a bundled file) and the "fixed at
runtime" rule in 9.3 / C1.

## Q25 — What can you do in the catalogue editor?
"Adding" alone won't replace the placeholders: "Competency 1" needs to become its real name.
- Add a Competency or an OBS BEH
- Rename a Competency or an OBS BEH
- Reorder Competencies, and OBS BEH within a Competency
- Delete a Competency or an OBS BEH

**Recommendation:** add, rename and reorder are allowed. Delete is allowed only for an item no Note or Grade uses yet,
because saved Sessions refer to it (Q6: ids are never removed).

**Answer:**

## Q26 — What happens to existing Sessions when the catalogue changes?
Scenario: you add a 10th Competency. Sessions from last month had 9.
- Renamed items show the new name everywhere, old Sessions too (Q6).
- An added Competency also appears in old Sessions, empty and ungraded, so their progress drops to "Grades 9/10".

**Recommendation:** accept both. Old Sessions always show the current catalogue.

**Answer:**

## Q27 — Can you edit the catalogue in the middle of a Session?
Scenario: during a flight you notice an OBS BEH is missing and want to add it right away.

**Recommendation:** no. The editor lives in the settings, reachable from the home screen only, so the capture screen
never changes under your hands.

**Answer:**

## Q28 — Is the grading scale editable too?
The 6 Grades (Not Observed … Not Yet Competent) are in the same catalogue.

**Recommendation:** no, the scale stays fixed in the app for now. Only Competencies and OBS BEH are editable.

**Answer:**

## Q29 — Is the catalogue editor behind the PIN?
The catalogue contains no personal data, but a colleague could rename everything.

**Recommendation:** no PIN. It holds no crew data, and Q10 settled that colleagues fiddling with the phone is not the
app's problem.

**Answer:**

## Also open
- **Glossary:** should `CONTEXT.md` (the agreed terms: Session, Debrief, CM1/CM2, Staff Number, Catalogue, …) be
  written now, at the end, or not at all?

  **Answer:**
