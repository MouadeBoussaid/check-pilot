package com.mebiad.checkpilot.ui.generic.components

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.domain.note.CmTag

/** Says which Crew Member a Note is about, in that Crew Member's colour, or with both dots for BOTH; it can't be tapped. */
@Composable
fun CmTagBadge(
    cmTag: CmTag,
    modifier: Modifier = Modifier,
) {
    val color = cmTag.slot?.color ?: MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (cmTag == CmTag.BOTH) MaterialTheme.colorScheme.outline else color
    Row(
        modifier =
            modifier
                .border(width = 1.dp, color = border, shape = MaterialTheme.shapes.large)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val slots = cmTag.slot?.let { listOf(it) } ?: CrewMemberSlot.entries
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            slots.forEach { CrewMemberDot(slot = it, modifier = Modifier.size(8.dp)) }
        }
        Text(text = stringResource(cmTag.label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

val CmTag.label: Int
    @StringRes get() =
        when (this) {
            CmTag.CM1 -> R.string.crew_member_1
            CmTag.CM2 -> R.string.crew_member_2
            CmTag.BOTH -> R.string.crew_member_both
        }

/** The Crew Member slot a CM Tag names; null for BOTH. */
val CmTag.slot: CrewMemberSlot?
    get() =
        when (this) {
            CmTag.CM1 -> CrewMemberSlot.CM1
            CmTag.CM2 -> CrewMemberSlot.CM2
            CmTag.BOTH -> null
        }
