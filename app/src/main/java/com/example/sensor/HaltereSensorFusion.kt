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

    private var lastTimeAccel = 0L
    private var lastTimeGyro = 0L
    private var currentVel = 0f
    private var gyroMag = 0f

    // Noise Deadband Thresholds (센서 고유 노이즈 및 손떨림 차단 임계치)
    // 정지 상태 또는 미세 손떨림 시 가속도 0.20 m/s², 각속도 0.10 rad/s 미만은 순수 노이즈로 필터링
    private val ACCEL_NOISE_DEADBAND = 0.20f // m/s²
    private val GYRO_NOISE_DEADBAND = 0.10f  // rad/s

    // Velocity Leaky Integrator Damping (감쇠 계수)
    // 외력이 없을 때 속도가 지속 증가하거나 멈추지 않는 현상을 막기 위한 점성 감쇠
    private val VELOCITY_DAMPING_RATE = 4.0f // 1/s (감쇠 시정수 약 0.25초)

    fun start() {
        lastTimeAccel = 0L
        lastTimeGyro = 0L
        linearAccSensor?.let {
            try {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } catch (e: SecurityException) {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        gyroSensor?.let {
            try {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } catch (e: SecurityException) {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
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
        lastTimeAccel = 0L
        lastTimeGyro = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.nanoTime()

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                gyroMag = sqrt(gx * gx + gy * gy + gz * gz)
                _gyroMagnitude.value = gyroMag
                _omegaYaw.value = gy // Yaw angular velocity (rad/s)
                lastTimeGyro = currentTime
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (lastTimeAccel == 0L) {
                    lastTimeAccel = currentTime
                    return
                }
                val dt = ((currentTime - lastTimeAccel) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
                lastTimeAccel = currentTime

                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = sqrt(ax * ax + ay * ay + az * az)
                _accelMagnitude.value = mag

                // 1. ZUPT (Zero Velocity Update): 정지 상태 판별
                // 가속도 및 자이로 회전이 노이즈 임계치 이하인 경우 영속도 보정 즉시 적용
                val isMotionless = (mag < ACCEL_NOISE_DEADBAND) && (gyroMag < GYRO_NOISE_DEADBAND)

                if (isMotionless) {
                    // 정지 상태에서는 속도를 급속 감쇄시켜 0으로 복귀 (미동 시 자동 영점화)
                    currentVel *= (1.0f - 8.0f * dt).coerceAtLeast(0f)
                    if (currentVel < 0.005f) currentVel = 0f
                } else {
                    // 2. 유의미한 가속도 성분 추출 (노이즈 데드밴드 차감)
                    // 스위핑은 주로 스마트폰 X축(가로) 또는 합성 가속도 방향으로 발생
                    val dominantAcc = kotlin.math.max(abs(ax), mag * 0.7f)
                    val netAcc = (dominantAcc - ACCEL_NOISE_DEADBAND).coerceAtLeast(0f)

                    // 3. 누출 적분기 (Leaky Integrator) + 점성 감쇠 모델
                    // v[k] = v[k-1] * (1 - damping * dt) + a_net * dt
                    val dampingFactor = (1.0f - VELOCITY_DAMPING_RATE * dt).coerceIn(0f, 1f)
                    currentVel = currentVel * dampingFactor + netAcc * dt * 0.35f

                    // 최대 스위핑 속도 상한선 제한 (600 mm/s = 0.6 m/s)
                    currentVel = currentVel.coerceIn(0f, 0.6f)
                }

                // mm/s 단위로 환산하여 상태 발행
                val velMmS = currentVel * 1000f
                _velocity.value = velMmS
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
