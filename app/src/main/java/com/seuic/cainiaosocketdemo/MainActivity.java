package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.seuic.cainiaosocketdemo.util.BytesHexStrTranslate;
import com.seuic.cainiaosocketdemo.util.Data_syn;
import com.vilyever.socketclient.SocketClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    Socket socket;

    private void socketClient() {
        progressDialog.setTitle("启动中");
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress("192.168.80.80", 7777);
                    socket.connect(socketAddress, 10000);
                    InputStream is = socket.getInputStream();
                   /* InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader bf = new BufferedReader(isr);
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    String line = null;*/
                    while (true) {
                        boolean b = dealWithData(is);
                        if (b) {
                            System.out.println("成功啦成功啦成功啦成功啦成功啦 xdata.imgname = " + xdata.imgname);
                        } else {
                            System.out.println("失败啦失败啦失败啦 xdata.imgname = " + xdata.imgname);
                        }
                    }
                } catch (SocketException e) {
                    Log.e("socket", "socket连接失败1 e = " + e.getMessage());
                } catch (SocketTimeoutException e) {
                    Log.e("socket", "socket连接超时 e = " + e.getMessage());
                    toast("连接超时");
                } catch (IOException e) {
                    Log.e("socket", "socket连接失败3 e = " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            }
        }).start();
    }

    private boolean dealWithData(InputStream is) {
        xdata = new XData();

        byte[] buf = new byte[20]; //数据头->条码长度
        int count = 0;
        try {
            //第一步
            count = is.read(buf, 0, 20); //将输入流写入tmp字节数组,先取20长度
            //如果不是20，说明不是想要的数据，直接pass（可能是服务器的ok提示）
            if (count != 20) {
                return false;
            }
            //数据头不匹配
            if (!Data_syn.bytesToHexString(buf, 2).equals("FEFE")) {
                Log.e("socket", "Read 0xFEFE time out");
                return false;
            }
            Log.i("socket", "读取数据头成功");

            //第二步，条码长度,4
            //12000000(服务器从高到低，需要转成00000012从低到高)
            byte[] temp = new byte[4];
            temp[0] = buf[19];
            temp[1] = buf[18];
            temp[2] = buf[17];
            temp[3] = buf[16];
            //条码长度,18
            int codeLength = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket", "条码长度 = " + codeLength);

            //第三步，取条码,上面已经算出长度(18)
            //564533383137313230303534362d312d312d
            buf = new byte[codeLength];
            temp = new byte[codeLength];//读取codeLength长度字节
            int received_Total = 0;
            int remain_length = temp.length;//已收文件头被覆盖
            int time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, codeLength);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket", "Read barcodelen Exception " + ex.toString());
                        xdata.error_msg = "Read barcodelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket", "barcodelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket", "Read barcode time out ");
                    xdata.error_msg = "Read barcode time out ";
                    return false;
                }

                //解析到的条码
                xdata.barcode = BytesHexStrTranslate.bytesToHexFun1(buf);
            }

            Log.i("socket", "读取条码成功 barcode =" + xdata.barcode);

            //第四步，读图片名称长度为4
            buf = new byte[4];
            temp = new byte[4];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket", "Read imagenamelen Exception " + ex.toString());
                        xdata.error_msg = "Read imagenamelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket", "imagenamelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket", "Read imagenamelen time out ");
                    xdata.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket", "imagenamelen received_Total != 4");
                xdata.error_msg = "imagenamelen received_Total != 4";
                return false;
            }
            Log.i("socket", "读图片名称长度成功");

            //读图片名称长度,长度为buf数组的十进制值
            //2f000000(服务器从高到低，需要转成0000002f从低到高)
            temp = new byte[4];
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];
            //图片名称长度,
            int imagenamelen = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket", "读图片名称长度成功 imagenamelen = " + imagenamelen);

            //第五步，读图片名称
            if (imagenamelen > 0) {

                buf = new byte[imagenamelen];
                temp = new byte[imagenamelen];
                received_Total = 0;
                remain_length = buf.length;//已收文件头被覆盖
                time_count = 100;
                while (true) {
                    if (remain_length > 0) {
                        try {
                            count = is.read(temp, 0, remain_length);
                            System.arraycopy(temp, 0, buf, received_Total, count);
                            received_Total += count;
                            remain_length -= count;
                        } catch (Exception ex) {
                            Log.e("socket", "Read imagename Exception " + ex.toString());
                            xdata.error_msg = "Read imagename Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.e("socket", "imagename received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket", "Read imagename time out ");
                        xdata.error_msg = "Read imagename time out ";
                        return false;
                    }
                }
                //解析到的图片名称
                xdata.imgname = BytesHexStrTranslate.bytesToHexFun1(buf);
            }

            Log.i("socket", "读图片名称成功 imgname = " + xdata.imgname);

            //第六步，读取图片长度，4字节
            buf = new byte[4];
            temp = new byte[4];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket", "Read imagelen Exception " + ex.toString());
                        xdata.error_msg = "Read imagelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket", "imagelen received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket", "Read imagelen time out ");
                    xdata.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket", "imagelen received_Total != 4");
                xdata.error_msg = "imagelen received_Total != 4";
                return false;
            }

            //读图片数据长度为buf数组的十进制值
            //2f000000(服务器从高到低，需要转成0000002f从低到高)
            temp = new byte[4];
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];
            //图片数据长度,字节数
            int imagedatalen = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket", "读取图片长度成功 图片数据长度 imagedatalen = " + imagedatalen);

            //第七步，读图片数据,可能数组过长导致内存溢出，可考虑写入io文件（FileHelper类提供byte[]写入文件）
            if (imagedatalen > 0) {
                buf = new byte[imagedatalen];
                temp = new byte[imagedatalen];
                received_Total = 0;
                remain_length = buf.length;//已收文件头被覆盖
                time_count = 100;
                while (true) {
                    if (remain_length > 0) {
                        try {
                            count = is.read(temp, 0, remain_length);
                            System.arraycopy(temp, 0, buf, received_Total, count);
                            received_Total += count;
                            remain_length -= count;
                        } catch (Exception ex) {
                            Log.e("socket", "Read imagedata Exception " + ex.toString());
                            xdata.error_msg = "Read imagedata Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.e("socket", "imagedata received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket", "Read imagedata time out ");
                        xdata.error_msg = "Read imagedata time out ";
                        return false;
                    }
                }

                xdata.bitmap = BytesHexStrTranslate.bytes2Bitmap(buf);//图片
            }
            Log.i("socket", "读图片数据成功");

            //第八步，取扫描时间，校验码，一共19字节
            buf = new byte[19];
            temp = new byte[19];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket", "Read scantime Exception " + ex.toString());
                        xdata.error_msg = "Read scantime Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket", "scantime received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket", "Read scantime time out ");
                    xdata.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket", "scantime received_Total != 19");
                xdata.error_msg = "scantime received_Total != 19";
                return false;
            }
            xdata.scantime = BytesHexStrTranslate.bytesToHexFun1(buf);
            Log.i("socket", "读图片数据成功 scantime = " + xdata.scantime + " 17位时间 = " + Data_syn.bytesToHexString(buf, 17));
            //DateTime.TryParseExact(ASCIIEncoding.UTF8.GetString(buf), "yyyyMMddHHmmssfff", CultureInfo.CurrentCulture, DateTimeStyles.None, out xdata.scantime);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseData(InputStream netWStream) {
        xdata = new XData();
        byte[] buf = new byte[18];
        // check StartCode 检测数据包头0xFEFE
        int goodIdx = 0;
        int received = -1;
        int time_count = 100;

        while (true) {
            try {
                received = netWStream.read(buf, 0, 1);
                //System.out.println("buf[0] = "+buf[0]);
            } catch (Exception ex) {
                Log.i("Socket", "Read Exception " + ex.getMessage());
                xdata.error_msg = "Read Exception " + ex.getMessage();
                return false;
            }
            if (received > 0) {
                //System.out.println("buf[0] = "+buf[0]);
                if (buf[0] == -2) {
//                if (buf[0] == 0xFE) {
//                if (buf[0] == 254) {
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

        Log.i("ssss", "开始取条码长度");
        //取条码长度
        byte[] temp_revbuf = new byte[18];
        int received_Total = 0;
        int remain_length = buf.length;//已收文件头被覆盖
        time_count = 100;
        while (true) {
            if (remain_length > 0) {
                try {
                    Toast.makeText(this, "取条码长度", Toast.LENGTH_SHORT).show();
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

        Log.i("ssss", "开始取条码");
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

        Log.i("ssss", "取图片名称长度");
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

        Log.i("ssss", "读图片名称");
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

        Log.i("ssss", "取图片长度");
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

        Log.i("ssss", "读图片数据");
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

        Log.i("ssss", "取扫描时间，校验码");
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
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                        Toast.makeText(this, "已关闭连接", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "关闭失败", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.clear_data:
                break;
            default:
        }
    }

/*    private void originSocket() {
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
                    parseData(new ByteArrayInputStream(echo.getBytes()));
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
    }*/

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
