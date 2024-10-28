package com.example.blurit

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.blurit.base.BaseActivity
import com.example.blurit.databinding.ActivityMainBinding
import com.example.blurit.edit.EditFragment
import com.example.blurit.home.HomeFragment

class MainActivity : BaseActivity<ActivityMainBinding>(
    ActivityMainBinding::inflate
) {
    private val mainViewModel: MainViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.layout_main_fragment, HomeFragment())
            .commit()

        val action: String = intent.action ?:""
        val type: String? = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("image/*" == type) {
                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                mainViewModel.setUri(imageUri)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.layout_main_fragment, EditFragment())
                    .commit()
            }
        }

    }
}