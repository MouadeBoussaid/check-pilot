package com.mebiad.checkpilot.ui.generic.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mebiad.checkpilot.R
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

/** A Crew Member slot. CM1 is blue with a round dot, CM2 orange with a square one: shape as well as colour. */
enum class CrewMemberSlot(
    @param:StringRes val label: Int,
) {
    CM1(R.string.crew_member_1),
    CM2(R.string.crew_member_2),
}

@Composable
fun CrewMemberDot(
    slot: CrewMemberSlot,
    modifier: Modifier = Modifier,
) {
    val colors = CheckPilotTheme.colors
    val (color, shape) =
        when (slot) {
            CrewMemberSlot.CM1 -> colors.crewMember1 to CircleShape
            CrewMemberSlot.CM2 -> colors.crewMember2 to RoundedCornerShape(2.dp)
        }
    Box(modifier = modifier.size(12.dp).background(color = color, shape = shape))
}
