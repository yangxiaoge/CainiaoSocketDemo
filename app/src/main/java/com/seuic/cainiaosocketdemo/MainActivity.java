package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.seuic.cainiaosocketdemo.util.BytesHexStrTranslate;
import com.seuic.cainiaosocketdemo.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
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
    private XData xdata1 = new XData();
    private XData xdata2 = new XData();
    private XData xdata3 = new XData();
    private Socket socket1;
    private Socket socket2;
    private Socket socket3;
    private InputStream inputStream1;
    private InputStream inputStream2;
    private InputStream inputStream3;
    private boolean isEnableDeal = true;//默认可以解析
    private boolean isEnableDea2 = true;//默认可以解析
    private boolean isEnableDea3 = true;//默认可以解析
    private SharedPreferences sharedPreferences;
    private String ips = "192.168.80.64,192.168.80.64,192.168.80.64"; //169.254.173.207,169.254.222.233
    private String[] ipArray;

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

    private void socketClient1() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    progressDialog.setTitle("启动中");
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Log.e("socket1", "socketClient1");
        while (socket1 == null) {
            try {
                socket1 = new Socket(ipArray[0],
                        Integer.parseInt(sharedPreferences.getString(Constants.PORT, "7777")));
                toast("连接成功");
                isEnableDeal = true;
            } catch (IOException e) {
                isEnableDeal = false;
                SystemClock.sleep(1000);
                System.out.println("（暂时没有处理）连接服务端失败，重试中retry...");

                if (e instanceof SocketTimeoutException) {
                    toast("连接超时，正在重连");
                    releaseSocket1();
                } else if (e instanceof NoRouteToHostException) {
                    toast("该地址不存在，请检查");
                    //stopSelf();
                } else if (e instanceof ConnectException) {
                    toast("连接异常或被拒绝，请检查");
                    //stopSelf();
                    releaseSocket1();
                }

            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }

            //接受服务器数据
            try {
                if (socket1 == null || !socket1.isConnected()) return;
                inputStream1 = socket1.getInputStream();
                while (isEnableDeal) {
                    boolean getSuccess = dealWithData(inputStream1, xdata1);
                    if (getSuccess) {
                        System.out.println("成功啦成功啦成功啦成功啦成功啦 xdata1.imgname = " + xdata1.imgname);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("socket1", "data size1 =  " + data.size());
                                //UI线程刷新条码区域
                                codeListUpdate(xdata1.barcode);
                            }
                        });
                    } else {
                        break;
                    }

                    //测试socket1是否挂掉了
                    try {
                        socket1.sendUrgentData(111);
                    } catch (IOException e) {
                        isEnableDeal = false;
                        releaseSocket1();
                        e.printStackTrace();
                    }
                }
                inputStream1.close();
                socket1.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void socketClient2() {
        Log.e("socket2", "socketClient2");
        while (socket2 == null) {
            try {
                socket2 = new Socket(ipArray[1],
                        Integer.parseInt(sharedPreferences.getString(Constants.PORT, "7777")));
                toast("连接成功");
                isEnableDea2 = true;
            } catch (IOException e) {
                isEnableDea2 = false;
                SystemClock.sleep(1000);
                System.out.println("（暂时没有处理）连接服务端失败，重试中retry...");

                if (e instanceof SocketTimeoutException) {
                    toast("连接超时，正在重连");
                    releaseSocket2();
                } else if (e instanceof NoRouteToHostException) {
                    toast("该地址不存在，请检查");
                    //stopSelf();
                } else if (e instanceof ConnectException) {
                    toast("连接异常或被拒绝，请检查");
                    //stopSelf();
                    releaseSocket2();
                }

            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }

            //接受服务器数据
            try {
                if (socket2 == null || !socket2.isConnected()) return;
                inputStream2 = socket2.getInputStream();
                while (isEnableDea2) {
                    boolean getSuccess = dealWithData2(inputStream2, xdata2);
                    if (getSuccess) {
                        System.out.println("成功啦成功啦成功啦成功啦成功啦 xdata2.imgname = " + xdata2.imgname);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("socket1", "data size1 =  " + data.size());
                                //UI线程刷新条码区域
                                codeListUpdate(xdata2.barcode);
                            }
                        });
                    } else {
                        break;
                    }

                    //测试socket2是否挂掉了
                    try {
                        socket2.sendUrgentData(222);
                    } catch (IOException e) {
                        isEnableDea2 = false;
                        releaseSocket2();
                        e.printStackTrace();
                    }
                }
                inputStream2.close();
                socket2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void socketClient3() {
        Log.e("socket3", "socketClient3");
        while (socket3 == null) {
            try {
                socket3 = new Socket(ipArray[2],
                        Integer.parseInt(sharedPreferences.getString(Constants.PORT, "7777")));
                toast("连接成功");
                isEnableDeal = true;
            } catch (IOException e) {
                isEnableDeal = false;
                SystemClock.sleep(1000);
                System.out.println("（暂时没有处理）连接服务端失败，重试中retry...");

                if (e instanceof SocketTimeoutException) {
                    toast("连接超时，正在重连");
                    releaseSocket3();
                } else if (e instanceof NoRouteToHostException) {
                    toast("该地址不存在，请检查");
                    //stopSelf();
                } else if (e instanceof ConnectException) {
                    toast("连接异常或被拒绝，请检查");
                    //stopSelf();
                    releaseSocket3();
                }

            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
            //接受服务器数据
            try {
                if (socket3 == null || !socket3.isConnected()) return;
                inputStream3 = socket3.getInputStream();
                while (isEnableDea3) {
                    boolean getSuccess = dealWithData3(inputStream3, xdata3);
                    if (getSuccess) {
                        System.out.println("成功啦成功啦成功啦成功啦成功啦 xdata3.imgname = " + xdata3.imgname);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("socket1", "data size1 =  " + data.size());
                                //UI线程刷新条码区域
                                codeListUpdate(xdata3.barcode);
                            }
                        });
                    } else {
                        break;
                    }

                    //测试socket3是否挂掉了
                    try {
                        socket3.sendUrgentData(333);
                    } catch (IOException e) {
                        isEnableDea3 = false;
                        releaseSocket3();
                        e.printStackTrace();
                    }
                }
                inputStream3.close();
                socket3.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 更新数据
     *
     * @param barcode
     */
    private void codeListUpdate(String barcode) {
        Log.e("socket1", "data size1 =  " + data.size());
        //UI线程刷新条码区域,去重
//        if (!data.contains(barcode)) {
        if (true) {
            data.add(barcode);
            codeListAdapter.notifyDataSetChanged();
        }
        Log.e("socket1", "data size2 =  " + data.size());
        //赋值
        currentCode.setText(barcode);
        countCode.setText(String.valueOf(data.size()));
    }

    /*释放资源*/
    private void releaseSocket1() {

        if (inputStream1 != null) {
            try {
                inputStream1.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream1 = null;
        }

        if (socket1 != null) {
            try {
                socket1.close();
            } catch (IOException e) {
            }
            socket1 = null;
        }
        socketClient1();
    }

    private void releaseSocket2() {

        if (inputStream2 != null) {
            try {
                inputStream2.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream2 = null;
        }

        if (socket2 != null) {
            try {
                socket2.close();
            } catch (IOException e) {
            }
            socket2 = null;
        }
        socketClient2();
    }

    private void releaseSocket3() {

        if (inputStream3 != null) {
            try {
                inputStream3.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream3 = null;
        }

        if (socket3 != null) {
            try {
                socket3.close();
            } catch (IOException e) {
            }
            socket3 = null;
        }
        socketClient3();
    }

    private boolean dealWithData(InputStream is, XData xdata1) {
        try {
            Log.e("socket1", "dealWithData开始解析 is.available() " + is.available());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //第一步
            byte[] buf = new byte[16]; //数据头->条码长度
            byte[] temp = new byte[16];//读取codeLength长度字节
            int count = 0;
            int received_Total = 0;
            int remain_length = temp.length;//已收文件头被覆盖
            int time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read 0xFEFE Exception " + ex.toString());
                        xdata1.error_msg = "Read 0xFEFE Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "Read 0xFEFE time outreceived_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }
            }
            //数据头不匹配
            if (!BytesHexStrTranslate.bytesToHexString(buf, 2).equals("FEFE")) {
                Log.e("socket1", "Read 0xFEFE time out");
                return false;
            }
            Log.i("socket1", "读取数据头成功");
            //读取机器序列号
            byte[] deviceNum = new byte[12];
            System.arraycopy(buf, 4, deviceNum, 0, 12);
            //解析到的条码
            xdata1.deviceNum = new String(deviceNum, "utf-8");

            //第二步，条码长度,4
            //12000000(服务器从高到低，需要转成00000012从低到高)
            temp = new byte[4];
            count = is.read(temp, 0, 4);
            System.arraycopy(temp, 0, buf, 0, 4);
            if (count != 4) return false;
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];

            //条码长度
            long codeLength = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket1", "条码长度codeBytes = " + BytesHexStrTranslate.Bytes2HexString(temp));
            Log.i("socket1", "条码长度 = " + codeLength);

            //第三步，取条码,上面已经算出长度(18)
            //564533383137313230303534362d312d312d
            buf = new byte[(int) codeLength];
            temp = new byte[(int) codeLength];//读取codeLength长度字节
            received_Total = 0;
            remain_length = temp.length;//已收文件头被覆盖
            time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, (int) codeLength);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read barcodelen Exception " + ex.toString());
                        xdata1.error_msg = "Read barcodelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "barcodelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }

                //解析到的条码
                xdata1.barcode = new String(buf, "utf-8");
            }

            Log.i("socket1", "读取条码成功 barcode =" + xdata1.barcode);

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
                        Log.e("socket1", "Read imagenamelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagenamelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagenamelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagenamelen time out ");
                    xdata1.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagenamelen received_Total != 4");
                xdata1.error_msg = "imagenamelen received_Total != 4";
                return false;
            }
            Log.i("socket1", "读图片名称长度成功");

            //读图片名称长度,长度为buf数组的十进制值
            //2f000000(服务器从高到低，需要转成0000002f从低到高)
            temp = new byte[4];
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];
            //图片名称长度,
            //int imagenamelen = BytesHexStrTranslate.byteArrayToInt(temp);
            long longLen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读图片名称长度成功 imagenamelen = " + longLen);

            //第五步，读图片名称
            if (longLen > 0) {

                buf = new byte[(int) longLen];
                temp = new byte[(int) longLen];
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
                            Log.e("socket1", "Read imagename Exception " + ex.toString());
                            xdata1.error_msg = "Read imagename Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.e("socket1", "imagename received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket1", "Read imagename time out ");
                        xdata1.error_msg = "Read imagename time out ";
                        return false;
                    }
                }
                //解析到的图片名称
                xdata1.imgname = new String(buf, "utf-8");
            }

            Log.i("socket1", "读图片名称成功 imgname = " + xdata1.imgname);

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
                        Log.e("socket1", "Read imagelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagelen received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagelen time out ");
                    xdata1.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagelen received_Total != 4");
                xdata1.error_msg = "imagelen received_Total != 4";
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
            int imagedatalen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读取图片长度成功 图片数据长度 imagedatalen = " + imagedatalen);

            //第七步，读图片数据,可能数组过长导致内存溢出，可考虑写入io文件（FileHelper类提供byte[]写入文件）
            //int i = imagedatalen / 1024;
            int imageCount = 0; //已接收的字节数
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (imagedatalen - imageCount > 0) {
                buf = new byte[1024 * 10];
                int tempCount = 0;
                if (imagedatalen - imageCount < 1024 * 10) {
                    tempCount = is.read(buf, 0, imagedatalen - imageCount); //不足1024*10的时候不要读取过多字节流（可能把后面流也读了）
                } else {
                    tempCount = is.read(buf, 0, buf.length); //有可能读取的字节不是1024*10
                }
                imageCount += tempCount;
                bos.write(buf, 0, tempCount);

            }
            Log.i("socket1", "读图片数据成功 bos imagedatalen - imageCount = " + (imagedatalen - imageCount));
            Log.i("socket1", "读图片数据成功 bos length= " + bos.toByteArray().length);

            //第八步最后一步，取扫描时间，校验码，一共19字节
            buf = new byte[19];
            temp = new byte[19];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.out.println("取扫描时间1 count = " + count);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        System.out.println("取扫描时间1 = " + BytesHexStrTranslate.bytesToHexFun1(temp));
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read scantime Exception " + ex.toString());
                        xdata1.error_msg = "Read scantime Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "scantime received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read scantime time out ");
                    xdata1.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "scantime received_Total != 19");
                xdata1.error_msg = "scantime received_Total != 19";
                return false;
            }
            temp = new byte[17];
            System.arraycopy(buf, 0, temp, 0, 17); //只取时间17位
            xdata1.scantime = new String(temp, "utf-8");
            Log.i("socket1", "读图片数据成功 scantime = " + xdata1.scantime);

            final Bitmap imgBitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.toByteArray().length);
            Calendar calendar = Calendar.getInstance();  //获取当前时间，作为图标的名字
            saveImage(calendar, xdata1.barcode, xdata1.imgname, imgBitmap);
            savaBarcodeInfo(calendar, xdata1.barcode, xdata1.deviceNum, xdata1.imgname);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean dealWithData2(InputStream is, XData xdata1) {
        try {
            Log.e("socket1", "dealWithData2开始解析 is.available() " + is.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //第一步
            byte[] buf = new byte[16]; //数据头->条码长度
            byte[] temp = new byte[16];//读取codeLength长度字节
            int count = 0;
            int received_Total = 0;
            int remain_length = temp.length;//已收文件头被覆盖
            int time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read 0xFEFE Exception " + ex.toString());
                        xdata1.error_msg = "Read 0xFEFE Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "Read 0xFEFE time outreceived_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }
            }
            //数据头不匹配
            if (!BytesHexStrTranslate.bytesToHexString(buf, 2).equals("FEFE")) {
                Log.e("socket1", "Read 0xFEFE time out");
                return false;
            }
            Log.i("socket1", "读取数据头成功");
            //读取机器序列号
            byte[] deviceNum = new byte[12];
            System.arraycopy(buf, 4, deviceNum, 0, 12);
            //解析到的条码
            xdata1.deviceNum = new String(deviceNum, "utf-8");

            //第二步，条码长度,4
            //12000000(服务器从高到低，需要转成00000012从低到高)
            temp = new byte[4];
            count = is.read(temp, 0, 4);
            System.arraycopy(temp, 0, buf, 0, 4);
            if (count != 4) return false;
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];

            //条码长度
            long codeLength = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket1", "条码长度codeBytes = " + BytesHexStrTranslate.Bytes2HexString(temp));
            Log.i("socket1", "条码长度 = " + codeLength);

            //第三步，取条码,上面已经算出长度(18)
            //564533383137313230303534362d312d312d
            buf = new byte[(int) codeLength];
            temp = new byte[(int) codeLength];//读取codeLength长度字节
            received_Total = 0;
            remain_length = temp.length;//已收文件头被覆盖
            time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, (int) codeLength);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read barcodelen Exception " + ex.toString());
                        xdata1.error_msg = "Read barcodelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "barcodelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }

                //解析到的条码
                xdata1.barcode = new String(buf, "utf-8");
            }

            Log.i("socket1", "读取条码成功 barcode =" + xdata1.barcode);

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
                        Log.e("socket1", "Read imagenamelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagenamelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagenamelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagenamelen time out ");
                    xdata1.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagenamelen received_Total != 4");
                xdata1.error_msg = "imagenamelen received_Total != 4";
                return false;
            }
            Log.i("socket1", "读图片名称长度成功");

            //读图片名称长度,长度为buf数组的十进制值
            //2f000000(服务器从高到低，需要转成0000002f从低到高)
            temp = new byte[4];
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];
            //图片名称长度,
            //int imagenamelen = BytesHexStrTranslate.byteArrayToInt(temp);
            long longLen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读图片名称长度成功 imagenamelen = " + longLen);

            //第五步，读图片名称
            if (longLen > 0) {

                buf = new byte[(int) longLen];
                temp = new byte[(int) longLen];
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
                            Log.e("socket1", "Read imagename Exception " + ex.toString());
                            xdata1.error_msg = "Read imagename Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.e("socket1", "imagename received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket1", "Read imagename time out ");
                        xdata1.error_msg = "Read imagename time out ";
                        return false;
                    }
                }
                //解析到的图片名称
                xdata1.imgname = new String(buf, "utf-8");
            }

            Log.i("socket1", "读图片名称成功 imgname = " + xdata1.imgname);

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
                        Log.e("socket1", "Read imagelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagelen received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagelen time out ");
                    xdata1.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagelen received_Total != 4");
                xdata1.error_msg = "imagelen received_Total != 4";
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
            int imagedatalen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读取图片长度成功 图片数据长度 imagedatalen = " + imagedatalen);

            //第七步，读图片数据,可能数组过长导致内存溢出，可考虑写入io文件（FileHelper类提供byte[]写入文件）
            //int i = imagedatalen / 1024;
            int imageCount = 0; //已接收的字节数
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (imagedatalen - imageCount > 0) {
                buf = new byte[1024 * 10];
                int tempCount = 0;
                if (imagedatalen - imageCount < 1024 * 10) {
                    tempCount = is.read(buf, 0, imagedatalen - imageCount); //不足1024*10的时候不要读取过多字节流（可能把后面流也读了）
                } else {
                    tempCount = is.read(buf, 0, buf.length); //有可能读取的字节不是1024*10
                }
                imageCount += tempCount;
                bos.write(buf, 0, tempCount);

            }
            Log.i("socket1", "读图片数据成功 bos imagedatalen - imageCount = " + (imagedatalen - imageCount));
            Log.i("socket1", "读图片数据成功 bos length= " + bos.toByteArray().length);

            //第八步最后一步，取扫描时间，校验码，一共19字节
            buf = new byte[19];
            temp = new byte[19];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.out.println("取扫描时间1 count = " + count);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        System.out.println("取扫描时间1 = " + BytesHexStrTranslate.bytesToHexFun1(temp));
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read scantime Exception " + ex.toString());
                        xdata1.error_msg = "Read scantime Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "scantime received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read scantime time out ");
                    xdata1.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "scantime received_Total != 19");
                xdata1.error_msg = "scantime received_Total != 19";
                return false;
            }
            temp = new byte[17];
            System.arraycopy(buf, 0, temp, 0, 17); //只取时间17位
            xdata1.scantime = new String(temp, "utf-8");
            Log.i("socket1", "读图片数据成功 scantime = " + xdata1.scantime);

            final Bitmap imgBitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.toByteArray().length);
            Calendar calendar = Calendar.getInstance();  //获取当前时间，作为图标的名字
            saveImage(calendar, xdata1.barcode, xdata1.imgname, imgBitmap);
            savaBarcodeInfo(calendar, xdata1.barcode, xdata1.deviceNum, xdata1.imgname);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean dealWithData3(InputStream is, XData xdata1) {
        try {
            Log.e("socket1", "dealWithData3开始解析 is.available() " + is.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //第一步
            byte[] buf = new byte[16]; //数据头->条码长度
            byte[] temp = new byte[16];//读取codeLength长度字节
            int count = 0;
            int received_Total = 0;
            int remain_length = temp.length;//已收文件头被覆盖
            int time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read 0xFEFE Exception " + ex.toString());
                        xdata1.error_msg = "Read 0xFEFE Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "Read 0xFEFE time outreceived_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }
            }
            //数据头不匹配
            if (!BytesHexStrTranslate.bytesToHexString(buf, 2).equals("FEFE")) {
                Log.e("socket1", "Read 0xFEFE time out");
                return false;
            }
            Log.i("socket1", "读取数据头成功");
            //读取机器序列号
            byte[] deviceNum = new byte[12];
            System.arraycopy(buf, 4, deviceNum, 0, 12);
            //解析到的条码
            xdata1.deviceNum = new String(deviceNum, "utf-8");

            //第二步，条码长度,4
            //12000000(服务器从高到低，需要转成00000012从低到高)
            temp = new byte[4];
            count = is.read(temp, 0, 4);
            System.arraycopy(temp, 0, buf, 0, 4);
            if (count != 4) return false;
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];

            //条码长度
            long codeLength = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket1", "条码长度codeBytes = " + BytesHexStrTranslate.Bytes2HexString(temp));
            Log.i("socket1", "条码长度 = " + codeLength);

            //第三步，取条码,上面已经算出长度(18)
            //564533383137313230303534362d312d312d
            buf = new byte[(int) codeLength];
            temp = new byte[(int) codeLength];//读取codeLength长度字节
            received_Total = 0;
            remain_length = temp.length;//已收文件头被覆盖
            time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, (int) codeLength);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read barcodelen Exception " + ex.toString());
                        xdata1.error_msg = "Read barcodelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "barcodelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xdata1.error_msg = "Read barcode time out ";
                    return false;
                }

                //解析到的条码
                xdata1.barcode = new String(buf, "utf-8");
            }

            Log.i("socket1", "读取条码成功 barcode =" + xdata1.barcode);

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
                        Log.e("socket1", "Read imagenamelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagenamelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagenamelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagenamelen time out ");
                    xdata1.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagenamelen received_Total != 4");
                xdata1.error_msg = "imagenamelen received_Total != 4";
                return false;
            }
            Log.i("socket1", "读图片名称长度成功");

            //读图片名称长度,长度为buf数组的十进制值
            //2f000000(服务器从高到低，需要转成0000002f从低到高)
            temp = new byte[4];
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];
            //图片名称长度,
            //int imagenamelen = BytesHexStrTranslate.byteArrayToInt(temp);
            long longLen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读图片名称长度成功 imagenamelen = " + longLen);

            //第五步，读图片名称
            if (longLen > 0) {

                buf = new byte[(int) longLen];
                temp = new byte[(int) longLen];
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
                            Log.e("socket1", "Read imagename Exception " + ex.toString());
                            xdata1.error_msg = "Read imagename Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.e("socket1", "imagename received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket1", "Read imagename time out ");
                        xdata1.error_msg = "Read imagename time out ";
                        return false;
                    }
                }
                //解析到的图片名称
                xdata1.imgname = new String(buf, "utf-8");
            }

            Log.i("socket1", "读图片名称成功 imgname = " + xdata1.imgname);

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
                        Log.e("socket1", "Read imagelen Exception " + ex.toString());
                        xdata1.error_msg = "Read imagelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "imagelen received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagelen time out ");
                    xdata1.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagelen received_Total != 4");
                xdata1.error_msg = "imagelen received_Total != 4";
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
            int imagedatalen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket1", "读取图片长度成功 图片数据长度 imagedatalen = " + imagedatalen);

            //第七步，读图片数据,可能数组过长导致内存溢出，可考虑写入io文件（FileHelper类提供byte[]写入文件）
            //int i = imagedatalen / 1024;
            int imageCount = 0; //已接收的字节数
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (imagedatalen - imageCount > 0) {
                buf = new byte[1024 * 10];
                int tempCount = 0;
                if (imagedatalen - imageCount < 1024 * 10) {
                    tempCount = is.read(buf, 0, imagedatalen - imageCount); //不足1024*10的时候不要读取过多字节流（可能把后面流也读了）
                } else {
                    tempCount = is.read(buf, 0, buf.length); //有可能读取的字节不是1024*10
                }
                imageCount += tempCount;
                bos.write(buf, 0, tempCount);

            }
            Log.i("socket1", "读图片数据成功 bos imagedatalen - imageCount = " + (imagedatalen - imageCount));
            Log.i("socket1", "读图片数据成功 bos length= " + bos.toByteArray().length);

            //第八步最后一步，取扫描时间，校验码，一共19字节
            buf = new byte[19];
            temp = new byte[19];
            received_Total = 0;
            remain_length = buf.length;//已收文件头被覆盖
            time_count = 100;
            while (true) {
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.out.println("取扫描时间1 count = " + count);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        System.out.println("取扫描时间1 = " + BytesHexStrTranslate.bytesToHexFun1(temp));
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read scantime Exception " + ex.toString());
                        xdata1.error_msg = "Read scantime Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.e("socket1", "scantime received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read scantime time out ");
                    xdata1.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "scantime received_Total != 19");
                xdata1.error_msg = "scantime received_Total != 19";
                return false;
            }
            temp = new byte[17];
            System.arraycopy(buf, 0, temp, 0, 17); //只取时间17位
            xdata1.scantime = new String(temp, "utf-8");
            Log.i("socket1", "读图片数据成功 scantime = " + xdata1.scantime);

            final Bitmap imgBitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.toByteArray().length);
            Calendar calendar = Calendar.getInstance();  //获取当前时间，作为图标的名字
            saveImage(calendar, xdata1.barcode, xdata1.imgname, imgBitmap);
            savaBarcodeInfo(calendar, xdata1.barcode, xdata1.deviceNum, xdata1.imgname);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存bitmap图片到本地文件
     *
     * @param imgName
     * @param bmp
     */
    private void saveImage(Calendar calendar, String barcode, String imgName, final Bitmap bmp) {
        if (data.contains(barcode)) return;//已存在

        String year = calendar.get(Calendar.YEAR) + "";
        String month = calendar.get(Calendar.MONTH) + 1 + "";
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        String hour = calendar.get(Calendar.HOUR_OF_DAY) + "";
        String fileName = imgName;
        String path = Environment.getExternalStorageDirectory() + File.separator + "CaiNiaoBarcode" +
                File.separator + year + File.separator + month + File.separator + day + File.separator + hour;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final File file = new File(path, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Glide.with(MainActivity.this)
                            .load(file)
                            .centerCrop()
                            //.placeholder(R.mipmap.ic_launcher) //占位图
                            .error(R.mipmap.ic_launcher) //出错时
                            .into(mImageview);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("socket1", "IOException e = " + e.getMessage());
        }
    }

    /**
     * 存储条码信息
     *
     * @param calendar  当前系统时间
     * @param barcode   条码
     * @param deviceNum 设备序列号
     * @param imgName   图片名称
     */
    private synchronized void savaBarcodeInfo(Calendar calendar, String barcode, String deviceNum, String imgName) {
        if (data.contains(barcode)) return; //已存在
        String year = calendar.get(Calendar.YEAR) + "";
        String month = calendar.get(Calendar.MONTH) + 1 + "";
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        String hour = calendar.get(Calendar.HOUR_OF_DAY) + "";
        String time = year + month + day + hour;
        //拼接图片信息内容
        String content = time + "," + barcode + "," + deviceNum + "," + imgName;
        String fileName = time + ".txt";
        //对应图片目录下的条码信息目录
        String path = Environment.getExternalStorageDirectory() + File.separator + "CaiNiaoBarcode" +
                File.separator + year + File.separator + month + File.separator + day + File.separator + hour;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final File file = new File(path, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.write("\r\n".getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("socket1", "IOException e = " + e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        SocketBarcodeUtil instance = SocketBarcodeUtil.getInstance();
        switch (v.getId()) {
            case R.id.start_socket:
                //开启
                instance.startLink(new SocketBarcodeUtil.SocketCallback() {
                    @Override
                    public void startStatus(boolean status) {
                        Log.d("MainActivity", "开启关闭状态 status:" + status);
                    }

                    @Override
                    public void barcode(String barcode) {
                        Log.d("MainActivity", "二维码 barcode:" + barcode);
                    }
                }, sharedPreferences.getString(Constants.IP, Constants.ipsDefault));
                break;
            case R.id.stop_socket:
                //关闭
                instance.stopLink();
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
        isEnableDeal = false;
        isEnableDea2 = false;
        isEnableDea3 = false;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (socket1 != null) {
            try {
                socket1.shutdownInput();
                socket1.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket2 != null) {
            try {
                socket2.shutdownInput();
                socket2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket3 != null) {
            try {
                socket3.shutdownInput();
                socket3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
