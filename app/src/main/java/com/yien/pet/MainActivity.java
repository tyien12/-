package com.yien.pet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private boolean floatRunning = false;

    // JS 桥接：HTML 里调用 AndroidBridge.xxx() 就能通知 Android
    public class AndroidBridge {
        // 换装：skin = "1" 粉发, "2" 紫发
        @JavascriptInterface
        public void onSkinChange(final String skin) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(MainActivity.this, PetFloatService.class);
                    i.putExtra("skin", skin);
                    startService(i);
                }
            });
        }
        // 闹钟响
        @JavascriptInterface
        public void onAlarmStart() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(MainActivity.this, PetFloatService.class);
                    i.putExtra("alarm", true);
                    startService(i);
                }
            });
        }
        // 闹钟停
        @JavascriptInterface
        public void onAlarmStop() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(MainActivity.this, PetFloatService.class);
                    i.putExtra("alarm", false);
                    startService(i);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.main_webview);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);

        // 加 JS 桥接
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        Button btnFloat = findViewById(R.id.btn_float);
        btnFloat.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, 100);
                Toast.makeText(this, "请允许悬浮窗权限", Toast.LENGTH_LONG).show();
                return;
            }
            startService(new Intent(this, PetFloatService.class));
            Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
        });

        loadHtml();
    }

    private void loadHtml() {
        // 优先读 /sdcard/Download/小狐狸伊恩.html（热更新）
        File ext = new File("/sdcard/Download/小狐狸伊恩.html");
        if (ext.exists()) {
            webView.loadUrl("file://" + ext.getAbsolutePath());
        } else {
            webView.loadUrl("file:///android_asset/小狐狸伊恩.html");
        }
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PetFloatService.class));
        super.onDestroy();
    }
}