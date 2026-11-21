# Check Pilot

A personal notebook for a pilot assessor: capture observations about a crew during a flight or sim session, then
grade each crew member against a set of competencies.

## People

**Assessor**:
The person using the app: the instructor, check captain or examiner observing the crew.
_Avoid_: User, instructor, check airman, observer

**Crew Member**:
A pilot being observed in a Session, identified only by a Staff Number. A Session has one or two.
_Avoid_: Trainee, pilot, candidate

**CM1 / CM2**:
The two Crew Member slots in a Session. They are labels for telling the Crew Members apart, not seat positions or
roles; either slot may be the empty one.
_Avoid_: Captain, First Officer, left seat, right seat

**Staff Number**:
The operator's 4–6 digit identifier for a Crew Member, and the only personal identifier the app holds.
_Avoid_: Staff ID, employee number

## Sessions

**Sector**:
One flight from a departure airport to a destination airport.
_Avoid_: Leg, flight

**Line Check**:
An assessment covering an outbound and a return Sector, recorded as one Session under the outbound Sector.

**Session**:
The record of one observed Sector or sim session: its Crew Members, route, date, Notes and Grades. There is one
living version of it, never a copy or snapshot.
_Avoid_: Report, save file, record, debriefing

**Last Session**:
The most recently created Session, whatever was edited since.
_Avoid_: Current session, recent session

## Observations and grading

**Note**:
A short free-text observation filed under one Competency in a Session.
_Avoid_: Observation, comment, entry

**CM Tag**:
Which Crew Member a Note is about: CM1, CM2 or Both.
_Avoid_: Target, attribution

**Competency**:
One of the areas a Crew Member is graded on; each Session shows every Competency in the Catalogue's order.
_Avoid_: Category, competence

**Observable Behaviour (OBS BEH)**:
A behaviour that belongs to one Competency; a Note may point to at most one, from its own Competency.
_Avoid_: OB, subcategory, behaviour

**Catalogue**:
The Competencies and their Observable Behaviours, in order. There is one, kept on the device and edited by the
Assessor; every Session, old or new, shows its current content.
_Avoid_: Competency list, OB list, framework

**Grade**:
The Assessor's verdict on one Competency for one Crew Member in one Session, chosen from a fixed scale. A Grade is
never set automatically, and a Competency may have one without any Notes.
_Avoid_: Score, rating, grading

**Not Observed**:
A Grade the Assessor chooses deliberately; it is not the same as having no Grade.

**Debrief**:
One Crew Member's view of a Session: the Notes about them (CM Tag theirs or Both), sorted by evidence, with their
Grades.
_Avoid_: Debriefing report, CM report, report
