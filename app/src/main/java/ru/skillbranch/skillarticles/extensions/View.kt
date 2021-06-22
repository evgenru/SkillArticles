package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop

fun View.setMarginOptionally(
    left: Int = marginLeft,
    top: Int = marginTop,
    right: Int = marginRight,
    bottom: Int = marginBottom
) {
    if (this.layoutParams is MarginLayoutParams) {
        val lp = this.layoutParams as MarginLayoutParams
        this.layoutParams = lp.apply {
            this.setMargins(left, top, right, bottom)
        }
        this.requestLayout()
    }
}