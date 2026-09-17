package com.example.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShakeListener: () -> Unit) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimestamp: Long = 0

    companion object {
        // Acceleration threshold (g-force)
        private const val SHAKE_THRESHOLD_G_FORCE = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 1000 // Minimum time between shake events
    }

    fun start(manager: SensorManager): Boolean {
        sensorManager = manager
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        return accelerometer?.let { sensor ->
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        } ?: false
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_G_FORCE) {
            val now = System.currentTimeMillis()
            if (lastShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }
            lastShakeTimestamp = now
            onShakeListener()
        }
    }
}
