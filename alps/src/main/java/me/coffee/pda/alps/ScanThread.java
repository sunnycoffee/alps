package me.coffee.pda.alps;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import cn.pda.serialport.SerialPort;

class ScanThread extends Thread {

    private static final String TAG = ScanThread.class.getSimpleName();

    private final SerialPort mSerialPort;
    private final InputStream is;
    private final OutputStream os;
    public static int port = 0;
    private final int baudrate = 9600;
    private final int flags = 0;
    private final Handler handler;

    public final static int SCAN = 1001; // messege recv mode

    private Timer mTimer;

    /**
     * if throw exception, serialport initialize fail.
     *
     * @throws SecurityException
     * @throws IOException
     */
    public ScanThread(Handler handler) throws SecurityException, IOException {
        this.handler = handler;
        mSerialPort = new SerialPort(port, baudrate, flags);
        if (port == 0) {
            mSerialPort.scaner_poweron();
        }
        is = mSerialPort.getInputStream();
        os = mSerialPort.getOutputStream();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /** clear useless data **/
        byte[] temp = new byte[1024];
        is.read(temp);
    }

    @Override
    public void run() {
        try {
            int size = 0;
            byte[] buffer = new byte[1024];
            int available = 0;
            while (!isInterrupted()) {
                available = is.available();
                if (available > 0) {
                    Log.e(TAG, "available = " + available);
                    size = is.read(buffer);
                    if (size > 0) {
                        sendMessege(buffer, size, SCAN);
                        stopScan();
                    }
                    Thread.sleep(50);
                }
            }
        } catch (Exception e) {
            // 返回错误信息
            e.printStackTrace();
        }
        super.run();
    }

    private void sendMessege(byte[] data, int dataLen, int mode) {
        try {
            String dataStr = new String(data, 0, dataLen, "GBK");
            Bundle bundle = new Bundle();
            bundle.putString("data", dataStr);
            byte[] dataBytes = new byte[dataLen];
            System.arraycopy(data, 0, dataBytes, 0, dataLen);
            bundle.putByteArray("dataBytes", dataBytes);
            Message msg = new Message();
            msg.what = mode;
            msg.setData(bundle);
            handler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scan() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mSerialPort.scaner_trig_stat()) {
            Log.e(TAG, "scan reset ");
            mSerialPort.scaner_trigoff();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mSerialPort.scaner_trigon();
        Log.e(TAG, "scan start ");
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mSerialPort.scaner_trigoff();
                Log.e(TAG, "scan terminate ");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(SCAN);
            }
        }, 3000);
    }

    public void stopScan() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mSerialPort.scaner_trig_stat()) {
            Log.e(TAG, "scan reset ");
            mSerialPort.scaner_trigoff();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (mSerialPort != null) {
            if (port == 0) {
                mSerialPort.scaner_poweroff();
            }
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.close(port);
        }
    }

}
