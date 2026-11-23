package com.mebiad.checkpilot.domain.catalogue

/** An Observable Behaviour (OBS BEH) of the Competency [competencyId]. */
data class ObservableBehaviour(
    val id: Long,
    val competencyId: Long,
    val name: String,
)
