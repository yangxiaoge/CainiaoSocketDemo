package com.seuic.cainiaosocketdemo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.seuic.cainiaosocketdemo.util.BytesHexStrTranslate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//import com.seuic.cainiaosocketdemo.util.BytesHexStrTranslate;

/**
 * Created by yangjianan on 2018/6/25.
 * 1，socket连接，断开
 * 2，字节流解析，条码/图片存储
 * 3，StartSocketCallback接口回调状态
 */
public class SocketBarcodeUtil_bakkkk {
    private XData xdata1 = new XData();
    private XData xdata2 = new XData();
    private XData xdata3 = new XData();
    private Socket socket1;
    private Socket socket2;
    private Socket socket3;
    private InputStream inputStream1;
    private InputStream inputStream2;
    private InputStream inputStream3;
    private String[] ipArray;
    // 单例模式
    private static SocketBarcodeUtil_bakkkk instance;
    // 全局的线程池
    private static Executor executor;
    private static final int PORT = 7777;
    private SocketCallback callback; //开启socket是否成功的回调
    private boolean connectSuccess1; //socket1连接成功
    private boolean connectSuccess2; //socket2连接成功
    private boolean connectSuccess3; //socket3连接成功
    //存储条码string集合，条码去重用
    private List<String> data = new ArrayList<>();
    //文件存储目录名称,客户可以自己定义
    public static String DIRNAME = "CaiNiaoBarcode";

    /**
     * 外部获取单例
     *
     * @return Application
     */
    public static SocketBarcodeUtil_bakkkk getInstance() {
        if (instance == null)
            instance = new SocketBarcodeUtil_bakkkk();

        if (executor == null)
            // 新建一个3个线程的线程池
            executor = Executors.newFixedThreadPool(3);
        return instance;
    }

    /**
     * 开启socket是否成功的回调(由调用方注册)
     */
    public interface SocketCallback {
        //目前只有boolean，后续可加入状态码，标志各种状态事件

        /**
         * 开启关闭状态,异常时为false
         *
         * @param status status
         */
        void startStatus(boolean status);

        /**
         * 回调二维码
         *
         * @param barcode barcode
         */
        void barcode(String barcode);
    }

    /**
     * 开启socket扫描解码方法
     *
     * @param inIps ip配置文件的字符串，ip之间以英文","分隔
     */
    public void startLink(SocketCallback startSocketCallback, String inIps) {
        callback = startSocketCallback;
//        String ips = FileIOUtils.readFile2String(filePath);
        String ips = inIps;
        ipArray = ips.split(",");
        if (ipArray.length >= 1 && (socket1 == null || !socket1.isConnected())) {
            executor.execute(runnable1);
        }

        if (ipArray.length >= 2 && (socket2 == null || !socket2.isConnected())) {
            executor.execute(runnable2);
        }

        if (ipArray.length == 3 && (socket3 == null || !socket3.isConnected())) {
            executor.execute(runnable3);
        }
    }

    /**
     * 关闭socket扫描解码方法
     */
    public void stopLink() {
        //socket1
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
                socket1.shutdownInput();
                socket1.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket1 = null;
        }

