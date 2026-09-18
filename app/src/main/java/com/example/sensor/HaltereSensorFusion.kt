package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class HaltereSensorFusion(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _velocity = MutableStateFlow(0f)
    val velocity = _velocity.asStateFlow()

    private val _gyroMagnitude = MutableStateFlow(0f)
    val gyroMagnitude = _gyroMagnitude.asStateFlow()

    private val _accelMagnitude = MutableStateFlow(0f)
    val accelMagnitude = _accelMagnitude.asStateFlow()

    private val _omegaYaw = MutableStateFlow(0f)
    val omegaYaw = _omegaYaw.asStateFlow()

    private var lastTime = 0L
    private var currentVel = 0f
    private var gyroMag = 0f

    fun start() {
        linearAccSensor?.let {
            try {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            } catch (e: SecurityException) {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        gyroSensor?.let {
            try {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            } catch (e: SecurityException) {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun calibrateZeroPoint() {
        currentVel = 0f
        _velocity.value = 0f
        _omegaYaw.value = 0f
        lastTime = System.nanoTime()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.nanoTime()
        if (lastTime == 0L) {
            lastTime = currentTime
            return
        }

        val dt = (currentTime - lastTime) / 1_000_000_000f // seconds
        lastTime = currentTime

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                gyroMag = sqrt(gx * gx + gy * gy + gz * gz)
                _gyroMagnitude.value = gyroMag
                _omegaYaw.value = gy // Yaw angular velocity (rad/s)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = sqrt(ax * ax + ay * ay + az * az)
                _accelMagnitude.value = mag
                
                // ZUPT: Zero Velocity Update when motionless
                if (mag < 0.08f && gyroMag < 0.04f) {
                    currentVel *= 0.85f // Smooth decay towards 0
                    if (currentVel < 0.001f) currentVel = 0f
                } else {
                    // Combine X-axis dominant sweeping motion and multi-axis acceleration
                    val sweepAcc = if (abs(ax) > 0.05f) abs(ax) else mag * 0.6f
                    currentVel += sweepAcc * dt
                    currentVel = currentVel.coerceIn(0f, 0.6f) // Cap max sweeping speed to 600 mm/s
                }
                
                // Convert to mm/s
                val velMmS = currentVel * 1000f
                _velocity.value = velMmS
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
