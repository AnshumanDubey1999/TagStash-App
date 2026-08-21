package com.anshuman.tagstash.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import androidx.compose.material.icons.filled.ContentCopy
import com.anshuman.tagstash.ui.components.DeleteConfirmationDialog
import com.anshuman.tagstash.ui.components.OperationProgressDialog
import com.anshuman.tagstash.ui.components.OperationProgressState
import com.anshuman.tagstash.ui.components.RenameDialog
import com.anshuman.tagstash.ui.theme.TagStashTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-xhdpi") // Dark theme standard size
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        kotlinx.coroutines.runBlocking {
            RecycleBinDatabaseHelper.getInstance(context).clearAll()
            AuditLogDatabaseHelper.getInstance(context).clearAllLogs()
        }
        AppClipboard.clear()
    }

    @After
    fun tearDown() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        kotlinx.coroutines.runBlocking {
            RecycleBinDatabaseHelper.getInstance(context).clearAll()
            AuditLogDatabaseHelper.getInstance(context).clearAllLogs()
        }
        AppClipboard.clear()
    }

    private val testDataDir = if (File("src/test/resources/testData").exists()) {
        File("src/test/resources/testData")
    } else {
        File("app/src/test/resources/testData")
    }

    @Test
    fun testPermissionRequestView() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = false,
                    onRequestPermission = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/permission_request_view.png")
    }

    @Test
    fun testMainScreenWithTestData() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_testdata.png")
    }

    @Test
    fun testMainScreenNavigateToImages() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir,
                    initialDirectory = File(testDataDir, "images")
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_images_subfolder.png")
    }

    @Test
    fun testStateRestorationOnConfigurationChange() {
        val restorationTester = androidx.compose.ui.test.junit4.StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Navigate into images folder
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("images").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("pngs").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("pngs").assertIsDisplayed()
        composeTestRule.onNodeWithText("gifs").assertIsDisplayed()

        // Emulate screen rotation / state restoration
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // Assert we are STILL in images subfolder (NOT back to homeDirectory!)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("pngs").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("pngs").assertIsDisplayed()
        composeTestRule.onNodeWithText("gifs").assertIsDisplayed()

        // Capture visual proof of the preserved state after rotation
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_subfolder_after_rotation.png")
    }

    @Test
    fun testFolderNavigationAndSelectionPreservedAcrossRotation() {
        val restorationTester = androidx.compose.ui.test.junit4.StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // 1. Navigate into 'images' subfolder
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("images").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("pngs").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("pngs").assertIsDisplayed()

        // 2. Enter selection mode and select 'pngs'
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("pngs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()

        // 3. Emulate device rotation / state restoration
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // 4. Verify we are STILL in 'images' subfolder AND '1 item selected' is retained
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("pngs").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("pngs").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()

        // Capture Roborazzi screenshot showing preserved subfolder and selection mode after rotation
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_selection_and_folder_after_rotation.png")

        // Clean exit from selection mode
        composeTestRule.onNodeWithContentDescription("Close selection mode").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMediaPlayerScreenWithImage() {
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/image_viewer_screen.png")
    }

    @Test
    fun testMediaPlayerScreenWithImageAndHeader() {
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }
        
        // Click on the center to toggle header visibility
        composeTestRule.onRoot().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/image_viewer_screen_with_header.png")
    }

    @Test
    fun testVideoPlayerScreen() {
        val videosFolder = File(testDataDir, "videos")
        val file1 = File(videosFolder, "1.mp4")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/video_player_screen.png")
    }

    @Test
    fun testVideoPlayerScreenWithControls() {
        val videosFolder = File(testDataDir, "videos")
        val file1 = File(videosFolder, "1.mp4")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }
        
        // Click in the center to toggle controls visibility
        composeTestRule.onRoot().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/video_player_screen_with_controls.png")
    }

    @Test
    fun testMainScreenFolderPropertiesDialog() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_folder_properties_dialog.png")
    }

    @Test
    fun testMediaPlayerScreenPropertiesDialog() {
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }

        // Toggle overlays
        composeTestRule.onRoot().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        // Click 3-dot overflow menu in header
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        // Click Info
        composeTestRule.onNodeWithText("Info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("screenshots/media_player_properties_dialog.png")
    }

    @Test
    fun testMainScreenSelectionModeViaTopMenu() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Open top menu and click Select
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()

        // Verify bottom hovering bar is displayed with 0 items selected
        composeTestRule.onNodeWithText("0 items selected").assertIsDisplayed()

        // Click images folder to select it
        composeTestRule.onNodeWithText("images").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()

        // Click videos folder to select it
        composeTestRule.onNodeWithText("videos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 items selected").assertIsDisplayed()

        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_selection_mode.png")

        // Close selection mode
        composeTestRule.onNodeWithContentDescription("Close selection mode").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMainScreenSelectionModeViaLongPress() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Long press on images folder
        composeTestRule.onNodeWithText("images").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Click Select
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Select").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()

        // Verify selection mode active and images is selected
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()

        // Close selection mode
        composeTestRule.onNodeWithContentDescription("Close selection mode").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMainScreenSelectionModeSelectAllAndProperties() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter selection mode
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()

        // Select All
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select All").performClick()
        composeTestRule.waitForIdle()

        // Open Info for the selection
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Info").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Selection Properties").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_selection_properties_dialog.png")

        // Close Selection Properties Dialog
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Close selection mode
        composeTestRule.onNodeWithContentDescription("Close selection mode").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMainScreenSelectionModeCloseButton() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter selection mode and select an item
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("images").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()

        // Close selection mode
        composeTestRule.onNodeWithContentDescription("Close selection mode").performClick()
        composeTestRule.waitForIdle()

        // Verify "Select" is back in top menu (meaning selection mode exited)
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").assertIsDisplayed()
    }

    @Test
    fun testMediaPlayerScreenCutCopyAndClipboardDialog() {
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")

        composeTestRule.setContent {
            TagStashTheme {
                MediaPlayerScreen(
                    file = file1,
                    globalLoopEnabled = true,
                    onToggleGlobalLoop = {},
                    onClose = {},
                    onNavigateToMedia = {}
                )
            }
        }

        // Toggle overlays
        composeTestRule.onRoot().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Click 3 dots menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()

        // Click Copy
        composeTestRule.onNodeWithText("Copy").performClick()
        composeTestRule.waitForIdle()

        // Verify item added to clipboard
        org.junit.Assert.assertEquals(1, AppClipboard.size)

        // Open 3 dots menu again -> Click Clipboard (1)
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Clipboard (1)").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Clipboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("COPY").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/media_player_clipboard_dialog.png")
    }

    @Test
    fun testMainScreenPasteAndConflictDialog() {
        val tempDir = File.createTempFile("testPasteConflict", "").apply {
            delete()
            mkdir()
        }
        try {
            // Create a file in tempDir
            val existingFile = File(tempDir, "sample.txt").apply { writeText("original content") }

            // Put a file with same name into AppClipboard
            val sourceFile = File(testDataDir, "sample.txt").apply { writeText("new incoming content") }
            AppClipboard.addItem(sourceFile, ClipboardOpType.COPY)

            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = tempDir
                    )
                }
            }

            // Open top 3-dot menu
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()

            // Verify Paste (1) and Clipboard (1) are present
            composeTestRule.onNodeWithText("Paste (1)").assertIsDisplayed()
            composeTestRule.onNodeWithText("Clipboard (1)").assertIsDisplayed()

            // Click Paste (1)
            composeTestRule.onNodeWithText("Paste (1)").performClick()
            composeTestRule.waitForIdle()

            // Verify Conflict Dialog is shown
            composeTestRule.onNodeWithText("File Conflict").assertIsDisplayed()
            composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_conflict_resolution_dialog.png")

            // Click Keep Both
            composeTestRule.onNodeWithText("Keep Both (Rename with copy)").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(5000) { AppClipboard.size == 0 }

            // Verify clipboard is cleared
            org.junit.Assert.assertEquals(0, AppClipboard.size)
        } finally {
            tempDir.deleteRecursively()
            File(testDataDir, "sample.txt").delete()
        }
    }

    @Test
    fun testMainScreenClipboardDialog() {
        val file1 = File(testDataDir, "sample.txt").apply { writeText("text") }
        AppClipboard.addItem(file1, ClipboardOpType.CUT)

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = testDataDir
                    )
                }
            }

            // Open top 3-dot menu -> Click Clipboard (1)
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Clipboard (1)").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Clipboard").assertIsDisplayed()
            composeTestRule.onNodeWithText("CUT").assertIsDisplayed()

            // Click Clear All
            composeTestRule.onNodeWithText("Clear All").performClick()
            composeTestRule.waitForIdle()

            // Verify clipboard cleared
            org.junit.Assert.assertEquals(0, AppClipboard.size)
        } finally {
            file1.delete()
        }
    }

    @Test
    fun testFileRowLongPressCutAndCopy() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Long press on images folder
        composeTestRule.onNodeWithText("images").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Verify options
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Copy").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Select").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Info").assertIsDisplayed()

        // Click Copy
        composeTestRule.onNodeWithText("Copy").performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertEquals(1, AppClipboard.size)
        val imagesFile = File(testDataDir, "images")
        org.junit.Assert.assertEquals(ClipboardOpType.COPY, AppClipboard.getOpType(imagesFile))
    }

    @Test
    fun testSelectionModeCutAndCopyBottomBar() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Wait for directory contents to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter selection mode
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()

        // Select images and videos
        composeTestRule.onNodeWithText("images").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("videos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 items selected").assertIsDisplayed()

        // Click Cut button on bottom bar
        composeTestRule.onNodeWithContentDescription("Cut selected files").performClick()
        composeTestRule.waitForIdle()

        // Verify 2 items in clipboard as CUT and selection mode exited
        org.junit.Assert.assertEquals(2, AppClipboard.size)
        org.junit.Assert.assertEquals(ClipboardOpType.CUT, AppClipboard.getOpType(File(testDataDir, "images")))
        org.junit.Assert.assertEquals(ClipboardOpType.CUT, AppClipboard.getOpType(File(testDataDir, "videos")))

        // Verify selection mode exited
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select").assertIsDisplayed()
    }

    @Test
    fun testFloatingPasteButtonWithBadge() {
        val file1 = File(testDataDir, "sample.txt").apply { writeText("text") }
        AppClipboard.addItem(file1, ClipboardOpType.COPY)

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = testDataDir
                    )
                }
            }

            // Verify FAB is displayed with badge
            composeTestRule.onNodeWithContentDescription("Paste from clipboard").assertIsDisplayed()
            composeTestRule.onNodeWithText("1").assertIsDisplayed()
            composeTestRule.onRoot().captureRoboImage("screenshots/main_screen_floating_paste_button.png")

            // Long press images row -> context menu opens -> FAB should be hidden
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("images").performTouchInput { longClick() }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Paste from clipboard").assertDoesNotExist()

            // Dismiss menu by clicking Info
            composeTestRule.onNodeWithText("Info").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Close properties dialog").performClick()
            composeTestRule.waitForIdle()

            // FAB should reappear
            composeTestRule.onNodeWithContentDescription("Paste from clipboard").assertIsDisplayed()
        } finally {
            file1.delete()
        }
    }

    @Test
    fun testSettingsScreenNavigationAndPastActions() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbHelper = com.anshuman.tagstash.data.database.AuditLogDatabaseHelper.getInstance(context)
        kotlinx.coroutines.runBlocking { dbHelper.clearAllLogs() }

        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open 3-dot menu -> Click Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Verify Settings Screen
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Past Actions").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/settings_screen.png")

        // Click Past Actions
        composeTestRule.onNodeWithText("Past Actions").performClick()
        composeTestRule.waitForIdle()

        // Wait for Past Actions to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("No Past Actions").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Past Actions Screen with Empty State
        composeTestRule.onNodeWithText("Past Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Past Actions").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/past_actions_empty_screen.png")

        // Click Back -> Returns to Settings
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Click Back -> Returns to MainScreen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("More options").assertIsDisplayed()
    }

    @Test
    fun testPasteAuditLoggingAndDetailDialog() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbHelper = com.anshuman.tagstash.data.database.AuditLogDatabaseHelper.getInstance(context)
        kotlinx.coroutines.runBlocking { dbHelper.clearAllLogs() }

        val tempDir = File(testDataDir, "dest_audit_folder").apply { mkdirs() }
        val sampleFile = File(testDataDir, "audit_sample.txt").apply { writeText("audit text") }
        AppClipboard.addItem(sampleFile, ClipboardOpType.COPY)

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = tempDir,
                        initialDirectory = tempDir
                    )
                }
            }

            // Click top 3-dot menu -> Paste (1)
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Paste (1)").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(5000) { AppClipboard.size == 0 }

            // Navigate to Settings -> Past Actions
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Past Actions").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(5000) { composeTestRule.onAllNodesWithText("1 item • To: dest_audit_folder").fetchSemanticsNodes().isNotEmpty() }

            // Verify Action card is displayed
            composeTestRule.onNodeWithText("1 item • To: dest_audit_folder").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Paste Action").assertIsDisplayed()
            composeTestRule.onRoot().captureRoboImage("screenshots/past_actions_with_entries.png")

            // Click the action card to open details dialog
            composeTestRule.onNodeWithText("1 item • To: dest_audit_folder").performClick()
            composeTestRule.waitForIdle()

            // Verify Action Details Dialog
            composeTestRule.onNodeWithText("Action Details").assertIsDisplayed()
            composeTestRule.onNodeWithText("audit_sample.txt").assertIsDisplayed()
            composeTestRule.onNodeWithText("Source").assertIsDisplayed()
            composeTestRule.onNodeWithText("Destination").assertIsDisplayed()
            composeTestRule.onNodeWithText("COPY").assertIsDisplayed()
            composeTestRule.onNodeWithText("COPIED").assertIsDisplayed()
            composeTestRule.onRoot().captureRoboImage("screenshots/action_detail_dialog.png")

            // Click Delete Log
            composeTestRule.onNodeWithText("Delete Log").performClick()
            composeTestRule.waitForIdle()
            // Confirm deletion
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Verify empty state is shown after deletion
            composeTestRule.onNodeWithText("No Past Actions").assertIsDisplayed()

            // Navigate back to Settings -> MainScreen
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        } finally {
            sampleFile.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking { dbHelper.clearAllLogs() }
        }
    }

    @Test
    fun testDeleteConfirmationDialogSingleFile() {
        var confirmed = false
        val dummyFile = File(testDataDir, "images/pngs/1.png")
        composeTestRule.setContent {
            TagStashTheme {
                DeleteConfirmationDialog(
                    files = listOf(dummyFile),
                    onConfirmDelete = { confirmed = true },
                    onDismissRequest = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Delete File").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click here if you are sure you want to delete this item").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/delete_confirmation_dialog_single.png")

        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
        composeTestRule.waitForIdle()
        org.junit.Assert.assertTrue(confirmed)
    }

    @Test
    fun testDeleteConfirmationDialogMultipleFiles() {
        val file1 = File(testDataDir, "images/pngs/1.png")
        val file2 = File(testDataDir, "images/jpgs/1.jpg")
        composeTestRule.setContent {
            TagStashTheme {
                DeleteConfirmationDialog(
                    files = listOf(file1, file2),
                    onConfirmDelete = {},
                    onDismissRequest = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Delete Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click here if you are sure you want to delete these 2 items").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.jpg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete (2)").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("screenshots/delete_confirmation_dialog_multiple.png")
    }

    @Test
    fun testDeleteFileMovesToRecycleBinAndLogsAudit() {
        val tempDir = File(testDataDir, "temp_delete_test_folder")
        if (!tempDir.exists()) tempDir.mkdirs()
        val fileToDelete = File(tempDir, "to_be_deleted.txt").apply {
            writeText("Delete me please")
        }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val recycleBinHelper = RecycleBinDatabaseHelper.getInstance(context)
        val auditHelper = AuditLogDatabaseHelper.getInstance(context)

        try {
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
            }

            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = tempDir,
                        initialDirectory = tempDir
                    )
                }
            }

            // Wait for file list to load
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("to_be_deleted.txt").fetchSemanticsNodes().isNotEmpty()
            }

            // Long click file to open context menu
            composeTestRule.onNodeWithText("to_be_deleted.txt").performTouchInput { longClick() }
            composeTestRule.waitForIdle()

            // Click Delete in context menu
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Verify Delete Confirmation Dialog is shown
            composeTestRule.onNodeWithText("Delete File").assertIsDisplayed()
            composeTestRule.onNodeWithText("Click here if you are sure you want to delete this item").assertIsDisplayed()

            // Click checkbox row to confirm
            composeTestRule.onNode(isToggleable()).performClick()
            composeTestRule.waitForIdle()

            // Click Delete button in dialog
            composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
            composeTestRule.waitForIdle()

            // Wait for file to be moved to recycle bin and audit log to be written
            composeTestRule.waitUntil(5000) {
                !fileToDelete.exists() && kotlinx.coroutines.runBlocking { auditHelper.getAllLogs().isNotEmpty() }
            }
            org.junit.Assert.assertFalse(fileToDelete.exists())

            // Verify recycle bin item and audit log
            val trashed = kotlinx.coroutines.runBlocking { recycleBinHelper.getAllItems() }
            org.junit.Assert.assertEquals(1, trashed.size)
            org.junit.Assert.assertEquals("to_be_deleted.txt", trashed.first().fileName)

            val logs = kotlinx.coroutines.runBlocking { auditHelper.getAllLogs() }
            org.junit.Assert.assertEquals(1, logs.size)
            org.junit.Assert.assertEquals(com.anshuman.tagstash.data.model.AuditActionType.DELETE, logs.first().actionType)
        } finally {
            fileToDelete.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
            }
        }
    }

    @Test
    fun testRecycleBinScreenEmptyState() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val recycleBinHelper = RecycleBinDatabaseHelper.getInstance(context)
        kotlinx.coroutines.runBlocking { recycleBinHelper.clearAll() }

        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Click Recycle Bin row
        composeTestRule.onNodeWithText("Recycle Bin").performClick()
        composeTestRule.waitForIdle()

        // Wait for Empty State to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Recycle Bin is Empty").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Empty State is shown
        composeTestRule.onNodeWithText("Recycle Bin is Empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deleted files will appear here and be kept for 1 hour before permanent removal.").assertIsDisplayed()

        // Click Back -> Settings
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Click Back -> MainScreen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testRecycleBinScreenRestoreAndPermanentDelete() {
        val tempDir = File(testDataDir, "temp_recycle_bin_screen_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "restore_ui_test.txt").apply { writeText("Content 1") }
        val file2 = File(tempDir, "permanent_delete_ui_test.txt").apply { writeText("Content 2") }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val recycleBinHelper = RecycleBinDatabaseHelper.getInstance(context)
        val auditHelper = AuditLogDatabaseHelper.getInstance(context)

        try {
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
                recycleBinHelper.moveToRecycleBin(listOf(file1, file2), context)
            }

            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = testDataDir
                    )
                }
            }

            // Open Settings
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.waitForIdle()

            // Click Recycle Bin row
            composeTestRule.onNodeWithText("Recycle Bin").performClick()
            composeTestRule.waitForIdle()

            // Wait for items to load from database
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("restore_ui_test.txt").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify items are displayed
            composeTestRule.onNodeWithText("restore_ui_test.txt").assertIsDisplayed()
            composeTestRule.onNodeWithText("permanent_delete_ui_test.txt").assertIsDisplayed()
            composeTestRule.onNodeWithText("1-Hour Temporary Storage").assertIsDisplayed()

            // Click Restore on file 1
            composeTestRule.onNodeWithContentDescription("Restore restore_ui_test.txt").performClick()
            composeTestRule.waitForIdle()

            // Verify file1 is restored on disk
            composeTestRule.waitUntil(5000) { file1.exists() }
            org.junit.Assert.assertTrue(file1.exists())

            // Click Delete Permanently on file 2
            composeTestRule.onNodeWithContentDescription("Delete permanently permanent_delete_ui_test.txt").performClick()
            composeTestRule.waitForIdle()

            // Confirm permanent deletion in dialog
            composeTestRule.onNodeWithText("Delete Permanently?").assertIsDisplayed()
            composeTestRule.onNodeWithText("Delete Permanently").performClick()
            composeTestRule.waitForIdle()

            // Verify Recycle bin is now empty
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Recycle Bin is Empty").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Recycle Bin is Empty").assertIsDisplayed()

            // Navigate back to Settings -> MainScreen
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        } finally {
            file1.delete()
            file2.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
            }
        }
    }

    @Test
    fun testMediaPlayerDeleteFirstFileNavigatesToNext() {
        val tempDir = File(testDataDir, "temp_mp_delete_test_1")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "img1.png").apply { writeText("img1") }
        val file2 = File(tempDir, "img2.png").apply { writeText("img2") }

        var navigatedTarget: File? = null
        var closed = false

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MediaPlayerScreen(
                        file = file1,
                        globalLoopEnabled = true,
                        onToggleGlobalLoop = {},
                        onClose = { closed = true },
                        onNavigateToMedia = { navigatedTarget = it }
                    )
                }
            }

            // Click center to show overlays
            composeTestRule.onRoot().performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Click 3-dots menu in header
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()

            // Click Delete
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Confirm delete
            composeTestRule.onNode(isToggleable()).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
            composeTestRule.waitForIdle()

            // Should navigate to img2.png
            composeTestRule.waitUntil(5000) { navigatedTarget != null }
            org.junit.Assert.assertEquals(file2.absolutePath, navigatedTarget?.absolutePath)
            org.junit.Assert.assertFalse(closed)
        } finally {
            file1.delete()
            file2.delete()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMediaPlayerDeleteLastFileNavigatesToPrevious() {
        val tempDir = File(testDataDir, "temp_mp_delete_test_2")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "img1.png").apply { writeText("img1") }
        val file2 = File(tempDir, "img2.png").apply { writeText("img2") }

        var navigatedTarget: File? = null
        var closed = false

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MediaPlayerScreen(
                        file = file2,
                        globalLoopEnabled = true,
                        onToggleGlobalLoop = {},
                        onClose = { closed = true },
                        onNavigateToMedia = { navigatedTarget = it }
                    )
                }
            }

            // Click center to show overlays
            composeTestRule.onRoot().performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Click 3-dots menu in header
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()

            // Click Delete
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Confirm delete
            composeTestRule.onNode(isToggleable()).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
            composeTestRule.waitForIdle()

            // Should navigate back to img1.png
            composeTestRule.waitUntil(5000) { navigatedTarget != null }
            org.junit.Assert.assertEquals(file1.absolutePath, navigatedTarget?.absolutePath)
            org.junit.Assert.assertFalse(closed)
        } finally {
            file1.delete()
            file2.delete()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMediaPlayerDeleteSingleFileClosesPlayer() {
        val tempDir = File(testDataDir, "temp_mp_delete_test_3")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "single_img.png").apply { writeText("single") }

        var navigatedTarget: File? = null
        var closed = false

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MediaPlayerScreen(
                        file = file1,
                        globalLoopEnabled = true,
                        onToggleGlobalLoop = {},
                        onClose = { closed = true },
                        onNavigateToMedia = { navigatedTarget = it }
                    )
                }
            }

            // Click center to show overlays
            composeTestRule.onRoot().performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Click 3-dots menu in header
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()

            // Click Delete
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Confirm delete
            composeTestRule.onNode(isToggleable()).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
            composeTestRule.waitForIdle()

            // Should close player
            composeTestRule.waitUntil(5000) { closed }
            org.junit.Assert.assertTrue(closed)
            org.junit.Assert.assertNull(navigatedTarget)
        } finally {
            file1.delete()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testRenameDialogStandaloneValidationAndExtensionChange() {
        val tempDir = File(testDataDir, "temp_rename_unit_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "sample_document.pdf").apply { writeText("pdf content") }
        val fileExisting = File(tempDir, "already_exists.pdf").apply { writeText("exists") }

        var confirmedName: String? = null
        var dismissed = false

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    RenameDialog(
                        file = file1,
                        onConfirmRename = { confirmedName = it },
                        onDismissRequest = { dismissed = true }
                    )
                }
            }

            // Verify title
            composeTestRule.onNodeWithText("Rename File").assertIsDisplayed()

            // Test collision validation
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("already_exists.pdf")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("An item with this name already exists").assertIsDisplayed()

            // Test invalid char validation
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("bad/name.pdf")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Name cannot contain '/' characters").assertIsDisplayed()

            // Test extension change warning
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("sample_document.txt")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Extension Changed").assertIsDisplayed()
            composeTestRule.onNodeWithText("Rename Anyway").assertIsDisplayed()

            // Test revert extension
            composeTestRule.onNodeWithText("Undo Extension (.pdf)").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithText("Extension Changed").fetchSemanticsNodes().isEmpty()

            // Perform valid rename
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("final_document.pdf")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Confirm Rename").performClick()
            composeTestRule.waitForIdle()

            org.junit.Assert.assertEquals("final_document.pdf", confirmedName)
        } finally {
            file1.delete()
            fileExisting.delete()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMainScreenRenameFlowAndAuditLog() {
        val tempDir = File(testDataDir, "temp_rename_ms_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "doc_to_rename.txt").apply { writeText("some text") }
        val targetRenamed = File(tempDir, "doc_renamed.txt")

        val auditHelper = AuditLogDatabaseHelper.getInstance(androidx.test.core.app.ApplicationProvider.getApplicationContext())

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = tempDir
                    )
                }
            }

            // Long press file to open context menu
            composeTestRule.onNodeWithText("doc_to_rename.txt").performTouchInput { longClick() }
            composeTestRule.waitForIdle()

            // Click Rename
            composeTestRule.onNodeWithText("Rename").performClick()
            composeTestRule.waitForIdle()

            // Change name
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("doc_renamed.txt")
            composeTestRule.waitForIdle()

            // Confirm
            composeTestRule.onNodeWithContentDescription("Confirm Rename").performClick()
            composeTestRule.waitForIdle()

            // Verify file renamed on disk
            composeTestRule.waitUntil(5000) { targetRenamed.exists() && !file1.exists() }
            org.junit.Assert.assertTrue(targetRenamed.exists())
            org.junit.Assert.assertFalse(file1.exists())

            // Verify audit log
            val logs = kotlinx.coroutines.runBlocking { auditHelper.getAllLogs() }
            val renameLog = logs.find { it.actionType == AuditActionType.RENAME }
            org.junit.Assert.assertNotNull(renameLog)
            org.junit.Assert.assertEquals(1, renameLog?.totalItems)
        } finally {
            file1.delete()
            targetRenamed.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking {
                auditHelper.clearAllLogs()
            }
        }
    }

    @Test
    fun testMediaPlayerScreenRename() {
        val tempDir = File(testDataDir, "temp_mp_rename_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "img_to_rename.png").apply { writeText("img") }
        val fileRenamed = File(tempDir, "img_renamed.png")

        var navigatedTarget: File? = null

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MediaPlayerScreen(
                        file = file1,
                        globalLoopEnabled = true,
                        onToggleGlobalLoop = {},
                        onClose = {},
                        onNavigateToMedia = { navigatedTarget = it }
                    )
                }
            }

            // Click center to show overlays
            composeTestRule.onRoot().performClick()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Click 3-dots menu in header
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()

            // Click Rename
            composeTestRule.onNodeWithText("Rename").performClick()
            composeTestRule.waitForIdle()

            // Replace name
            composeTestRule.onNodeWithContentDescription("Rename text input")
                .performTextReplacement("img_renamed.png")
            composeTestRule.waitForIdle()

            // Confirm
            composeTestRule.onNodeWithContentDescription("Confirm Rename").performClick()
            composeTestRule.waitForIdle()

            // Check disk and navigation target
            composeTestRule.waitUntil(5000) { fileRenamed.exists() && navigatedTarget != null }
            org.junit.Assert.assertTrue(fileRenamed.exists())
            org.junit.Assert.assertFalse(file1.exists())
            org.junit.Assert.assertEquals(fileRenamed.absolutePath, navigatedTarget?.absolutePath)
        } finally {
            file1.delete()
            fileRenamed.delete()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPastActionsScreenAutoPurgeAuditLogDisplay() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val recycleBinHelper = RecycleBinDatabaseHelper.getInstance(context)
        val auditHelper = AuditLogDatabaseHelper.getInstance(context)

        val tempDir = File(testDataDir, "temp_autopurge_ui_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "auto_purge_sample.txt").apply { writeText("expired content") }

        try {
            kotlinx.coroutines.runBlocking {
                val trashed = recycleBinHelper.moveToRecycleBin(listOf(file1), context)
                val db = recycleBinHelper.writableDatabase
                val pastTime = System.currentTimeMillis() - 10000L
                val values = android.content.ContentValues().apply {
                    put(RecycleBinDatabaseHelper.COLUMN_EXPIRY_TIMESTAMP, pastTime)
                }
                db.update(RecycleBinDatabaseHelper.TABLE_RECYCLE_BIN, values, "${RecycleBinDatabaseHelper.COLUMN_ID} = ?", arrayOf(trashed[0].id.toString()))
                recycleBinHelper.cleanupExpiredItems(context)
            }

            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = testDataDir
                    )
                }
            }

            // Open Settings
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.waitForIdle()

            // Open Past Actions
            composeTestRule.onNodeWithText("Past Actions").performClick()
            composeTestRule.waitForIdle()

            // Verify Auto-Purge Action is listed
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("1 item • To: Auto Cleanup").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 item • To: Auto Cleanup").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Auto-Purge Action").assertIsDisplayed()

            // Click Auto-Purge item to open details
            composeTestRule.onNodeWithText("1 item • To: Auto Cleanup").performClick()
            composeTestRule.waitForIdle()

            // Verify Action Details dialog
            composeTestRule.onNodeWithText("Action Details").assertIsDisplayed()
            composeTestRule.onNodeWithText("AUTO_DELETE").assertIsDisplayed()
            composeTestRule.onNodeWithText("EXPIRED").assertIsDisplayed()

            // Close dialog
            composeTestRule.onNodeWithText("Close").performClick()
            composeTestRule.waitForIdle()

            // Navigate back to Settings -> MainScreen
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        } finally {
            file1.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
            }
        }
    }

    @Test
    fun testOperationProgressDialogStandalone() {
        val state = OperationProgressState(
            title = "Copying items...",
            currentItem = 5,
            totalItems = 20,
            currentFileName = "sample_video.mp4",
            icon = androidx.compose.material.icons.Icons.Default.ContentCopy,
            iconTint = androidx.compose.ui.graphics.Color(0xFF4FC3F7)
        )

        composeTestRule.setContent {
            TagStashTheme {
                OperationProgressDialog(state = state)
            }
        }

        // Verify Dialog Elements
        composeTestRule.onNodeWithContentDescription("Operation Progress Dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copying items...").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 of 20 items (25%)").assertIsDisplayed()
        composeTestRule.onNodeWithText("sample_video.mp4").assertIsDisplayed()
    }

    @Test
    fun testBatchOperationProgressInMainScreenDelete() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val recycleBinHelper = RecycleBinDatabaseHelper.getInstance(context)
        val auditHelper = AuditLogDatabaseHelper.getInstance(context)

        val tempDir = File(testDataDir, "temp_progress_delete_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        val file1 = File(tempDir, "prog1.txt").apply { writeText("1") }
        val file2 = File(tempDir, "prog2.txt").apply { writeText("2") }

        try {
            composeTestRule.setContent {
                TagStashTheme {
                    MainScreen(
                        permissionGranted = true,
                        onRequestPermission = {},
                        homeDirectory = tempDir
                    )
                }
            }

            // Enter selection mode
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Select").performClick()
            composeTestRule.waitForIdle()

            // Select prog1.txt and prog2.txt
            composeTestRule.onNodeWithText("prog1.txt").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("prog2.txt").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("2 items selected").assertIsDisplayed()

            // Click Delete in top bar overflow menu
            composeTestRule.onNodeWithContentDescription("More options").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Confirm Delete in dialog
            composeTestRule.onNode(isToggleable()).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Confirm Delete").performClick()
            composeTestRule.waitForIdle()

            // Verify files moved to recycle bin
            composeTestRule.waitUntil(5000) { !file1.exists() && !file2.exists() }
            org.junit.Assert.assertFalse(file1.exists())
            org.junit.Assert.assertFalse(file2.exists())

            // Wait for progress dialog to dismiss
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Moving to Recycle Bin...").fetchSemanticsNodes().isEmpty()
            }
        } finally {
            file1.delete()
            file2.delete()
            tempDir.deleteRecursively()
            kotlinx.coroutines.runBlocking {
                recycleBinHelper.clearAll()
                auditHelper.clearAllLogs()
            }
        }
    }

    @Test
    fun testSearchDialogOpenAndCancel() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open 3-dot overflow menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()

        // Click Search menu item
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Verify Search Dialog is displayed
        composeTestRule.onNodeWithContentDescription("Search Dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search Files").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search text input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Include subfolders checkbox").assertIsDisplayed()

        // Click Cancel
        composeTestRule.onNodeWithContentDescription("Cancel Search").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog dismissed
        org.junit.Assert.assertTrue(
            composeTestRule.onAllNodesWithText("Search Files").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun testSearchInCurrentFolder() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open 3-dot menu and click Search
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Enter search term "image"
        composeTestRule.onNodeWithContentDescription("Search text input").performTextInput("image")
        composeTestRule.waitForIdle()

        // Click Search button
        composeTestRule.onNodeWithContentDescription("Confirm Search").performClick()
        composeTestRule.waitForIdle()

        // Verify search results header
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Results for: \"image\"").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Results for: \"image\"").assertIsDisplayed()

        // Verify matches in current folder (images folder)
        composeTestRule.onNodeWithText("images").assertIsDisplayed()
        composeTestRule.onNodeWithText("Location: .").assertIsDisplayed()

        // Capture screenshot
        composeTestRule.onRoot().captureRoboImage("screenshots/search_results_current_folder.png")
    }

    @Test
    fun testSearchRecursiveSubfolders() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open Search
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Enter query "1.png" and check include subfolders
        composeTestRule.onNodeWithContentDescription("Search text input").performTextInput("1.png")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Include subfolders checkbox").performClick()
        composeTestRule.waitForIdle()

        // Click Search
        composeTestRule.onNodeWithContentDescription("Confirm Search").performClick()
        composeTestRule.waitForIdle()

        // Verify results
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("1.png").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("1.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("Location: images/pngs").assertIsDisplayed()

        // Capture screenshot
        composeTestRule.onRoot().captureRoboImage("screenshots/search_results_recursive_subfolders.png")
    }

    @Test
    fun testSearchEmptyResultsAndBackToFolder() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open Search
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Enter nonexistent query
        composeTestRule.onNodeWithContentDescription("Search text input").performTextInput("nonexistent_xyz_123")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Confirm Search").performClick()
        composeTestRule.waitForIdle()

        // Verify empty search results view
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("No matching files found").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No matching files found").assertIsDisplayed()

        // Capture screenshot
        composeTestRule.onRoot().captureRoboImage("screenshots/search_results_empty.png")

        // Click "Back to Folder"
        composeTestRule.onNodeWithText("Back to Folder").performClick()
        composeTestRule.waitForIdle()

        // Verify back in folder view
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("images").assertIsDisplayed()
    }

    @Test
    fun testSearchBackNavigationWithTopBarArrow() {
        composeTestRule.setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = true,
                    onRequestPermission = {},
                    homeDirectory = testDataDir
                )
            }
        }

        // Open search
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Enter query "video"
        composeTestRule.onNodeWithContentDescription("Search text input").performTextInput("video")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Confirm Search").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Results for: \"video\"").fetchSemanticsNodes().isNotEmpty()
        }

        // Click top bar back arrow
        composeTestRule.onNodeWithContentDescription("Exit search").performClick()
        composeTestRule.waitForIdle()

        // Verify back in normal folder view
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("images").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("images").assertIsDisplayed()
    }
}
