package com.example.watertracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterAdapter : ListAdapter<WaterEntry, WaterAdapter.WaterViewHolder>(WaterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_water_entry, parent, false)
        return WaterViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaterViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class WaterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
        private val textViewDrinkType: TextView = itemView.findViewById(R.id.textViewDrinkType)
        private val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(entry: WaterEntry) {
            textViewAmount.text = "${entry.amount} ml"
            textViewDrinkType.text = entry.drinkType
            textViewTimestamp.text = dateFormat.format(Date(entry.timestamp))
        }
    }

    class WaterDiffCallback : DiffUtil.ItemCallback<WaterEntry>() {
        override fun areItemsTheSame(oldItem: WaterEntry, newItem: WaterEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WaterEntry, newItem: WaterEntry): Boolean {
            return oldItem == newItem
        }
    }
}
