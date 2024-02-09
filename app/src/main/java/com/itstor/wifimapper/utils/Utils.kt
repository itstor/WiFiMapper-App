package com.itstor.wifimapper.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.itstor.wifimapper.models.Position
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

class Utils {
    companion object {
        /**
         * Calculates the device's heading in degrees from the provided rotation vector.
         *
         * @param rotationVector The rotation vector of the device, usually obtained from a SensorEvent.values when using a RotationVector type sensor.
         * @return The device's heading in degrees, in the range of 0 to 360.
         */
        fun getDeviceHeading(rotationVector: FloatArray): Float {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

            val orientationVal = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationVal)

            val azimuthRadians = orientationVal[0]
            val azimuthDegrees = toDegrees(azimuthRadians.toDouble()).toFloat()

            return (azimuthDegrees + 360) % 360
        }

        /**
         * Finds the nearest value in a list to a given value.
         *
         * @param value The value to which the nearest value is to be found.
         * @param values The list of values in which to find the nearest value.
         * @return The nearest value in the list to the given value.
         */
        fun nearestValue(value: Int, values: List<Int>): Int {
            return values.minBy { abs(it - value) }
        }

        /**
         * Rounds an integer to the nearest value based on a specified rounding digit.
         *
         * @param input The integer number to be rounded.
         * @param digit The digit at which rounding should occur. For example, if the digit is 1,
         *              rounding will happen at the tens place, if 2 then at the hundreds place, and so on.
         * @return The rounded integer.
         */
        fun roundNumber(input: Int, digit: Int): Int {
            val factor = 10.0.pow(digit.toDouble()).toInt()

            return ((input + factor / 2) / factor) * factor
        }

        /**
         * Calculates the next point in a 2D space given the current position and an angle.
         *
         * @param current The current position in the 2D space.
         * @param angle The angle in degrees at which the next point is to be calculated.
         * @return The next position in the 2D space.
         */
        fun nextPoint(current: Position, angle: Int): Position {
            val rad = angle * (Math.PI / 180)
            val dx = round(cos(rad)).toInt()
            val dy = round(sin(rad)).toInt()

            return Position(current.x + dx, current.y + dy)
        }

        /**
         * Finds the closest standard angle to a given angle.
         *
         * This function takes an angle in degrees and finds the closest standard angle to it.
         * The standard angles are defined as 0, 90, 180, 270, and 360 degrees.
         *
         * @param angle The angle for which the closest standard angle is to be found. Represented in degrees.
         * @return The closest standard angle to the given angle. Represented in degrees.
         */
        fun closestStandardAngle(angle: Int): Int {
            val standardAngles = listOf(0, 90, 180, 270, 360)

            var normAngle = angle % 360
            if (normAngle < 0) normAngle += 360

            if (normAngle <= standardAngles.first()) return standardAngles.first()
            if (normAngle >= standardAngles.last()) return standardAngles.last()

            var low = 0
            var high = standardAngles.size - 1

            while (low <= high) {
                val mid = low + (high - low) / 2

                if (standardAngles[mid] == normAngle) {
                    return normAngle
                } else if (standardAngles[mid] < normAngle) {
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            val diffLow = abs(normAngle - standardAngles[low])
            val diffHigh = abs(normAngle - standardAngles[high])

            return if (diffLow < diffHigh) standardAngles[low] else standardAngles[high]
        }

        /**
         * Rotates a point around a pivot by a specified angle.
         *
         * @param point The point to be rotated. Represented as a Position object.
         * @param pivot The pivot point around which the rotation is to be performed. Represented as a Position object.
         * @param angleDegrees The angle by which the point is to be rotated around the pivot. Represented in degrees.
         * @return The position of the rotated point. Represented as a Position object.
         */
        fun rotatePointAroundPivot(
            point: Position,
            pivot: Position,
            angleDegrees: Double
        ): Position {
            val angleRadians = Math.toRadians(angleDegrees)

            val prime = point - pivot

            val xNew = prime.x * cos(angleRadians) + prime.y * sin(angleRadians)
            val yNew = -prime.x * sin(angleRadians) + prime.y * cos(angleRadians)

            return pivot + Position(xNew.toInt(), yNew.toInt())
        }

        /**
         * Checks if the necessary WiFi permissions are granted.
         *
         * This function checks if the following permissions are granted:
         * - ACCESS_FINE_LOCATION
         * - ACCESS_COARSE_LOCATION
         * - ACCESS_WIFI_STATE
         * - CHANGE_WIFI_STATE
         *
         * @param context The context in which the permissions are checked.
         * @return A boolean value indicating whether all necessary permissions are granted.
         */
        fun isWiFiPermissionGranted(context: Context): Boolean {
            // Check if ACCESS_FINE_LOCATION permission is granted
            val fineLocationPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

            // Check if ACCESS_COARSE_LOCATION permission is granted
            val coarseLocationPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

            // Check if ACCESS_WIFI_STATE permission is granted
            val wifiStatePermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)

            // Check if CHANGE_WIFI_STATE permission is granted
            val wifiScanPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE)

            // Return true if all permissions are granted, false otherwise
            return fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED && wifiStatePermission == PackageManager.PERMISSION_GRANTED && wifiScanPermission == PackageManager.PERMISSION_GRANTED
        }

        fun isStoragePermissionGranted(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}