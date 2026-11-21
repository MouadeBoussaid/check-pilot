# The Catalogue is edited in the app and stored on the device

The real Competency and Observable Behaviour names aren't available yet, and the Assessor must be able to fill them
in without a new app build. So the Catalogue is stored in the app's database: the bundled placeholder list
(Competency 1–9, each with OBS BEH a–g) is only its starting content on first install, and the Assessor adds, renames
and reorders Competencies and Observable Behaviours in a settings screen. This replaces D25 (Catalogue read into memory
from a bundled JSON asset, never stored) and the rule that the Catalogue is fixed for the user at runtime.

## Consequences

- Catalogue ids are never removed or reused. An item can be deleted only while no Note or Grade refers to it.
- Every Session, old or new, shows the current Catalogue: renames apply everywhere, and an added Competency appears in
  old Sessions without a Grade. Sessions no longer store a catalogue version.
- An app update never overwrites the Catalogue on the device.
- The editor is reachable only from the home screen, so the capture screen never changes during a Session, and it has
  no PIN because it holds no crew data.
- The grading scale is not part of what the Assessor edits; it stays fixed in the app.
