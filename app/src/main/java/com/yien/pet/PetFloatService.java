package com.yien.pet;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

public class PetFloatService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams iconParams;
    private View iconView;
    private ImageView petImg;
    private TextView bubble;

    private Bitmap bmpOpen, bmpClosed;
    private String currentSkin = "1";
    private Handler handler = new Handler();
    private boolean isClosed = false;
    private boolean alarming = false;
    private Vibrator vibrator;

    private int touchStartX, touchStartY;
    private float iconX, iconY;
    private boolean isDragging = false;
    private int clickCount = 0;

    private String[] LINES = {"别碰我~", "哼！", "干嘛啦...", "痒痒的...", "呜哇！", "好害羞...", "在这儿呢~", "再摸咬你哦"};

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        loadSkin("1");
        createIcon();
        startBlinkLoop();
        startBreath();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("skin")) {
                loadSkin(intent.getStringExtra("skin"));
                updateImage();
            }
            if (intent.hasExtra("alarm")) {
                if (intent.getBooleanExtra("alarm", false)) {
                    startAlarmJump();
                } else {
                    stopAlarmJump();
                }
            }
        }
        return START_STICKY;
    }

    private void loadSkin(String skin) {
        currentSkin = skin;
        String openName = "skin" + skin + "_open.png";
        String closedName = "skin" + skin + "_closed.png";
        bmpOpen = loadAsset(openName);
        bmpClosed = loadAsset(closedName);
    }

    private WindowManager.LayoutParams overlayParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(type);
        p.format = PixelFormat.TRANSLUCENT;
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        p.gravity = Gravity.TOP | Gravity.START;
        p.width = dp(100);
        p.height = dp(130);
        p.x = dp(16);
        p.y = dp(250);
        return p;
    }

    private void createIcon() {
        iconParams = overlayParams();
        iconView = LayoutInflater.from(this).inflate(R.layout.float_icon, null);
        petImg = iconView.findViewById(R.id.float_pet);
        bubble = iconView.findViewById(R.id.float_bubble);
        petImg.setImageBitmap(bmpOpen);

        iconView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartX = (int) event.getRawX();
                        touchStartY = (int) event.getRawY();
                        iconX = iconParams.x;
                        iconY = iconParams.y;
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchStartX);
                        int dy = (int) (event.getRawY() - touchStartY);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) isDragging = true;
                        iconParams.x = (int) iconX + dx;
                        iconParams.y = (int) iconY + dy;
                        try { wm.updateViewLayout(iconView, iconParams); } catch (Exception ignored) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) onClickPet();
                        return true;
                }
                return false;
            }
        });

        wm.addView(iconView, iconParams);
    }

    private void updateImage() {
        if (petImg != null) petImg.setImageBitmap(isClosed ? bmpClosed : bmpOpen);
    }

    private Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!alarming) {
                isClosed = !isClosed;
                updateImage();
            }
            handler.postDelayed(this, isClosed ? 150 : 2800);
        }
    };
    private void startBlinkLoop() {
        handler.postDelayed(blinkRunnable, 2000);
    }

    private void startBreath() {
        TranslateAnimation breath = new TranslateAnimation(0, 0, 0, -dp(5));
        breath.setDuration(1500);
        breath.setRepeatMode(Animation.REVERSE);
        breath.setRepeatCount(Animation.INFINITE);
        petImg.startAnimation(breath);
    }

    // 闹钟响：持续跳 + 震动
    private Runnable alarmJumpRunnable = new Runnable() {
        @Override
        public void run() {
            if (!alarming) return;
            TranslateAnimation jump = new TranslateAnimation(0, 0, 0, -dp(30));
            jump.setDuration(300);
            jump.setRepeatMode(Animation.REVERSE);
            jump.setRepeatCount(1);
            iconView.startAnimation(jump);
            if (vibrator != null) vibrator.vibrate(300);
            handler.postDelayed(this, 700);
        }
    };
    private void startAlarmJump() {
        alarming = true;
        bubble.setText("⏰ 闹钟响啦！");
        bubble.setVisibility(View.VISIBLE);
        handler.post(alarmJumpRunnable);
    }
    private void stopAlarmJump() {
        alarming = false;
        bubble.setVisibility(View.GONE);
        handler.removeCallbacks(alarmJumpRunnable);
        iconView.clearAnimation();
    }

    private void onClickPet() {
        // 闹钟响时点击 = 停止闹钟
        if (alarming) {
            stopAlarmJump();
            // 通知 HTML 闹钟也停
            Intent stop = new Intent("com.yien.pet.ALARM_STOP");
            sendBroadcast(stop);
            return;
        }
        clickCount++;
        updateImage();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { updateImage(); }
        }, 1200);

        TranslateAnimation jump = new TranslateAnimation(0, 0, 0, -dp(20));
        jump.setDuration(200);
        jump.setRepeatMode(Animation.REVERSE);
        jump.setRepeatCount(1);
        iconView.startAnimation(jump);

        String say = LINES[clickCount % LINES.length];
        bubble.setText(say);
        bubble.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideBubble);
        handler.postDelayed(hideBubble, 1500);
    }
    private Runnable hideBubble = new Runnable() {
        @Override
        public void run() { bubble.setVisibility(View.GONE); }
    };

    private Bitmap loadAsset(String name) {
        try {
            InputStream is = getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) { return null; }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(blinkRunnable);
        handler.removeCallbacks(hideBubble);
        handler.removeCallbacks(alarmJumpRunnable);
        try { if (iconView != null) wm.removeView(iconView); } catch (Exception ignored) {}
        super.onDestroy();
    }
}