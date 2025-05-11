package com.example.kuluckakontrolu.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.DialogNewCycleBinding
import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class NewCycleDialogFragment : DialogFragment() {
    private var _binding: DialogNewCycleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogNewCycleBinding.inflate(inflater, container, false)

        // Dialog genişliğini ekran genişliğinin %60'ı olarak ayarla
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.6).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(IncubatorViewModel::class.java)

        // Dialog başlığı ayarla
        dialog?.setTitle(R.string.new_incubation_cycle)

        // Hayvan türleri listesini ayarla
        val animalTypes = resources.getStringArray(R.array.animal_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, animalTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimalType.adapter = adapter

        // İptal butonu
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Başlat butonu
        binding.buttonStart.setOnClickListener {
            startNewCycle()
        }
    }

    override fun onStart() {
        super.onStart()

        // Dialog genişliğini ekranın %90'ı olarak ayarla
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun startNewCycle() {
        val name = binding.editTextCycleName.text.toString().trim()
        if (name.isEmpty()) {
            binding.editTextCycleName.error = getString(R.string.required_field)
            return
        }

        val animalType = binding.spinnerAnimalType.selectedItem.toString()

        val totalEggsStr = binding.editTextTotalEggs.text.toString().trim()
        val totalEggs = if (totalEggsStr.isNotEmpty()) totalEggsStr.toInt() else 0

        val notes = binding.editTextNotes.text.toString().trim()

        val cycle = IncubationCycle(
            name = name,
            animalType = animalType,
            startDate = System.currentTimeMillis(),
            notes = notes,
            totalEggs = totalEggs,
            isActive = true
        )

        viewModel.startNewCycle(cycle)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}