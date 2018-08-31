@file:Suppress("UNUSED_EXPRESSION")

package com.android.pageturnview

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 *@author:nisiyuan
 *@file: FoldView.kt
 *@time: 2018/01/26
 *@describe：翻页加入折线效果
 */
class FoldView : View {

    private val mPaint: Paint = Paint()
    private val mPath: Path = Path()
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0

    private var mPointX: Int = 0
    private var mPointY: Int = 0
    private var mValueAdded = 0
    private var mBuffArea = 0
    private var mAutoAreaRight = 0f
    private var mAutoAreaBottom = 0f
    private var mAutoAreaLeft = 0f
    private var isSlide = false

    private val BUFF_AREA = 1 / 50f // 底部缓冲区域占比
    private val VALUE_ADDED = 1 / 500f // 精度附加值占比
    private val AUTO_AREA_BOTTOM_RIGHT = 3 / 4f // 右下角自滑区域占比
    private val AUTO_AREA_BOTTOM_LEFT = 1 / 8f // 左侧自滑区域占比
    private var mStart_X = 0 // 直线起点坐标
    private var mStart_Y = 0 // 直线起点坐标
    private var mRectCurrent: Rect = Rect() // 当前区域，也就是控件的大小
    private var mPathFoldAndNext: Path = Path()// 一个包含折叠和下一页的区域path

    private var mSlide: Slide = Slide.RIGHT_BOTTOM // 定义当前滑动是往下滑还是右下滑

    private var mRegionShortSize: Region = Region()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(context, attributeSet, defStyle) {
        mPaint.color = Color.BLACK
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
    }

    enum class Slide {
        LEFT_BOTTOM, RIGHT_BOTTOM
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
        // 当手指在移动
            MotionEvent.ACTION_MOVE -> {
                mPointX = event.x.toInt()
                mPointY = event.y.toInt()
                invalidate()
            }

