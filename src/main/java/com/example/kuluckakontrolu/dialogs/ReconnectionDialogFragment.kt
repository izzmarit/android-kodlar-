package com.example.kuluckakontrolu.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.kuluckakontrolu.databinding.DialogReconnectionBinding
import android.content.DialogInterface
import android.util.Log
import com.example.kuluckakontrolu.R

class ReconnectionDialogFragment : DialogFragment() {

    private var _binding: DialogReconnectionBinding? = null
    private val binding get() = _binding!!

    // Fragment sonlandığında null olmasın diye callback'i bir companion property'de tut
    companion object {
        private var reconnectCallbackStatic: ((Boolean) -> Unit)? = null

        fun newInstance(callback: (Boolean) -> Unit): ReconnectionDialogFragment {
            reconnectCallbackStatic = callback
            return ReconnectionDialogFragment()
        }
    }

    // Fragment'in kendi instance'ında da tut
    private var reconnectCallback: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Callback'i static'ten al
        reconnectCallback = reconnectCallbackStatic

        // Dialog kapatıldığında otomatik iptal için
        isCancelable = false

        // Dialog stilini ayarla
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogReconnectionBinding.inflate(inflater, container, false)

        // Make dialog corners rounded
        dialog?.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Dialog boyutunu ekran boyutuna göre ayarla
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRetry.setOnClickListener {
            try {
                reconnectCallback?.invoke(true)
            } catch (e: Exception) {
                Log.e("ReconnectionDialog", "Retry callback hatası: ${e.message}")
            }
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            try {
                reconnectCallback?.invoke(false)
            } catch (e: Exception) {
                Log.e("ReconnectionDialog", "Cancel callback hatası: ${e.message}")
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Dialog'un kapanışında callback'i temizle
        reconnectCallbackStatic = null
    }
}