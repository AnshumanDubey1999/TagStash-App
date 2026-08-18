package com.anshuman.tagstash.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.ui.components.DeleteConfirmationDialog
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
        AppClipboard.clear()
    }

    @After
    fun tearDown() {
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

        // Long press on images folder
        composeTestRule.onNodeWithText("images").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Click Select
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.waitForIdle()

        // Verify selection mode active and images is selected
        composeTestRule.onNodeWithText("1 item selected").assertIsDisplayed()
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

        // Long press on images folder
        composeTestRule.onNodeWithText("images").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Verify options
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
                        homeDirectory = testDataDir,
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
                        homeDirectory = testDataDir,
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

        // Verify Empty State is shown
        composeTestRule.onNodeWithText("Recycle Bin is Empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deleted files will appear here and be kept for 1 hour before permanent removal.").assertIsDisplayed()

        // Click Back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
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
}
