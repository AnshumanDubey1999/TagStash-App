package com.anshuman.tagstash.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.anshuman.tagstash.ui.theme.TagStashTheme
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-xhdpi") // Dark theme standard size
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
        composeTestRule.onRoot().captureRoboImage("screenshots/video_player_screen_with_controls.png")
    }
}
