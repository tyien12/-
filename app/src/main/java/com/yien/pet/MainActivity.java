package com.yien.pet;

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
    private Button btnFloat;
    private boolean floatRunning = false;

    public class AndroidBridge {
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
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        btnFloat = findViewById(R.id.btn_float);
        btnFloat.setText("开启悬浮窗");
        btnFloat.setOnClickListener(v -> {
            if (floatRunning) {
                stopService(new Intent(this, PetFloatService.class));
                floatRunning = false;
                btnFloat.setText("开启悬浮窗");
                Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, 100);
                    Toast.makeText(this, "请允许悬浮窗权限", Toast.LENGTH_LONG).show();
                    return;
                }
                startService(new Intent(this, PetFloatService.class));
                floatRunning = true;
                btnFloat.setText("关闭悬浮窗");
                Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
            }
        });

        loadHtml();
    }

    private void loadHtml() {
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