package ch.privately.posintegration

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/*
* A fragment that displays an AlertDialog with the recognized document.
*/
class AlertDialogFragment : DialogFragment() {
    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (host is Callbacks) {
            callbacks = host as Callbacks?
        } else {
            throw ClassCastException("Parent fragment doesn't implement Callbacks!")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @StringRes val titleRes = requireArguments().getInt(KEY_TITLE_RES)
        val message = requireArguments().getString(KEY_MESSAGE)

        return AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        callbacks!!.onAlertDismissed()
    }

    interface Callbacks {
        fun onAlertDismissed()
    }

    companion object {
        private const val KEY_TITLE_RES = "title_res"
        private const val KEY_MESSAGE = "message"

        fun newInstance(@StringRes title: Int, message: String?): AlertDialogFragment {
            val arguments = Bundle()
            arguments.putInt(KEY_TITLE_RES, title)
            arguments.putString(KEY_MESSAGE, message)

            val fragment = AlertDialogFragment()
            fragment.arguments = arguments

            return fragment
        }
    }
}