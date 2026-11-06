package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.CustomActionDao
import com.example.data.db.CustomActionEntity
import com.example.data.db.HiddenItemDao
import com.example.data.db.HiddenItemEntity
import com.example.data.db.MotionDatabase
import com.example.data.db.MotionSessionDao
import com.example.data.db.MotionSessionEntity
import com.example.data.db.UserProfileDao
import com.example.data.db.UserProfileEntity
import com.example.data.export.CsvExporter
import com.example.data.model.ActivityPresets
import com.example.data.model.SensorReading
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var database: MotionDatabase
  private lateinit var sessionDao: MotionSessionDao
  private lateinit var userDao: UserProfileDao
  private lateinit var customActionDao: CustomActionDao
  private lateinit var hiddenItemDao: HiddenItemDao

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, MotionDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    sessionDao = database.sessionDao()
    userDao = database.userDao()
    customActionDao = database.customActionDao()
    hiddenItemDao = database.hiddenItemDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testAppNameResource() {
    val appName = context.getString(R.string.app_name)
    assertEquals("MotionML Collector", appName)
  }

  @Test
  fun testDefaultActionsList() {
    val expectedActions = listOf(
      "stand up",
      "sit down",
      "Squatting",
      "laying-to-stand",
      "stand-to-sit",
      "stand-to laying",
      "laying down",
      "sitting",
      "walking",
      "running",
      "going upstairs",
      "going down stairs",
      "jumping",
      "falling"
    )

    expectedActions.forEach { action ->
      val preset = ActivityPresets.findPreset(action)
      assertNotNull("Expected action '$action' to be in presets", preset)
    }

    val fallPreset = ActivityPresets.findPreset("falling")
    assertNotNull(fallPreset)
    assertEquals("⚠️", fallPreset?.emoji)

    val squatPreset = ActivityPresets.findPreset("Squatting")
    assertNotNull(squatPreset)
    assertEquals("Squatting", squatPreset?.label)
  }

  @Test
  fun testDynamicUserCreationAndDeletion() {
    runBlocking {
      userDao.insertUser(UserProfileEntity(name = "Subject_Alpha"))
      userDao.insertUser(UserProfileEntity(name = "Subject_Beta"))

      val users = userDao.getAllUsers().first()
      assertEquals(2, users.size)
      assertTrue(users.any { it.name == "Subject_Alpha" })
      assertTrue(users.any { it.name == "Subject_Beta" })

      // Delete user
      userDao.deleteUserByName("Subject_Alpha")
      hiddenItemDao.insertHiddenItem(HiddenItemEntity(id = "USER:Subject_Alpha", name = "Subject_Alpha", type = "USER"))

      val afterDelete = userDao.getAllUsers().first()
      assertEquals(1, afterDelete.size)
      assertEquals("Subject_Beta", afterDelete[0].name)

      val hiddenUsers = hiddenItemDao.getHiddenNamesByType("USER").first()
      assertEquals(listOf("Subject_Alpha"), hiddenUsers)

      // Restore user
      hiddenItemDao.unhideItem("Subject_Alpha", "USER")
      userDao.insertUser(UserProfileEntity(name = "Subject_Alpha"))
      val afterRestore = userDao.getAllUsers().first()
      assertEquals(2, afterRestore.size)
    }
  }

  @Test
  fun testDynamicActionCreationAndDeletion() {
    runBlocking {
      customActionDao.insertCustomAction(CustomActionEntity(label = "boxing jab", emoji = "🥊"))

      val actions = customActionDao.getAllCustomActions().first()
      assertEquals(1, actions.size)
      assertEquals("boxing jab", actions[0].label)
      assertEquals("🥊", actions[0].emoji)

      // Delete action
      customActionDao.deleteCustomActionByLabel("boxing jab")
      hiddenItemDao.insertHiddenItem(HiddenItemEntity(id = "ACTION:boxing jab", name = "boxing jab", type = "ACTION"))

      val afterDelete = customActionDao.getAllCustomActions().first()
      assertTrue(afterDelete.isEmpty())

      val hiddenActions = hiddenItemDao.getHiddenNamesByType("ACTION").first()
      assertEquals(listOf("boxing jab"), hiddenActions)

      // Restore action
      hiddenItemDao.unhideItem("boxing jab", "ACTION")
      val afterUnhide = hiddenItemDao.getHiddenNamesByType("ACTION").first()
      assertTrue(afterUnhide.isEmpty())
    }
  }

  @Test
  fun testSensorReadingCsvRowFormat() {
    val reading = SensorReading(
      timestampMs = 1700000000000L,
      elapsedSeconds = 1.25,
      accelX = 0.12345f,
      accelY = 9.81000f,
      accelZ = -0.45678f,
      gyroX = 0.01234f,
      gyroY = -0.05678f,
      gyroZ = 0.09101f,
      userName = "User_01",
      actionLabel = "stand up",
      sessionId = "sess-123"
    )

    val row = reading.toCsvRow()
    assertTrue(row.contains("1700000000000"))
    assertTrue(row.contains("1.2500"))
    assertTrue(row.contains("\"User_01\""))
    assertTrue(row.contains("\"stand up\""))
    assertTrue(row.contains("\"sess-123\""))
    assertTrue(reading.accelMagnitude > 9.8f)
  }

  @Test
  fun testRoomDatabaseOperations() {
    runBlocking {
      val session = MotionSessionEntity(
        id = "test-uuid-1",
        userName = "Participant_Alpha",
        actionLabel = "falling",
        startTimeEpochMs = 1700000000000L,
        endTimeEpochMs = 1700000010000L,
        durationSeconds = 10.0,
        sampleCount = 500,
        samplingRateHz = 50.0,
        filePath = "/mock/path.csv",
        fileName = "motion_test.csv",
        fileSizeBytes = 2048L,
        notes = "Test run"
      )

      sessionDao.insertSession(session)

      val all = sessionDao.getAllSessions().first()
      assertEquals(1, all.size)
      assertEquals("Participant_Alpha", all[0].userName)
      assertEquals("falling", all[0].actionLabel)

      val users = sessionDao.getDistinctUsers().first()
      assertEquals(listOf("Participant_Alpha"), users)

      val actions = sessionDao.getDistinctActions().first()
      assertEquals(listOf("falling"), actions)

      sessionDao.deleteSessionById("test-uuid-1")
      val afterDelete = sessionDao.getAllSessions().first()
      assertTrue(afterDelete.isEmpty())
    }
  }

  @Test
  fun testCsvExporterReadPreview() {
    runBlocking {
      val tempFile = File(context.cacheDir, "test_preview.csv")
      tempFile.writeText(
        """
          ${SensorReading.CSV_HEADER}
          1700000000000,0.02,0.1,9.8,0.2,0.01,0.02,0.03,9.81,0.04,"User1","stand up","s1"
          1700000000020,0.04,0.2,9.9,0.3,0.02,0.03,0.04,9.91,0.05,"User1","stand up","s1"
        """.trimIndent()
      )

      val (headers, rows) = CsvExporter.readCsvPreview(tempFile, 10)
      assertEquals(13, headers.size)
      assertEquals("timestamp_ms", headers[0])
      assertEquals("user_name", headers[10])
      assertEquals(2, rows.size)

      val stats = CsvExporter.computeSessionFeatureSummary(tempFile)
      assertNotNull(stats)
      assertEquals(2, stats?.sampleCount)
      assertTrue((stats?.effectiveHz ?: 0.0) > 0)
      tempFile.delete()
    }
  }
}
