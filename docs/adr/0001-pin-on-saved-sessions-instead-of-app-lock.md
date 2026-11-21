# A PIN on saved Sessions instead of an app lock; a forgotten PIN wipes them

The app has no lock on opening: the Assessor holds the phone during the assessment, and a PIN prompt every time they
switch apps would cost time in front of the crew. Instead, the Session list sits behind a 4–6 digit app PIN, separate
from the phone's screen lock; one entry unlocks the list until the Assessor goes back to the home screen or leaves the
app. Only the Last Session (the most recently created one) opens without the PIN, so that the Session being worked on
is always one tap away. There is no server or account to recover a forgotten PIN, so "Forgot PIN" deletes all saved
Sessions before a new PIN is set; a reset that kept the data would let anyone holding the phone bypass the PIN. The
database stays encrypted at rest regardless. This replaces D14 (encrypted database plus a device-credential app lock).

## Consequences

- "Last Session" means most recently *created*, never most recently *edited*: otherwise opening an old Session with
  the PIN and editing it would make it reachable without the PIN.
- The encryption key must not depend on the device credential, because a phone without a screen lock must still be
  able to run the app.
- There is no backup or export of any kind, so Sessions don't survive a lost or replaced phone. The owner accepted
  this.
