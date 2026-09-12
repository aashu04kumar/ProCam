package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiProcessingEngine
import com.example.data.AiScene
import com.example.data.CameraMode
import com.example.filter.FilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lite Pro Cam", appName)
  }

  @Test
  fun `verify AI scene analyzer`() {
    val scenePortrait = AiProcessingEngine.analyzeScene(
      luminanceAvg = 0.5f,
      warmthRatio = 1.0f,
      isFaceDetected = true,
      mode = CameraMode.PORTRAIT
    )
    assertEquals(AiScene.PORTRAIT, scenePortrait)

    val sceneNight = AiProcessingEngine.analyzeScene(
      luminanceAvg = 0.10f,
      warmthRatio = 1.0f,
      isFaceDetected = false,
      mode = CameraMode.PHOTO
    )
    assertEquals(AiScene.NIGHT, sceneNight)
  }

  @Test
  fun `verify FilterEngine filters count`() {
    assertTrue(FilterEngine.filters.size >= 15)
    val cinemaFilter = FilterEngine.filters.find { it.id == "cinematic_teal_orange" }
    assertNotNull(cinemaFilter)
  }

  @Test
  fun `verify camera and storage permissions configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
    assertTrue(requestedPermissions.contains("android.permission.CAMERA"))
    assertTrue(requestedPermissions.contains("android.permission.RECORD_AUDIO"))
    assertTrue(requestedPermissions.contains("android.permission.READ_MEDIA_IMAGES"))
  }
}

