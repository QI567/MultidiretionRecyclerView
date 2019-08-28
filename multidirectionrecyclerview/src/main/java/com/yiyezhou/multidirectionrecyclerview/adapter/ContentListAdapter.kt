package com.yiyezhou.multidirectionrecyclerview.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.AbsListView
import com.yiyezhou.multidirectionrecyclerview.MultidirectionRecyclerView

class ContentListAdapter<ContentViewHolder : RecyclerView.ViewHolder>(
    private val listView: MultidirectionRecyclerView,
    private val multiDirectionAdapter: MultidirectionRecyclerView.MultidirectionAdapter<*, ContentViewHolder>
) : RecyclerView.Adapter<ContentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val viewHolder = multiDirectionAdapter.onCreateContentViewHolder(parent.context)
        viewHolder.itemView.layoutParams =
            AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, listView.cellHeight)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return multiDirectionAdapter.getItemCount()
    }

    override fun onBindViewHolder(viewHolder: ContentViewHolder, position: Int) {
        multiDirectionAdapter.onBindContentViewHolder(viewHolder, position)
    }

}
