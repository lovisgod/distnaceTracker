package com.lovisgod.distancetracker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.distance_layout.view.*

class DistanceBottomDialog(): BottomSheetDialogFragment() {


    companion object{
        fun newInstance(distance: Float?, time: String): DistanceBottomDialog? {
            val bottomSheet = DistanceBottomDialog()
            val bundle = Bundle()
            if (distance != null) {
                bundle.putFloat("distance", distance)
                bundle.putString("time", time)
            }
            bottomSheet.setArguments(bundle)
            return bottomSheet
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.distance_layout, container, false)
        arguments?.let {
            val distance = it.getFloat("distance")
            val time = it.getString("time")


            view.distance_convered.text = "$distance m"
            view.time.text = "in $time Min"
        }
        return  view
    }
}