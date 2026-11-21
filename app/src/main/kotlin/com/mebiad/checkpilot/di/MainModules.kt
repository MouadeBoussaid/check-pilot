package com.mebiad.checkpilot.di

import com.mebiad.checkpilot.core.clock.Clock
import com.mebiad.checkpilot.core.clock.ClockImpl
import org.koin.dsl.module

val coreModule =
    module {
        single<Clock> { ClockImpl() }
    }

val mainModules = listOf(coreModule)
