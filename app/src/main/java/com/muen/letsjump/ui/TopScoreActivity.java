package com.muen.letsjump.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muen.letsjump.R;

public class TopScoreActivity extends AppCompatActivity {
    private TextView txTopScore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top);
        txTopScore=findViewById(R.id.ranktx);
        SharedPreferences sharedPreferences=getSharedPreferences("topScore", Context.MODE_PRIVATE);
        int score=sharedPreferences.getInt("score",0);//获取最高分
        txTopScore.setText( score + "分");
    }
}
