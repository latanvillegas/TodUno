package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CalculationHistoryEntity
import com.example.data.SeleniaDatabase
import com.example.ui.SeleniaViewModel
import com.example.ui.theme.ApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TodUno", appName)
    }

    @Test
    fun `room database atomic insertAndPruneHistoryRecord enforces bounded retention`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SeleniaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.seleniaDao()
            for (i in 1..12) {
                dao.insertAndPruneHistoryRecord(
                    record = CalculationHistoryEntity(
                        expression = "$i × 2",
                        result = "${i * 2}",
                        createdAt = 1_000L + i
                    ),
                    keepLimit = 5
                )
            }
            assertEquals(5, dao.countHistory())

            dao.insertNote(
                com.example.data.CalendarNoteEntity(
                    dateKey = "2026-08-12",
                    year = 2026,
                    month = 8,
                    day = 12,
                    title = "Eclipse Solar Total",
                    category = "Astronomía",
                    reminderEnabled = true
                )
            )
            dao.insertNote(
                com.example.data.CalendarNoteEntity(
                    dateKey = "2026-08-13",
                    year = 2026,
                    month = 8,
                    day = 13,
                    title = "Nota sin alarma",
                    category = "Personal",
                    reminderEnabled = false
                )
            )
            val reminders = dao.getNotesWithReminders()
            assertEquals(1, reminders.size)
            assertEquals("Eclipse Solar Total", reminders.first().title)
        } finally {
            db.close()
        }
    }

    @Test
    fun `moon widget bitmap flips illuminated limb for southern hemisphere`() {
        // First Quarter (phaseFraction = 0.25):
        // Northern Hemisphere -> right side lit, left side dark
        // Southern Hemisphere -> left side lit, right side dark
        val northBmp = com.example.widget.SeleniaMoonWidgetProvider.renderMoonBitmap(
            sizePx = 120,
            phaseFraction = 0.25,
            isNorthernHemisphere = true
        )
        val southBmp = com.example.widget.SeleniaMoonWidgetProvider.renderMoonBitmap(
            sizePx = 120,
            phaseFraction = 0.25,
            isNorthernHemisphere = false
        )
        val leftX = 42
        val rightX = 78
        val midY = 60

        val northRightLum = android.graphics.Color.red(northBmp.getPixel(rightX, midY))
        val northLeftLum = android.graphics.Color.red(northBmp.getPixel(leftX, midY))
        org.junit.Assert.assertTrue(northRightLum > northLeftLum + 100)

        val southLeftLum = android.graphics.Color.red(southBmp.getPixel(leftX, midY))
        val southRightLum = android.graphics.Color.red(southBmp.getPixel(rightX, midY))
        org.junit.Assert.assertTrue(southLeftLum > southRightLum + 100)
    }

    @Test
    fun `seleniaApp navigates cleanly across all four modules`() {
        val vm = SeleniaViewModel()
        composeTestRule.setContent {
            ApplicationTheme {
                SeleniaApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calendar_screen_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_moon").performClick()
        composeTestRule.waitForIdle()
        vm.selectDate(com.example.domain.SimpleDate(2026, 3, 3))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_lunar_eclipse_badge")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("moon_observatory_list")
            .performScrollToNode(hasTestTag("moon_telemetry_grid"))
        composeTestRule.onNodeWithTag("moon_telemetry_grid").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_clock").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("clock_subtab_world").assertIsDisplayed()

        // Verify Customizable World Clock starts empty with zero pre-populated cities,
        // allows the user to add a city, and returns to empty when removed.
        composeTestRule.onNodeWithTag("world_clock_list")
            .performScrollToNode(hasTestTag("world_clocks_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("world_clocks_empty_state").assertIsDisplayed()

        composeTestRule.onNodeWithTag("manage_world_cities_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("picker_city_option_America/Lima").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("world_cities_dialog_done").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("world_clock_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("world_clocks_card"))
        composeTestRule.onNodeWithTag("world_city_row_America/Lima").assertIsDisplayed()

        composeTestRule.onNodeWithTag("remove_world_city_America/Lima").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("world_clocks_empty_state").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_calculator").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calc_result_display").assertIsDisplayed()
        composeTestRule.onNodeWithTag("calc_inv_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("calc_copy_chip").assertIsDisplayed()
    }

    @Test
    fun `json backup export and import round-trips notes history cities and settings`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = Room.inMemoryDatabaseBuilder(context, SeleniaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val db2 = Room.inMemoryDatabaseBuilder(context, SeleniaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo1 = com.example.data.SeleniaRepository(context, db1.seleniaDao())
            repo1.setNorthernHemisphere(false)
            repo1.setSelectedWorldCityIds(listOf("Europe/Madrid", "Asia/Tokyo"))
            repo1.insertNote(
                date = com.example.domain.SimpleDate(2026, 8, 12),
                title = "Eclipse Solar Total España",
                category = "Astronomía",
                reminderEnabled = true,
                reminderHour = 19,
                reminderMinute = 45
            )
            repo1.insertCalculation("5!", "120", null)

            val exportedJson = repo1.exportBackupJson()
            org.junit.Assert.assertTrue(exportedJson.contains("Eclipse Solar Total España"))
            org.junit.Assert.assertTrue(exportedJson.contains("Europe/Madrid"))

            // Import into fresh DB (db2)
            val repo2 = com.example.data.SeleniaRepository(context, db2.seleniaDao())
            val summary = repo2.importBackupJson(exportedJson)
            assertEquals(1, summary.notesImported)
            assertEquals(1, summary.calculationsImported)
            assertEquals(2, summary.worldCitiesCount)

            val restoredSettings = repo2.getPersistedSettingsSnapshot()
            assertEquals(false, restoredSettings.isNorthernHemisphere)
            org.junit.Assert.assertTrue("Europe/Madrid" in restoredSettings.selectedWorldCityIds)
            org.junit.Assert.assertTrue("Asia/Tokyo" in restoredSettings.selectedWorldCityIds)

            val restoredReminders = repo2.getReminderNotes()
            assertEquals(1, restoredReminders.size)
            assertEquals(19, restoredReminders.single().reminderHour)
            assertEquals(45, restoredReminders.single().reminderMinute)

            // Re-importing the exact same backup should deduplicate (0 duplicates added)
            val secondImport = repo2.importBackupJson(exportedJson)
            assertEquals(0, secondImport.notesImported)
            assertEquals(0, secondImport.calculationsImported)
        } finally {
            db1.close()
            db2.close()
        }
    }
}

