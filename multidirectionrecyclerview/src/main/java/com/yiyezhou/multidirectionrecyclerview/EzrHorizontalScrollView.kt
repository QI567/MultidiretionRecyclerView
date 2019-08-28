package com.yiyezhou.multidirectionrecyclerview

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

class EzrHorizontalScrollView : HorizontalScrollView {

    var onScrollListener: ((scrollView: EzrHorizontalScrollView, l: Int, t: Int, oldl: Int, oldt: Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleRes: Int) : super(
        context,
        attributeSet,
        defStyleRes
    )

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollListener?.invoke(this,l,t,oldl,oldt)
    }
}