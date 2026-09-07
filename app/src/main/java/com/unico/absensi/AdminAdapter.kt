package com.unico.absensi

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class AdminAdapter(
    private var items: List<AdminRow>,
    private val onEmployeeClick: (AdminEmployeeRow) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DEPT = 0
        const val TYPE_EMP = 1

        private val avatarColors = intArrayOf(
            R.color.avatar_1, R.color.avatar_2, R.color.avatar_3,
            R.color.avatar_4, R.color.avatar_5, R.color.avatar_6, R.color.avatar_7
        )

        fun getInitials(name: String): String {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first()}${parts[1].first()}".uppercase()
            }
        }
    }

    fun updateItems(newItems: List<AdminRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is AdminRow.Dept) TYPE_DEPT else TYPE_EMP

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DEPT) {
            DeptVH(inflater.inflate(R.layout.item_department_header, parent, false))
        } else {
            EmpVH(inflater.inflate(R.layout.item_admin_employee, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AdminRow.Dept -> (holder as DeptVH).bind(item)
            is AdminRow.Emp -> (holder as EmpVH).bind(item, onEmployeeClick)
        }
    }

    class DeptVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: AdminRow.Dept) {
            (itemView as TextView).text = "/// ${item.name.uppercase()} — ${item.count}"
        }
    }

    class EmpVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvIn: TextView = view.findViewById(R.id.tvIn)
        private val tvOut: TextView = view.findViewById(R.id.tvOut)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)

        fun bind(item: AdminRow.Emp, onClick: (AdminEmployeeRow) -> Unit) {
            val emp = item.employee
            tvName.text = emp.name
            tvIn.text = "IN ${emp.inTime}"
            tvOut.text = if (emp.outTime == "-") "OUT -" else "OUT ${emp.outTime}"

            tvAvatar.text = getInitials(emp.name)
            val colorRes = avatarColors[abs(emp.name.hashCode()) % avatarColors.size]
            val color = ContextCompat.getColor(itemView.context, colorRes)
            val border = ContextCompat.getColor(itemView.context, R.color.border_dark)
            val cornerPx = (8 * itemView.resources.displayMetrics.density).toInt()
            val strokePx = (1.5 * itemView.resources.displayMetrics.density).toInt()
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerPx.toFloat()
                setColor(color)
                setStroke(strokePx, border)
            }
            tvAvatar.background = drawable

            val (badgeText, bg, fg) = when (emp.status) {
                "hadir" -> Triple("HADIR", R.drawable.bg_badge_success, R.color.badge_success_text)
                "telat" -> Triple("TELAT", R.drawable.bg_badge, R.color.badge_danger_text)
                else -> Triple("BELUM", R.drawable.bg_badge, R.color.badge_danger_text)
            }
            tvBadge.text = badgeText
            tvBadge.background = ContextCompat.getDrawable(itemView.context, bg)
            tvBadge.setTextColor(ContextCompat.getColor(itemView.context, fg))

            itemView.setOnClickListener { onClick(emp) }
        }
    }
}
