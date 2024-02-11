package com.itstor.wifimapper

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.itstor.wifimapper.DeviceHeading.CompassListener
import com.itstor.wifimapper.customview.MapView
import com.itstor.wifimapper.databinding.ActivityMainBinding
import com.itstor.wifimapper.models.AppState
import com.itstor.wifimapper.models.DataFormat
import com.itstor.wifimapper.models.Direction
import com.itstor.wifimapper.models.PointStatus
import com.itstor.wifimapper.models.Position
import com.itstor.wifimapper.models.WiFiData
import com.itstor.wifimapper.models.WiFiPoint
import com.itstor.wifimapper.models.ZoomType
import com.itstor.wifimapper.utils.Utils.Companion.isStoragePermissionGranted
import com.itstor.wifimapper.utils.Utils.Companion.isWiFiPermissionGranted
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var deviceHeading: DeviceHeading
    private lateinit var mapView: MapView
    private lateinit var wifiScanner: WiFiScanner
    private lateinit var loadProjectLauncher: ActivityResultLauncher<String>

    private val scanLoopHandler = Handler(Looper.getMainLooper())
    private val runnableScanLoop = object : Runnable {
        override fun run() {
            wifiScanner.startScan()

            val delay = if (viewModel.appState.value == AppState.ON_RECORDING) {
                viewModel.projectSetting?.wifiScanInterval?.toLong() ?: 1000L
            } else {
                10000L
            }

            scanLoopHandler.postDelayed(this, delay)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        wifiScanner = WiFiScanner(this) { wifiList, successfulTimestamp, success ->
            onSuccessfulWifiScanCallback(wifiList, successfulTimestamp, success)
        }

        deviceHeading = DeviceHeading(this).apply {
            listener = getCompassListener()
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        mapView = MapView(this, null).apply {
            yOffset = -150f
        }

        binding.flMap.addView(mapView, params)

        mapView.setPoints(viewModel.mapPoints.value!!)

        loadProjectLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(uri)
                val jsonData = inputStream?.bufferedReader().use { it?.readText() } ?: ""

                try {
                    val parsedData = Json.decodeFromString(DataFormat.serializer(), jsonData)

                    viewModel.loadProjectFromFile(parsedData)
                } catch (e: Exception) {
                    Toast.makeText(this, "The file is not a valid project file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.appState.observe(this) {
            when (it) {
                AppState.NO_PROJECT -> {
                    binding.btnAddClear.isEnabled = false
                    binding.btnChangeOrigin.isEnabled = false
                    binding.btnRecord.isEnabled = false
                    binding.btnSave.isEnabled = false
                    binding.btnOpenProject.isEnabled = true
                    binding.btnNewProject.isEnabled = true
                    binding.btnLeft.isEnabled = false
                    binding.btnRight.isEnabled = false
                    binding.btnUp.isEnabled = false
                    binding.btnDown.isEnabled = false
                    binding.llProject.visibility = android.view.View.VISIBLE
                    binding.llControlPanel.visibility = android.view.View.GONE
                    binding.cvStats.visibility = android.view.View.GONE
                    binding.etYPosition.isEnabled = false
                    binding.etXPosition.isEnabled = false
                    binding.btnSettings.isEnabled = false
                }

                AppState.ON_IDLE -> {
                    binding.btnAddClear.isEnabled = true
                    binding.btnChangeOrigin.isEnabled = true
                    binding.btnRecord.isEnabled = true
                    binding.btnSave.isEnabled = true
                    binding.btnOpenProject.isEnabled = true
                    binding.btnNewProject.isEnabled = true
                    binding.btnLeft.isEnabled = true
                    binding.btnRight.isEnabled = true
                    binding.btnUp.isEnabled = true
                    binding.btnDown.isEnabled = true
                    binding.llProject.visibility = android.view.View.GONE
                    binding.llControlPanel.visibility = android.view.View.VISIBLE
                    binding.cvStats.visibility = android.view.View.VISIBLE
                    binding.btnRecord.icon = AppCompatResources.getDrawable(
                        this, R.drawable.round_fiber_manual_record_24
                    )
                    binding.etYPosition.isEnabled = true
                    binding.etXPosition.isEnabled = true
                    binding.btnSettings.isEnabled = true
                }

                AppState.ON_RECORDING -> {
                    binding.btnAddClear.isEnabled = false
                    binding.btnChangeOrigin.isEnabled = false
                    binding.btnRecord.isEnabled = true
                    binding.btnSave.isEnabled = false
                    binding.btnOpenProject.isEnabled = false
                    binding.btnNewProject.isEnabled = false
                    binding.btnLeft.isEnabled = false
                    binding.btnRight.isEnabled = false
                    binding.btnUp.isEnabled = false
                    binding.btnDown.isEnabled = false
                    binding.llProject.visibility = android.view.View.GONE
                    binding.llControlPanel.visibility = android.view.View.VISIBLE
                    binding.cvStats.visibility = android.view.View.VISIBLE
                    binding.btnRecord.icon = AppCompatResources.getDrawable(
                        this, R.drawable.round_stop_24
                    )
                    binding.etYPosition.isEnabled = false
                    binding.etXPosition.isEnabled = false
                    binding.btnSettings.isEnabled = false
                }

                AppState.ON_SAVING -> {
                    binding.btnAddClear.isEnabled = false
                    binding.btnChangeOrigin.isEnabled = false
                    binding.btnRecord.isEnabled = false
                    binding.btnSave.isEnabled = false
                    binding.btnOpenProject.isEnabled = false
                    binding.btnNewProject.isEnabled = false
                    binding.btnLeft.isEnabled = true
                    binding.btnRight.isEnabled = true
                    binding.btnUp.isEnabled = true
                    binding.btnDown.isEnabled = true
                    binding.llProject.visibility = android.view.View.GONE
                    binding.llControlPanel.visibility = android.view.View.VISIBLE
                    binding.cvStats.visibility = android.view.View.VISIBLE
                }

                else -> return@observe
            }
        }

        viewModel.mapPoints.observe(this) {
            mapView.invalidateMap()
            updateStatsUI()
            updateButtonClearAdd()
        }

        viewModel.currentPosition.observe(this) {
            mapView.setFocusPoint(it)

            binding.etXPosition.setText(it.x.toString())
            binding.etYPosition.setText(it.y.toString())

            updateButtonClearAdd()
            updateStatsUI()
        }

        viewModel.deviceHeading.observe(this) {
            mapView.rotationDegrees = it
            binding.gizmo.angle = it
        }

        viewModel.nextPosition.observe(this) {
            mapView.nextPosition = it
        }

        viewModel.zoomType.observe(this) {
            mapView.setZoomType(it)

            binding.btnToggleShowAll.icon = when (it) {
                ZoomType.FOCUS -> AppCompatResources.getDrawable(
                    this, R.drawable.round_zoom_out_map_24
                )

                ZoomType.SHOW_ALL -> AppCompatResources.getDrawable(
                    this, R.drawable.round_zoom_in_map_24
                )

                else -> return@observe
            }
        }

        viewModel.azimuthOffset.observe(this) {
            deviceHeading.setAzimuthFix(it)
        }

        binding.etXPosition.setOnEditorActionListener { _, _, _ ->
            viewModel.setCurrentPosition(
                viewModel.currentPosition.value?.copy(
                    x = binding.etXPosition.text.toString().toInt()
                ) ?: Position(0, 0)
            )
            true
        }

        binding.etYPosition.setOnEditorActionListener { _, _, _ ->
            viewModel.setCurrentPosition(
                viewModel.currentPosition.value?.copy(
                    y = binding.etYPosition.text.toString().toInt()
                ) ?: Position(0, 0)
            )
            true
        }

        binding.btnToggleShowAll.setOnClickListener {
            when (viewModel.zoomType.value) {
                ZoomType.FOCUS -> viewModel.setZoomType(ZoomType.SHOW_ALL)
                ZoomType.SHOW_ALL -> viewModel.setZoomType(ZoomType.FOCUS)
                else -> return@setOnClickListener
            }
        }

        binding.btnZoomIn.setOnClickListener {
            mapView.zoomIn()
        }

        binding.btnZoomOut.setOnClickListener {
            mapView.zoomOut()
        }

        binding.btnUp.setOnClickListener {
            viewModel.movePosition(Direction.FORWARD)
        }

        binding.btnDown.setOnClickListener {
            viewModel.movePosition(Direction.BACKWARD)
        }

        binding.btnLeft.setOnClickListener {
            viewModel.movePosition(Direction.LEFT)
        }

        binding.btnRight.setOnClickListener {
            viewModel.movePosition(Direction.RIGHT)
        }

        binding.btnResetHeading.setOnClickListener {
            viewModel.setAzimuthOffset(-deviceHeading.getRawAzimuth())
        }

        binding.btnChangeOrigin.setOnClickListener {
            viewModel.setOriginPosition(viewModel.currentPosition.value ?: Position(0, 0))
            mapView.invalidateMap()
        }

        binding.btnAddClear.setOnClickListener {
            val currentPosition = viewModel.currentPosition.value ?: Position(0, 0)
            val currentPoint = viewModel.mapPoints.value!!.get(currentPosition)

            when (currentPoint?.status) {
                PointStatus.RECORDED -> {
                    viewModel.addNewPointToMap(
                        currentPosition, WiFiPoint(status = PointStatus.UNRECORDED)
                    )
                }

                PointStatus.UNRECORDED -> {
                    viewModel.deletePointFromMap(currentPosition)
                }

                else -> {
                    viewModel.addNewPointToMap(
                        currentPosition, WiFiPoint(status = PointStatus.UNRECORDED)
                    )
                }
            }

            mapView.invalidateMap()
            viewModel.mapPoints.value.let { mapPoints ->
                viewModel.currentPosition.value?.let { currentPosition ->
                    binding.tvRecordedPoints.text =
                        (mapPoints?.get(currentPosition)?.data?.size ?: 0).toString()
                }
            }
        }

        binding.btnRecord.setOnClickListener {
            when (viewModel.appState.value) {
                AppState.ON_IDLE -> startRecording()

                AppState.ON_RECORDING -> stopRecording()
                else -> return@setOnClickListener
            }
        }

        binding.btnOpenProject.setOnClickListener {
            loadProjectLauncher.launch("application/json")
        }

        binding.btnNewProject.setOnClickListener {
            showNewProjectBottomSheet()

//            // For debug
//            val projectSetting = ProjectSetting(
//                projectName = "Test Project",
//                wifiRegex = ".*",
//                wifiScanInterval = 1000,
//                distanceBetweenPoints = 1,
//                stopScanAt = 10,
//                createdAt = System.currentTimeMillis()
//            )

//            viewModel.createProject(projectSetting)
        }

        binding.btnSettings.setOnClickListener {
            showProjectSettingBottomSheet()
        }

        binding.btnSave.setOnClickListener {
            if (!isStoragePermissionGranted(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )

                return@setOnClickListener
            }

            viewModel.setAppState(AppState.ON_SAVING)

            GlobalScope.launch(Dispatchers.IO) {
                viewModel.projectSetting?.let {
                    viewModel.mapPoints.value?.let { mapPoints ->
                        val fileName = it.projectName.replace(" ", "_") + ".json"

                        val selectionArgs = arrayOf(fileName)
                        contentResolver.query(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            null,
                            "${MediaStore.Downloads.DISPLAY_NAME}=?",
                            selectionArgs,
                            null
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val columnIndex = cursor.getColumnIndex(MediaStore.Downloads._ID)
                                if (columnIndex != -1) {
                                    val id = cursor.getLong(columnIndex)
                                    contentResolver.delete(
                                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                        "${MediaStore.Downloads._ID}=?",
                                        arrayOf(id.toString())
                                    )
                                }
                            }
                        }

                        val data = DataFormat(
                            projectName = it.projectName,
                            wifiRegex = it.wifiRegex,
                            wifiScanInterval = it.wifiScanInterval,
                            distanceBetweenPoints = it.distanceBetweenPoints,
                            stopScanAt = it.stopScanAt,
                            createdAt = it.createdAt,
                            data = mapPoints.toExportFormat()
                        )

                        val json = Json { prettyPrint = true }
                        val jsonData = json.encodeToString(DataFormat.serializer(), data)

                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/json")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val resolver = contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                        uri?.let {
                            try {
                                val outputStream: OutputStream? = resolver.openOutputStream(it)
                                outputStream?.use { os ->
                                    os.write(jsonData.toByteArray())
                                }

                                values.clear()
                                values.put(MediaStore.Downloads.IS_PENDING, 0)
                                resolver.update(uri, values, null, null)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "File saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "File save error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    viewModel.setAppState(AppState.ON_IDLE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        deviceHeading.start()

        if (isWiFiPermissionGranted(this)) {
            scanLoopHandler.post(runnableScanLoop)
        } else {
            requestLocationAndWifiPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
        deviceHeading.stop()
        scanLoopHandler.removeCallbacks(runnableScanLoop)
    }

    override fun onStop() {
        super.onStop()
        deviceHeading.stop()
        scanLoopHandler.removeCallbacks(runnableScanLoop)
        wifiScanner.unregisterReceiver()
    }

    private fun showNewProjectBottomSheet() {
        val bottomSheetFragment = NewProjectBottomSheetFragment()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun showProjectSettingBottomSheet() {
        val bottomSheetFragment = ProjectSettingBottomSheetFragment()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun getCompassListener(): CompassListener {
        return object : CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                viewModel.setDeviceHeading(azimuth)
            }
        }
    }

    private fun updateButtonClearAdd() {
        val currentPositionStatus =
            viewModel.mapPoints.value?.get(viewModel.currentPosition.value!!)?.status

        binding.btnAddClear.icon = when (currentPositionStatus) {
            PointStatus.RECORDED -> AppCompatResources.getDrawable(
                this, R.drawable.round_clear_24
            )

            PointStatus.UNRECORDED -> AppCompatResources.getDrawable(
                this, R.drawable.round_delete_24
            )

            else -> AppCompatResources.getDrawable(
                this, R.drawable.round_add_24
            )
        }
    }

    private fun startRecording() {
        val currentPoint = viewModel.mapPoints.value!!.get(viewModel.currentPosition.value!!)

        if (currentPoint == null) {
            viewModel.addNewPointToMap(
                viewModel.currentPosition.value!!, WiFiPoint(status = PointStatus.UNRECORDED)
            )
        } else if (currentPoint.status == PointStatus.RECORDED) {
            viewModel.clearWifiPointData(viewModel.currentPosition.value!!)
        }

        viewModel.startRecording()
    }

    private fun stopRecording() {
        viewModel.stopRecording()
        updateStatsUI()
    }

    private fun updateStatsUI() {
        viewModel.mapPoints.value?.let { mapPoints ->
            viewModel.currentPosition.value?.let { currentPosition ->
                binding.tvTotalPoints.text = mapPoints.size().toString()
                binding.tvRemainingPoints.text =
                    mapPoints.sumOf { _, wiFiPoint -> if (wiFiPoint.status == PointStatus.RECORDED) 1 else 0 }
                        .toString()
                binding.tvRecordedPoints.text =
                    (mapPoints.get(currentPosition)?.recordedDataCount ?: 0).toString()
            }
        }
    }

    private fun onSuccessfulWifiScanCallback(
        wifiList: List<ScanResult>, successfulTimestamp: Long, success: Boolean
    ) {
        val filteredWifiList =
            wifiList.filter { it.SSID.matches(Regex(viewModel.projectSetting?.wifiRegex ?: ".*")) }

        if (viewModel.appState.value != AppState.NO_PROJECT) {
            binding.tvDetectedAp.text = filteredWifiList.size.toString()
        }

        if (viewModel.appState.value == AppState.ON_RECORDING) {
            // Ignore scans that were started before the recording started
            if (successfulTimestamp < viewModel.startRecordingTimestamp || !success) {
                return
            }

            val wifiDataList = filteredWifiList.map {
                WiFiData(
                    ssid = it.SSID,
                    bssid = it.BSSID,
                    rssi = it.level,
                    frequency = it.frequency,
                    row = viewModel.wifiRecordedCounter
                )
            }

            viewModel.wifiScanResultBuffer.addAll(wifiDataList)
            viewModel.wifiRecordedCounter++
            binding.tvRecordedPoints.text = viewModel.wifiRecordedCounter.toString()

            if (viewModel.projectSetting?.stopScanAt != null
                && viewModel.wifiRecordedCounter >= viewModel.projectSetting!!.stopScanAt
                && viewModel.projectSetting!!.stopScanAt > 0
            ) {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val mediaPlayer = MediaPlayer.create(this, notification)
                mediaPlayer.start()

                stopRecording()
            }
        }
    }

    private fun requestLocationAndWifiPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            ), LOCATION_WIFI_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_WIFI_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    scanLoopHandler.post(runnableScanLoop)
                } else {
                    Toast.makeText(
                        this,
                        "Location and wifi permissions are required to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(
                        this,
                        "Storage permission is required to save the data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_WIFI_REQUEST_CODE = 2
        private const val READ_REQUEST_CODE = 42
    }
}