package com.itstor.wifimapper

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat

const val WIFI_SCAN_INTERVAL = 1000L // 1 seconds. This is the minimum interval allowed by Android 10, when the throttling config is disabled.

/**
 * A helper class to manage WiFi scanning.
 * @param context The application context.
 * @param onSuccessfulScan A callback to be called when a WiFi scan is successful.
 */
class WiFiScanner(private val context: Context, private val onSuccessfulScan: (List<ScanResult>, Long, Boolean) -> Unit) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver: BroadcastReceiver
    private var isBroadcastReceiverRegistered = false
    private var latestSuccessfulScanTimestamp = 0L
    private var latestRequestedScanTimestamp = 0L

    init {
        // Register a BroadcastReceiver to listen to WiFi scan results.
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

                if (success) {
                    latestSuccessfulScanTimestamp = System.currentTimeMillis()
                }

                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    onSuccessfulScan(getScanResults(), latestSuccessfulScanTimestamp, success)
                }
            }
        }
    }

    /**
     * Starts a WiFi scan.
     * @throws Exception if WiFi is not enabled.
     */
    fun startScan() {
        if (!wifiManager.isWifiEnabled) {
            throw Exception("WiFi is not enabled.")
        }

        if (!isBroadcastReceiverRegistered) {
            registerReceiver()
            Log.d(TAG, "Registered broadcast receiver.")
        }

        val currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp - latestRequestedScanTimestamp > WIFI_SCAN_INTERVAL) {
            wifiManager.startScan()
            latestRequestedScanTimestamp = currentTimestamp
            Log.d(TAG, "Started WiFi scan.")
        } else {
            Log.d(TAG, "Skipped WiFi scan.")
        }
    }

    /**
     * Gets the latest scan results.
     * @return A list of ScanResult.
     * @throws SecurityException if the ACCESS_FINE_LOCATION permission is not granted.
     * @throws Exception if WiFi is not enabled.
     */
    fun getScanResults(): List<ScanResult> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Location permission not granted.")
        }

        if (!wifiManager.isWifiEnabled) {
            throw Exception("WiFi is not enabled.")
        }

        wifiManager.scanResults[0].timestamp

        return wifiManager.scanResults
    }

    /**
     * Registers the BroadcastReceiver.
     */
    private fun registerReceiver() {
        if (!isBroadcastReceiverRegistered) {
            context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            isBroadcastReceiverRegistered = true
        }
    }

    /**
     * Unregisters the BroadcastReceiver.
     * Should be called when the activity is destroyed.
     */
    fun unregisterReceiver() {
        context.unregisterReceiver(wifiScanReceiver)
    }

    companion object {
        private const val TAG = "WiFiScanner"
    }
}