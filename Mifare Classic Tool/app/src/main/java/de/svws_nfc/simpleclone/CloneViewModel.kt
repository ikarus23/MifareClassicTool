package de.svws_nfc.simpleclone

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class CloneViewModel(app: Application) : AndroidViewModel(app) {

    enum class Phase { WAIT_READ, READ_RUNNING, WAIT_WRITE, WRITE_RUNNING, DONE, ERROR }

    data class UiState(
        val phase: Phase = Phase.WAIT_READ,
        val message: String = "",
        val progress: Int = 0
    )

    val uiState = MutableLiveData(UiState())

    fun onTagScanned(tag: Tag) {
        when (uiState.value?.phase) {
            Phase.WAIT_READ -> {
                service?.startRead(tag)
            }
            Phase.WAIT_WRITE -> {
                service?.startWrite(tag)
            }
            else -> {} // DONE, ERROR 일 땐 무시
        }
    }

    /** Service 콜백이 호출할 메서드 */
    fun update(phase: CloneService.Phase, msg: String) {
        when (phase) {
            CloneService.Phase.READ  ->
                uiState.postValue(uiState.value?.copy(phase = Phase.READ_RUNNING, message = msg))
            CloneService.Phase.WRITE ->
                uiState.postValue(uiState.value?.copy(phase = Phase.WRITE_RUNNING, message = msg))
        }
    }

    // ...추가 로직
}
