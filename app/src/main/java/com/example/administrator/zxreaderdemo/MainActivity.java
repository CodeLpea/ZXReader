package com.example.administrator.zxreaderdemo;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Lib.FWReader.S8.function_S8;

import static com.example.administrator.zxreaderdemo.MyApplication.getInstance;
import static com.example.administrator.zxreaderdemo.StringTool.getRfid;
import static com.example.administrator.zxreaderdemo.StringTool.hdHex;
import static com.example.administrator.zxreaderdemo.StringTool.isHexStrValid;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView tv_status, tv_data;

    private MyHandler myHandler = new MyHandler();
    private function_S8 call_contactLess;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static boolean isRun = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initReader();
    }

    private void initReader() {

        call_contactLess = new function_S8( MyApplication.getInstance());

        call_contactLess.SetTransPara(0x20, 1137, 41234);


    }

    private void initViews() {
        tv_status = findViewById(R.id.tv_status);
        tv_data = findViewById(R.id.tv_data);

    }

    public void auToStart(View view) {
        tv_status.setText("自动寻卡状态....");
        if (!isRun) {
            isRun = true;
            executorService.execute(new AutoCard());
        }


    }

    public void stop(View view) {
        tv_status.setText("关闭自动寻卡....");
        tv_data.setText("");
        if (isRun) {
            isRun = false;
            executorService.shutdown();
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            tv_data.setText(msg.obj.toString());
        }
    }

    private class AutoCard implements Runnable {

        @Override
        public void run() {
            while (isRun) {
                try {
                    Thread.sleep(100);
                    startRead();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int startRead() {
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pSnrM1 = new char[255];
        // TODO Auto-generated method stub
        do {
            hdev = call_contactLess.fw_init_ex(2, null, 0);
        } while (hdev == -1);
        Log.e(TAG, "hdev: " + hdev);
        if (hdev != -1) {
            sendMsg("连接成功", false);
            //获取一次硬件版本号
            //try to get module version
            result = call_contactLess.fw_getver(hdev, pModVer);

            if (0 == result) {
                sendMsg("-", true);
                sendMsg("Module Version: " + String.valueOf(pModVer), true);
                //获取卡号 UID
                result = call_contactLess.fw_card_str(hdev, (short) 1, pSnrM1);
                call_contactLess.fw_exit(hdev);
                //4A0FFBE0
                Log.e(TAG, "pSnrM1: " + pSnrM1);
                sendMsg("_card:ok ", true);
                //只截取前面8位
                String substring = String.valueOf(pSnrM1).substring(0, 8);
                //判断是否符合十六进制
                boolean hexStrValid = isHexStrValid(substring);
                Log.e(TAG, "hexStrValid: " + hexStrValid);
                if (!hexStrValid) {
                    return result;
                }
                call_contactLess.fw_beep(hdev,1);
                //高低位算法
                String strUid = hdHex(substring, substring.length());
                //十六进制转换为十进制
                BigInteger rfid = getRfid(strUid);
                Log.e(TAG, "UID: " + rfid);
                sendMsg(String.valueOf(rfid), false);

            }
        } else {
            sendMsg("_Link reader failed", true);
        }
        return result;
    }



    private void sendMsg(String msg, boolean isApand) {
        Message message = myHandler.obtainMessage();
        if (isApand) {
            message.obj = tv_data.getText() + "\n\r" + msg;
        } else {
            message.obj = msg;
        }
        myHandler.sendMessage(message);

    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
