package com.rvc.vehicledocsnfc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DocsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    val tabs = listOf("Vehículo", "Permiso", "SOAP", "Rev. Técnica")

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> VehiculoFragment()
        1 -> PermisoFragment()
        2 -> SoapFragment()
        3 -> RevisionFragment()
        else -> VehiculoFragment()
    }
}
