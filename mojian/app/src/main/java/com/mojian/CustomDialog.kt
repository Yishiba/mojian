package com.mojian

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View


/**
 * Author: pengmutian
 * Date: 2022/10/21 18:03
 * Description: CustomEditDialog
 */
/**
 * Created by wangfei
 */
class CustomDialog(context:Context) : Dialog(context) , View.OnClickListener {
    private var show = 2





    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_add_food)
//        val dialog_title = findViewById<View>(R.id.edit1).setOnClickListener(this)

    }

    override fun onClick(v: View?) {
    }

    fun setCanotBackPress() {
        setOnKeyListener { dialog, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_BACK && event.action === KeyEvent.ACTION_UP
        }
    }

    companion object {
        private const val SHOW_ONE = 1
    }
}