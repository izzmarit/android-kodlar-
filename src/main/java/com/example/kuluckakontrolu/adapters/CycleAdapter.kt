package com.example.kuluckakontrolu.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.model.IncubationCycle
import java.text.SimpleDateFormat
import java.util.*

class CycleAdapter(private val onCycleSelected: (IncubationCycle) -> Unit)
    : ListAdapter<IncubationCycle, CycleAdapter.CycleViewHolder>(CycleDiffCallback()) {

    private var selectedPosition = -1

    class CycleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCycle: CardView = view.findViewById(R.id.cardCycle)
        val textName: TextView = view.findViewById(R.id.textCycleName)
        val textType: TextView = view.findViewById(R.id.textCycleType)
        val textDate: TextView = view.findViewById(R.id.textCycleDate)
        val textStatus: TextView = view.findViewById(R.id.textCycleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cycle, parent, false)
        return CycleViewHolder(view)
    }

    override fun onBindViewHolder(holder: CycleViewHolder, position: Int) {
        val cycle = getItem(position)

        holder.textName.text = cycle.name
        holder.textType.text = cycle.animalType

        // Tarih formatla
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.textDate.text = dateFormat.format(Date(cycle.startDate))

        // Durum
        if (cycle.isActive) {
            holder.textStatus.text = holder.itemView.context.getString(R.string.active)
            holder.textStatus.setTextColor(holder.itemView.context.getColor(R.color.green))
        } else {
            val successRateText = holder.itemView.context.getString(R.string.success_rate_short, cycle.successRate)
            holder.textStatus.text = successRateText
            holder.textStatus.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
        }

        // Seçili öğe varsa vurgula
        if (selectedPosition == position) {
            holder.cardCycle.setCardBackgroundColor(holder.itemView.context.getColor(R.color.card_selected))
        } else {
            holder.cardCycle.setCardBackgroundColor(holder.itemView.context.getColor(R.color.card_background))
        }

        // Tıklama olayı
        holder.itemView.setOnClickListener {
            // Önceki seçiliyi sıfırla
            val oldSelectedPosition = selectedPosition
            selectedPosition = position

            // Görünümleri güncelle
            if (oldSelectedPosition != -1) {
                notifyItemChanged(oldSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            // Seçilen döngüyü bildir
            onCycleSelected(cycle)
        }
    }

    class CycleDiffCallback : DiffUtil.ItemCallback<IncubationCycle>() {
        override fun areItemsTheSame(oldItem: IncubationCycle, newItem: IncubationCycle): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncubationCycle, newItem: IncubationCycle): Boolean {
            return oldItem == newItem
        }
    }
}