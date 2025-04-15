
// FlowProvider.kt
package kr.gachon.adigo.service

import kotlinx.coroutines.flow.MutableStateFlow

object FlowProvider {
    @JvmStatic
    fun createFloatStateFlow(initial: Float): MutableStateFlow<Float> {
        return MutableStateFlow(initial)
    }
}
