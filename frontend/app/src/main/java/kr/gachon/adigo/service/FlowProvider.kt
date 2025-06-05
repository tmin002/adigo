
// FlowProvider.kt
package kr.gachon.adigo.service

import kotlinx.coroutines.flow.MutableStateFlow

object FlowProvider {
    @JvmStatic
    fun createFloatStateFlow(initial: Float): MutableStateFlow<Float> {
        return MutableStateFlow(initial)
    }

    @JvmStatic // Makes this callable as a static method from Java
    fun <T> createMutableStateFlow(initialValue: T): MutableStateFlow<T> {
        return MutableStateFlow(initialValue)
    }
}
