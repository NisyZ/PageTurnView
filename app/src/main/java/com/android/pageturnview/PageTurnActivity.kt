package com.android.pageturnview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_page_turn.*

/**
 *@author:nisiyuan
 *@file: PageTurnActivity.kt
 *@time: 2018/01/26
 *@describe：
 */
class PageTurnActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_turn)
        val bitmap1 = BitmapFactory.decodeResource(resources, R.mipmap.temp1)
        val bitmap2 = BitmapFactory.decodeResource(resources, R.mipmap.temp2)
        val bitmap3 = BitmapFactory.decodeResource(resources, R.mipmap.temp3)
        val bitmap4 = BitmapFactory.decodeResource(resources, R.mipmap.temp4)
        val bitmap5 = BitmapFactory.decodeResource(resources, R.mipmap.temp5)
        val mBitmap = mutableListOf<Bitmap>()
        mBitmap.add(bitmap5)
        mBitmap.add(bitmap4)
        mBitmap.add(bitmap3)
        mBitmap.add(bitmap2)
        mBitmap.add(bitmap1)
        page_turn.setBitmaps(mBitmap.toTypedArray())
    }
}