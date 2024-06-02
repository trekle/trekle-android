package space.trekle.app.ui.modal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import space.trekle.app.R


class ModalBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.bottom_modal_poi, container, false)

        // Example: Setting a click listener
        /*
        val textView = view.findViewById<TextView>(R.id.textView)
        teIxtView.setOnClickListener {
            // Handle the click event
            dismiss() // Close the Bottom Sheet
        }

         */

        return view
    }
}
