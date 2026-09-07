package com.unico.absensi

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var etSearch: EditText
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: AdminAdapter

    private val tvStat by lazy { mapOf(
        "total" to findViewById<android.view.View>(R.id.statTotal).findViewById<TextView>(R.id.tvStatValue),
        "hadir" to findViewById<android.view.View>(R.id.statHadir).findViewById<TextView>(R.id.tvStatValue),
        "telat" to findViewById<android.view.View>(R.id.statTelat).findViewById<TextView>(R.id.tvStatValue),
        "belum" to findViewById<android.view.View>(R.id.statBelum).findViewById<TextView>(R.id.tvStatValue)
    ) }

    private var fullRows = listOf<AdminRow>()
    private var currentFilter = ""
    private var rotateAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        tvDate = findViewById(R.id.tvDate)
        etSearch = findViewById(R.id.etSearch)
        fabRefresh = findViewById(R.id.fabRefresh)
        llEmptyState = findViewById(R.id.llEmptyState)

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.accent)
        )

        adapter = AdminAdapter(emptyList()) { emp ->
            startActivity(Intent(this, HistoryActivity::class.java)
                .putExtra("emp_code", emp.code)
                .putExtra("emp_name", emp.name))
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupFabRotation()

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            Thread { AbsensiApi.logout() }.start()
            finish()
        }

        findViewById<TextView>(R.id.filterAll).setOnClickListener { setFilter("") }
        findViewById<TextView>(R.id.filterHadir).setOnClickListener { setFilter("hadir") }
        findViewById<TextView>(R.id.filterTelat).setOnClickListener { setFilter("telat") }
        findViewById<TextView>(R.id.filterBelum).setOnClickListener { setFilter("belum") }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fabRefresh.setOnClickListener { fetchData() }
        swipeRefresh.setOnRefreshListener { fetchData() }
        fetchData()
    }

    private fun setupFabRotation() {
        rotateAnimator = ObjectAnimator.ofFloat(fabRefresh, "rotation", 0f, 360f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun fetchData() {
        swipeRefresh.isRefreshing = true
        if (rotateAnimator?.isStarted != true) rotateAnimator?.start()

        Thread {
            val dash = try {
                AbsensiApi.adminDashboard()
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                swipeRefresh.isRefreshing = false
                if (rotateAnimator?.isStarted == true) {
                    rotateAnimator?.cancel()
                    fabRefresh.rotation = 0f
                }
                if (dash == null) {
                    tvDate.text = "Gagal konek ke server"
                    return@runOnUiThread
                }

                tvDate.text = "MONITOR ${dash.date}"

                tvStat["total"]?.text = dash.stat.total.toString()
                tvStat["hadir"]?.text = dash.stat.hadir.toString()
                tvStat["telat"]?.text = dash.stat.telat.toString()
                tvStat["belum"]?.text = dash.stat.belum.toString()

                val rows = mutableListOf<AdminRow>()
                for (dept in dash.departments) {
                    rows.add(AdminRow.Dept(dept.department, dept.employees.size))
                    for (emp in dept.employees) {
                        rows.add(AdminRow.Emp(emp))
                    }
                }
                fullRows = rows
                applyFilterAndSearch()
            }
        }.start()
    }

    private fun setFilter(f: String) {
        currentFilter = f
        val all = findViewById<TextView>(R.id.filterAll)
        val hadir = findViewById<TextView>(R.id.filterHadir)
        val telat = findViewById<TextView>(R.id.filterTelat)
        val belum = findViewById<TextView>(R.id.filterBelum)
        listOf(all, hadir, telat, belum).forEach {
            it.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            it.background = ContextCompat.getDrawable(this, R.drawable.bg_tag_pill)
        }
        val active = when (f) {
            "hadir" -> hadir
            "telat" -> telat
            "belum" -> belum
            else -> all
        }
        active.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val q = etSearch.text.toString().trim()
        val rows = mutableListOf<AdminRow>()
        var currentDept: AdminRow.Dept? = null
        var empList = mutableListOf<AdminEmployeeRow>()

        for (row in fullRows) {
            when (row) {
                is AdminRow.Dept -> {
                    if (currentDept != null && empList.isNotEmpty()) {
                        rows.add(AdminRow.Dept(currentDept.name, empList.size))
                        rows.addAll(empList.map { AdminRow.Emp(it) })
                    }
                    currentDept = row
                    empList = mutableListOf()
                }
                is AdminRow.Emp -> {
                    val emp = row.employee
                    val filterOk = currentFilter.isEmpty() || emp.status == currentFilter
                    val searchOk = q.isEmpty() || emp.name.contains(q, ignoreCase = true) || emp.code.contains(q, ignoreCase = true)
                    if (filterOk && searchOk) empList.add(emp)
                }
            }
        }
        if (currentDept != null && empList.isNotEmpty()) {
            rows.add(AdminRow.Dept(currentDept.name, empList.size))
            rows.addAll(empList.map { AdminRow.Emp(it) })
        }

        adapter.updateItems(rows)
        llEmptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }
}
