package hx.safe.activity;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import hx.safe.R;
import hx.safe.util.StreamUtils;

public class SplashActivity extends AppCompatActivity {

    private TextView tvVersion;
    private TextView tvProgress;
    private String versionName;
    private int versionCode;
    private String description;
    private String downloadUrl;

    private final static int CODE_UPDATE_DIALOG =0;
    private final static int CODE_URL_ERROR =1;
    private final static int CODE_NET_ERROR =2;
    private final static int CODE_JSON_ERROR =3;
    private final static int CODE_ENTER_HOME =4;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_UPDATE_DIALOG:
                    showUpdateDialog();
                    break;
                case CODE_URL_ERROR:
                    Toast.makeText(SplashActivity.this, "url错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_NET_ERROR:
                    Toast.makeText(SplashActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_JSON_ERROR:
                    Toast.makeText(SplashActivity.this, "json解析错误", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case CODE_ENTER_HOME:
                    enterHome();
                    break;
            }
        }
    };
    //显示升级对话框
    private void showUpdateDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("最新版本:" + versionName);
        builder.setMessage(description);
        //builder.setCancelable(false);//用户体验差
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                download();
            }
        });
        builder.setNegativeButton("以后再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enterHome();
            }
        });
        //用户点击返回键触发
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                enterHome();
            }
        });
        builder.show();
    }
    //进入主界面
    private void enterHome() {

    }

    //下载更新文件
    private void download() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvVersion = (TextView) findViewById(R.id.tv_version);
        tvProgress = (TextView) findViewById(R.id.tv_progress);//默认隐藏
        tvVersion.setText("版本号:"+getVersionName());
        checkVersion();

    }
    //从服务器获得版本信息进行校验
    private void checkVersion() {
        new Thread(){
            @Override
            public void run() {
                Message msg=Message.obtain();
                HttpURLConnection conn=null;

                try {
                    URL url = new URL("http://192.168.100.6:8080/update.json");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream in = conn.getInputStream();
                        String result = StreamUtils.readFromStream(in);
                        //解析json
                        JSONObject obj = new JSONObject(result);
                        versionName = obj.getString("versionName");
                        versionCode = obj.getInt("versionCode");
                        description = obj.getString("description");
                        downloadUrl = obj.getString("downloadUrl");
                        //判断是否需要更新,并发通知
                        if(versionCode>getVersionCode()){
                            msg.what=CODE_UPDATE_DIALOG;

                        }else{
                            msg.what=CODE_ENTER_HOME;
                        }
                    }
                } catch (MalformedURLException e) {
                    //url错误
                    msg.what=CODE_URL_ERROR;
                    e.printStackTrace();
                } catch (IOException e) {
                    //网络连接错误
                    msg.what=CODE_NET_ERROR;
                    e.printStackTrace();
                } catch (JSONException e) {
                    msg.what=CODE_JSON_ERROR;
                    e.printStackTrace();
                }finally {
                    //最后给主线程发消息
                    handler.sendMessage(msg);
                    //关闭连接
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }.start();
    }
    //获得版本号
    private int getVersionCode() {
        PackageManager manager=getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            int versionCode = info.versionCode;
            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //获取版本名称
    private String getVersionName() {
        PackageManager manager=getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);

            int versionCode=info.versionCode;
            String versionName=info.versionName;

            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //没有找到包名走此异常
            e.printStackTrace();
        }

        return "";
    }
}
