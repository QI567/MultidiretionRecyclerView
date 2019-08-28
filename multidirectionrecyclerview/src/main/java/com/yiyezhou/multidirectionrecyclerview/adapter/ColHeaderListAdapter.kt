package com.yiyezhou.multidirectionrecyclerview.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.AbsListView
import com.yiyezhou.multidirectionrecyclerview.MultidirectionRecyclerView

class ColHeaderListAdapter<ColHeaderViewHolder : RecyclerView.ViewHolder>(
    private val listView: MultidirectionRecyclerView,
    private val multiDirectionAdapter: MultidirectionRecyclerView.MultidirectionAdapter<ColHeaderViewHolder, *>
) : RecyclerView.Adapter<ColHeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColHeaderViewHolder {
        val viewHolder = multiDirectionAdapter.onCreateColHeaderViewHolder(parent.context)
        viewHolder.itemView.layoutParams =
                AbsListView.LayoutParams(listView.colHeaderWidth, listView.cellHeight)
        return viewHolder
    }


    override fun getItemCount(): Int {
        return multiDirectionAdapter.getItemCount()
    }

    override fun onBindViewHolder(viewHolder: ColHeaderViewHolder, position: Int) {
        multiDirectionAdapter.onBindColHeaderViewHolder(viewHolder, position)
    }

}