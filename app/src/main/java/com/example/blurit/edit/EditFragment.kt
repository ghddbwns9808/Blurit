package com.example.blurit.edit

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.transition.Visibility
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentEditBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior


private const val TAG = "EditFragment_hong"
class EditFragment : BaseFragment<FragmentEditBinding>(
    FragmentEditBinding::bind, R.layout.fragment_edit
) {
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView(){
        binding.ivPhoto.setImageURI(mainViewModel.getUri())

        binding.tvCancle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.sdBlur.setLabelFormatter{
            it.toInt().toString()
        }

        binding.sdThick.setLabelFormatter {
            it.toInt().toString()
        }

        binding.ivAuto.setOnClickListener {
            binding.llBlur.visibility = View.VISIBLE
            binding.llThick.visibility = View.GONE
        }

        binding.ivManual.setOnClickListener {
            binding.llBlur.visibility = View.VISIBLE
            binding.llThick.visibility = View.VISIBLE
        }

        binding.ivErase.setOnClickListener {
            binding.llBlur.visibility = View.GONE
            binding.llThick.visibility = View.VISIBLE
        }
    }
}