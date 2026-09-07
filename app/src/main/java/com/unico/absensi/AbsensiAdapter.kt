package com.unico.absensi

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class AbsensiAdapter(private var items: List<ListRow>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_EMPLOYEE = 1

        private val avatarColors = intArrayOf(
            R.color.avatar_1,
            R.color.avatar_2,
            R.color.avatar_3,
            R.color.avatar_4,
            R.color.avatar_5,
            R.color.avatar_6,
            R.color.avatar_7
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

    fun updateItems(newItems: List<ListRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ListRow.DeptHeader -> TYPE_HEADER
            is ListRow.Employee -> TYPE_EMPLOYEE
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_department_header, parent, false))
        } else {
            EmployeeVH(inflater.inflate(R.layout.item_employee, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListRow.DeptHeader -> (holder as HeaderVH).bind(item)
            is ListRow.Employee -> (holder as EmployeeVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ListRow.DeptHeader) {
            val text = if (item.count > 0) "/// ${item.name.uppercase()}  —  ${item.count}" else "/// ${item.name.uppercase()}"
            (itemView as TextView).text = text
        }
    }

    class EmployeeVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvEmpName)
        private val tvDept: TextView = view.findViewById(R.id.tvEmpDept)

        fun bind(item: ListRow.Employee) {
            tvName.text = item.name
            tvDept.text = item.dept
            tvAvatar.text = getInitials(item.name)

            val colorRes = avatarColors[abs(item.name.hashCode()) % avatarColors.size]
            val color = ContextCompat.getColor(itemView.context, colorRes)
            val borderColor = ContextCompat.getColor(itemView.context, R.color.border_dark)

            val cornerPx = (8 * itemView.resources.displayMetrics.density).toInt()
            val strokePx = (1.5 * itemView.resources.displayMetrics.density).toInt()

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerPx.toFloat()
                setColor(color)
                setStroke(strokePx, borderColor)
            }
            tvAvatar.background = drawable
        }
    }
}
