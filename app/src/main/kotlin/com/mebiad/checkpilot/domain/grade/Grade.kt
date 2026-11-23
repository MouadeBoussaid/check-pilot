package com.mebiad.checkpilot.domain.grade

/**
 * The Grade scale, in the order the Assessor picks from. The names are the codes stored in the database, never the
 * ordinals, so reordering is safe but renaming isn't. Not Observed is a Grade: it counts as graded.
 */
enum class Grade {
    NOT_OBSERVED,
    EXEMPLARY,
    EFFECTIVE,
    ADEQUATE,
    MINIMUM_ACCEPTABLE_LEVEL,
    NOT_YET_COMPETENT,
}
