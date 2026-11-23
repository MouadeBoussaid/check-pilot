package com.mebiad.checkpilot.domain.catalogue

/** [observableBehaviours] are the Competency's own OBS BEH, in Catalogue order. */
data class Competency(
    val id: Long,
    val name: String,
    val observableBehaviours: List<ObservableBehaviour> = emptyList(),
)
