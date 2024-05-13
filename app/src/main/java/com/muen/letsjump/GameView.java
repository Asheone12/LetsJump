package com.muen.letsjump;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    private static final int GAME_OVER = 0x1;

    private Paint paint;//绘图用的画笔
    private Paint textPaint;//画文本用的画笔
    //图画
    private Bitmap bitCurrFloor;
    private Bitmap bitNexFloor;
    private Bitmap bitPlayer;
    private Bitmap bitSmallPlayer;
    private Bitmap bitBack;
    //主窗体
    private GameActivity gameActivity;
    private LongClickThread longClickThread;
    private Canvas canvas;
    private SurfaceHolder mHolder;
    private boolean alive = true;       //标识画图线程是否结束
    private boolean longClick = false;  //标识玩家是否在长按屏幕准备跳跃
    private boolean jump = false;         //玩家是否处于跳跃阶段
    private boolean moveFloor = false;    //地板是否处于移动状态
    private boolean stay = true;          //玩家是否处于站立不动状态
    private boolean clickAlive = false; //长按线程是否结束
    private boolean gameOver = false;     //游戏是否结束
    private int screenWidth;    //屏幕宽度
    private int screenHeight;   //屏幕高度
    private int score = 0;        //分数
    private float pressTime = 0;  //手指按下的时长，用float表示时间方便乘以弹力系数
    private int bounceCoefficient = 120;  //弹力系数
    private float bounceDistance;       //玩家松开手指时小人应当弹跳的距离
    private float randomCurr;           //当前地板所用的随机数
    private float randomNex;            //下一块地板所用的随机数
    private float playerCurrX;          //玩家当前的X坐标
    private float playerCurrY;          //玩家当前的Y坐标
    private float playerNexX;           //玩家下次应该到达的X坐标
    private float playerNexY;           //玩家下次应该到达的Y坐标
    private float floorCurrX;           //当前玩家所站地板的X坐标
    private float floorCurrY;           //当前玩家所站地板的Y坐标
    private float floorNexX;            //下块地板的X坐标
    private float floorNexY;            //下块地板的Y坐标
    private float sinAngle;             //sin角度
    private float cosAngle;             //cos角度，都是用于计算玩家应该移动的方向

    private int randomY = 100;          //y高度的随机因子


    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gameActivity = (GameActivity) context;      //获取主窗体
        screenHeight = gameActivity.getScreenH();
        screenWidth = gameActivity.getScreenW();
        init();
    }

    private void init() {
        getHolder().addCallback(this);              //添加回调，这样该view在创建时就会调用surfaceCreated方法，在结束时就会调用surfaceDestroyed方法
        setOnTouchListener(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setTextSize(30);                  //绘画字体的大小
        bitCurrFloor = BitmapFactory.decodeResource(getResources(), R.drawable.floor);
        bitNexFloor = BitmapFactory.decodeResource(getResources(), R.drawable.floor);
        bitPlayer = BitmapFactory.decodeResource(getResources(), R.drawable.player);    //初始化需要用到的变量
        bitSmallPlayer = bitPlayer;                                                     //先让缩小的玩家等于普通大小的玩家
        bitBack = BitmapFactory.decodeResource(getResources(), R.drawable.scroll_background);
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case GAME_OVER:         //如果判定游戏结束了，显示对话框
                    gameOverDialog();
                    break;
            }
            return false;
        }
    });


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;       //得到该view的持有者
        startGame();            //开始游戏
        new DrawThread().start();//同时开启绘画线程，开始画游戏界面
    }

    public void startGame() {
        randomCurr = getRandomPara();
        randomNex = getRandomPara();    //随机得到一个0~1的浮点型数据

        floorCurrX = (float) getWidth() * randomCurr;
        floorCurrY = getHeight() - bitCurrFloor.getHeight();
        playerCurrX = floorCurrX + 25;
        playerCurrY = floorCurrY - bitPlayer.getHeight() + 50;
        floorNexX = (float) getWidth() * randomNex;
        floorNexY = getHeight() - bitCurrFloor.getHeight() * 3  - randomNex * randomY;   //用这些随机数来设置玩家和地板的位置
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        alive = false;
    }

    private float getRandomPara() {
        float temp = new Random().nextFloat();
        if (temp == 0) {
            temp = 0.1f;
        }
        if (temp > 0.8) {
            temp = 0.8f;
        }
        return temp;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:       //手指按下时开启缩图线程
                Log.i("按下", "yessssss");
                bitSmallPlayer = bitPlayer;     //先获得普通小人的大小
                if (longClickThread == null) {
                    Log.i("线程", "空");
                    longClickThread = new LongClickThread();
                    longClick = true;//开始长按
                    stay = false;     //此时小人不处于站立不动状态
                    jump = false;     //也不处于跳跃模式
                    clickAlive = true;//开启缩图标识
                    longClickThread.start();
                }
                break;
            case MotionEvent.ACTION_UP:         //手指松开时关闭缩图线程
                longClick = false;              //不处于长按状态
                stay = false;                     //也不处于站立不动状态
                //计算斜边距离
                double hypotenuse = Math.sqrt(Math.pow(Math.abs(floorNexY - floorCurrY), 2) + Math.pow(Math.abs(floorNexX - floorCurrX), 2));
                //计算sin和cos的值
                sinAngle = (float) (Math.abs(floorNexY - floorCurrY) / hypotenuse);
                cosAngle = (float) (Math.abs(floorNexX - floorCurrX) / hypotenuse);
                bounceDistance = bounceCoefficient * pressTime;
                Log.i("bounceDistance", String.valueOf(bounceDistance));
                Log.i("pressTime", String.valueOf(pressTime));
                if (floorNexX < floorCurrX) {
                    playerNexX = playerCurrX - bounceDistance * cosAngle; //计算下次玩家应该到达位置的X坐标
                    Log.i("playerCurrX", String.valueOf(playerCurrX));
                    Log.i("playerNexX", String.valueOf(playerNexX));
                } else {
                    playerNexX = playerCurrX + bounceDistance * cosAngle;
                    Log.i("playerCurrX", String.valueOf(playerCurrX));
                    Log.i("playerNexX", String.valueOf(playerNexX));
                }
                playerNexY = playerCurrY - bounceDistance * sinAngle;     //计算下次玩家应该到达位置的Y坐标
                Log.i("playerCurrY", String.valueOf(playerCurrY));
                Log.i("playerNexY", String.valueOf(playerNexY));
                jump = true;                                          //玩家处于跳跃状态
                Log.i("值", String.valueOf(hypotenuse) + " " + String.valueOf(sinAngle) + " " + String.valueOf(cosAngle));
                longClickThread = null;                             //置空缩图线程
                clickAlive = false;
                Log.i("缩图线程", "停止");
                break;
        }
        return true;
    }

    class DrawThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.i("开始线程", "启动");
            Log.i("界面宽", String.valueOf(getWidth()));
            Log.i("界面高", String.valueOf(getHeight()));
            Log.i("图片宽", String.valueOf(bitPlayer.getWidth()));
            Log.i("图片高", String.valueOf(bitPlayer.getHeight()));
            while (alive) {
                try {
                    canvas = mHolder.lockCanvas();      //从持有者那里获取画布
                    canvas.save();
                    //根据屏幕按照比例放大画布
                    canvas.scale((float) screenWidth / bitBack.getWidth(), (float) screenHeight / bitBack.getHeight());
                    canvas.drawBitmap(bitBack, 0, 0, paint);
                    canvas.restore();   //后面的图不需要这种比例的画布，所以恢复
                    canvas.drawText("当前分数:" + score, getWidth() * 0.3f, 60, textPaint);
                    if (stay)        //站立不动的画图情况
                    {
                        if (!gameOver) {    //游戏没结束的话（跳到了下一块地板上）把小人放在地板的正上方
                            playerCurrX = floorCurrX + 25;
                            playerCurrY = floorCurrY - bitPlayer.getHeight() + 50;
                        }
                        //游戏结束的时候不用管
                        canvas.drawBitmap(bitPlayer, playerCurrX, playerCurrY, paint);
                    }
                    if (longClick)  //长按情况下画图情况
                    {
                        canvas.drawBitmap(bitSmallPlayer, playerCurrX, playerCurrY + bitPlayer.getHeight() - bitSmallPlayer.getHeight(), paint);
                    }
                    if (jump)        //跳跃时的画图情况
                    {
                        if (playerCurrY > playerNexY)  //如果还没跳完
                        {
                            if (floorNexX > floorCurrX) {
                                playerCurrX += 30 * cosAngle;
                            } else {
                                playerCurrX -= 30 * cosAngle;
                            }
                            playerCurrY -= 30 * sinAngle;
                            canvas.drawBitmap(bitPlayer, playerCurrX, playerCurrY, paint);
                        } else {        //如果跳完了
                            jump = false; //不处于跳跃状态
                            stay = true;  //处于站立状态
                            pressTime = 0;    //长按时间置0
                            //判断是否落到地板上
                            boolean re = isCollision(playerCurrX, playerCurrY, bitPlayer.getWidth(), bitPlayer.getHeight(),
                                    floorNexX, floorNexY, bitNexFloor.getWidth() / 2, bitNexFloor.getHeight() / 2);
                            if (!re) //如果没有，宣告游戏结束
                            {
                                handler.sendEmptyMessage(GAME_OVER);
                                gameOver = true;
                            } else {    //否者分数+1
                                score++;
                                moveFloor = true; //移动地板
                                stay = false;     //不处于站立状态，因为要跟着地板一起移动
                            }
                        }
                    }
                    if (moveFloor) //地板移动
                    {
                        canvas.drawBitmap(bitCurrFloor, floorCurrX, floorCurrY, paint);
                        canvas.drawBitmap(bitNexFloor, floorNexX, floorNexY, paint);
                        canvas.drawBitmap(bitPlayer, playerCurrX, playerCurrY, paint);//玩家跟着移动
                        playerCurrY += 20;    //每次移动20
                        if (floorNexY < getHeight() - bitNexFloor.getHeight()) {     //将当前的地板移出屏幕外
                            floorNexY += 20;
                        } else {
                            //移动完毕
                            exchangFloor(); //交换当前地板和下一块地板
                            randomNex = getRandomPara();
                            floorNexX = (float) getWidth() * randomNex;      //重新获取下块地板的坐标
                            floorNexY = getHeight() - bitCurrFloor.getHeight() * 3 - randomNex * randomY;
                            moveFloor = false;    //不处于移动地板状态
                            stay = true;          //处于不动状态
                        }
                        if (floorCurrY < getHeight()) {        //将当前地板移到屏幕底部
                            floorCurrY += 20;
                        }
                    } else {    //没有移动的时候画不动的图
                        canvas.drawBitmap(bitCurrFloor, floorCurrX, floorCurrY, paint);
                        canvas.drawBitmap(bitNexFloor, floorNexX, floorNexY, paint);
                    }
                    mHolder.unlockCanvasAndPost(canvas);    //回收这块画布
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class LongClickThread extends Thread {
        private float coefficient = 1f;//线程通过不断减小这个值，来达到不断缩小图片的效果

        @Override
        public void run() {
            super.run();
            while (clickAlive) {
                Log.i("缩图线程", "运行");
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Matrix matrix = new Matrix();
                matrix.postScale(1, coefficient);   //y轴缩放参数
                bitSmallPlayer = Bitmap.createBitmap(bitPlayer, 0, 0, bitPlayer.getWidth(), bitPlayer.getHeight(), matrix, true);//用这个参数来缩放
                if (coefficient < 0.70) {   //缩小比0.7小后表示图片不能再缩小了，将clickAlive置为false终止线程，此时缩图参数和长按时间的值都有了
                    clickAlive = false;
                } else {
                    coefficient -= 0.01f;
                    pressTime += 0.1;
                    Log.i("缩图参数", String.valueOf(coefficient) + " " + String.valueOf(pressTime));
                }
            }
        }
    }

    private void exchangFloor() {
        Bitmap temp;
        temp = bitNexFloor;
        bitNexFloor = bitCurrFloor;
        bitCurrFloor = temp;
        float tem;
        tem = floorCurrX;
        floorCurrX = floorNexX;
        floorNexX = tem;
        tem = floorCurrY;
        floorCurrY = floorNexY;
        floorNexY = tem;
    }

    /**
     * @param x1 x坐标
     * @param y1 y坐标
     * @param w1 宽度
     * @param h1 高度
     * @param x2
     * @param y2
     * @param w2
     * @param h2
     * @return true表示相碰，false表示没相碰
     */
    public boolean isCollision(float x1, float y1, int w1, int h1, float x2, float y2, int w2, int h2) {
        if (x1 > x2 && x1 >= x2 + w2) {
            return false;
        } else if (x1 <= x2 && x1 + w1 <= x2) {
            return false;
        } else if (y1 >= y2 && y1 >= y2 + h2) {
            return false;
        } else if (y1 < y2 && y1 + h1 <= y2) {
            return false;
        }
        return true;
    }

    public void gameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final EditText editText = new EditText(getContext());
        editText.setFocusable(true);
        String message;
        String strButton;
        SharedPreferences preferences = gameActivity.getSharedPreferences("topScore", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        int topScore = preferences.getInt("score", 0);
        if (score > topScore)  //判断当前分数是否大于最高分
        {
            message = "你的最终得分为" + score + "\n恭喜你打破最高分！请输入你的姓名";
            builder.setView(editText);//给出输入姓名的输入框
            strButton = "确定";
        } else {
            message = "你的最终得分为" + score;
            strButton = "重新开始";
        }
        builder.setTitle("游戏结束");
        builder.setMessage(message);
        builder.setPositiveButton(strButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startGame();
                if (!editText.getText().toString().isEmpty())    //如果没有输入姓名就算了
                {
                    editor.putString("name", editText.getText().toString());
                    editor.putInt("score", score);
                    editor.apply();
                }
                score = 0;    //分数置0
                gameOver = false;
            }
        });
        builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                gameActivity.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
