package com.example.progetto.utils // Change this package name if you put it in a different folder!

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.repositories.InsightsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class InsightsFilter {
    APP_DETECTED,
    USER_EXPERIENCED
}

class InsightsViewModel : ViewModel() {
    private val insightsRepository = InsightsRepository()

    // Holds the raw data from the server so we don't have to re-download it when toggling
    private var fullInsightsData: InsightsResponse? = null

    // UI State: Which toggle is selected?
    private val _selectedFilter = MutableStateFlow(InsightsFilter.APP_DETECTED)
    val selectedFilter: StateFlow<InsightsFilter> = _selectedFilter.asStateFlow()

    // UI State: The data currently being shown on the pie chart
    private val _chartData = MutableStateFlow(EmotionStats())
    val chartData: StateFlow<EmotionStats> = _chartData.asStateFlow()

    fun loadInsights(currentUserId: String) {
        viewModelScope.launch {
            try {
                val response = insightsRepository.getInsights(currentUserId)
                if (response.isSuccessful && response.body() != null) {
                    fullInsightsData = response.body()
                    // Populate the chart with the "App Detected" data by default
                    updateChartData(InsightsFilter.APP_DETECTED)
                } else {
                    Log.e("InsightsViewModel", "Data came back null or error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("InsightsViewModel", "Error fetching data", e)
            }
        }
    }

    // Called when the user clicks the toggle switch
    fun setFilter(filter: InsightsFilter) {
        _selectedFilter.value = filter
        updateChartData(filter)
    }

    // Swaps the pie chart data between App Detected and User Experienced
    private fun updateChartData(filter: InsightsFilter) {
        val currentData = fullInsightsData ?: return

        _chartData.value = when (filter) {
            InsightsFilter.APP_DETECTED -> currentData.app_detected
            InsightsFilter.USER_EXPERIENCED -> currentData.user_experienced
        }
    }
}