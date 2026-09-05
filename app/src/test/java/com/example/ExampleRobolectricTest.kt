package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.BackupCrypto
import com.example.data.backup.BackupPayload
import com.example.data.model.NoteEntity
import com.example.data.model.PlannerItemEntity
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
    assertEquals("Multi Tools", appName)
  }

  @Test
  fun `backup crypto encrypts and decrypts correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val originalData = "{\"message\":\"Sensitive multi-tools user data\",\"items\":[1,2,3]}"

    val encryptedBytes = BackupCrypto.encryptPayload(context, originalData)
    assertTrue(encryptedBytes.isNotEmpty())

    val decryptedData = BackupCrypto.decryptPayload(context, encryptedBytes)
    assertEquals(originalData, decryptedData)
  }

  @Test
  fun `backup payload serializes and deserializes accurately`() {
    val note = NoteEntity(
      id = 101L,
      title = "Secret Meeting Notes",
      content = "Discuss quarterly goals and architecture roadmap",
      mode = "TYPE",
      stylusDrawingData = "",
      colorHex = "#3B82F6",
      tags = "Work",
      isPinned = true,
      createdAt = 1700000000000L,
      updatedAt = 1700000010000L
    )

    val planner = PlannerItemEntity(
      id = 202L,
      title = "Deploy Applet",
      description = "Final testing and push to production",
      dueDateMillis = 1700005000000L,
      priority = "HIGH",
      category = "WORK",
      isCompleted = false,
      alarmEnabled = true,
      createdAt = 1700000000000L
    )

    val payload = BackupPayload(
      version = 1,
      timestamp = 1700000000000L,
      notes = listOf(note),
      plannerItems = listOf(planner),
      scannedDocs = emptyList()
    )

    val json = payload.toJson()
    val restored = BackupPayload.fromJson(json)

    assertEquals(1, restored.notes.size)
    assertEquals("Secret Meeting Notes", restored.notes[0].title)
    assertEquals(1, restored.plannerItems.size)
    assertEquals("Deploy Applet", restored.plannerItems[0].title)
  }
}
