package com.example.upload.myload;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.upload.myload.api.MyServer;
import com.example.upload.myload.bean.HeaderBean;
import com.example.upload.myload.bean.NormalBean;
import com.example.upload.myload.bean.UserBean;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.btn_head)
    Button btnHead;
    @BindView(R.id.img_head)
    ImageView imgHead;
    @BindView(R.id.btn_video)
    Button btnVideo;
    @BindView(R.id.video)
    VideoView video;
    @BindView(R.id.take_photo)
    Button takePhoto;
    @BindView(R.id.select_photo)
    Button selectPhoto;

    private String uid;

    /*********相机相册*************/
    public static final int TAKE_PHOTO = 1;
    public static final int SELECT_PHOTO = 2;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_login, R.id.btn_head, R.id.btn_video,R.id.take_photo,R.id.select_photo})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_head:
                if (!TextUtils.isEmpty(uid)) {
                    updateHead();
                } else {
                    showTips("没有UID");
                }
                break;
            case R.id.btn_video:
                uploadVideo();
                break;
            case R.id.take_photo:
                take_photo();
                break;
            case R.id.select_photo:
                select_photo();
                break;
        }
    }

    //登录
    private void login() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MyServer.base_url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        MyServer myServer = retrofit.create(MyServer.class);
        Observable<UserBean> observable = myServer.login("zq", "123456");
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UserBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(UserBean userBean) {
                        if (userBean.getCode() == 200) {
                            uid = userBean.getData().get(0).getUid();
                            updateHeader(userBean.getData().get(0).getHeader());
                        } else {
                            showTips(userBean.getRet());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        showTips(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void updateHead() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        //读取本地sd卡根目录下的图片文件
        File file = new File(Utils.getSDPath() + "/" + "img1.jpg");
        if (file.exists()) {
            //图片上传任选一种格式
            String TYPE = "application/octet-stream"; //application/json 、
            //file文件流
            RequestBody fileBody = RequestBody.create(MediaType.parse(TYPE), file);
            //创建文件上传 数据对象
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //把数据和文件流放入requestBody
            RequestBody requestBody = builder
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "1806b")  //设置上传图片的文件夹 1806a
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(MyServer.upload_url)
                    .post(requestBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String string = response.body().string();
                    final HeaderBean headerBean = new Gson().fromJson(string, HeaderBean.class);
                    if (headerBean.getCode() == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeader(headerBean.getData().getUrl());
                            }
                        });
                    } else {
                        showTips(headerBean.getRes());
                    }
                }
            });
        } else {
            Toast.makeText(this, "找不到本地文件", Toast.LENGTH_SHORT).show();
        }
    }

    //上传视频
    private void uploadVideo() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        //读取本地sd卡根目录下的图片文件
        File file = new File(Utils.getSDPath() + "/" + "123.mp4");
        if (file.exists()) {
            //图片上传任选一种格式
            String TYPE = "multipart/form-data"; //application/json 、
            //file文件流
            RequestBody fileBody = RequestBody.create(MediaType.parse(TYPE), file);
            //创建文件上传 数据对象
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //把数据和文件流放入requestBody
            RequestBody requestBody = builder
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "1806b")  //设置上传图片的文件夹 1806a
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(MyServer.normal_url)
                    .post(requestBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String string = response.body().string();
                    final NormalBean normalBean = new Gson().fromJson(string, NormalBean.class);
                    if (normalBean.getCode() == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Uri uri = Uri.parse(normalBean.getData().getUrl());
                                video.setVideoURI(uri);
                                video.start();
                            }
                        });
                    } else {
                        showTips(normalBean.getRes());
                    }
                }
            });
        } else {
            Toast.makeText(this, "找不到本地文件", Toast.LENGTH_SHORT).show();
        }
    }

    //更新头像
    private void updateHeader(String head) {
        if (!TextUtils.isEmpty(head)) {
            RequestOptions options = new RequestOptions().circleCrop();
            Glide.with(this).load(head).apply(options).into(imgHead);
        }
    }

    private void showTips(String tips) {
        Toast.makeText(this, tips, Toast.LENGTH_SHORT).show();
    }


    /*********************************** 相机相册操作**************************/

    private void uploadBitmap(Bitmap bitmap,String name){
        if(bitmap != null && !TextUtils.isEmpty(name)){
            OkHttpClient client = new OkHttpClient.Builder().build();
            byte[] bytes = Utils.getByteByBmp(bitmap);
            //图片上传任选一种格式
            String TYPE = "application/octet-stream"; //application/json 、
            //file文件流
            RequestBody fileBody = RequestBody.create(MediaType.parse(TYPE), bytes);
            //创建文件上传 数据对象
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //把数据和文件流放入requestBody
            RequestBody requestBody = builder
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "1806b")  //设置上传图片的文件夹 1806a
                    .addFormDataPart("uid",uid)
                    .addFormDataPart("file", name, fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(MyServer.upload_url)
                    .post(requestBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String string = response.body().string();
                    final HeaderBean headerBean = new Gson().fromJson(string, HeaderBean.class);
                    if (headerBean.getCode() == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHeader(headerBean.getData().getUrl());
                            }
                        });
                    } else {
                        showTips(headerBean.getRes());
                    }
                }
            });
        }
    }

    /**
     *拍照获取图片
     **/
    public void take_photo() {
        String status= Environment.getExternalStorageState();
        if(status.equals(Environment.MEDIA_MOUNTED)) {
            //创建File对象，用于存储拍照后的图片
            File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
            try {
                if (outputImage.exists()) {
                    outputImage.delete();
                }
                outputImage.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(this, "com.llk.study.activity.PhotoActivity", outputImage);
            } else {
                imageUri = Uri.fromFile(outputImage);
            }
            //启动相机程序
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
        }else
        {
            Toast.makeText(MainActivity.this, "没有储存卡",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 从相册中获取图片
     * */
    public void select_photo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            openAlbum();
        }
    }

    /**
     * 打开相册的方法
     * */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,SELECT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO :
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        uploadBitmap(bitmap,"1806b.jpg");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_PHOTO :
                if (resultCode == RESULT_OK) {
                    //判断手机系统版本号
                    if (Build.VERSION.SDK_INT > 19) {
                        //4.4及以上系统使用这个方法处理图片
                        handleImgeOnKitKat(data);
                    }else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     *4.4以下系统处理图片的方法
     * */
    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);
    }
    /**
     * 4.4及以上系统处理图片的方法
     * */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleImgeOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this,uri)) {
            //如果是document类型的uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                //解析出数字格式的id
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }else if ("content".equalsIgnoreCase(uri.getScheme())) {
                //如果是content类型的uri，则使用普通方式处理
                imagePath = getImagePath(uri,null);
            }else if ("file".equalsIgnoreCase(uri.getScheme())) {
                //如果是file类型的uri，直接获取图片路径即可
                imagePath = uri.getPath();
            }
            //根据图片路径显示图片
            displayImage(imagePath);
        }
    }

    /**
     * 根据图片路径显示图片的方法
     * */
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            int startPos = imagePath.lastIndexOf("/")+1;
            String name = imagePath.substring(startPos,imagePath.length());
            uploadBitmap(bitmap,name);
        }else {
            Toast.makeText(MainActivity.this,"failed to get image",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通过uri和selection来获取真实的图片路径
     * */
    private String getImagePath(Uri uri,String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1 :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                }else {
                    Toast.makeText(MainActivity.this,"failed to get image",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }



}
