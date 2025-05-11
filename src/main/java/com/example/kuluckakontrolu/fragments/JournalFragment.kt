package com.example.kuluckakontrolu.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.adapters.CycleAdapter
import com.example.kuluckakontrolu.adapters.NoteAdapter
import com.example.kuluckakontrolu.databinding.FragmentJournalBinding
import com.example.kuluckakontrolu.dialogs.AddNoteDialogFragment
import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.kuluckakontrolu.dialogs.FinishCycleDialogFragment
import android.content.ActivityNotFoundException
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JournalFragment : Fragment(), AddNoteDialogFragment.AddNoteListener {
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var cycleAdapter: CycleAdapter

    private var selectedCycleId = 0L
    private var photoFile: File? = null

    private val takePictureResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoFile?.let { file ->
                // Notu resimle eklemek için diyaloğu göster
                val dialog = AddNoteDialogFragment.newInstance(selectedCycleId, file.absolutePath)
                dialog.setAddNoteListener(this)
                dialog.show(parentFragmentManager, "add_note_dialog")
            }
        }
    }

    private val selectPictureResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // URI'dan dosyaya kopyala
                context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    // Notu resimle eklemek için diyaloğu göster
                    val dialog = AddNoteDialogFragment.newInstance(selectedCycleId, tempFile.absolutePath)
                    dialog.setAddNoteListener(this)
                    dialog.show(parentFragmentManager, "add_note_dialog")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(IncubatorViewModel::class.java)

        setupRecyclerViews()

        // Aktif döngüyü gözlemle
        lifecycleScope.launch {
            viewModel.activeCycle.collect { cycle ->
                updateActiveCycle(cycle)
            }
        }

        // Tüm döngüleri gözlemle
        lifecycleScope.launch {
            viewModel.allCycles.collect { cycles ->
                cycleAdapter.submitList(cycles)
                binding.textNoCycles.visibility = if (cycles.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Notları gözlemle
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                noteAdapter.submitList(notes)
                binding.textNoNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.buttonAddNote.setOnClickListener {
            val dialog = AddNoteDialogFragment.newInstance(selectedCycleId)
            dialog.setAddNoteListener(this)
            dialog.show(parentFragmentManager, "add_note_dialog")
        }

        binding.buttonTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.buttonSelectPhoto.setOnClickListener {
            selectPhotoFromGallery()
        }

        binding.buttonFinishCycle.setOnClickListener {
            // Kuluçka sonlandırma diyaloğunu göster
            FinishCycleDialogFragment().show(parentFragmentManager, "finish_cycle_dialog")
        }

        binding.buttonExportReport.setOnClickListener {
            exportReport()
        }
    }

    private fun setupRecyclerViews() {
        // Notlar için RecyclerView
        noteAdapter = NoteAdapter(requireContext())
        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }

        // Döngüler için RecyclerView
        cycleAdapter = CycleAdapter { cycle ->
            // Döngü seçildiğinde
            selectedCycleId = cycle.id
            updateSelectedCycle(cycle)
        }

        binding.recyclerViewCycles.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = cycleAdapter
        }
    }

    private fun updateActiveCycle(cycle: IncubationCycle?) {
        if (cycle != null) {
            binding.layoutActiveCycle.visibility = View.VISIBLE

            // Aktif döngü bilgilerini göster
            binding.textActiveCycleName.text = cycle.name
            binding.textActiveAnimalType.text = cycle.animalType

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.textActiveStartDate.text = dateFormat.format(Date(cycle.startDate))

            binding.textActiveTotalEggs.text = cycle.totalEggs.toString()

            // Bu aktif döngü, seçili değilse seçili yap
            if (selectedCycleId != cycle.id) {
                selectedCycleId = cycle.id
                // loadNotes yerine viewModel'deki ilgili fonksiyonu çağırın
                viewModel.loadNotes(cycle.id)
            }

            // Döngü aktif olduğu için sonlandırma butonunu göster
            binding.buttonFinishCycle.visibility = View.VISIBLE
        } else {
            binding.layoutActiveCycle.visibility = View.GONE
            binding.buttonFinishCycle.visibility = View.GONE
        }
    }

    private fun updateSelectedCycle(cycle: IncubationCycle) {
        // Seçili döngü bilgilerini göster
        binding.textSelectedCycleName.text = cycle.name

        // Döngüye ait notları yükle
        lifecycleScope.launch {
            viewModel.loadNotes(cycle.id)
        }

// Not ekleme butonlarını göster
        binding.buttonAddNote.visibility = View.VISIBLE
        binding.buttonTakePhoto.visibility = View.VISIBLE
        binding.buttonSelectPhoto.visibility = View.VISIBLE

        // Eğer döngü aktif değilse rapor düğmesini göster
        binding.buttonExportReport.visibility = if (cycle.isActive) View.GONE else View.VISIBLE
    }

    private fun takePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val photoFile = File(requireContext().getExternalFilesDir(null), "JPEG_${timeStamp}_.jpg")
                this.photoFile = photoFile

                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureResult.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun selectPhotoFromGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            selectPictureResult.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("JournalFragment", "Galeri uygulaması bulunamadı: ${e.message}")
            Toast.makeText(
                context,
                "Galeri uygulaması bulunamadı. Lütfen cihazınızda bir galeri uygulaması olduğundan emin olun.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("JournalFragment", "Fotoğraf seçme hatası: ${e.message}")
            Toast.makeText(context, "Fotoğraf seçme işlemi başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Dosya kontrol işlemlerini arka planda yap
                val result = withContext(Dispatchers.IO) {
                    val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

                    // Dosya boyutunu kontrol et
                    val fileSize = requireContext().contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
                    if (fileSize > 10 * 1024 * 1024) { // 10MB'dan büyük dosyaları reddet
                        return@withContext "size_error"
                    }

                    // Dosya türünü kontrol et
                    val mimeType = requireContext().contentResolver.getType(uri)
                    if (mimeType?.startsWith("image/") != true) {
                        return@withContext "type_error"
                    }

                    // Dosyayı kopyala
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@withContext "read_error"

                    // Görüntüyü yeniden boyutlandır (isteğe bağlı)
                    // compressAndResizeImage(tempFile)

                    return@withContext tempFile.absolutePath
                }

                // Sonuçları ana thread'de işle
                when(result) {
                    "size_error" -> Toast.makeText(context, "Dosya boyutu çok büyük (max: 10MB)", Toast.LENGTH_SHORT).show()
                    "type_error" -> Toast.makeText(context, "Desteklenmeyen dosya türü", Toast.LENGTH_SHORT).show()
                    "read_error" -> Toast.makeText(context, "Dosya okunamadı", Toast.LENGTH_SHORT).show()
                    else -> {
                        // Resim başarıyla işlendi, not eklemek için diyaloğu göster
                        val dialog = AddNoteDialogFragment.newInstance(selectedCycleId, result)
                        dialog.setAddNoteListener(this@JournalFragment)
                        dialog.show(parentFragmentManager, "add_note_dialog")
                    }
                }
            } catch (e: Exception) {
                Log.e("JournalFragment", "Resim işlenirken hata: ${e.message}")
                Toast.makeText(context, "Resim işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportReport() {
        if (selectedCycleId <= 0) return

        binding.progressExport.visibility = View.VISIBLE
        binding.buttonExportReport.isEnabled = false

        viewModel.generateReport(selectedCycleId).observe(viewLifecycleOwner) { file ->
            binding.progressExport.visibility = View.GONE
            binding.buttonExportReport.isEnabled = true

            if (file != null) {
                // PDF dosyasını paylaş - dosya sağlama kontrolleri ile
                if (file.exists() && file.length() > 0) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "application/pdf"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Raporu Paylaş"))
                } else {
                    Toast.makeText(context, "Rapor oluşturulamadı", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Rapor oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNoteAdded(cycleId: Long, text: String, imagePath: String?) {
        if (imagePath != null) {
            viewModel.addNoteWithImage(cycleId, text, File(imagePath))
        } else {
            viewModel.addNote(cycleId, text)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}