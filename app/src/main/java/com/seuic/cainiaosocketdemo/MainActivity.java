package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketResponsePacket;

/**
 * Created by yangjianan on 2018/6/1.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SocketClient socketClient;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        findViewById(R.id.start_socket).setOnClickListener(this);
        findViewById(R.id.clear_data).setOnClickListener(this);
        findViewById(R.id.stop_socket).setOnClickListener(this);
    }

    private void socketClient() {
        progressDialog.setTitle("启动中");
        progressDialog.show();
        socketClient = new SocketClient("192.168.1.114", 7777);
//        socketClient = new SocketClient("192.168.1.117", 20006);
        socketClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                }
                Log.i("Socket", "已连接onConnected:");
                socketClient.send("Android 你好");
                //socketClient.setHeartBeatMessage("hello, server !");
                //socketClient.sendString("string数据");
            }

            @Override
            public void onDisconnected(SocketClient client) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "连接超时", Toast.LENGTH_SHORT).show();
                }
                Log.i("Socket", "超时timeout");
                String error = client.getCharsetName();
                Log.i("Server", "timeoutData:" + error);
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                String responseMsg = responsePacket.getMessage();
                int i = 1;
                Log.i("Socket", "响应信息：" + responseMsg);
            }
        });

        socketClient.setConnectionTimeout(1000 * 15);
        socketClient.setHeartBeatInterval(1000);
        socketClient.setRemoteNoReplyAliveTimeout(1000 * 60);
        socketClient.setCharsetName("UTF-8");
        socketClient.connect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_socket:
                //开启socket连接
                socketClient();
                break;
            case R.id.stop_socket:
                //关闭socket连接
                if (socketClient != null) {
                    socketClient.disconnect();
                    Toast.makeText(this, "已关闭连接", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.clear_data:
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
