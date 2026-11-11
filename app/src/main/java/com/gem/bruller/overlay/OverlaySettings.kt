package com.gem.bruller.overlay

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class RulerStyle {
    AIM_LINE,
    PROTRACTOR,
    CROSSHAIR
}

enum class RulerColor(val argb: Int) {
    CRIMSON(0xFFE15B5B.toInt()),
    CYAN(0xFF74D0E0.toInt()),
    LIME(0xFFA8DE5A.toInt()),
    IVORY(0xFFF4EEDF.toInt()),
    GOLD(0xFFE0B15A.toInt())
}

data class OverlaySettings(
    val style: RulerStyle = RulerStyle.AIM_LINE,
    val color: RulerColor = RulerColor.CRIMSON,
    val lengthFraction: Float = 0.62f,
    val thicknessDp: Float = 3f,
    val opacity: Float = 0.9f,
    val showTicks: Boolean = true,
    val showAngle: Boolean = true,
    val showGhostBall: Boolean = true,
    val ghostBallDp: Float = 14f,
    val snapDegrees: Int = 0,
    val angleDegrees: Float = -35f,
    val pivotXFraction: Float = 0.22f,
    val pivotYFraction: Float = 0.62f
) {
    companion object {
        private const val PREFS_NAME = "ruler_overlay_settings"

        private const val KEY_STYLE = "style"
        private const val KEY_COLOR = "color"
        private const val KEY_LENGTH = "length_fraction"
        private const val KEY_THICKNESS = "thickness_dp"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_TICKS = "show_ticks"
        private const val KEY_ANGLE_LABEL = "show_angle"
        private const val KEY_GHOST_BALL = "show_ghost_ball"
        private const val KEY_GHOST_SIZE = "ghost_ball_dp"
        private const val KEY_SNAP = "snap_degrees"
        private const val KEY_ANGLE = "angle_degrees"
        private const val KEY_PIVOT_X = "pivot_x_fraction"
        private const val KEY_PIVOT_Y = "pivot_y_fraction"

        fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun load(context: Context): OverlaySettings {
            val p = prefs(context)
            return OverlaySettings(
                style = RulerStyle.entries.firstOrNull {
                    it.name == p.getString(KEY_STYLE, RulerStyle.AIM_LINE.name)
                } ?: RulerStyle.AIM_LINE,
                color = RulerColor.entries.firstOrNull {
                    it.name == p.getString(KEY_COLOR, RulerColor.CRIMSON.name)
                } ?: RulerColor.CRIMSON,
                lengthFraction = p.getFloat(KEY_LENGTH, 0.62f).coerceIn(0.15f, 1.1f),
                thicknessDp = p.getFloat(KEY_THICKNESS, 3f).coerceIn(1f, 8f),
                opacity = p.getFloat(KEY_OPACITY, 0.9f).coerceIn(0.15f, 1f),
                showTicks = p.getBoolean(KEY_TICKS, true),
                showAngle = p.getBoolean(KEY_ANGLE_LABEL, true),
                showGhostBall = p.getBoolean(KEY_GHOST_BALL, true),
                ghostBallDp = p.getFloat(KEY_GHOST_SIZE, 14f).coerceIn(6f, 30f),
                snapDegrees = p.getInt(KEY_SNAP, 0).coerceIn(0, 45),
                angleDegrees = p.getFloat(KEY_ANGLE, -35f),
                pivotXFraction = p.getFloat(KEY_PIVOT_X, 0.22f).coerceIn(0.02f, 0.98f),
                pivotYFraction = p.getFloat(KEY_PIVOT_Y, 0.62f).coerceIn(0.02f, 0.98f)
            )
        }

        fun save(context: Context, settings: OverlaySettings) {
            prefs(context).edit {
                putString(KEY_STYLE, settings.style.name)
                putString(KEY_COLOR, settings.color.name)
                putFloat(KEY_LENGTH, settings.lengthFraction)
                putFloat(KEY_THICKNESS, settings.thicknessDp)
                putFloat(KEY_OPACITY, settings.opacity)
                putBoolean(KEY_TICKS, settings.showTicks)
                putBoolean(KEY_ANGLE_LABEL, settings.showAngle)
                putBoolean(KEY_GHOST_BALL, settings.showGhostBall)
                putFloat(KEY_GHOST_SIZE, settings.ghostBallDp)
                putInt(KEY_SNAP, settings.snapDegrees)
            }
        }

        fun saveGeometry(
            context: Context,
            angleDegrees: Float,
            pivotXFraction: Float,
            pivotYFraction: Float,
            lengthFraction: Float
        ) {
            prefs(context).edit {
                putFloat(KEY_ANGLE, angleDegrees)
                putFloat(KEY_PIVOT_X, pivotXFraction.coerceIn(0.02f, 0.98f))
                putFloat(KEY_PIVOT_Y, pivotYFraction.coerceIn(0.02f, 0.98f))
                putFloat(KEY_LENGTH, lengthFraction.coerceIn(0.15f, 1.1f))
            }
        }
    }
}
