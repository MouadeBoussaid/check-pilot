package com.mebiad.checkpilot.di

import android.content.Context
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.ui.apps.debrief.DebriefViewModel
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class MainModulesVerifyTest {
    @Test
    fun everyDefinitionInMainModulesCanBeResolved() {
        // Given
        val app = module { includes(mainModules) }

        // When / Then: verify() throws on a dependency no definition provides
        app.verify(
            extraTypes = listOf(Context::class),
            injections = injectedParameters(definition<DebriefViewModel>(String::class, CrewMember::class))
        )
    }
}
