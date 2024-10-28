package com.example.blurit.home

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.fragment.app.activityViewModels
import com.example.blurit.MainActivity
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentHomeBinding
import com.example.blurit.edit.EditFragment

private const val TAG = "HomeFragment_hong"

class HomeFragment : BaseFragment<FragmentHomeBinding>(
    FragmentHomeBinding::bind, R.layout.fragment_home
) {
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity
    private var backPressedOnce = false

    private val pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            mainViewModel.setUri(uri)
            parentFragmentManager.beginTransaction()
                .replace(R.id.layout_main_fragment, EditFragment())
                .addToBackStack("home")
                .commit()

        } else {
            Log.d(TAG, "uri is null")
            mainViewModel.setUri(null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = _activity as MainActivity

        binding.ivPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            backPressToQuit()
        }
    }

    private fun backPressToQuit() {
        if (backPressedOnce) {
            requireActivity().finish()
            return
        }
        backPressedOnce = true
        activity.showToast(activity.getString(R.string.backpress_to_quit))

        Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
    }
}