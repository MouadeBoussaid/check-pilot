package com.mebiad.checkpilot.di

import android.content.Context
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class MainModulesVerifyTest {
    @Test
    fun everyDefinitionInMainModulesCanBeResolved() {
        // Given
        val app = module { includes(mainModules) }

        // When / Then: verify() throws on a dependency no definition provides
        app.verify(extraTypes = listOf(Context::class))
    }
}
