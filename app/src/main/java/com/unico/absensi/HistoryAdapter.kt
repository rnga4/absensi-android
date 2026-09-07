package com.unico.absensi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var items: List<HistoryDay>) :
    RecyclerView.Adapter<HistoryAdapter.Holder>() {

    fun updateItems(newItems: List<HistoryDay>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val tvDay: TextView = view.findViewById(R.id.tvDay)
        private val tvIn: TextView = view.findViewById(R.id.tvIn)
        private val tvOut: TextView = view.findViewById(R.id.tvOut)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)

        fun bind(item: HistoryDay) {
            tvDate.text = item.dateFormatted
            tvDay.text = item.dayName
            tvIn.text = "IN ${item.inTime}"
            tvOut.text = "OUT ${item.outTime}"

            val (text, bg, fg) = when (item.status) {
                "hadir" -> Triple("HADIR", R.drawable.bg_badge_success, R.color.badge_success_text)
                "telat" -> Triple("TELAT", R.drawable.bg_badge, R.color.badge_danger_text)
                else -> Triple("BELUM", R.drawable.bg_badge, R.color.badge_danger_text)
            }
            tvBadge.text = text
            tvBadge.background = ContextCompat.getDrawable(itemView.context, bg)
            tvBadge.setTextColor(ContextCompat.getColor(itemView.context, fg))
        }
    }
}
