package com.seuic.cainiaosocketdemo.util;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileHelper {
    /**
     * 第一种获取文件内容方式
     *
     * @param filePath 路径
     * @return byte[]数组
     * @throws IOException
     */
    public byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);

        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }

        FileInputStream fi = new FileInputStream(file);

        byte[] buffer = new byte[(int) fileSize];

        int offset = 0;

        int numRead = 0;

        while (offset < buffer.length

                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {

            offset += numRead;

        }

        // 确保所有数据均被读取

        if (offset != buffer.length) {

            throw new IOException("Could not completely read file " + file.getName());

        }

        fi.close();

        return buffer;
    }

    /**
     * 第二种获取文件内容方式
     *
     * @param filePath 路径
     * @return byte[]数组
     * @throws IOException
     */
    public byte[] getContent2(String filePath) throws IOException {
        FileInputStream in = new FileInputStream(filePath);

        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        System.out.println("bytes available:" + in.available());

        byte[] temp = new byte[1024];

        int size = 0;

        while ((size = in.read(temp)) != -1) {
            out.write(temp, 0, size);
        }

        in.close();

        byte[] bytes = out.toByteArray();
        System.out.println("bytes size got is:" + bytes.length);

        return bytes;
    }

    /**
     * 将byte数组写入文件 (不追加)
     *
     * @param path    路径
     * @param content byte[]数组
     * @throws IOException
     */
    public void createFile(String path, byte[] content) throws IOException {

        FileOutputStream fos = new FileOutputStream(path);

        fos.write(content);
        fos.close();
    }

    /**
     * 方法1：
     * 将byte数组(追加)写入文件
     *
     * @param filePath       路径
     * @param content    byte[]数组
     * @param Appendable 是否追加
     * @throws IOException
     */
    public static void createFileAdd(File filePath, byte[] content, boolean Appendable) throws IOException {
        //程序写好之后每次存储数据都刷新
        //FileOutputStream fos = new FileOutputStream(path);
        //研究了一下，原来FileOutPutStream也可以设置模式的，只是和openFileOutput不一样 我这样写FileOutputStream fos=new FileOutputStream(_sdpath1,Appendable)；就可以实现数据追加功能
//        FileOutputStream fos = new FileOutputStream(path, Appendable);
        FileOutputStream fos = new FileOutputStream(filePath, Appendable);
        fos.write(content);
        fos.write("\r\n".getBytes());
        fos.close();
    }

    /**
     * 方法二：
     * 根据byte数组，生成文件
     *
     * @param bfile    byte[]数组
     * @param filePath 路径
     * @param fileName 文件名
     */
    public void writeFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;

        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + "\\" + fileName);
            /* 使用以下2行代码时，不追加方式*/
            /*bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(bfile); */

            /* 使用以下3行代码时，追加方式*/
            bos = new BufferedOutputStream(new FileOutputStream(file, true));
            bos.write(bfile);
            bos.write("\r\n".getBytes());


            bos.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    /**
     * byte数组写入文件中
     */
    public static void saveBytes2File() {
        String fileName = "0001.jpg";
        String path = Environment.getExternalStorageDirectory() + File.separator + "CiaoNiaoBarCode";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path, fileName);
        byte[] temp1 = new byte[4];
        temp1[0] = 0;
        temp1[1] = 0;
        temp1[2] = 0;
        temp1[3] = 12;
        try {
            createFileAdd(file,temp1,true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            byte[] buf = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

            createFileAdd(file,buf,true);
            System.out.println("写好啦");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String fileName = "0001.jpg";
        File file = new File(Environment.getExternalStorageDirectory()+ "/CiaoNiaoBarCode", fileName);
//        String path = Environment.getExternalStorageDirectory() + "CiaoNiaoBarCode" + "1111.bmp";

        byte[] temp1 = new byte[4];
        temp1[0] = 0;
        temp1[1] = 0;
        temp1[2] = 0;
        temp1[3] = 12;
        try {
            createFileAdd(file,temp1,true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            byte[] buf = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

            createFileAdd(file,buf,true);
            System.out.println("写好啦");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}