package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketResponsePacket;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangjianan on 2018/6/1.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SocketClient socketClient;
    private ProgressDialog progressDialog;
    private RecyclerView codeListRV;
    private CodeListAdapter codeListAdapter;
    private List<CodeItem> data;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        codeListRV = findViewById(R.id.id_recyclerview);
        findViewById(R.id.start_socket).setOnClickListener(this);
        findViewById(R.id.clear_data).setOnClickListener(this);
        findViewById(R.id.stop_socket).setOnClickListener(this);
        initRV();
    }

    private void initRV() {
        data = new ArrayList<>();
        codeListAdapter = new CodeListAdapter(data);
        codeListRV.setLayoutManager(new LinearLayoutManager(this));
        codeListRV.setAdapter(codeListAdapter);

        //for test
        codeListRV.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    data.add(new CodeItem("test code "+i));
                }
                codeListAdapter.addData(data);
                codeListAdapter.notifyDataSetChanged();
            }
        },2000);
    }

    private void socketClient() {
        progressDialog.setTitle("启动中");
        progressDialog.setCancelable(false);
        progressDialog.show();
        socketClient = new SocketClient("192.168.1.117", 20006);
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
                //连接终端，可以给一些页面提示
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "连接超时", Toast.LENGTH_SHORT).show();
                }
                String error = client.getCharsetName();
                Log.i("Server", "onDisconnected超时timeout:" + error);
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
//                String responseMsg = responsePacket.getMessage();
//                Log.i("Socket", "响应信息：" + responseMsg);
                byte[] data = responsePacket.getData();
                Log.i("Socket", "响应字节："+ new String(data));
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                parseData(byteArrayInputStream);
            }
        });

        socketClient.setConnectionTimeout(1000 * 15);
        socketClient.setHeartBeatInterval(1000);
        //socketClient.setRemoteNoReplyAliveTimeout(1000 * 60); //远程端在一定时间间隔没有消息后自动断开
        socketClient.setCharsetName("UTF-8");
        socketClient.connect();
    }

    private void parseData(ByteArrayInputStream netWStream) {
        byte[] buf = new byte[18];

        // check StartCode 检测数据包头0xFEFE
        int goodIdx = 0;
        int received = -1;
        int time_count = 100;

        received = netWStream.read(buf,0,1);
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

    class CodeListAdapter extends BaseQuickAdapter<CodeItem, BaseViewHolder>{

        public CodeListAdapter(@Nullable List<CodeItem> data) {
            super(data);
            mLayoutResId = android.R.layout.simple_list_item_1;
        }

        @Override
        protected void convert(BaseViewHolder helper, CodeItem item) {
            helper.setText(android.R.id.text1,item.barcode);
        }
    }
}
