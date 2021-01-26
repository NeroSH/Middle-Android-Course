package ru.skillbranch.skillarticles.extensions

import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.widget.NestedScrollView

fun NestedScrollView.setMarginOptionally(
    left:Int = marginLeft,
    top : Int = marginTop,
    right : Int = marginRight,
    bottom : Int = marginBottom
) {
    val params = this.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(left, top,right, bottom)
    this.layoutParams = params
}