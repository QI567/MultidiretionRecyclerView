package com.example.excellayout

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.yiyezhou.multidirectionrecyclerview.EzrHorizontalScrollView
import com.yiyezhou.multidirectionrecyclerview.MultidirectionRecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val headerTitle = findViewById<EzrHorizontalScrollView>(R.id.headerTitle)
        val listView = findViewById<MultidirectionRecyclerView>(R.id.multiDirectionList)
        val headerView = TextView(this)
        val footerView = TextView(this)
        val adapter = MyAdapter()
        listView.setAdapter(adapter)
        listView.setTitle(headerTitle)
        listView.setFooter(footerView)
        listView.setHeader(headerView)
        adapter.size = 4
        adapter.notifyDataSetChanged()
        headerView.text = "刷新中..."
        footerView.text = "加载中..."
        listView.onRefresh = {
            Thread {
                Thread.sleep(1000)
                runOnUiThread {
                    headerView.text = "刷新中..."
                    adapter.size = 20
                    listView.refresh(false)
                    adapter.notifyDataSetChanged()
                }
            }.start()
        }
        listView.onLoadMore = {
            Thread {
                Thread.sleep(1000)
                runOnUiThread {
                    adapter.size += 10
                    listView.refresh(false)
                    adapter.notifyDataSetChanged()

                }
            }.start()
        }
    }

    class MyAdapter :
        MultidirectionRecyclerView.MultidirectionAdapter<RecyclerView.ViewHolder, RecyclerView.ViewHolder>() {

        var size = 0

        override fun onCreateColHeaderViewHolder(context: Context): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(View.inflate(context, R.layout.header_col, null)) {}
        }

        override fun onCreateContentViewHolder(context: Context?): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(View.inflate(context, R.layout.header_row, null)) {}
        }

        override fun onBindColHeaderViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            val textView: TextView = viewHolder.itemView.findViewById(R.id.textView)
            textView.text = "第${position}行"
        }

        override fun onBindContentViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        }

        override fun getItemCount(): Int {
            return size
        }
    }
}
