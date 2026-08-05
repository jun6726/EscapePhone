package com.example.escapephone.core.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.example.escapephone.core.model.DeviceAttitude

interface MotionController {
    val isAvailable: Boolean
    var onAttitudeChanged: ((DeviceAttitude) -> Unit)?
    fun start()
    fun stop()
    fun calibrate()
}

class AndroidMotionController(context: Context) : MotionController, SensorEventListener {
    private val appContext = context
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var isStarted = false
    private var baseRotationMatrix: FloatArray? = null
    override val isAvailable get() = sensor != null
    override var onAttitudeChanged: ((DeviceAttitude) -> Unit)? = null
    override fun start() { if (!isStarted && sensor != null) { baseRotationMatrix = null; isStarted = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME) } }
    override fun stop() { if (isStarted) sensorManager.unregisterListener(this); isStarted = false }
    override fun calibrate() { baseRotationMatrix = null }
    override fun onSensorChanged(event: SensorEvent) {
        val deviceRotation = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(deviceRotation, event.values)
        val screenRotation = remapForCurrentDisplay(deviceRotation)
        val base = baseRotationMatrix
        if (base == null) {
            baseRotationMatrix = screenRotation.copyOf()
            onAttitudeChanged?.invoke(DeviceAttitude(0.0, 0.0, 0.0))
            return
        }
        val relativeRotation = relativeRotationMatrix(base, screenRotation)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(relativeRotation, orientation)
        onAttitudeChanged?.invoke(DeviceAttitude(orientation[1].toDouble(), orientation[2].toDouble(), orientation[0].toDouble()))
    }

    private fun remapForCurrentDisplay(rotation: FloatArray): FloatArray {
        val axes = when (currentDisplayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> return rotation
        }
        return FloatArray(9).also { SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, it) }
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        appContext.display.rotation
    } else {
        (appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

internal fun relativeRotationMatrix(base: FloatArray, current: FloatArray): FloatArray {
    require(base.size == 9 && current.size == 9)
    return FloatArray(9).also { result ->
        for (row in 0..2) for (column in 0..2) {
            result[row * 3 + column] = (0..2).sumOf { index ->
                (base[index * 3 + row] * current[index * 3 + column]).toDouble()
            }.toFloat()
        }
    }
}

class MockMotionController(override val isAvailable: Boolean = false) : MotionController {
    override var onAttitudeChanged: ((DeviceAttitude) -> Unit)? = null
    override fun start() = Unit
    override fun stop() = Unit
    override fun calibrate() = Unit
    fun send(attitude: DeviceAttitude) { onAttitudeChanged?.invoke(attitude) }
}
