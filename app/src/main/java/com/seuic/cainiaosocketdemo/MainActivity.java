package com.seuic.cainiaosocketdemo;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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
import com.seuic.cainiaosocketdemo.util.Data_syn;
import com.vilyever.socketclient.SocketClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangjianan on 2018/6/1.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SocketClient socketClient;
    private ProgressDialog progressDialog;
    private ImageView mImageview;
    private TextView currentCode;
    private TextView countCode;
    private RecyclerView codeListRV;
    private CodeListAdapter codeListAdapter;
    private List<CodeItem> data;
    private XData xdata = new XData();
    private Socket socket;
    private InputStream is;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        codeListRV = findViewById(R.id.id_recyclerview);
        mImageview = findViewById(R.id.current_imageview);
        currentCode = findViewById(R.id.current_barcode);
        countCode = findViewById(R.id.count_code);
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

    }

    private void socketClient() {
        progressDialog.setTitle("启动中");
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.80.64", 7777);
                    is = socket.getInputStream();
                    toast("连接成功");
                    while (!MainActivity.this.isFinishing()) {
                        progressDialog.dismiss();
                        boolean b = dealWithData(is);
                        if (b) {
                            System.out.println("成功啦成功啦成功啦成功啦成功啦 xdata.imgname = " + xdata.imgname);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("socket", "data size1 =  " + data.size());
                                    //UI线程刷新条码区域
                                    data.add(new CodeItem(xdata.barcode));
                                    //codeListAdapter.addData(data);
                                    codeListAdapter.notifyDataSetChanged();
                                    Log.e("socket", "data size2 =  " + data.size());
                                    //赋值
                                    currentCode.setText(xdata.barcode);
                                    countCode.setText(String.valueOf(data.size()));
                                }
                            });

                        } else {
                            //System.out.println("失败啦失败啦失败啦 xdata.imgname = " + xdata.imgname);
                        }
                    }
                    //关闭流
                    is.close();
                    socket.close();
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
        Log.e("socket","dealWithData开始解析");
        byte[] buf = new byte[16]; //数据头->条码长度
        int count = 0;
        try {
            //第一步
            count = is.read(buf, 0, 16); //将输入流写入tmp字节数组,先取20长度
            System.out.println("文件头：" + BytesHexStrTranslate.bytesToHexFun1(buf));
            //如果不是20，说明不是想要的数据，直接pass（可能是服务器的ok提示）
            if (count != 16) {
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
            count = is.read(temp, 0, 4);
            System.arraycopy(temp, 0, buf, 0, 4);
            if (count != 4) return false;
            temp[0] = buf[3];
            temp[1] = buf[2];
            temp[2] = buf[1];
            temp[3] = buf[0];

            //条码长度
            long codeLength = BytesHexStrTranslate.bytes2int(temp);
            Log.i("socket", "条码长度codeBytes = " + Data_syn.Bytes2HexString(temp));
            Log.i("socket", "条码长度 = " + codeLength);

            //第三步，取条码,上面已经算出长度(18)
            //564533383137313230303534362d312d312d
            buf = new byte[(int) codeLength];
            temp = new byte[(int) codeLength];//读取codeLength长度字节
            int received_Total = 0;
            int remain_length = temp.length;//已收文件头被覆盖
            int time_count = 100;//超时次数
            while (true) {
                //循环读取18长度， 可能后台一次没有发足够数据
                if (remain_length > 0) {
                    try {
                        count = is.read(temp, 0, (int) codeLength);
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
                xdata.barcode = new String(buf, "utf-8");
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
            //int imagenamelen = BytesHexStrTranslate.byteArrayToInt(temp);
            long longLen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket", "读图片名称长度成功 imagenamelen = " + longLen);

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
                xdata.imgname = new String(buf, "utf-8");
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
            int imagedatalen = BytesHexStrTranslate.byteArrayToInt(temp);
            Log.i("socket", "读取图片长度成功 图片数据长度 imagedatalen = " + imagedatalen);

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
            Log.i("socket", "读图片数据成功 bos imagedatalen - imageCount = " + (imagedatalen - imageCount));
            Log.i("socket", "读图片数据成功 bos length= " + bos.toByteArray().length);

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
            temp = new byte[17];
            System.arraycopy(buf, 0, temp, 0, 17); //只取时间17位
            xdata.scantime = new String(temp, "utf-8");
            Log.i("socket", "读图片数据成功 scantime = " + xdata.scantime);

            final Bitmap imgBitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.toByteArray().length);
            //xdata.bitmap = imgBitmap;
            saveImage(xdata.scantime, imgBitmap);
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
    private void saveImage(String imgName, Bitmap bmp) {
        String fileName = imgName + ".jpg";
        String path = Environment.getExternalStorageDirectory() + File.separator + "CiaoNiaoBarCode";
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
            Log.i("rxjava", "IOException e = " + e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_socket:
                //开启socket连接
                socketClient();
//                String fileName = "0001.jpg";
//                File file = new File(Environment.getExternalStorageDirectory(), fileName);
//                Glide.with(MainActivity.this).load(file).into(mImageview);
                break;
            case R.id.stop_socket:
                //关闭socket连接
                if (socket != null && socket.isConnected()) {
                    try {
                        is.close();
                        socket.close();
                        Toast.makeText(this, "已关闭连接", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "关闭失败", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.clear_data:
                currentCode.setText("");
                countCode.setText("");

                data.clear();
                codeListAdapter.notifyDataSetChanged();
                break;
            default:
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
