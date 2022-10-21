package com.mojian

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Author: pengmutian
 * Date: 2022/10/21 15:46
 * Description: SplashActivity
 */
class SplashActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
//        window.decorView.postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
//        }, 1000)
    }
}