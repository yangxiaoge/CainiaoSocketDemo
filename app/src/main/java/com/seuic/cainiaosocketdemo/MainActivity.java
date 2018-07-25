package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.seuic.cainiaosocketdemo.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangjianan on 2018/6/1.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ProgressDialog progressDialog;
    private ImageView mImageview;
    private TextView currentCode;
    private TextView countCode;
    private RecyclerView codeListRV;
    private CodeListAdapter codeListAdapter;
    private List<String> data;
    private SharedPreferences sharedPreferences;
    private String ips = "192.168.80.64,192.168.80.64,192.168.80.64"; //169.254.173.207,169.254.222.233
    private String[] ipArray;
    private SocketBarcodeUtil socketInstance = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences(Constants.SPNAME, Context.MODE_PRIVATE);
        progressDialog = new ProgressDialog(this);
        codeListRV = findViewById(R.id.id_recyclerview);
        mImageview = findViewById(R.id.current_imageview);
        currentCode = findViewById(R.id.current_barcode);
        countCode = findViewById(R.id.count_code);
        findViewById(R.id.start_socket).setOnClickListener(this);
        findViewById(R.id.clear_data).setOnClickListener(this);
        findViewById(R.id.stop_socket).setOnClickListener(this);
        findViewById(R.id.imageButton).setOnClickListener(this);
        initRV();
    }

    private void initRV() {
        data = new ArrayList<>();
        codeListAdapter = new CodeListAdapter(data);
        codeListRV.setLayoutManager(new LinearLayoutManager(this));
        codeListRV.setAdapter(codeListAdapter);

    }

    @Override
    public void onClick(View v) {
        if (socketInstance == null)
            socketInstance = SocketBarcodeUtil.getInstance();

        switch (v.getId()) {
            case R.id.start_socket:
                //手动触发清零
                retryTime = 0;
                //开启
                startLink();
                break;
            case R.id.stop_socket:
                //关闭
                socketInstance.stopLink();
                break;
            case R.id.clear_data:
                currentCode.setText("");
                countCode.setText("0");

                data.clear();
                codeListAdapter.notifyDataSetChanged();
                break;
            case R.id.imageButton:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            default:
        }
    }

    //重新连接计数
    private int retryTime = 0;

    private void startLink() {
        socketInstance.startLink(new SocketBarcodeUtil.SocketCallback() {
            @Override
            public void startStatus(boolean status) {
                if (!status) {
                    retryTime++;
                    startLink();
                } else {
                    retryTime = 0;
                }
                Log.d("MainActivity", "开启关闭状态 status:" + status + " retryTime = " + retryTime);
            }

            @Override
            public void barcode(String barcode) {
                Log.d("MainActivity", "二维码 barcode:" + barcode);
                //刷新数据
                codeListUpdate(barcode);
            }
        }, sharedPreferences.getString(Constants.IP, Constants.ipsDefault));
    }

    /**
     * 更新数据
     *
     * @param barcode 条码
     */
    private void codeListUpdate(final String barcode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                data.add(barcode);
                codeListAdapter.notifyDataSetChanged();
                //赋值
                currentCode.setText(barcode);
                countCode.setText(String.valueOf(data.size()));
            }
        });
    }

    class CodeListAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        public CodeListAdapter(@Nullable List<String> data) {
            super(data);
            mLayoutResId = android.R.layout.simple_list_item_1;
        }

        @Override
        protected void convert(BaseViewHolder helper, String barcode) {
            helper.setText(android.R.id.text1, barcode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
