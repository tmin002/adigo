package kr.gachon.adigo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UwbLocationViewModel @Inject constructor() : ViewModel() {

    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance.asStateFlow()

    private val _angle = MutableStateFlow(0f)
    val angle: StateFlow<Float> = _angle.asStateFlow()

    fun updateLocation(distance: Float, angle: Float) {
        viewModelScope.launch {
            _distance.value = distance
            _angle.value = angle
        }
    }
}