        //socket2
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
                socket2.shutdownInput();
                socket2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket2 = null;
        }

        //socket3
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
                socket3.shutdownInput();
                socket3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket3 = null;
        }

        //关闭后回调连接状态为false
        if (callback != null) {
            callback.startStatus(false);
            callback = null;
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mHeartBeatRunnable1);// 移除线程
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    /**
     * socket连接相机1
     */
    Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            Log.e("socket1", "启动socket1");
            socketClient1();
        }
    };

    /**
     * 连接socket1
     */
    private void socketClient1() {
        try {
            connectSuccess1 = true;
            // 创建Socket对象 & 指定服务端的IP 及 端口号
            //1、创建监听指定服务器地址以及指定服务器监听的端口号
            socket1 = new Socket(ipArray[0], PORT);
            // 判断客户端和服务器是否连接成功
            System.out.println("socket1连接" + socket1.isConnected());
            //2、拿到socket的输入流，这里存储的是服务器返回的数据
            inputStream1 = socket1.getInputStream();

            // 心跳检测，检测socket是否连接
            mHandler.postDelayed(mHeartBeatRunnable1, HEART_BEAT_RATE);

            //这个条件有待思考
            while (socket1 != null && !socket1.isClosed()) {
                boolean getSuccess = dealWithData(inputStream1, xdata1);
//                if (getSuccess && !data.contains(xdata1.barcode)) {
                if (getSuccess) {
                    data.add(xdata1.barcode);
                    if (callback != null)
                        callback.barcode(xdata1.barcode);
                }
            }

            //3、关闭IO资源（注：实际开发中需要放到finally中）
            if (inputStream1 != null) {
                inputStream1.close();
            }
            if (socket1 != null) {
                socket1.close();
            }

        } catch (IOException e) {
            connectSuccess1 = false;
            if (callback != null)
                callback.startStatus(false);
            e.printStackTrace();
        } finally {
            if (ipArray.length == 1 && connectSuccess1) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 2 && connectSuccess1 && connectSuccess2) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 3 && connectSuccess1 && connectSuccess2 && connectSuccess3) {
                if (callback != null)
                    callback.startStatus(true);
            }
        }
    }

    /**
     * socket连接相机2
     */
    Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            Log.e("socket2", "启动socket2");
            socketClient2();
        }
    };

    /**
     * 连接socket2
     */
    private void socketClient2() {
        try {
            connectSuccess2 = true;
            // 创建Socket对象 & 指定服务端的IP 及 端口号
            socket2 = new Socket(ipArray[1], PORT);
            // 判断客户端和服务器是否连接成功
            System.out.println("socket2连接" + socket2.isConnected());

            inputStream2 = socket2.getInputStream();

            //这个条件有待思考
            while (socket2 != null && !socket2.isClosed()) {
                boolean getSuccess = dealWithData(inputStream2, xdata2);
                //                if (getSuccess && !data.contains(xdata1.barcode)) {
                if (getSuccess) {

                    data.add(xdata2.barcode);
                    if (callback != null)
                        callback.barcode(xdata2.barcode);
                }
            }

            //3、关闭IO资源（注：实际开发中需要放到finally中）
            if (inputStream2 != null) {
                inputStream2.close();
            }
            if (socket2 != null) {
                socket2.close();
            }
        } catch (IOException e) {
            connectSuccess2 = false;
            if (callback != null)
                callback.startStatus(false);
            e.printStackTrace();
        } finally {
            if (ipArray.length == 1 && connectSuccess1) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 2 && connectSuccess1 && connectSuccess2) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 3 && connectSuccess1 && connectSuccess2 && connectSuccess3) {
                if (callback != null)
                    callback.startStatus(true);
            }
        }
    }

    /**
     * socket连接相机3
     */
    Runnable runnable3 = new Runnable() {
        @Override
        public void run() {
            Log.e("socket3", "启动socket3");
            socketClient3();
        }
    };

    /**
     * 连接socket3
     */
    private void socketClient3() {
        try {
            connectSuccess3 = true;
            // 创建Socket对象 & 指定服务端的IP 及 端口号
            socket3 = new Socket(ipArray[2], PORT);
            // 判断客户端和服务器是否连接成功
            System.out.println("socket3连接" + socket3.isConnected());

            inputStream3 = socket3.getInputStream();

            //这个条件有待思考
            while (socket3 != null && !socket3.isClosed()) {
                boolean getSuccess = dealWithData(inputStream3, xdata3);
                //                if (getSuccess && !data.contains(xdata1.barcode)) {
                if (getSuccess) {

                    data.add(xdata3.barcode);
                    if (callback != null)
                        callback.barcode(xdata3.barcode);
                }
            }

            //3、关闭IO资源（注：实际开发中需要放到finally中）
            if (inputStream3 != null) {
                inputStream3.close();
            }
            if (socket3 != null) {
                socket3.close();
            }
        } catch (IOException e) {
            connectSuccess3 = false;
            if (callback != null)
                callback.startStatus(false);
            e.printStackTrace();
        } finally {
            if (ipArray.length == 1 && connectSuccess1) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 2 && connectSuccess1 && connectSuccess2) {
                if (callback != null)
                    callback.startStatus(true);
            } else if (ipArray.length == 3 && connectSuccess1 && connectSuccess2 && connectSuccess3) {
                if (callback != null)
                    callback.startStatus(true);
            }
        }
    }

    //    private static final long HEART_BEAT_RATE = 4 * 1000;
    private static final long HEART_BEAT_RATE = 500;
    private long sendTime = 0L;
    private Runnable mHeartBeatRunnable1 = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {//每隔4秒检测一次
                boolean isSuccess = sendHeartBeatMsg("hb", socket1);
                if (!isSuccess) {
                    Log.i("mHeartBeatRunnable", "连接已断开，正在重连……");
                    mHandler.removeCallbacks(mHeartBeatRunnable1);// 移除线程，重连时保证该线程已停止上次调用时的工作
                    //连接失败
                    if (callback != null)
                        callback.startStatus(false);
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    /**
     * 发送心跳包
     *
     * @param msg
     * @return
     */
    public boolean sendHeartBeatMsg(String msg, final Socket mSocket) {
        if (null == mSocket) {
            return false;
        }
        try {
            if (!mSocket.isClosed() && !mSocket.isOutputShutdown()) {
                String message = msg + "";
                OutputStream mDataOutputStream = mSocket.getOutputStream();
                mDataOutputStream.write(message.getBytes());
                mDataOutputStream.flush();
                sendTime = System.currentTimeMillis();

            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 解析数据
     *
     * @param is    socket输入流
     * @param xData 数据实体类
     * @return 解析是否成功
     */
    private boolean dealWithData(InputStream is, XData xData) {
        try {
            //第一步
            byte[] buf = new byte[2]; //数据头->条码长度
            byte[] temp = new byte[2];//读取codeLength长度字节
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
                        xData.error_msg = "Read 0xFEFE Exception " + ex.toString();

                        //异常后需要关闭socket
                        stopLink();
                        return false;
                    }
                } else {
                    Log.i("socket1", "Read 0xFEFE time outreceived_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xData.error_msg = "Read barcode time out ";
                    return false;
                }
            }

            if (new String(buf, "utf-8").equalsIgnoreCase("ok")) {
                Log.e("socket1", "心跳 ok");
                return false;
            }
            //数据头不匹配
            if (!BytesHexStrTranslate.bytesToHexString(buf, 2).equals("FEFE")) {
                Log.e("socket1", "Read 0xFEFE time out");
                return false;
            }
            Log.i("socket1", "读取数据头成功");

            //版本号 + 机器序列号
            buf = new byte[14];
            temp = new byte[14];//读取codeLength长度字节
            received_Total = 0;
            remain_length = temp.length;//已收文件头被覆盖
            time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, remain_length);
                        System.arraycopy(temp, 0, buf, received_Total, count);
                        received_Total += count;
                        remain_length -= count;
                    } catch (Exception ex) {
                        Log.e("socket1", "Read 版本号机器序列号 Exception " + ex.toString());
                        xData.error_msg = "Read 版本号机器序列号 Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.i("socket1", "版本号机器序列号 received_Total == " + received_Total);
                    break;
                }

                if (time_count-- <= 0) {
                    Log.e("socket1", "Read 版本号机器序列号 time out ");
                    xData.error_msg = "Read 版本号机器序列号 time out ";
                    return false;
                }
            }

            //读取机器序列号
            byte[] deviceNum = new byte[12];
            System.arraycopy(buf, 2, deviceNum, 0, 12);
            //解析到的条码
            xData.deviceNum = new String(deviceNum, "utf-8");

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
                        xData.error_msg = "Read barcodelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.i("socket1", "barcodelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read barcode time out ");
                    xData.error_msg = "Read barcode time out ";
                    return false;
                }

                //解析到的条码
                xData.barcode = new String(buf, "utf-8");
            }

            Log.i("socket1", "读取条码成功 barcode =" + xData.barcode);

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
                        xData.error_msg = "Read imagenamelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.i("socket1", "imagenamelen received_Total == " + received_Total);
                    break;
                }
                //Thread.sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagenamelen time out ");
                    xData.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagenamelen received_Total != 4");
                xData.error_msg = "imagenamelen received_Total != 4";
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
                            xData.error_msg = "Read imagename Exception " + ex.toString();
                            return false;
                        }
                    } else {
                        Log.i("socket1", "imagename received_Total == " + received_Total);
                        break;
                    }
                    //Thread.Sleep(10);
                    if (time_count-- <= 0) {
                        Log.e("socket1", "Read imagename time out ");
                        xData.error_msg = "Read imagename time out ";
                        return false;
                    }
                }
                //解析到的图片名称
                xData.imgname = new String(buf, "utf-8");
            }

            Log.i("socket1", "读图片名称成功 imgname = " + xData.imgname);

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
                        xData.error_msg = "Read imagelen Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.i("socket1", "imagelen received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read imagelen time out ");
                    xData.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "imagelen received_Total != 4");
                xData.error_msg = "imagelen received_Total != 4";
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
                        xData.error_msg = "Read scantime Exception " + ex.toString();
                        return false;
                    }
                } else {
                    Log.i("socket1", "scantime received_Total == " + received_Total);
                    break;
                }
                //Thread.Sleep(10);
                if (time_count-- <= 0) {
                    Log.e("socket1", "Read scantime time out ");
                    xData.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.length) {
                Log.e("socket1", "scantime received_Total != 19");
                xData.error_msg = "scantime received_Total != 19";
                return false;
            }
            temp = new byte[17];
            System.arraycopy(buf, 0, temp, 0, 17); //只取时间17位
            xData.scantime = new String(temp, "utf-8");
            Log.i("socket1", "读图片数据成功 scantime = " + xData.scantime);

            final Bitmap imgBitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.toByteArray().length);
            Calendar calendar = Calendar.getInstance();  //获取当前时间，作为图标的名字
            saveImage(calendar, xData.barcode, xData.deviceNum, xData.imgname, imgBitmap);
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
    private synchronized void saveImage(Calendar calendar, String barcode, String deviceNum, String imgName, final Bitmap bmp) {
        // wrong的条码不需要过滤
        if (data.contains(barcode) && !barcode.equalsIgnoreCase("wrong")) return;//已存在
        System.out.println("saveImage保存图片1");
        String year = calendar.get(Calendar.YEAR) + "";
        String month = calendar.get(Calendar.MONTH) + 1 + "";
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        String hour = calendar.get(Calendar.HOUR_OF_DAY) + "";
        String fileName = imgName;
        String path = Environment.getExternalStorageDirectory() + File.separator + DIRNAME +
                File.separator + year + File.separator + month + File.separator + day + File.separator + hour;
        File dir = new File(path);
        System.out.println("saveImage保存图片2" + dir.exists());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        System.out.println("saveImage保存图片3" + dir.exists());
        final File file = new File(path, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            savaBarcodeInfo(calendar, barcode, deviceNum, imgName, file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储条码信息
     *
     * @param calendar  当前系统时间
     * @param barcode   条码
     * @param deviceNum 设备序列号
     * @param imgName   图片名称
     * @param imgPath   图片相对路径
     */
    private synchronized void savaBarcodeInfo(Calendar calendar, String barcode, String deviceNum,
                                              String imgName, String imgPath) {
        // wrong的条码不需要过滤
        if (data.contains(barcode) && !barcode.equalsIgnoreCase("wrong")) return;//已存在
        String year = calendar.get(Calendar.YEAR) + "";
        String month = calendar.get(Calendar.MONTH) + 1 + "";
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
        String hour = calendar.get(Calendar.HOUR_OF_DAY) + "";
        String time = year + month + day + hour;
        //截取图片相对路径
        imgPath = imgPath.substring(imgPath.indexOf(DIRNAME) + DIRNAME.length(), imgPath.length());
        //拼接图片信息内容
        String content = time + "\t" + barcode + "\t" + deviceNum + "\t" + imgName + "\t" + imgPath;
//        String fileName = time + ".txt";
        String fileName = "barcodeInfo.txt";
        //对应图片目录下的条码信息目录
        String path = Environment.getExternalStorageDirectory() + File.separator + DIRNAME;
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
        }
    }
}
