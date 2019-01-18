package com.example.upload.myload.api;

import com.example.upload.myload.bean.UserBean;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface MyServer {

    //登录地址
    String base_url = "http://yun918.cn/study/public/index.php/";
    //绑定用户信息的文件上传地址
    String upload_url = "http://yun918.cn/study/public/fileupload";
    //普通的文件上传地址
    String normal_url = "http://yun918.cn/study/public/file_upload.php";

    @POST("login")
    @FormUrlEncoded
    Observable<UserBean> login(@Field("username") String un,@Field("password") String pw);


}
