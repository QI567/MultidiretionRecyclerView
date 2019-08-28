package com.yiyezhou.multidirectionrecyclerview

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.yiyezhou.multidirectionrecyclerview.adapter.ColHeaderListAdapter
import com.yiyezhou.multidirectionrecyclerview.adapter.ContentListAdapter
import kotlin.math.abs


class MultidirectionRecyclerView : LinearLayout {

    private var adapter: MultidirectionAdapter<*, *>? = null
    /** 列头布局 */
    private lateinit var mLeftListView: RecyclerView
    /** 内容列 */
    private lateinit var mRightListView: RecyclerView
    /** 内容列横线扩展 */
    private var mRightScrollView: EzrHorizontalScrollView
    /** 列头宽度 */
    var colHeaderWidth = -2
    /** 普通行高 */
    var cellHeight = -2
    /** 上拉加载监听 */
    var onLoadMore: (() -> Unit)? = null
    /** 下拉刷新监听 */
    var onRefresh: (() -> Unit)? = null
    private var mStartY = 0f
    private var mContentLayoutManager: LinearLayoutManager? = null
    private var mColHeaderLayoutManager: LinearLayoutManager? = null
    private var scaledTouchSlop = 0
    /** FooterView的最大高度 */
    var mMaxFooterHeight = 0
    var mMaxHeaderHeight = 0
    private var mLastMoveY = 0f
    /** 滑动偏移量 */
    private var mMoveOff = 0f
    private var mHeaderView: FrameLayout
    private var mFooterView: FrameLayout
    private var mMainLayout: View
    private var isLoading: Boolean = false
    private var isRefreshing = false
    /** 是否可以刷新 */
    var refreshable = true
    /** 是否可以加载更多 */
    var loadable = true
    private var mShowFooter = false
    /** MainLayout原始高度 */
    private var mLastMainHeight = 0
    private val colHeaderScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE)
                mRightListView.scrollBy(0, dy)
        }
    }

    private val contentListScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE)
                mLeftListView.scrollBy(0, dy)
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(context, attributeSet, defStyle) {
        val density = context.resources.displayMetrics.density
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MultidirectionRecyclerView)
        colHeaderWidth =
            typedArray.getDimension(R.styleable.MultidirectionRecyclerView_mdr_col_header_width, 150 * density).toInt()
        cellHeight = typedArray.getDimensionPixelOffset(
            R.styleable.MultidirectionRecyclerView_mdr_cell_height,
            (50 * density).toInt()
        )
        mMaxFooterHeight =
            typedArray.getDimensionPixelOffset(R.styleable.MultidirectionRecyclerView_mdr_max_footer_height, cellHeight)
        mMaxHeaderHeight =
            typedArray.getDimensionPixelOffset(R.styleable.MultidirectionRecyclerView_mdr_max_header_height, cellHeight)
        typedArray.recycle()
        inflate(context, R.layout.layout_list_mutidirection, this)
        mMainLayout = findViewById(R.id.mainLayout)
        mRightScrollView = findViewById(R.id.rightScrollView)
        mFooterView = findViewById(R.id.footerView)
        mHeaderView = findViewById(R.id.headerView)
        mLeftListView = createColHeaderLayout()
        mRightListView = createContentLayout()

        if (mShowFooter) {
            val footerLP = mFooterView.layoutParams
            footerLP.height = mMaxFooterHeight
            mFooterView.layoutParams = footerLP
        }
    }

    private fun createContentLayout(): RecyclerView {
        mRightListView = findViewById(R.id.contentListView)
        mContentLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRightListView.layoutManager = mContentLayoutManager
        mContentLayoutManager?.isSmoothScrollbarEnabled = true
        mContentLayoutManager?.isAutoMeasureEnabled = true
        mRightListView.addOnScrollListener(contentListScrollListener)
        mRightListView.setHasFixedSize(true)
        mRightListView.isNestedScrollingEnabled = true
        mRightListView.layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        return mRightListView
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        var intercept = false
        if (!isRefreshing && !isLoading) {
            val firstVisibleItem = mColHeaderLayoutManager!!.findFirstCompletelyVisibleItemPosition()
            val lastVisibleItem = mColHeaderLayoutManager!!.findLastCompletelyVisibleItemPosition()
            val totalItemCount = mColHeaderLayoutManager!!.itemCount
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    mStartY = ev.y
                    mLastMainHeight = mMainLayout.measuredHeight
                }
                MotionEvent.ACTION_MOVE -> {
                    // 当列表展示第一个item，且向下滑动拦截事件；当列表第一个item没有出现，最后一条item出现，且向上滑动拦截事件
                    val offY = ev.y - mStartY
                    if ((refreshable && firstVisibleItem == 0 && offY > 0) || (firstVisibleItem != 0) && (loadable && totalItemCount - 1 == lastVisibleItem && offY < 0)) {
                        intercept = true
                    }
                }
            }
            mLastMoveY = ev!!.y
        }
        return intercept
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        var touch = false
        if (!isRefreshing && !isLoading) {
            val firstVisibleItem = mColHeaderLayoutManager!!.findFirstCompletelyVisibleItemPosition()
            val lastVisibleItem = mColHeaderLayoutManager!!.findLastCompletelyVisibleItemPosition()
            val totalItemCount = mColHeaderLayoutManager!!.itemCount
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    mStartY = ev.y
                }
                MotionEvent.ACTION_MOVE -> {
                    // 累计偏移总量
                    mMoveOff += ((ev.y - mLastMoveY) * 0.8f)
                    // 计算滑动方向
                    val offY = ev.y - mStartY
                    if (firstVisibleItem == 0 && offY > 0) {
                        // 下拉刷新
                        touch = true
                        val footerLP = mFooterView.layoutParams
                        footerLP.height = if (mShowFooter) mMaxFooterHeight else 0
                        mFooterView.layoutParams = footerLP
                        val headerLP = mHeaderView.layoutParams
                        headerLP.height = abs(mMoveOff).toInt()
                        mHeaderView.layoutParams = headerLP
                    } else if (totalItemCount - 1 == lastVisibleItem && offY < 0) {
                        // 上拉加载
                        touch = true
                        val footerLP = mFooterView.layoutParams
                        footerLP.height = abs(mMoveOff).toInt()
                        mFooterView.layoutParams = footerLP
                        val headerLP = mHeaderView.layoutParams
                        headerLP.height = 0
                        mHeaderView.layoutParams = headerLP
                        if (mMainLayout.measuredHeight > measuredHeight - abs(mMoveOff).toInt()) {
                            val mainLP = mMainLayout.layoutParams
                            mainLP.height = (measuredHeight - abs(mMoveOff)).toInt()
                            mMainLayout.layoutParams = mainLP
                        }
                        mLeftListView.scrollToPosition(totalItemCount - 1)
                        mRightListView.scrollToPosition(totalItemCount - 1)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    when {
                        mFooterView.measuredHeight >= mMaxFooterHeight / 2 -> {
                            // 如果FooterView的高度大于最大高度的1/2，加载数据
                            loadMore()
                        }
                        mHeaderView.measuredHeight >= mMaxHeaderHeight / 2 -> {
                            // 如果HeaderView的高度大于最大高度的1/2，刷新数据
                            refresh()
                        }
                        else -> reset()
                    }
                    // 偏移量设置为0
                    mMoveOff = 0f
                }
            }
            mLastMoveY = ev!!.y
        }
        return touch
    }

    private fun reset() {
        isRefreshing = false
        isLoading = false
        val headerLP = mHeaderView.layoutParams
        headerLP.height = 0
        mHeaderView.layoutParams = headerLP
        val footerLP = mFooterView.layoutParams
        footerLP.height = if (mShowFooter) mMaxFooterHeight else 0
        mFooterView.layoutParams = footerLP
        val mainLP = mMainLayout.layoutParams
        mainLP.height = measuredHeight - footerLP.height
        mMainLayout.layoutParams = mainLP
    }

    private fun refresh() {
        val headerLP = mHeaderView.layoutParams
        headerLP.height = mMaxHeaderHeight
        mHeaderView.layoutParams = headerLP
        isRefreshing = true
        onRefresh?.invoke()
    }

    private fun loadMore() {
        val mainLP = mMainLayout.layoutParams
        if (mLastMainHeight > measuredHeight - mMaxFooterHeight) {
            // 如果MainLayout展示不下，则减小MainLayout的高度
            mainLP.height = measuredHeight - mMaxFooterHeight
        } else {
            mainLP.height = mLastMainHeight
        }
        mMainLayout.layoutParams = mainLP
        val footLP = mFooterView.layoutParams
        footLP.height = mMaxFooterHeight
        mFooterView.layoutParams = footLP
        mLeftListView.scrollToPosition(mColHeaderLayoutManager!!.itemCount - 1)
        mRightListView.scrollToPosition(mColHeaderLayoutManager!!.itemCount - 1)
        isLoading = true
        onLoadMore?.invoke()
    }

    /** 设置FooterView一直存在 */
    fun showFooter(showFooter: Boolean) {
        this.mShowFooter = showFooter
        if (mShowFooter) {
            // 设置FooterView的高度
            val footLP = mFooterView.layoutParams
            footLP.height = mMaxFooterHeight
            mFooterView.layoutParams = footLP
            mLeftListView.scrollToPosition(mColHeaderLayoutManager!!.itemCount - 1)
            mRightListView.scrollToPosition(mColHeaderLayoutManager!!.itemCount - 1)
        }
    }

    private fun createColHeaderLayout(): RecyclerView {
        mLeftListView = findViewById(R.id.colHeaderLayout)
        mColHeaderLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mColHeaderLayoutManager?.isSmoothScrollbarEnabled = true
        mColHeaderLayoutManager?.isAutoMeasureEnabled = true
        mLeftListView.layoutManager = mColHeaderLayoutManager
        mLeftListView.setHasFixedSize(true)
        mLeftListView.isNestedScrollingEnabled = true
        mLeftListView.addOnScrollListener(colHeaderScrollListener)
        mLeftListView.layoutParams = LayoutParams(colHeaderWidth, LayoutParams.WRAP_CONTENT)
        return mLeftListView
    }

    fun <Adapter : MultidirectionAdapter<*, *>> setAdapter(adapter: Adapter) {
        this.adapter = adapter
        val colHeaderListAdapter = ColHeaderListAdapter(this, adapter)
        mLeftListView.adapter = colHeaderListAdapter
        val contentListAdapter = ContentListAdapter(this, adapter)
        mRightListView.adapter = contentListAdapter
        adapter.setAdapter(colHeaderListAdapter, contentListAdapter)
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration, index: Int) {
        mLeftListView.addItemDecoration(decor, index)
        mRightListView.addItemDecoration(decor, index)
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        mLeftListView.addItemDecoration(decor)
        mRightListView.addItemDecoration(decor)
    }

    /** 横向滑动监听 */
    fun setOnHorizontalScrollListener(onScrollListener: ((scrollView: EzrHorizontalScrollView, l: Int, t: Int, oldl: Int, oldt: Int) -> Unit)?) {
        mRightScrollView.onScrollListener = onScrollListener
    }

    fun setTitle(titleLayout: EzrHorizontalScrollView) {
        mRightScrollView.onScrollListener = { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            titleLayout.scrollTo(scrollX, titleLayout.y.toInt())
        }
        titleLayout.onScrollListener = { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            mRightScrollView.scrollTo(scrollX, titleLayout.y.toInt())
        }
    }

    fun refresh(isRefreshing: Boolean) {
        if (isRefreshing) refresh()
        else reset()
    }

    fun finishLoadMore() {
        reset()
    }

    fun setFooter(footerView: View) {
        mFooterView.removeAllViews()
        mFooterView.addView(footerView, LayoutParams(-1, -1))
    }

    fun setHeader(headerView: View) {
        mHeaderView.removeAllViews()
        mHeaderView.addView(headerView, LayoutParams(-1, -1))
    }

    abstract class MultidirectionAdapter<ColHeaderViewHolder : RecyclerView.ViewHolder, ContentViewHolder : RecyclerView.ViewHolder> {

        enum class ViewType {
            HEADER, FOOTER, CONTENT
        }

        var mColHeaderListAdapter: ColHeaderListAdapter<*>? = null
        var mContentListAdapter: ContentListAdapter<*>? = null

        /** 创建列头ViewHolder */
        abstract fun onCreateColHeaderViewHolder(context: Context): ColHeaderViewHolder

        abstract fun onCreateContentViewHolder(context: Context?): ContentViewHolder

        /** 设置列头 */
        abstract fun onBindColHeaderViewHolder(viewHolder: ColHeaderViewHolder, position: Int)

        /** 设置内容 */
        abstract fun onBindContentViewHolder(viewHolder: ContentViewHolder, position: Int)

        abstract fun getItemCount(): Int

        fun notifyDataSetChanged() {
            mContentListAdapter?.notifyDataSetChanged()
            mColHeaderListAdapter?.notifyDataSetChanged()
        }

        fun notifyItemInserted(position: Int) {
            mContentListAdapter?.notifyItemInserted(position)
            mColHeaderListAdapter?.notifyItemInserted(position)
        }

        fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            mColHeaderListAdapter?.notifyItemRangeInserted(positionStart, itemCount)
            mContentListAdapter?.notifyItemRangeInserted(positionStart, itemCount)
        }

        fun notifyItemRemoved(position: Int) {
            mContentListAdapter?.notifyItemRemoved(position)
            mColHeaderListAdapter?.notifyItemRemoved(position)
        }

        fun notifyItemRemoved(positionStart: Int, itemCount: Int) {
            mColHeaderListAdapter?.notifyItemRangeRemoved(positionStart, itemCount)
            mContentListAdapter?.notifyItemRangeRemoved(positionStart, itemCount)
        }

        fun setAdapter(colHeaderListAdapter: ColHeaderListAdapter<*>, contentListAdapter: ContentListAdapter<*>) {
            mColHeaderListAdapter = colHeaderListAdapter
            mContentListAdapter = contentListAdapter
        }
    }

}