package com.mebiad.checkpilot.testutils

import android.app.Application

/** Leaves Koin to [AppHarness], so each test decides which modules and overrides it starts. */
class TestApplication : Application()
