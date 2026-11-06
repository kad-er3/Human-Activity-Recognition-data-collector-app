package com.example.data.model

data class ActivityPreset(
  val id: String,
  val label: String,
  val emoji: String,
  val category: String,
  val description: String
)

object ActivityPresets {
  val DEFAULT_LIST = listOf(
    ActivityPreset("stand_up", "stand up", "🧍⬆️", "Postural Transition", "Transition from sitting/laying to standing"),
    ActivityPreset("sit_down", "sit down", "🪑⬇️", "Postural Transition", "Transition from standing to sitting"),
    ActivityPreset("squatting", "Squatting", "🏋️", "Dynamic Posture", "Lowering body by bending knees"),
    ActivityPreset("laying_to_stand", "laying-to-stand", "🛏️⬆️", "Postural Transition", "Transition from laying on ground/bed to standing upright"),
    ActivityPreset("stand_to_sit", "stand-to-sit", "🧍➡️🪑", "Postural Transition", "Transition from standing to seated position"),
    ActivityPreset("stand_to_laying", "stand-to laying", "🧍➡️🛏️", "Postural Transition", "Transition from standing to laying down"),
    ActivityPreset("laying_down", "laying down", "🛌", "Stationary", "Resting in a horizontal/laying posture"),
    ActivityPreset("sitting", "sitting", "🪑", "Stationary", "Seated stationary position"),
    ActivityPreset("walking", "walking", "🚶", "Locomotion", "Walking at normal speed"),
    ActivityPreset("running", "running", "🏃", "Locomotion", "Jogging or running at active speed"),
    ActivityPreset("going_upstairs", "going upstairs", "🪜⬆️", "Locomotion", "Ascending stairs"),
    ActivityPreset("going_down_stairs", "going down stairs", "🪜⬇️", "Locomotion", "Descending stairs"),
    ActivityPreset("jumping", "jumping", "🦘", "Dynamic", "Jumping vertically or hops"),
    ActivityPreset("falling", "falling", "⚠️", "Hazard / Event", "Sudden slip, trip, or falling impact")
  )

  fun findPreset(label: String): ActivityPreset? {
    return DEFAULT_LIST.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
  }
}
