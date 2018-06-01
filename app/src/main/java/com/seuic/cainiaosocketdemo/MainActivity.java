package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
    private XData xdata;

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
                    data.add(new CodeItem("test code " + i));
                }
                codeListAdapter.addData(data);
                codeListAdapter.notifyDataSetChanged();
            }
        }, 2000);
    }

    private void socketClient() {
        progressDialog.setTitle("启动中");
        progressDialog.setCancelable(false);
        progressDialog.show();
//        socketClient = new SocketClient("192.168.1.117", 20006);
        socketClient = new SocketClient("192.168.80.80", 7777);
//        socketClient = new SocketClient("192.168.1.117", 20006);
        socketClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                }
                Log.i("Socket", "已连接onConnected:");
                //socketClient.send("Android 你好");
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

                // 可在此实现自动重连
                try {
                    //socketClient.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
//                String responseMsg = responsePacket.getMessage();
//                Log.i("Socket", "响应信息：" + responseMsg);
                byte[] data = responsePacket.getData();
                System.out.println(new String(data));
                Log.i("Socket", "响应字节：" + BytesHexStrTranslate.bytesToHexFun1(data));
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
                //解析字节流数据
                parseData(byteArrayInputStream);
            }
        });

        socketClient.setConnectionTimeout(1000 * 15);
        socketClient.setHeartBeatInterval(1000);
//        socketClient.setRemoteNoReplyAliveTimeout(1000 * 60); //远程端在一定时间间隔没有消息后自动断开
        socketClient.setCharsetName("UTF-8");
        socketClient.connect();
    }

    private boolean parseData(ByteArrayInputStream netWStream) {
        xdata = new XData();
        byte[] buf = new byte[18];
        // check StartCode 检测数据包头0xFEFE
        int goodIdx = 0;
        int received = -1;
        int time_count = 100;

        while (true) {
            try {
                received = netWStream.read(buf, 0, 1);
            } catch (Exception ex) {
                Log.i("Socket", "Read Exception " + ex.getMessage());
                xdata.error_msg = "Read Exception " + ex.getMessage();
                return false;
            }
            if (received > 0) {
                if (buf[0] == 0xFE) {
                    if (goodIdx == 1) {
                        break;
                    } else {
                        goodIdx++;
                    }
                } else {
                    goodIdx = 0;
                }
            }
            //Thread.Sleep(10);
            if (time_count-- <= 0) {
                xdata.error_msg = "Read 0xFE time out ";
                Log.i("Socket", "Read 0xFE time out");
                return false;
            }
        }

        //取条码长度
        byte[] temp_revbuf = new byte[18];
        int received_Total = 0;
        int remain_length = buf.length;//已收文件头被覆盖
        time_count = 100;
        while (true) {
            if (remain_length > 0) {
                try {
                    received = netWStream.read(temp_revbuf, 0, remain_length);
                    //Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                    System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                    received_Total += received;
                    remain_length -= received;
                } catch (Exception ex) {
                    Log.i("Socket", "Read Header Exception");
                    xdata.error_msg = "Read Header Exception " + ex.getMessage();
                    return false;
                }
            } else {
                Log.i("Socket", "received_Total == " + received_Total);
                break;
            }
//            Thread.Sleep(10);
            if (time_count-- <= 0) {
                Log.i("Socket", "Read Header time out");
                xdata.error_msg = "Read Header time out ";
                return false;
            }
        }

        if (received_Total != buf.length) {
            Log.i("Socket", "received_Total != 18");
            xdata.error_msg = "received_Total != 18";
            return false;
        }

        //读条码
        byte[] bufbarcodelen = new byte[4];
        // TODO: 2018/6/1  Buffer.BlockCopy什么意思
        //Buffer.BlockCopy(buf, 14, bufbarcodelen, 0, 4);
        System.arraycopy(buf, 14, bufbarcodelen, 0, 4);
//        int barcodelen = Convert.ToInt32(ConvertByteArrayToLong(bufbarcodelen));
        int barcodelen = Integer.parseInt(String.valueOf(bytesToLong(buf)));

        if (barcodelen > 0) {
            buf = new byte[barcodelen];
            temp_revbuf = new byte[barcodelen];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        received = netWStream.read(temp_revbuf, 0, remain_length);
                        //Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    } catch (Exception ex) {
                        Log.i("Socket", "Read barcodelen Exception " + ex.getMessage());
                        xdata.error_msg = "Read barcodelen Exception " + ex.getMessage();
                        return false;
                    }
                } else {
                    Log.i("Socket", "received_Total == " + received_Total);
                    break;
                }
//                Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.i("Socket", "Read barcodelen time out ");
                    xdata.error_msg = "Read barcodelen time out ";
                    return false;
                }
            }

//            xdata.barcode = ASCIIEncoding.UTF8.GetString(buf);//解析到的条码
            try {
                xdata.barcode = new String(buf, "utf-8");//解析到的条码
            } catch (UnsupportedEncodingException e) {
                Log.i("Socket", "string utf-8 解析条码失败 ");
                e.printStackTrace();
            }
        }

        //取图片名称长度
        buf = new byte[4];
        temp_revbuf = new byte[4];
        received_Total = 0;
        remain_length = buf.length;//已收文件头被覆盖
        time_count = 100;
        while (true) {
            if (remain_length > 0) {
                try {
                    received = netWStream.read(temp_revbuf, 0, remain_length);
                    //Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                    System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                    received_Total += received;
                    remain_length -= received;
                } catch (Exception ex) {
                    //LogWrite("Read imagenamelen Exception " + ex.ToString());
                    xdata.error_msg = "Read imagenamelen Exception " + ex.getMessage();
                    return false;
                }
            } else {
                //LogWrite("received_Total == " + received_Total.ToString());
                break;
            }
//            Thread.Sleep(10);
            if (time_count-- <= 0) {
//                LogWrite("Read imagenamelen time out ");
                xdata.error_msg = "Read imagenamelen time out ";
                return false;
            }
        }

        if (received_Total != buf.length) {
//            LogWrite("received_Total != 4");
            xdata.error_msg = "received_Total != 4";
            return false;
        }

        //读图片名称
