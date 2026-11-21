package com.mebiad.checkpilot.testutils

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppHarnessTest {
    private val harness = AppHarness()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(harness).around(composeRule)

    @Test
    fun sendingTheAppToTheBackgroundStopsTheProcessLifecycle() {
        // Given
        harness.launch()

        // When
        harness.sendToBackground()

        // Then
        assertEquals(Lifecycle.State.CREATED, ProcessLifecycleOwner.get().lifecycle.currentState)
    }

    @Test
    fun bringingTheAppBackResumesTheProcessLifecycle() {
        // Given
        harness.launch()
        harness.sendToBackground()

        // When
        harness.bringToForeground()

        // Then
        assertEquals(Lifecycle.State.RESUMED, ProcessLifecycleOwner.get().lifecycle.currentState)
    }

    @Test
    fun aRestartKeepsTheDatabaseAndTheSettings() {
        // Given
        val key = stringPreferencesKey("probe")
        harness.launch()
        harness.get<CheckPilotDatabase>().openHelper.writableDatabase.execSQL(
            "UPDATE competency SET name = 'Communication' WHERE name = 'Competency 1'"
        )
        runBlocking { harness.get<DataStore<Preferences>>().edit { it[key] = "kept" } }

        // When
        harness.restart()

        // Then
        val name =
            harness
                .get<CheckPilotDatabase>()
                .openHelper.readableDatabase
                .query("SELECT name FROM competency ORDER BY sortIndex LIMIT 1")
                .use { cursor -> cursor.moveToFirst().let { cursor.getString(0) } }
        val setting = runBlocking { harness.get<DataStore<Preferences>>().data.first()[key] }
        assertEquals("Communication", name)
        assertEquals("kept", setting)
    }

    @Test
    fun aRestartClosesTheDatabaseItReplaces() {
        // Given
        harness.launch()
        val before = harness.get<CheckPilotDatabase>().also { it.openHelper.writableDatabase }

        // When
        harness.restart()

        // Then
        assertFalse(before.isOpen)
    }
}
