package me.coffee.pda.alps;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import cn.pda.serialport.SerialPort;

/**
 * 扫码管理
 *
 * @author kongfei
 */
class ScanManager {

    private static final int PORT = 0;//默认串口号
    private static final int BAUD_RATE = 9600;//默认波特率
    private static final int FLAGS = 0;//默认状态

    private MyHandler mHandler;
    private SerialPort mSerialPort;
    private InputStream is;

    private PDAScanListener scanListener;
    private LoopThread mThread;


    public void init(Context context) {
        mHandler = new MyHandler(this);
        mThread = new LoopThread();
        start();
    }

    public void start() {
        if (mThread == null) return;
        try {
            if (mSerialPort == null) {
                mSerialPort = new SerialPort(PORT, BAUD_RATE, FLAGS);
                mSerialPort.scaner_poweron();
                is = mSerialPort.getInputStream();
            }
            mThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scan() {
        if (mThread == null || mSerialPort == null) return;
        if (mSerialPort.scaner_trig_stat()) {
            mSerialPort.scaner_trigoff();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mSerialPort.scaner_trigon();
    }

    public void stop() {
        if (mThread == null) return;
        mThread.stop();
    }


    public void close() {
        if (mThread == null) return;
        mThread.stop();
        if (mSerialPort != null) {
            mSerialPort.scaner_poweroff();
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.close(PORT);
            mSerialPort = null;
        }
    }

    public void setScanListener(PDAScanListener scanListener) {
        this.scanListener = scanListener;
    }


    private static class MyHandler extends Handler {

        private final WeakReference<ScanManager> mWManager;

        private MyHandler(ScanManager manager) {
            mWManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanManager pda = mWManager.get();
            if (pda == null) return;
            pda.scanListener.onScan(msg.obj.toString());
        }
    }

    private class LoopThread implements Runnable {

        private volatile Thread mThread;

        private void start() {
            mThread = new Thread(this);
            mThread.start();
        }

        private void stop() {
            mThread = null;
        }

        @Override
        public void run() {
            Thread thisThread = Thread.currentThread();
            byte[] buffer = new byte[2048];
            try {
                while (mThread == thisThread) {
                    Thread.sleep(50);

                    int available = is.available();
                    if (available == 0) continue;
                    int size = is.read(buffer);
                    if (size == 0) continue;

                    Message message = mHandler.obtainMessage();
                    String value = new String(buffer, 0, size);
                    message.obj = value;
                    mHandler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface PDAScanListener {

        void onScan(String value);
    }
}
