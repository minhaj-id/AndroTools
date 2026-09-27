package com.bimantara

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bimantara.core.security.BackupCrypto
import com.bimantara.data.backup.BackupPayload
import com.bimantara.data.model.NoteEntity
import com.bimantara.data.model.PlannerItemEntity
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

  @Test
  fun `ocr languages configured and recognized correctly`() {
    val indonesian = com.bimantara.feature.scanner.ocr.OcrLanguage.findByCode("ind")
    assertEquals("Bahasa Indonesia", indonesian.name)
    assertTrue(indonesian.isBundled)

    val english = com.bimantara.feature.scanner.ocr.OcrLanguage.findByCode("eng")
    assertEquals("English", english.name)
    assertTrue(english.isBundled)

    val spanish = com.bimantara.feature.scanner.ocr.OcrLanguage.findByCode("spa")
    assertEquals("Spanyol", spanish.name)
    assertEquals("spa", spanish.code)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.bimantara.feature.scanner.ocr.TesseractOcrManager.getInstance(context)
    assertNotNull(manager)
  }

  @Test
  fun `notes speech dictation appends text accurately`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.bimantara.feature.notes.NotesViewModel(application)

    viewModel.startVoiceNote()
    assertTrue(viewModel.isEditing.value)
    assertTrue(viewModel.consumeAutoStartDictation())

    // Append to content
    viewModel.setDictationTarget("CONTENT")
    viewModel.appendVoiceInput("halo dunia")
    assertEquals("Halo dunia", viewModel.content.value)

    viewModel.appendVoiceInput("dari mikrofon")
    assertEquals("Halo dunia dari mikrofon", viewModel.content.value)

    // Dictate to title
    viewModel.setDictationTarget("TITLE")
    viewModel.appendVoiceInput("Rapat Pagi")
    assertEquals("Rapat Pagi", viewModel.title.value)

    // Supported languages test
    assertTrue(viewModel.supportedSpeechLanguages.any { it.code == "id-ID" })
    assertTrue(viewModel.supportedSpeechLanguages.any { it.code == "en-US" })
    viewModel.setDictationLanguage("es-ES")
    assertEquals("es-ES", viewModel.dictationLanguage.value)
  }
}
