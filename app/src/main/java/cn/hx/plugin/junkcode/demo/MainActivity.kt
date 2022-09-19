package cn.hx.plugin.junkcode.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.Keep

@Keep
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}