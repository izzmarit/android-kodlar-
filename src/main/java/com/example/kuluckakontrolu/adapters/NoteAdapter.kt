package com.example.kuluckakontrolu.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.model.IncubationNote
import com.squareup.picasso.Picasso
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val context: Context)
    : ListAdapter<IncubationNote, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDate: TextView = view.findViewById(R.id.textNoteDate)
        val textContent: TextView = view.findViewById(R.id.textNoteContent)
        val imageNote: ImageView = view.findViewById(R.id.imageNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)

        // Tarih formatla
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.textDate.text = dateFormat.format(Date(note.timestamp))

        // Not içeriği
        holder.textContent.text = note.text

        // Resim varsa göster
        if (note.hasImage && note.imagePath.isNotEmpty()) {
            holder.imageNote.visibility = View.VISIBLE
            Picasso.get().load(File(note.imagePath)).into(holder.imageNote)

            // Resme tıklandığında tam ekran görüntüleme
            holder.imageNote.setOnClickListener {
                // ImageViewerActivity.start(context, note.imagePath)
                // Not: Tam ekran görüntüleyici sınıfı eklenecek
            }
        } else {
            holder.imageNote.visibility = View.GONE
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<IncubationNote>() {
        override fun areItemsTheSame(oldItem: IncubationNote, newItem: IncubationNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncubationNote, newItem: IncubationNote): Boolean {
            return oldItem == newItem
        }
    }
}