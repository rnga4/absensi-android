package com.unico.absensi

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.concurrent.CopyOnWriteArrayList

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: HistoryAdapter

    private var empCode = ""
    private var empName = ""
    private val historyItems = CopyOnWriteArrayList<HistoryDay>()
    private var isLoading = false
    private var hasMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        empCode = intent.getStringExtra("emp_code") ?: ""
        empName = intent.getStringExtra("emp_name") ?: empCode

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        findViewById<TextView>(R.id.tvEmpHeader).text =
            "${empName.ifEmpty { "Karyawan" }}  ·  EMP CODE $empCode"

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            Haptics.click(this)
            finish()
        }

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))

        adapter = HistoryAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= total - 3) loadMore()
            }
        })

        swipeRefresh.setOnRefreshListener {
            resetAndLoad()
        }
        loadMore()
    }

    private fun resetAndLoad() {
        historyItems.clear()
        adapter.updateItems(emptyList())
        hasMore = true
        swipeRefresh.isRefreshing = false
        loadMore()
    }

    private fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true
        Thread {
            val list = try {
                AbsensiApi.history(empCode, historyItems.size)
            } catch (e: Exception) {
                null
            }
            runOnUiThreadSafe {
                if (list != null) {
                    historyItems.addAll(list)
                    adapter.updateItems(historyItems.toList())
                    if (list.size < 30) hasMore = false
                }
                isLoading = false
            }
        }.start()
    }
}
