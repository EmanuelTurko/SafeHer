package com.example.safeher.home_screen.support

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SupportCallViewModel : ViewModel() {

    enum class CallMode {NONE, AI ,SISTER}

    private val _callMode = MutableLiveData<CallMode>(CallMode.NONE)
    val callMode : LiveData<CallMode> = _callMode

    fun selectCallMode(callMode: CallMode){
        _callMode.value = callMode
    }
    fun reset() {
        _callMode.value = CallMode.NONE
    }
}