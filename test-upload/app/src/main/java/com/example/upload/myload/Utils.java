package com.example.upload.myload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class Utils {

    //sd卡是否存在
    public static boolean sdCardIsAvailable(){
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            return true;
        }
        return false;
    }


    //获取sd卡路径
    public static String getSDPath(){
        File sdDir = null;
        if(sdCardIsAvailable()){
            sdDir = Environment.getExternalStorageDirectory(); //获取根目录
        }
        return sdDir.toString();
    }


    //byte[]转bitmap
    public static Bitmap getBitmapByBytes(byte[] bytes, BitmapFactory.Options options){
        if(bytes != null){
            if(options != null){
                return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            }else{
                return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            }
        }
        return null;
    }

    //Bitmap转byte[]
    public static byte[] getByteByBmp(Bitmap bmp){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        return outputStream.toByteArray();
    }


}