//        int imagenamelen = Convert.ToInt32(ConvertByteArrayToLong(buf));
        int imagenamelen = Integer.parseInt(String.valueOf(bytesToLong(buf)));

        if (imagenamelen > 0) {
            buf = new byte[imagenamelen];
            temp_revbuf = new byte[imagenamelen];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        received = netWStream.read(temp_revbuf, 0, remain_length);
//                        Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    } catch (Exception ex) {
                        Log.i("Socket", "Read imagename Exception " + ex.getMessage());
                        xdata.error_msg = "Read imagename Exception " + ex.getMessage();
                        return false;
                    }
                } else {
                    Log.i("Socket", "received_Total == " + received_Total);
                    break;
                }
//                Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.i("Socket", "Read imagename time out ");
                    xdata.error_msg = "Read imagename time out ";
                    return false;
                }
            }

//            xdata.imgname = ASCIIEncoding.UTF8.GetString(buf);//解析到的图片名称
            try {
                xdata.imgname = new String(buf, "utf-8");//解析到的图片名称
            } catch (UnsupportedEncodingException e) {
                Log.i("Socket", "string utf-8 解析图片名称失败 ");
                e.printStackTrace();
            }
        }

        //region 取图片长度
        buf = new byte[4];
        temp_revbuf = new byte[4];
        received_Total = 0;
        remain_length = buf.length;//已收文件头被覆盖
        time_count = 100;
        while (true) {
            if (remain_length > 0) {
                try {
                    received = netWStream.read(temp_revbuf, 0, remain_length);
                    //Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                    System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                    received_Total += received;
                    remain_length -= received;
                } catch (Exception ex) {
                    Log.i("Socket", "Read imagelen Exception " + ex.getMessage());
                    xdata.error_msg = "Read imagelen Exception " + ex.getMessage();
                    return false;
                }
            } else {
                Log.i("Socket", "received_Total == " + received_Total);
                break;
            }
//            Thread.Sleep(10);
            if (time_count-- <= 0) {
                Log.i("Socket", "Read imagelen time out ");
                xdata.error_msg = "Read imagelen time out ";
                return false;
            }
        }

        if (received_Total != buf.length) {
            Log.i("Socket", "received_Total != 4");
            xdata.error_msg = "received_Total != 4";
            return false;
        }

        //读图片数据
//        long imagedatalen = Convert.ToInt32(ConvertByteArrayToLong(buf));
        int imagedatalen = Integer.parseInt(String.valueOf(bytesToLong(buf)));

        if (imagedatalen > 0) {
            buf = new byte[imagedatalen];
            temp_revbuf = new byte[imagedatalen];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        received = netWStream.read(temp_revbuf, 0, remain_length);
                        //Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    } catch (Exception ex) {
                        Log.i("Socket", "Read imagedata Exception " + ex.getMessage());
                        xdata.error_msg = "Read imagedata Exception " + ex.getMessage();
                        return false;
                    }
                } else {
                    Log.i("Socket", "received_Total == " + received_Total);
                    break;
                }
//                Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.i("Socket", "Read imagedata time out ");
                    xdata.error_msg = "Read imagedata time out ";
                    return false;
                }
            }

            // TODO: 2018/6/1 将byte数组转成image图片
//            xdata.img = Image.FromStream(new MemoryStream(buf));//图片
            if (buf.length != 0) {
                xdata.bitmap = BitmapFactory.decodeByteArray(buf, 0, buf.length);
            }
        }

        //取扫描时间，校验码
        buf = new byte[19];
        temp_revbuf = new byte[19];
        received_Total = 0;
        remain_length = buf.length;//已收文件头被覆盖
        time_count = 100;
        while (true) {
            if (remain_length > 0) {
                try {
                    received = netWStream.read(temp_revbuf, 0, remain_length);
//                    Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                    System.arraycopy(temp_revbuf, 0, buf, received_Total, received);
                    received_Total += received;
                    remain_length -= received;
                } catch (Exception ex) {
                    Log.i("Socket", "Read scantime Exception " + ex.getMessage());
                    xdata.error_msg = "Read scantime Exception " + ex.getMessage();
                    return false;
                }
            } else {
                Log.i("Socket", "received_Total == " + received_Total);
                break;
            }
//            Thread.Sleep(10);
            if (time_count-- <= 0) {
                Log.i("Socket", "Read scantime time out ");
                xdata.error_msg = "Read scantime time out ";
                return false;
            }
        }

        if (received_Total != buf.length) {
            Log.i("Socket", "received_Total != 19");
            xdata.error_msg = "received_Total != 19";
            return false;
        }
        xdata.scantime = new String(buf);
//        xdata.scantime = Encoding.Default.GetString(buf);
        //DateTime.TryParseExact(ASCIIEncoding.UTF8.GetString(buf), "yyyyMMddHHmmssfff", CultureInfo.CurrentCulture, DateTimeStyles.None, out xdata.scantime);
        return true;
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

    class CodeListAdapter extends BaseQuickAdapter<CodeItem, BaseViewHolder> {

        public CodeListAdapter(@Nullable List<CodeItem> data) {
            super(data);
            mLayoutResId = android.R.layout.simple_list_item_1;
        }

        @Override
        protected void convert(BaseViewHolder helper, CodeItem item) {
            helper.setText(android.R.id.text1, item.barcode);
        }
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }
}
