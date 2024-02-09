package com.itstor.wifimapper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class DeviceHeading(context: Context) : SensorEventListener {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetoSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private val rMatrix = FloatArray(9)
    private val iMatrix = FloatArray(9)
    private var azimuth = 0f
    private var rawAzimuth = 0f
    private var azimuthFix = 0f

    var listener: CompassListener? = null

    interface CompassListener {
        fun onNewAzimuth(azimuth: Float)
    }

    fun start() {
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetoSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun setAzimuthFix(fix: Float) {
        azimuthFix = fix
    }

    fun resetAzimuthFix() {
        azimuthFix = 0f
    }

    fun getRawAzimuth(): Float {
        return rawAzimuth
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        synchronized(this) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                    mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                    mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
                    mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
                    mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
                }
            }

            val success = SensorManager.getRotationMatrix(rMatrix, iMatrix, mGravity, mGeomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rMatrix, orientation)
                rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = (rawAzimuth + azimuthFix + 360) % 360
                listener?.onNewAzimuth(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
}

