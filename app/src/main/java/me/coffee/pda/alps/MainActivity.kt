package me.coffee.pda.alps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.start_btn).setOnClickListener {
//            AlpsManager.startUHF()
            AlpsManager.startScan()
        }
        findViewById<Button>(R.id.stop_btn).setOnClickListener {
//            AlpsManager.stopUHF()
            AlpsManager.stopScan()
        }
        AlpsManager.init(applicationContext)
        AlpsManager.readerCallback = { list ->
            list?.forEach {
                Log.d("UHF-READ", it)
            }
        }

        AlpsManager.scanCallback = {
            Log.d("UHF-SCAN", it.orEmpty())
        }
    }

    override fun onStart() {
        super.onStart()
        AlpsManager.initUHF()
        AlpsManager.setScanKeyDisable()
    }

    override fun onStop() {
        super.onStop()
        AlpsManager.releaseUHF()
        AlpsManager.setScanKeyEnable()
    }

    class BarcodeDataReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val barCode = intent.getStringExtra("data")
            Log.d("QRCODE", barCode + "")
        }
    }
}