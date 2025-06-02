package com.example.demoidimgktx.utils

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.example.demoidimgktx.R
import com.example.demoidimgktx.ui.fragment.AdjustFragment
import com.example.demoidimgktx.ui.fragment.TemplateFragment

object NavManager {

    fun navigateToAdjust(manager: FragmentManager) {
        try {
            val fragment = AdjustFragment().apply {
                arguments = Bundle().apply {}
            }
            manager.beginTransaction().apply {
                add(R.id.frame_main, fragment)
                commitAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun navigateToTemplate(manager: FragmentManager) {
        try {
            val fragment = TemplateFragment().apply {
                arguments = Bundle().apply {}
            }
            manager.beginTransaction().apply {
                add(R.id.frame_main, fragment)
                commitAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}