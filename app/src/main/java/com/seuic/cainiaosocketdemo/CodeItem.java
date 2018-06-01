package com.seuic.cainiaosocketdemo;

import android.media.Image;

/**
 * Created by yangjianan on 2018/6/1.
 */
class CodeItem {
    public String barcode;
    public String imgname;
    public Image img;
    public String scantime;
    public String error_msg;

    public CodeItem(String barcode) {
        this.barcode = barcode;
    }
}
