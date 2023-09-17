package me.coffee.pda.alps

import android.content.Context
import android.os.Build
import cn.pda.serialport.Tools
import com.handheld.uhfr.UHFRManager
import com.uhf.api.cls.Reader
import com.uhf.api.cls.Reader.TAGINFO

object AlpsManager {

    private var isConnectUHF = false

    private var mUhf: UHFRManager? = null
    private var mScanUtil: ScanUtil? = null

    private val mUHFThread: UHFThread by lazy { UHFThread() }
    private val mScanManager: ScanManager by lazy {
        ScanManager().apply { setScanListener { scanCallback?.invoke(it) } }
    }

    private var isReading = false

    var readerCallback: ((List<String>?) -> Unit)? = null
    var scanCallback: ((String?) -> Unit)? = null

    fun init(context: Context) {
        mScanUtil = ScanUtil.getInstance(context)
        mScanManager.init(context)
    }

    fun initUHF() {
        mUhf = UHFRManager.getInstance()
        val err = mUhf?.setPower(33, 33)
        if (err === Reader.READER_ERR.MT_OK_ERR) {
            isConnectUHF = true
            mUhf?.region = Reader.Region_Conf.valueOf(1)
            mUhf?.gen2session = 0
            mUhf?.target = 0
            mUhf?.setQvaule(0)
            mUhf?.setFastID(false)
        }
    }


    fun releaseUHF() {
        mUhf?.close()
        mUhf = null
    }

    fun setScanKeyDisable() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            mScanUtil?.disableScanKey("134")
            mScanUtil?.disableScanKey("137")
        }
    }

    fun setScanKeyEnable() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            mScanUtil?.enableScanKey("134")
            mScanUtil?.enableScanKey("137")
        }
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    fun startScan() {
        mScanManager.start()
        mScanManager.scan()
    }

    fun stopScan() {
        mScanManager.stop()
    }

    fun startUHF() {
        isReading = true
        mUHFThread.start()
    }

    fun stopUHF() {
        isReading = false
        mUHFThread.stop()
    }

    fun release() {
        stopUHF()
        releaseUHF()
        mScanManager.close()
    }

    fun handleData(info: TAGINFO): String {
        val epc = Tools.Bytes2HexString(info.EpcId, info.EpcId.size)
//        if ( info.EmbededData != null) {
//            val tid = Tools.Bytes2HexString(info.EmbededData, info.EmbededData.size)
//        }
        return epc
    }

    private class UHFThread() : Runnable {

        @Volatile
        private var mThread: Thread? = null

        fun start() {
            mThread = Thread(this)
            mThread?.start()
        }

        fun stop() {
            mThread = null
        }

        override fun run() {
            val thisThread = Thread.currentThread()
            while (mThread === thisThread && isReading) {
                try {
                    val list = mUhf?.tagInventoryByTimer(50)
                    val result = list?.map { handleData(it) }
                    readerCallback?.invoke(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}