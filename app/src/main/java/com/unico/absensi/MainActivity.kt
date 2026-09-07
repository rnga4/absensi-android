package com.unico.absensi

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private enum class RefreshState { IDLE, SYNCING, DONE }

    private val urls = listOf(
        "http://192.168.1.37:9790/api_public.php",
        "http://100.102.13.11:9790/api_public.php"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var etSearch: EditText
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var llEmptyState: android.widget.LinearLayout
    private lateinit var adapter: AbsensiAdapter
    private lateinit var refreshIndicator: LinearLayout
    private lateinit var tvRefreshStatus: TextView
    private lateinit var refreshStrip: View

    private var allRows = listOf<ListRow>()
    private var rotateAnimator: ObjectAnimator? = null
    private var stripAnimator: ValueAnimator? = null
    private var stripState = RefreshState.IDLE

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        tvDate = findViewById(R.id.tvDate)
        etSearch = findViewById(R.id.etSearch)
        fabRefresh = findViewById(R.id.fabRefresh)
        llEmptyState = findViewById(R.id.llEmptyState)
        refreshIndicator = findViewById(R.id.refreshIndicator)
        tvRefreshStatus = findViewById(R.id.tvRefreshStatus)
        refreshStrip = findViewById(R.id.refreshStrip)

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.bg_primary)
        )
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(this, R.color.bg_primary)
        )

        adapter = AbsensiAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupFabAnimation()
        NotificationHelper.createChannel(this)
        requestNotifPermission()
        scheduleWorker()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filterList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fabRefresh.setOnClickListener { fetchData(0) }
        swipeRefresh.setOnRefreshListener { fetchData(0) }
        fetchData(0)
    }

    private fun setupFabAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(fabRefresh, "rotation", 0f, 360f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun startRefreshAnim() {
        swipeRefresh.isRefreshing = true
        if (rotateAnimator?.isStarted != true) {
            rotateAnimator?.start()
        }
        showRefreshState(RefreshState.SYNCING)
    }

    private fun stopRefreshAnim() {
        swipeRefresh.isRefreshing = false
        if (rotateAnimator?.isStarted == true) {
            rotateAnimator?.cancel()
            fabRefresh.rotation = 0f
        }
        showRefreshState(RefreshState.DONE)
        refreshIndicator.postDelayed({
            if (stripState == RefreshState.DONE) {
                hideRefreshIndicator()
            }
        }, 900)
    }

    private fun showRefreshState(state: RefreshState) {
        stripState = state
        tvRefreshStatus.text = when (state) {
            RefreshState.SYNCING -> "[ SYNCING... ]"
            RefreshState.DONE -> "[ DONE ]"
            RefreshState.IDLE -> "[ READY ]"
        }

        refreshIndicator.visibility = View.VISIBLE
        refreshIndicator.animate().alpha(1f).setDuration(200).start()

        stripAnimator?.cancel()
        val lp = refreshStrip.layoutParams

        stripAnimator = when (state) {
            RefreshState.SYNCING -> ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1400
                repeatCount = ValueAnimator.INFINITE
                interpolator = null
                addUpdateListener {
                    lp.width =
                        (it.animatedValue as Float * parentWidthPx).toInt()
                    refreshStrip.layoutParams = lp
                }
            }
            RefreshState.DONE -> ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                addUpdateListener {
                    lp.width =
                        (it.animatedValue as Float * parentWidthPx).toInt()
                    refreshStrip.layoutParams = lp
                }
            }
            RefreshState.IDLE -> null
        }

        if (stripAnimator != null && state != RefreshState.IDLE) {
            if (state == RefreshState.DONE) {
                lp.width = 0
                refreshStrip.layoutParams = lp
            }
            stripAnimator?.start()
        }
    }

    private fun hideRefreshIndicator() {
        stripState = RefreshState.IDLE
        refreshIndicator.animate().alpha(0f).setDuration(200).withEndAction {
            refreshIndicator.visibility = View.GONE
            val lp = refreshStrip.layoutParams
            lp.width = 0
            refreshStrip.layoutParams = lp
        }.start()
    }

    private val parentWidthPx: Int
        get() = refreshIndicator.width.coerceAtLeast(1)

    private fun filterList(query: String) {
        if (query.isBlank()) {
            adapter.updateItems(allRows)
            llEmptyState.visibility = if (allRows.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        val filtered = mutableListOf<ListRow>()
        var currentHeader: ListRow.DeptHeader? = null
        var deptEmpList = mutableListOf<ListRow.Employee>()

        for (row in allRows) {
            if (row is ListRow.DeptHeader) {
                if (currentHeader != null && deptEmpList.isNotEmpty()) {
                    filtered.add(ListRow.DeptHeader(currentHeader.name, deptEmpList.size))
                    filtered.addAll(deptEmpList)
                }
                currentHeader = row
                deptEmpList = mutableListOf()
            } else if (row is ListRow.Employee && row.name.contains(query, ignoreCase = true)) {
                deptEmpList.add(row)
            }
        }
        if (currentHeader != null && deptEmpList.isNotEmpty()) {
            filtered.add(ListRow.DeptHeader(currentHeader.name, deptEmpList.size))
            filtered.addAll(deptEmpList)
        }

        adapter.updateItems(filtered)
        llEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleWorker() {
        val request = PeriodicWorkRequestBuilder<AbsensiWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "absensi_check", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun fetchData(urlIndex: Int) {
        if (urlIndex >= urls.size) {
            runOnUiThread {
                stopRefreshAnim()
                tvDate.text = "Gagal konek ke server"
                llEmptyState.visibility = if (allRows.isEmpty()) View.VISIBLE else View.GONE
            }
            return
        }

        startRefreshAnim()
        val request = Request.Builder().url(urls[urlIndex]).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                fetchData(urlIndex + 1)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    runOnUiThread { bindData(json) }
                } catch (e: Exception) {
                    fetchData(urlIndex + 1)
                }
            }
        })
    }

    private fun bindData(json: JSONObject) {
        stopRefreshAnim()

        val date = json.optString("date", "-")
        val time = json.optString("time", "-")
        val total = json.optInt("total_not_absen", 0)
        val allPresent = json.optBoolean("all_present", false)

        tvDate.text = if (allPresent) "Semua sudah absen · $date $time"
                       else "$total karyawan belum absen · $date $time"

        val rows = mutableListOf<ListRow>()
        val departments = json.optJSONArray("departments")
        if (departments != null) {
            for (i in 0 until departments.length()) {
                val dept = departments.getJSONObject(i)
                val deptName = dept.optString("department", "-")
                val employees = dept.optJSONArray("employees")
                val empCount = employees?.length() ?: 0

                rows.add(ListRow.DeptHeader(deptName, empCount))
                if (employees != null) {
                    for (j in 0 until employees.length()) {
                        val emp = employees.getJSONObject(j)
                        rows.add(
                            ListRow.Employee(
                                empCode = emp.optString("emp_code", "-"),
                                name = emp.optString("name", "-"),
                                dept = deptName
                            )
                        )
                    }
                }
            }
        }
        allRows = rows
        filterList(etSearch.text.toString())
    }
}
