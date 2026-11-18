package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

@RunWith(RobolectricTestRunner::class)
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
        } finally {
            db.close()
        }
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
        composeTestRule.onNodeWithTag("moon_observatory_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_clock").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("clock_subtab_world").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_calculator").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calc_result_display").assertIsDisplayed()
    }
}
