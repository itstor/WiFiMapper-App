package com.itstor.wifimapper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.itstor.wifimapper.models.AppState
import com.itstor.wifimapper.models.Direction
import com.itstor.wifimapper.models.PointStatus
import com.itstor.wifimapper.models.Position
import com.itstor.wifimapper.models.ProjectSetting
import com.itstor.wifimapper.models.SparseGrid
import com.itstor.wifimapper.models.WiFiData
import com.itstor.wifimapper.models.WiFiPoint
import com.itstor.wifimapper.models.ZoomType
import com.itstor.wifimapper.utils.Utils
import com.itstor.wifimapper.utils.Utils.Companion.nextPoint
import com.itstor.wifimapper.utils.Utils.Companion.rotatePointAroundPivot

class MainViewModel : ViewModel() {
    private val _zoomType = MutableLiveData(ZoomType.FOCUS)
    val zoomType: LiveData<ZoomType> = _zoomType

    private val _currentPosition = MutableLiveData(Position(0, 0))
    val currentPosition: LiveData<Position> = _currentPosition

    private val _nextPosition = MutableLiveData(Position(0, 0))
    val nextPosition: LiveData<Position> = _nextPosition

    private val _deviceHeading = MutableLiveData(0f)
    val deviceHeading: LiveData<Float> = _deviceHeading

    private val _azimuthOffset = MutableLiveData(0f)
    val azimuthOffset: LiveData<Float> = _azimuthOffset

    private val _originPosition = MutableLiveData(Position(0, 0))
    val originPosition: LiveData<Position> = _originPosition

    private val _appState = MutableLiveData(AppState.NO_PROJECT)
    val appState: LiveData<AppState> = _appState

    private val _mapPoints: MutableLiveData<SparseGrid<WiFiPoint>> =
        MutableLiveData(SparseGrid(WiFiPoint()))
    val mapPoints: LiveData<SparseGrid<WiFiPoint>> = _mapPoints

    var wifiRecordedCounter = 0

    var projectSetting: ProjectSetting? = null
        private set

    var startRecordingTimestamp: Long = 0
        private set

    var wifiScanResultBuffer: MutableList<WiFiData> = mutableListOf()
        private set

    fun setAppState(appState: AppState) {
        _appState.value = appState
    }

    fun startRecording() {
        _appState.value = AppState.ON_RECORDING
        startRecordingTimestamp = System.currentTimeMillis()
    }

    fun stopRecording() {
        _appState.value = AppState.ON_IDLE
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - startRecordingTimestamp

        if (_currentPosition.value == null) {
            return
        }

        _mapPoints.value?.get(_currentPosition.value!!.x)?.set(
            _currentPosition.value!!.y, WiFiPoint(
                data = wifiScanResultBuffer.toMutableList(),
                scannedAt = currentTime,
                scanDuration = elapsedTime,
                status = if (wifiScanResultBuffer.isEmpty()) PointStatus.UNRECORDED else PointStatus.RECORDED,
                recordedDataCount = wifiRecordedCounter
            )
        )

        forceUpdateMap()

        wifiScanResultBuffer.clear()
        startRecordingTimestamp = 0
        wifiRecordedCounter = 0
    }

    fun addNewPointToMap(position: Position, point: WiFiPoint) {
        _mapPoints.value?.get(position.x)?.set(position.y, point)
        forceUpdateMap()
    }

    fun deletePointFromMap(position: Position) {
        _mapPoints.value?.remove(position)
        forceUpdateMap()
    }

    fun clearWifiPointData(position: Position) {
        _mapPoints.value?.get(position.x)?.set(position.y, WiFiPoint())
        forceUpdateMap()
    }

    fun createProject(projectSetting: ProjectSetting) {
        this.projectSetting = projectSetting
        _appState.value = AppState.ON_IDLE
    }

    fun setOriginPosition(position: Position) {
        _originPosition.value = position
        _mapPoints.value!!.shiftOrigin(position)
        setCurrentPosition(Position(0, 0))
        forceUpdateMap()
    }

    fun setAzimuthOffset(offset: Float) {
        _azimuthOffset.value = offset
    }

    fun setZoomType(zoomType: ZoomType) {
        _zoomType.value = zoomType
    }

    fun setCurrentPosition(position: Position) {
        _currentPosition.value = position
    }

    fun setDeviceHeading(heading: Float) {
        _deviceHeading.postValue(heading)
        calculateNextPoint()
    }

    fun movePosition(direction: Direction) {
        when (direction) {
            Direction.FORWARD -> _currentPosition.value = _nextPosition.value
            Direction.BACKWARD -> _currentPosition.value = rotatePointAroundPivot(
                _nextPosition.value ?: Position(0, 0),
                _currentPosition.value ?: Position(0, 0),
                180.0
            )

            Direction.LEFT -> _currentPosition.value = rotatePointAroundPivot(
                _nextPosition.value ?: Position(0, 0),
                _currentPosition.value ?: Position(0, 0),
                -90.0
            )

            Direction.RIGHT -> _currentPosition.value = rotatePointAroundPivot(
                _nextPosition.value ?: Position(0, 0),
                _currentPosition.value ?: Position(0, 0),
                90.0
            )
        }
    }

    private fun calculateNextPoint() {
        _nextPosition.value = nextPoint(
            _currentPosition.value ?: Position(0, 0),
            Utils.closestStandardAngle(-(_deviceHeading.value?.toInt() ?: 0) + 90)
        )
    }

    private fun forceUpdateMap() {
        _mapPoints.value = _mapPoints.value
    }
}