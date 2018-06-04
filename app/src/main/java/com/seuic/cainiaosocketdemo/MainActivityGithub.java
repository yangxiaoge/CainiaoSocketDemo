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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangjianan on 2018/6/1.
 */
public class MainActivityGithub extends AppCompatActivity implements View.OnClickListener {
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
                    Toast.makeText(MainActivityGithub.this, "连接成功", Toast.LENGTH_SHORT).show();
                }
                Log.i("Socket", "已连接onConnected:");
                //socketClient.send("Android 你好");
                //socketClient.setHeartBeatMessage("hello, server !");
                //socketClient.sendString("string数据");
            }

            @Override
            public void onDisconnected(SocketClient client) {
                Log.i("Server", "onDisconnected超时timeout: = " +    client.getHeartBeatInterval() + " "+client.getState()+"");
                //连接终端，可以给一些页面提示
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivityGithub.this, "连接超时", Toast.LENGTH_SHORT).show();
                }
                String error = client.getCharsetName();
                Log.i("Server", "onDisconnected超时timeout:" + error);
                forIndex = 0;

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
                byte[] datas = responsePacket.getData();
                System.out.println(new String(datas, Charset.forName("utf-8")));
                Log.i("Socket", "响应字节：datas.size = " + datas.length);
                Log.i("Socket", "响应字节：" + BytesHexStrTranslate.bytesToHexFun1(datas));
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(datas);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);

                //如果是服务器返回“ok”，不需要解析
                if(datas.length==2&&BytesHexStrTranslate.bytesToHexFun1(datas).equals("6f6b")){
                    return;
                }
                //解析字节流数据
                parseData(datas.length, byteArrayInputStream);

                byte data = datas[0];
                byte[] bytes = new byte[1];
                bytes[0] = data;
                if (BytesHexStrTranslate.bytesToHexFun1(bytes).equals("fe")) {

                }
            }
        });

        socketClient.setConnectionTimeout(1000 * 15);
        socketClient.setHeartBeatInterval(1000);
//        socketClient.disableHeartBeat(); //禁止心跳,否则后台一直返回ok成功提示
//        socketClient.setRemoteNoReplyAliveTimeout(1000 * 60); //远程端在一定时间间隔没有消息后自动断开
        socketClient.setCharsetName("UTF-8");
        socketClient.connect();
    }

    private int forIndex = 0; //第一次取到了数据头并且是FEFE

    private boolean parseData(int dataLength, ByteArrayInputStream netWStream) {
        xdata = new XData();
        byte[] buf = new byte[18];

        // check StartCode 检测数据包头0xFEFE
        int goodIdx = 0;
        int received = -1;
        int time_count = 100;
        System.out.println("buf[0]1 = " + buf[0]);
        System.out.println("buf[0]1 dataLength= " + dataLength);
       if (forIndex==0 && dataLength == 16){ //只有第一次判断头FEFE需要执行这个while
           while (true) {
               try {
                   received = netWStream.read(buf, 0, 1);
                   //System.out.println("buf[0]2 = "+buf[0]);
//                System.out.println("buf[0]2 new buf = "+BytesHexStrTranslate.bytesToHexFun1(buf));
                   //System.out.println("buf[0]2 new buf = " + BytesHexStrTranslate.bytesToHexFun2(buf));
               } catch (Exception ex) {
                   Log.i("Socket", "Read Exception " + ex.getMessage());
                   xdata.error_msg = "Read Exception " + ex.getMessage();
                   return false;
               }
               if (received > 0) {
                   if (buf[0] == -2) {
//                if (buf[0] == 254) {
                       if (goodIdx == 1) {
                           forIndex = 1;
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
       }

        if (dataLength == 16) return true;//第一次的数据长度是16，执行到这里说明数据头已取到,但是请求头数据不要往下解析

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
//        int barcodelen = Integer.parseInt(String.valueOf(bytesToLong(buf)));
        int barcodelen = BytesHexStrTranslate.bytes2int(buf);
        Log.i("Socket", "读条码 barcodelen = " + barcodelen);
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
        Log.i("Socket", "图片名称长度 = " + received_Total);

        Log.i("Socket", "读图片名称");
        //读图片名称
//        int imagenamelen = Convert.ToInt32(ConvertByteArrayToLong(buf));
//        int imagenamelen = Integer.parseInt(String.valueOf(bytesToLong(buf)));
        int imagenamelen =  BytesHexStrTranslate.bytes2int(buf);
        Log.i("Socket", "读图片名称 imagenamelen = "+imagenamelen);
        // TODO: 2018/6/4 这里会内存溢出
        /*if (imagenamelen > 0) {
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
        }*/

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
//        int imagedatalen = Integer.parseInt(String.valueOf(bytesToLong(buf)));
        int imagedatalen = BytesHexStrTranslate.bytes2int(buf);
        Log.i("Socket", "读图片数据 imagedatalen = "+imagedatalen);
        // TODO: 2018/6/4 内存溢出 byte长度太长了
        /*if (imagedatalen > 0) {
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
        }*/

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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        originSocket();
                    }
                }).start();
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

    private void originSocket() {
        // 客户端请求与本机在 20006 端口建立 TCP 连接
//        Socket client = new Socket("127.0.0.1", 20006);
        Socket client = null;
        try {
            client = new Socket("192.168.80.80", 7777);
            client.setSoTimeout(10000);
            // 获取 Socket 的输入流，用来接收从服务端发送过来的数据
            BufferedReader buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            boolean flag = true;
            while (flag) {
                try {
                    // 从服务器端接收数据有个时间限制（系统自设，也可以自己设置），超过了这个时间，便会抛出该异常
                    String echo = buf.readLine();
                    System.out.println(echo);
                    System.out.println(BytesHexStrTranslate.bytesToHexFun1(echo.getBytes()));
                    parseData(111111, new ByteArrayInputStream(echo.getBytes()));
                } catch (SocketTimeoutException e) {
                    System.out.println("Time out, No response");
                }

            }
            if (client != null) {
                // 如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
                client.close(); // 只关闭 socket，其关联的输入输出流也会被关闭
            }
        } catch (IOException e) {
            e.printStackTrace();
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
