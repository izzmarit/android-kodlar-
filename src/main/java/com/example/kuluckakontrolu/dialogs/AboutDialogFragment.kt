package com.example.kuluckakontrolu.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.kuluckakontrolu.BuildConfig
import com.example.kuluckakontrolu.databinding.DialogAboutBinding

class AboutDialogFragment : DialogFragment() {

    private var _binding: DialogAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAboutBinding.inflate(inflater, container, false)

        // Make dialog corners rounded
        dialog?.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Diyalog genişliğini ekran genişliğinin %60'ı olarak ayarla
            setLayout(
                (resources.displayMetrics.widthPixels * 0.6).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set app version
        binding.textVersion.text = "v${BuildConfig.VERSION_NAME}"

        // Close button
        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}