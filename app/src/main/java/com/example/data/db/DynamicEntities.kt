package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey
  val name: String,
  val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_actions")
data class CustomActionEntity(
  @PrimaryKey
  val label: String,
  val emoji: String = "⚡",
  val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_items")
data class HiddenItemEntity(
  @PrimaryKey
  val id: String, // e.g. "USER:Subject_01" or "ACTION:jumping"
  val name: String,
  val type: String, // "USER" or "ACTION"
  val deletedAtEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface UserProfileDao {
  @Query("SELECT * FROM user_profiles ORDER BY createdAtEpochMs ASC")
  fun getAllUsers(): Flow<List<UserProfileEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserProfileEntity)

  @Delete
  suspend fun deleteUser(user: UserProfileEntity)

  @Query("DELETE FROM user_profiles WHERE name = :name")
  suspend fun deleteUserByName(name: String)
}

@Dao
interface CustomActionDao {
  @Query("SELECT * FROM custom_actions ORDER BY createdAtEpochMs ASC")
  fun getAllCustomActions(): Flow<List<CustomActionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomAction(action: CustomActionEntity)

  @Delete
  suspend fun deleteCustomAction(action: CustomActionEntity)

  @Query("DELETE FROM custom_actions WHERE label = :label")
  suspend fun deleteCustomActionByLabel(label: String)
}

@Dao
interface HiddenItemDao {
  @Query("SELECT * FROM hidden_items WHERE type = :type ORDER BY deletedAtEpochMs DESC")
  fun getHiddenItemsByType(type: String): Flow<List<HiddenItemEntity>>

  @Query("SELECT name FROM hidden_items WHERE type = :type")
  fun getHiddenNamesByType(type: String): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHiddenItem(item: HiddenItemEntity)

  @Query("DELETE FROM hidden_items WHERE name = :name AND type = :type")
  suspend fun unhideItem(name: String, type: String)

  @Query("DELETE FROM hidden_items WHERE type = :type")
  suspend fun clearHiddenItems(type: String)
}
