/*
 * Copyright 2016-2019 Juliane Lehmann <jl@lambdasoup.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.lambdasoup.quickfit.util.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * See https://gist.github.com/alexfu/0f464fc3742f134ccd1e
 *
 *
 * and http://stackoverflow.com/a/30386358/1428514
 */
class DividerItemDecoration(context: Context, private val drawAtEnd: Boolean) : RecyclerView.ItemDecoration() {

    private val divider: Drawable

    init {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        divider = a.getDrawable(0)!!
        a.recycle()
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(c: Canvas, parent: RecyclerView) {
        val manager = parent.layoutManager
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val lastDecoratedChild = getLastDecoratedChild(parent)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)

            if (pos == NO_POSITION || pos > lastDecoratedChild) {
                continue
            }

            val ty = (child.translationY + 0.5f).toInt()
            val tx = (child.translationX + 0.5f).toInt()
            val bottom = manager!!.getDecoratedBottom(child) + ty
            val top = bottom - divider.intrinsicHeight
            divider.setBounds(left + tx, top, right + tx, bottom)
            divider.draw(c)
        }
    }

    private fun drawHorizontal(c: Canvas, parent: RecyclerView) {
        val manager = parent.layoutManager
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom

        val lastDecoratedChild = getLastDecoratedChild(parent)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)

            if (pos == NO_POSITION || pos > lastDecoratedChild) {
                continue
            }

            val ty = (child.translationY + 0.5f).toInt()
            val tx = (child.translationX + 0.5f).toInt()
            val right = manager!!.getDecoratedRight(child) + tx
            val left = right - divider.intrinsicWidth
            divider.setBounds(left, top + ty, right, bottom + ty)
            divider.draw(c)
        }
    }

    private fun getLastDecoratedChild(parent: RecyclerView): Int {
        return parent.adapter!!.itemCount - 1 - if (drawAtEnd) 0 else 1
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val pos = parent.getChildAdapterPosition(view)

        if (pos != NO_POSITION) {
            if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                outRect.bottom = divider.intrinsicHeight
            } else {
                outRect.right = divider.intrinsicWidth
            }
        }
    }

    private fun getOrientation(parent: RecyclerView): Int {
        val layoutManager = parent.layoutManager
        require(layoutManager is LinearLayoutManager) { "DividerItemDecoration can only be added to RecyclerView with LinearLayoutManager" }
        return layoutManager.orientation
    }
}