        // 当手指抬起
            MotionEvent.ACTION_UP -> {
                // 获取当前事件点
                val x = event.x
                val y = event.y

                // 如果当前事件点位于右下角自滑区域
                if (x > mAutoAreaRight && y > mAutoAreaBottom) {
                    // 当前为往右下滑
                    mSlide = Slide.RIGHT_BOTTOM
                    // 开始滑动
                    justSlide(x, y)
                }
                // 如果当前事件点位于左侧自滑区域
                if (x < mAutoAreaLeft) {
                    // 当前为往左下滑动
                    mSlide = Slide.LEFT_BOTTOM
                    // 开始滑动
                    justSlide(x, y)
                }
            }
        }
        return true
    }

    private fun justSlide(x: Float, y: Float) {
        // 获取当前点为直线方程坐标
        mStart_X = x.toInt()
        mStart_Y = y.toInt()
        // 要开始滑动了
        isSlide = true
        // 滑动
        slide()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mViewHeight = h
        mViewWidth = w

        // 计算当前区域
        mRectCurrent.set(0, 0, mViewWidth, mViewHeight)

        // 计算精度附加值
        mValueAdded = (mViewHeight * VALUE_ADDED).toInt()

        // 计算底部缓冲区域
        mBuffArea = (mViewHeight * BUFF_AREA).toInt()

        /*计算自滑区域*/
        mAutoAreaBottom = mViewHeight * AUTO_AREA_BOTTOM_RIGHT
        mAutoAreaRight = mViewWidth * AUTO_AREA_BOTTOM_RIGHT
        mAutoAreaLeft = mViewWidth * AUTO_AREA_BOTTOM_LEFT

        // 计算短边的有效区域
        computeShortSizeRegion()
    }

    /*计算短边的有效区域*/
    private fun computeShortSizeRegion() {
        // 短边圆形路径对象
        val pathShortSize = Path()
        // 用来装载path边界值的RectF对象
        val rectShortSize = RectF()

        // 添加圆形到path
        pathShortSize.addCircle(0f, mViewHeight.toFloat(), mViewWidth.toFloat(), Path.Direction.CCW)

        // 计算边界
        pathShortSize.computeBounds(rectShortSize, true)

        // 讲path转化为Region
        mRegionShortSize.setPath(pathShortSize, Region(rectShortSize.left.toInt(), rectShortSize.top.toInt()
                , rectShortSize.right.toInt(), rectShortSize.bottom.toInt()))

    }


    override fun onDraw(canvas: Canvas) {
        // 重绘时重置路径
        mPath.reset()

        // 绘制底色
        canvas.drawColor(Color.WHITE)

        /*如果坐标点在右下角则不执行绘制*/
        if (mPointX == 0 && mPointY == 0) {
            return
        }

        /*
        *  判断触摸点是否在短边的有效区域内
        * */
        if (!mRegionShortSize.contains(mPointX, mPointY)) {
            // 防止坐标出现错乱
            if (mPointX <= -mViewWidth) {
                mPointX = -mViewWidth
            }
            // 如果不在则通过x坐标强行重算y坐标
            mPointY = (Math.sqrt(Math.pow(mViewWidth.toDouble(), 2.0) - Math.pow(mPointX.toDouble(), 2.0)) - mViewHeight).toInt()

            // 精度附加值避免精度丢失
            mPointY = Math.abs(mPointY) + mValueAdded
        }

        /*缓冲区域判断*/
        val area: Int = mViewHeight - mBuffArea
        if (!isSlide && mPointY >= area) {
            mPointY = area
        }

        val mK = mViewWidth - mPointX
        val mL = mViewHeight - mPointY

        // 需要重复使用的参数存值避免重复计算
        val temp = Math.pow(mL.toDouble(), 2.0) + Math.pow(mK.toDouble(), 2.0)

        // 计算短边长边长度
        val sizeShort = temp / (2f * mK)
        val sizeLong = temp / (2f * mL)

        /*生成路径*/
        mPath.moveTo(mPointX.toFloat(), mPointY.toFloat())
        mPathFoldAndNext.moveTo(mPointX.toFloat(), mPointY.toFloat())

        if (sizeLong > mViewHeight) {
            val an = sizeLong - mViewHeight
            // 三角形AMN的MN边
            val largerTrianShortSize = an / (sizeLong - (mViewHeight - mPointY)) * (mViewWidth - mPointX)
            // 三角形AQN的QN边
            val smallTrianShortSize = an / sizeLong * sizeShort
            // 计算参数
            val topX1 = mViewWidth - largerTrianShortSize
            val topX2 = mViewWidth - smallTrianShortSize
            val btmX2 = mViewWidth - sizeShort

            // 生成四边形路径
            mPath.lineTo(topX1.toFloat(), 0f)
            mPath.lineTo(topX2.toFloat(), 0f)
            mPath.lineTo(btmX2.toFloat(), mViewHeight.toFloat())
            mPath.close()

            // 生成包含折叠和下一页的路径
            mPathFoldAndNext.lineTo(topX1.toFloat(), 0f)
            mPathFoldAndNext.lineTo(mViewWidth.toFloat(), 0f)
            mPathFoldAndNext.lineTo(mViewWidth.toFloat(), mViewHeight.toFloat())
            mPathFoldAndNext.lineTo(btmX2.toFloat(), mViewHeight.toFloat())
            mPathFoldAndNext.close()
        } else {
            // 计算参数
            val leftY = mViewHeight - sizeLong
            val btmX = mViewWidth - sizeShort

            // 生成三角形路径
            mPath.lineTo(mViewWidth.toFloat(), leftY.toFloat())
            mPath.lineTo(btmX.toFloat(), mViewHeight.toFloat())
            mPath.close()

            // 生成包含折叠和下一页的路径
            mPathFoldAndNext.lineTo(mViewWidth.toFloat(), leftY.toFloat())
            mPathFoldAndNext.lineTo(mViewWidth.toFloat(), mViewHeight.toFloat())
            mPathFoldAndNext.lineTo(btmX.toFloat(), mViewHeight.toFloat())
            mPathFoldAndNext.close()
        }
//        canvas.drawPath(mPath,mPaint)

        // 定义区域
        val rectFold: Rect = computeRegion(mPath)
        val rectNext: Rect = computeRegion(mPathFoldAndNext)

        // 计算当前页的区域
        canvas.save()
        canvas.clipRect(mRectCurrent)
        canvas.clipRect(rectNext)
        canvas.drawColor(Color.parseColor("#fff4D8B7"))
        canvas.restore()

        // 计算折叠也区域
        canvas.save()
        canvas.clipRect(rectFold)
        canvas.drawColor(Color.parseColor("#FF663C21"))
        canvas.restore()
//
//        // 计算下一页区域
//        canvas.save()
//        canvas.clipRect(rectNext)
//        canvas.clipRect(rectFold)
//        canvas.drawColor(Color.parseColor("#FF9596C4"))
//        canvas.restore()

    }

    private fun computeRegion(path: Path): Rect {
        val rect = Rect()
        val rectF = RectF()
        path.computeBounds(rectF, true)
        rect.set(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
        return rect
    }

    fun slide() {
        /*如果滑动标识值为false则返回*/
        if (!isSlide) {
            return
        }
        /**
         * 如果当前滑动标识为向右下滑动x坐标恒小于控件宽度
         */
        if (mSlide == Slide.RIGHT_BOTTOM && mPointX < mViewWidth) {
            // 则让x坐标自加
            mPointX += 10
            // 并根据x坐标的值重新计算y坐标的值
            mPointY = mStart_Y + (mPointX - mStart_X) * (mViewHeight - mStart_Y) / (mViewWidth - mStart_X)
            // 让SlideHandler处理重绘
            SlideHandler().sleep(25)
        }
        /**
         * 如果当前滑动标识为向左下滑动x坐标恒大于控件宽度的负值
         */
        if (mSlide == Slide.LEFT_BOTTOM && mPointX > -mViewWidth) {
            // 则让x坐标自减
            mPointX -= 20
            // 并根据x坐标的值重新计算y坐标的值
            mPointY = mStart_Y + (mPointX - mStart_X) * (mViewHeight - mStart_Y) / (mViewWidth - mStart_X)
            // 让SlideHandler处理重绘
            SlideHandler().sleep(25)
        }
    }

    /*提供给外部关闭自滑动*/
    fun slideStop() {
        isSlide = false
    }

    inner class SlideHandler : Handler() {
        override fun handleMessage(msg: Message) {
            // 循环调用滑动计算
            this@FoldView.slide()

            // 重绘视图
            this@FoldView.invalidate()
        }

        /*延迟向handler发送消息实现时间间隔*/
        fun sleep(delayMillis: Long) {
            removeMessages(0)
            sendMessageDelayed(obtainMessage(), delayMillis)
        }
    }

}