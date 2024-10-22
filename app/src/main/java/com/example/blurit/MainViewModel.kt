package com.example.blurit

import android.net.Uri
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel(){
    private var uri: Uri? = null

    fun setUri(newUri: Uri?){
        uri = newUri
    }

    fun getUri(): Uri? = uri
}