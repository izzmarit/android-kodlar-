package com.example.kuluckakontrolu.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.DialogAddNoteBinding
import com.squareup.picasso.Picasso
import java.io.File

class AddNoteDialogFragment : DialogFragment() {
    private var _binding: DialogAddNoteBinding? = null
    private val binding get() = _binding!!

    private var cycleId: Long = 0
    private var imagePath: String? = null
    private var addNoteListener: AddNoteListener? = null

    interface AddNoteListener {
        fun onNoteAdded(cycleId: Long, text: String, imagePath: String?)
    }

    companion object {
        fun newInstance(cycleId: Long, imagePath: String? = null): AddNoteDialogFragment {
            val fragment = AddNoteDialogFragment()
            val args = Bundle()
            args.putLong("cycleId", cycleId)
            if (imagePath != null) {
                args.putString("imagePath", imagePath)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cycleId = arguments?.getLong("cycleId", 0) ?: 0
        imagePath = arguments?.getString("imagePath")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dialog başlığı ayarla
        dialog?.setTitle(R.string.add_note)

        // Resim varsa göster
        imagePath?.let { path ->
            binding.imagePreview.visibility = View.VISIBLE
            Picasso.get().load(File(path)).into(binding.imagePreview)
        }

        // İptal butonu
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Kaydet butonu
        binding.buttonSave.setOnClickListener {
            val noteText = binding.editTextNote.text.toString().trim()
            if (noteText.isEmpty()) {
                binding.editTextNote.error = getString(R.string.required_field)
                return@setOnClickListener
            }

            addNoteListener?.onNoteAdded(cycleId, noteText, imagePath)
            dismiss()
        }
    }

    fun setAddNoteListener(listener: AddNoteListener) {
        this.addNoteListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}