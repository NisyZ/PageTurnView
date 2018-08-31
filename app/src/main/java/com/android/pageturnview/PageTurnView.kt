package com.android.pageturnview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

/**
 *@author:nisiyuan
 *@file: PageTurnView.kt
 *@time: 2018/01/25
 *@describe：图片翻页
 */
class PageTurnView : View {

    private var mBitmaps: Array<Bitmap> = arrayOf()
    private var mTextPaint: Paint = Paint()
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    var mClipX: Int = 0
    private val mTitleText: String = "FBI WARNING"
    private val mContentText: String = "Please set data use setBitmaps method"

    private val TEXT_SIZE_NORMAL = 1 / 40f
    private val TEXT_SIZE_LARGER = 1 / 20f// 标准文字尺寸和大号文字尺寸的占比

    private var mTextSizeNormal: Float = 0f
    private var mTextSizeLarger: Float = 0f

    private var autoAreaLeft: Int = 0
    private var autoAreaRight: Int = 0
    private var isLastPage: Boolean = true
    private var pageIndex: Int = 0
    private var mCurPointX: Int = 0
    private var isNextPage: Boolean = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet!!, 0)
    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(context, attributeSet, defStyle) {
        initBitmap()
    }

    private fun initBitmap() {
        val temp = mBitmaps.indices.map { Bitmap.createScaledBitmap(mBitmaps[it], mViewWidth, mViewHeight, true) }
        mBitmaps = temp.toTypedArray()
    }


    @Synchronized
    fun setBitmaps(bitmaps: Array<Bitmap>) {
        /**
         * 如果数据为空则抛出异常
         */
        if (bitmaps == null || bitmaps.isEmpty()) {
            throw IllegalArgumentException("no bitmap to display")
        }
        /**
         * 如果数据长度小于2则抛出异常
         */
        if (bitmaps.size < 2) {
            throw IllegalArgumentException("fuck you and fuck to use imageview")
        }
        mBitmaps = bitmaps
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        isNextPage = true
        val mMoveValid = 0
        when (event.action) {
        // 触摸屏幕的时候
            MotionEvent.ACTION_DOWN -> {
                mCurPointX = event.x.toInt()

                /*如果事件位于回滚区域*/
                if (mCurPointX < autoAreaLeft) {
                    // 那就不翻下一页了而是上一页
                    isNextPage = false
                    pageIndex--
                    mClipX = mCurPointX
                    invalidate()
                }
            }
        // 滑动时
            MotionEvent.ACTION_MOVE -> {
                val SlideDis: Float = mCurPointX - event.x
                if (Math.abs(SlideDis) > mMoveValid) {
                    mClipX = event.x.toInt()
                    invalidate()
                }
            }
        // 触点抬起时
            MotionEvent.ACTION_UP -> {
                // 判断是否需要自滑动
                judgeSlideAuto()
                /*如果当前页不是最后一页如果是需要翻下一页并且上一页已经被clip掉*/
                if (!isLastPage && isNextPage && mClipX <= 0) {
                    pageIndex++
                    mClipX = mViewWidth
                    invalidate()
                }
            }
        }
        return true
    }

    /*判断是否需要自动滑动,根据参数的当前值判断绘制*/
    private fun judgeSlideAuto() {
        /*如果裁剪的右端坐标在控件左端五分之一区域内，那么我们直接让其自动滑到控件左端*/
        if (mClipX < autoAreaLeft) {
            while (mClipX > 0) {
                mClipX--
                invalidate()
            }
        }
        /*
         * 如果裁剪的右端点坐标在控件右端五分之一的区域内，那么我们直接让其自动滑到控件右端
         */
        if (mClipX > autoAreaRight) {
            while (mClipX < mViewWidth) {
                mClipX++
                invalidate()
            }
        }
    }

    private fun drawBitmap(canvas: Canvas) {
        // 绘制位图前重置isLastPage为false
        isLastPage = false

        // 限制pageIndex的值范围
        pageIndex = if (pageIndex < 0) 0 else pageIndex
        pageIndex = if (pageIndex > mBitmaps.size) mBitmaps.size else pageIndex

        // 计算数据起始位置
        var start: Int = mBitmaps.size - 2 - pageIndex// 3，
        var end: Int = mBitmaps.size - pageIndex// 5

        // 如果数据起点位置小于0，则表示当前已经到了最后一张图片
        if (start < 0) {
            // 此时设置isLastPage为true
            isLastPage = true
            // 并显示提示消息
//            showToast("This is fucking lastest page")
            // 强制重置起始位置
            start = 0
            end = 1
        }
        for (i in start until end) {
            canvas.save()

            /*仅裁剪位于最顶层的画布区域，如果到了末页则不再执行裁剪*/
            if (!isLastPage && i == end - 1) {
                canvas.clipRect(0, 0, mClipX, mViewHeight)
            }
            canvas.drawBitmap(mBitmaps[i], 0f, 0f, null)
            canvas.restore()
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mViewHeight = h
        mViewWidth = w

        // 计算文字尺寸
        mTextSizeNormal = TEXT_SIZE_NORMAL * mViewHeight
        mTextSizeLarger = TEXT_SIZE_LARGER * mViewHeight

        // 初始化裁剪右端点坐标
        mClipX = mViewWidth

        // 计算控件左侧和右侧自动吸附的区域
        autoAreaLeft = mViewWidth * 1 / 5
        autoAreaRight = mViewWidth * 4 / 5
    }

    override fun onDraw(canvas: Canvas?) {
//        super.onDraw(canvas)
        if (canvas != null) {
            if (mBitmaps.isEmpty()) {
                defaultDisplay(canvas)
            } else {
                drawBitmap(canvas)
            }
        }
    }

    /*默认显示*/
    private fun defaultDisplay(canvas: Canvas) {
        // 绘制底色
        canvas.drawColor(Color.WHITE)
        /*绘制标题文本*/
        mTextPaint.textSize = mTextSizeLarger
        mTextPaint.isAntiAlias = true
        mTextPaint.color = Color.RED
        canvas.drawText(mTitleText, (mViewWidth / 2) - mTextPaint.measureText(mTitleText) / 2,
                (mViewHeight / 4).toFloat(), mTextPaint)

        /*绘制文本*/
        mTextPaint.textSize = mTextSizeNormal
        mTextPaint.isAntiAlias = true
        mTextPaint.color = Color.BLACK
        canvas.drawText(mContentText, (mViewWidth / 2) - mTextPaint.measureText(mContentText) / 2,
                (mViewHeight / 3).toFloat(), mTextPaint)
    }
}