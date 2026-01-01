package com.example.billbro.screens.dialog

import android.app.Dialog
import android.content.Context
import com.example.billbro.R

class LoadingDialog(val context: Context) {

    private lateinit var dialog: Dialog

    fun show() {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.loading_dialog)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    fun dismiss() {
        if (this::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }
}