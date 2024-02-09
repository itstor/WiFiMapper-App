package com.itstor.wifimapper

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itstor.wifimapper.databinding.NewProjectBottomSheetBinding
import com.itstor.wifimapper.models.ProjectSetting

class NewProjectBottomSheetFragment: BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: NewProjectBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = NewProjectBottomSheetBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.isEnabled = false

        binding.etProjectName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateProjectName()
            }
        })

        binding.etDistancePoints.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateDistancePoints()
            }
        })

        binding.etStopScanAt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateStopScanAt()
            }
        })

        binding.etWifiRegex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateWifiRegex()
            }
        })

        binding.etScanInterval.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateScanInterval()
            }
        })

        binding.btnCreate.setOnClickListener {
            if (isFieldsValid()) {
                val projectName = binding.etProjectName.text.toString()
                val distanceBetweenPoints = binding.etDistancePoints.text.toString()
                val stopScanAt = binding.etStopScanAt.text.toString()
                val wifiRegex = binding.etWifiRegex.text.toString()
                val scanInterval = binding.etScanInterval.text.toString()

                val projectSetting = ProjectSetting(
                    projectName = projectName,
                    wifiRegex = wifiRegex,
                    wifiScanInterval = scanInterval.toInt(),
                    distanceBetweenPoints = distanceBetweenPoints.toInt(),
                    stopScanAt = stopScanAt.toInt(),
                    createdAt = System.currentTimeMillis()
                )

                viewModel.createProject(projectSetting)

                dismiss()

                Toast.makeText(requireContext(), "Project created", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateProjectName() {
        val projectName = binding.etProjectName.text.toString()
        val isProjectNameValid = projectName.isNotEmpty()

        binding.btnCreate.isEnabled = isProjectNameValid
        binding.tlProjectName.error = if (isProjectNameValid) null else "Project Name is required"
    }

    private fun validateDistancePoints() {
        val distanceBetweenPoints = binding.etDistancePoints.text.toString()
        val isDistanceValid = distanceBetweenPoints.isNotEmpty() && distanceBetweenPoints.toInt() > 0

        binding.btnCreate.isEnabled = isDistanceValid
        binding.tlDistancePoints.error =
            if (isDistanceValid) null else "Distance must be greater than 0"
    }

    private fun validateStopScanAt() {
        val stopScanAt = binding.etStopScanAt.text.toString()
        val isStopScanAtValid = stopScanAt.isNotEmpty() && stopScanAt.toInt() >= 0

        binding.btnCreate.isEnabled = isStopScanAtValid
        binding.tlStopScanAt.error =
            if (isStopScanAtValid) null else "Stop Scan At must be greater than or equal to 0"
    }

    private fun validateWifiRegex() {
        val wifiRegex = binding.etWifiRegex.text.toString()
        val isWifiRegexValid = wifiRegex.isNotEmpty()

        binding.btnCreate.isEnabled = isWifiRegexValid
        binding.tlWifiRegex.error = if (isWifiRegexValid) null else "WiFi Regex is required"
    }

    private fun validateScanInterval() {
        val scanInterval = binding.etScanInterval.text.toString()
        val isScanIntervalValid = scanInterval.isNotEmpty() && scanInterval.toInt() >= 1000

        binding.btnCreate.isEnabled = isScanIntervalValid
        binding.tlScanInterval.error =
            if (isScanIntervalValid) null else "Scan Interval must be greater or equal than 1000"
    }

    private fun isFieldsValid(): Boolean {
        val projectName = binding.etProjectName.text.toString()
        val distanceBetweenPoints = binding.etDistancePoints.text.toString()
        val stopScanAt = binding.etStopScanAt.text.toString()
        val wifiRegex = binding.etWifiRegex.text.toString()
        val scanInterval = binding.etScanInterval.text.toString()

        val isProjectNameValid = projectName.isNotEmpty()
        val isDistanceValid = distanceBetweenPoints.isNotEmpty() && distanceBetweenPoints.toInt() > 0
        val isStopScanAtValid = stopScanAt.isNotEmpty() && stopScanAt.toInt() >= 0
        val isWifiRegexValid = wifiRegex.isNotEmpty()
        val isScanIntervalValid = scanInterval.isNotEmpty() && scanInterval.toInt() >= 1000

        return isProjectNameValid && isDistanceValid && isStopScanAtValid && isWifiRegexValid && isScanIntervalValid
    }
}