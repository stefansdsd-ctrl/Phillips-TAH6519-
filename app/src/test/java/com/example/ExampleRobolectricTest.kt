package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.HeadphoneDao
import com.example.data.HeadphoneSettings
import com.example.data.HeadphoneRepository
import com.example.ui.HeadphoneViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var dao: FakeHeadphoneDao
    private lateinit var repository: HeadphoneRepository
    private lateinit var viewModel: HeadphoneViewModel

    class FakeHeadphoneDao : HeadphoneDao {
        private val state = MutableStateFlow<HeadphoneSettings?>(HeadphoneSettings())

        override fun getSettingsFlow(): Flow<HeadphoneSettings?> = state

        override suspend fun getSettings(): HeadphoneSettings? = state.value

        override suspend fun insertSettings(settings: HeadphoneSettings) {
            state.value = settings
        }
    }

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        dao = FakeHeadphoneDao()
        repository = HeadphoneRepository(dao)
        viewModel = HeadphoneViewModel(application, repository)
    }

    @Test
    fun testDefaultMultipointSettings() = runBlocking {
        val settings = repository.getSettings()
        assertTrue(settings.multipointEnabled)
        assertEquals("Google Pixel 8,MacBook Pro", settings.multipointDevices)
    }

    @Test
    fun testToggleMultipoint() = runBlocking {
        viewModel.toggleMultipoint(false)
        val settings = viewModel.settingsState.first { !it.multipointEnabled }
        assertFalse(settings.multipointEnabled)
    }

    @Test
    fun testAddAndRemoveMultipointDevice() = runBlocking {
        // Adding device
        viewModel.addMultipointDevice("iPad Air")
        var settings = viewModel.settingsState.first { it.multipointDevices.contains("iPad Air") }
        assertEquals("Google Pixel 8,MacBook Pro,iPad Air", settings.multipointDevices)

        // Removing device
        viewModel.removeMultipointDevice("MacBook Pro")
        settings = viewModel.settingsState.first { !it.multipointDevices.contains("MacBook Pro") }
        assertEquals("Google Pixel 8,iPad Air", settings.multipointDevices)
    }

    @Test
    fun testUpdateBatteryLevel() = runBlocking {
        viewModel.updateBatteryLevel(45)
        val settings = viewModel.settingsState.first { it.batteryLevel == 45 }
        assertEquals(45, settings.batteryLevel)
    }

    @Test
    fun testBatteryCoercion() = runBlocking {
        viewModel.updateBatteryLevel(120) // should coerce to 100
        val settings1 = viewModel.settingsState.first { it.batteryLevel == 100 }
        assertEquals(100, settings1.batteryLevel)

        viewModel.updateBatteryLevel(-10) // should coerce to 0
        val settings2 = viewModel.settingsState.first { it.batteryLevel == 0 }
        assertEquals(0, settings2.batteryLevel)
    }

    @Test
    fun testPopPresetAndCustomPresets() = runBlocking {
        // Test system Pop preset is available
        assertTrue(viewModel.presets.containsKey("Pop"))

        // Set Pop preset and verify bands are applied
        viewModel.setPreset("Pop")
        var settings = viewModel.settingsState.first { it.activePreset == "Pop" }
        assertEquals("Pop", settings.activePreset)
        assertEquals(listOf(3.0f, 2.0f, 0.5f, -1.0f, -1.5f, -1.0f, 0.5f, 1.5f, 2.5f, 3.0f), settings.getBands())

        // Save a Custom Preset
        val myBands = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, -5.0f, -4.0f, -3.0f, -2.0f, -1.0f)
        viewModel.saveCustomPreset("My Custom Bass Boost", myBands)
        settings = viewModel.settingsState.first { it.activePreset == "My Custom Bass Boost" }
        assertEquals("My Custom Bass Boost", settings.activePreset)
        assertEquals(myBands, settings.getBands())
        assertTrue(settings.getCustomPresetsMap().containsKey("My Custom Bass Boost"))

        // Delete Custom Preset
        viewModel.deleteCustomPreset("My Custom Bass Boost")
        settings = viewModel.settingsState.first { !it.getCustomPresetsMap().containsKey("My Custom Bass Boost") }
        assertFalse(settings.getCustomPresetsMap().containsKey("My Custom Bass Boost"))
        // Deleted active preset falls back to Flat
        assertEquals("Flat", settings.activePreset)
    }

    @Test
    fun testHearingProfileSaving() = runBlocking {
        val calculatedBands = listOf(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f, 5f, 5f)
        viewModel.saveCustomPreset("Gehoor-ID Profile", calculatedBands)
        val settings = viewModel.settingsState.first { it.getCustomPresetsMap().containsKey("Gehoor-ID Profile") }
        assertTrue(settings.getCustomPresetsMap().containsKey("Gehoor-ID Profile"))
        assertEquals(calculatedBands, settings.getCustomPresetsMap()["Gehoor-ID Profile"])
    }

    @Test
    fun testChargingSimulation() = runBlocking {
        assertFalse(viewModel.isCharging.value)
        
        // Start charging
        viewModel.toggleCharging(true)
        assertTrue(viewModel.isCharging.value)
        
        // Stop charging
        viewModel.toggleCharging(false)
        assertFalse(viewModel.isCharging.value)
    }

    @Test
    fun testResetAll() = runBlocking {
        // Change some settings first
        viewModel.updateBatteryLevel(75)
        viewModel.toggleMultipoint(false)
        
        // Trigger factory reset
        viewModel.resetAll()
        
        // Wait for state updates and verify defaults are restored
        val settings = viewModel.settingsState.first { it.multipointEnabled }
        assertTrue(settings.multipointEnabled)
        assertEquals(88, settings.batteryLevel) // Default battery level is 88
        assertEquals("v1.4.2", viewModel.firmwareVersion.value)
    }

    @Test
    fun testThemeToggling() {
        // Set initial state to Dark Mode
        com.example.ui.theme.ThemeState.isLightMode = false
        assertFalse(com.example.ui.theme.ThemeState.isLightMode)
        com.example.ui.theme.ThemeState.darkBg = androidx.compose.ui.graphics.Color(0xFF090D1A)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF090D1A), com.example.ui.theme.DarkBg)

        // Toggle to Philips Blue Light Mode
        com.example.ui.theme.ThemeState.isLightMode = true
        assertTrue(com.example.ui.theme.ThemeState.isLightMode)
        com.example.ui.theme.ThemeState.darkBg = androidx.compose.ui.graphics.Color(0xFFF0F4FC)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF0F4FC), com.example.ui.theme.DarkBg)

        // Toggle back to Dark Mode
        com.example.ui.theme.ThemeState.isLightMode = false
        assertFalse(com.example.ui.theme.ThemeState.isLightMode)
        com.example.ui.theme.ThemeState.darkBg = androidx.compose.ui.graphics.Color(0xFF090D1A)
    }

    @Test
    fun testCheckForUpdates() = runBlocking {
        // Initial state should be Idle
        assertEquals(com.example.ui.UpdateState.Idle, viewModel.updateState.value)
        
        // Trigger check for updates
        viewModel.checkForUpdates()
        
        // Should transition to Checking immediately
        assertEquals(com.example.ui.UpdateState.Checking, viewModel.updateState.value)
    }

    @Test
    fun testAutoPowerOffAndFastForward() = runBlocking {
        // Toggle Auto Power Off
        viewModel.toggleAutoPowerOff(true)
        var settings = viewModel.settingsState.first { it.autoPowerOffEnabled }
        assertTrue(settings.autoPowerOffEnabled)

        // Set minutes
        viewModel.setAutoPowerOffMinutes(15)
        settings = viewModel.settingsState.first { it.autoPowerOffMinutes == 15 }
        assertEquals(15, settings.autoPowerOffMinutes)

        // Since we are connected but not playing media and not wearing headphones initially,
        // it starts as inactive. Let's toggle wearing detection or make sure we can trigger fast forward.
        viewModel.toggleWearingState(false)
        assertFalse(viewModel.isWearingHeadphones.value)

        // Make sure settings shows connected is true
        viewModel.toggleSimulationMode(true)
        assertTrue(viewModel.settingsState.value.connected)

        // Wait a small moment for background loop to run and initialize the timer
        delay(1200)

        // Trigger fast forward
        viewModel.fastForwardAutoOff()
        // It should set the countdown remaining seconds to 10 if it is above 10
        assertEquals(10, viewModel.autoOffRemainingSeconds.value)
    }
}
