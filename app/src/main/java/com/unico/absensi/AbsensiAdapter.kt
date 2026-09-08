package com.unico.absensi

import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class AbsensiAdapter(
    private var items: List<ListRow>,
    private val onVoteClick: ((ListRow.Employee) -> Unit)? = null,
    private val onAvatarClick: ((ListRow.Employee, Bitmap?) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    private val photoCache = java.util.Collections.synchronizedMap(HashMap<String, Bitmap>())

    fun updateItems(newItems: List<ListRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun addPhoto(empCode: String, bitmap: Bitmap) {
        photoCache[empCode] = bitmap
    }

    fun updateVoteState(empCode: String, loveCount: Int, hasLoved: Boolean) {
        val index = items.indexOfFirst { it is ListRow.Employee && it.empCode == empCode }
        if (index != -1) {
            val emp = items[index] as ListRow.Employee
            val updated = emp.copy(loveCount = loveCount, hasLoved = hasLoved)
            val mutableList = items.toMutableList()
            mutableList[index] = updated
            items = mutableList
            notifyItemChanged(index, "VOTE_UPDATE")
        }
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

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains("VOTE_UPDATE") && holder is EmployeeVH) {
            val item = items[position] as? ListRow.Employee ?: return
            holder.bindVoteStateOnly(item)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = items.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ListRow.DeptHeader) {
            val text = if (item.count > 0) "/// ${item.name.uppercase()}  —  ${item.count}" else "/// ${item.name.uppercase()}"
            (itemView as TextView).text = text
        }
    }

    inner class EmployeeVH(view: View) : RecyclerView.ViewHolder(view) {
        private val flAvatar: View = view.findViewById(R.id.flAvatar)
        private val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        private val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvEmpName)
        private val tvDept: TextView = view.findViewById(R.id.tvEmpDept)
        private val btnVote: View = view.findViewById(R.id.btnVote)
        private val tvVoteIcon: TextView = view.findViewById(R.id.tvVoteIcon)
        private val tvVoteCount: TextView = view.findViewById(R.id.tvVoteCount)
        private var currentItem: ListRow.Employee? = null

        fun bindVoteStateOnly(item: ListRow.Employee) {
            currentItem = item
            tvVoteCount.text = item.loveCount.toString()
            if (item.hasLoved) {
                tvVoteIcon.text = "❤️"
                tvVoteIcon.alpha = 1.0f
                tvVoteCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_danger_text))
            } else {
                tvVoteIcon.text = "🤍"
                tvVoteIcon.alpha = 0.65f
                tvVoteCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            }
        }

        fun bind(item: ListRow.Employee) {
            currentItem = item
            tvName.text = item.name
            tvDept.text = item.dept
            tvAvatar.text = getInitials(item.name)

            bindVoteStateOnly(item)

            btnVote.setOnClickListener {
                val latest = currentItem ?: return@setOnClickListener

                tvVoteIcon.animate().cancel()
                tvVoteIcon.scaleX = 1.0f
                tvVoteIcon.scaleY = 1.0f

                tvVoteIcon.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .setDuration(120)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.5f))
                    .withEndAction {
                        tvVoteIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                onVoteClick?.invoke(latest)
            }

            val colorRes = avatarColors[abs(item.name.hashCode()) % avatarColors.size]
            val color = ContextCompat.getColor(itemView.context, colorRes)
            val borderColor = ContextCompat.getColor(itemView.context, R.color.border_dark)

            val strokePx = (1.5 * itemView.resources.displayMetrics.density).toInt()

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(strokePx, borderColor)
            }
            tvAvatar.background = drawable

            val handleAvatarClick = View.OnClickListener {
                val latest = currentItem ?: return@OnClickListener
                val bmp = photoCache[latest.empCode]
                onAvatarClick?.invoke(latest, bmp)
            }
            flAvatar.setOnClickListener(handleAvatarClick)
            ivAvatar.setOnClickListener(handleAvatarClick)
            tvAvatar.setOnClickListener(handleAvatarClick)

            val cached = photoCache[item.empCode]
            if (cached != null) {
                ivAvatar.setImageDrawable(cached.toCircularDrawable(ivAvatar.resources))
                ivAvatar.visibility = View.VISIBLE
                tvAvatar.visibility = View.GONE
            } else {
                ivAvatar.visibility = View.GONE
                tvAvatar.visibility = View.VISIBLE
                loadPhoto(item)
            }
        }

        private fun loadPhoto(item: ListRow.Employee) {
            Thread {
                val bmp = try {
                    AbsensiApi.getPublicPhotoByEmp(item.empCode)
                } catch (e: Exception) {
                    null
                }
                if (bmp != null) {
                    photoCache[item.empCode] = bmp
                    itemView.post {
                        val pos = bindingAdapterPosition
                        val cur = if (pos != RecyclerView.NO_POSITION) items.getOrNull(pos) else null
                        val same = cur is ListRow.Employee && cur.empCode == item.empCode
                        if (same) {
                            notifyItemChanged(pos)
                        }
                    }
                }
            }.start()
        }
    }
}