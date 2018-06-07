package com.seuic.cainiaosocketdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.seuic.cainiaosocketdemo.util.Constants;

public class SettingActivity extends AppCompatActivity {
    private EditText ipET;
    private EditText portET;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        sharedPreferences = getSharedPreferences(Constants.SPNAME, Context.MODE_PRIVATE);
        ipET= findViewById(R.id.ip_et);
        portET = findViewById(R.id.port_et);

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(ipET.getText().toString().trim())||TextUtils.isEmpty(portET.getText().toString().trim())){
                    Toast.makeText(SettingActivity.this, "请输入ip和port", Toast.LENGTH_SHORT).show();
                    return;
                }
                sharedPreferences.edit().putString(Constants.IP,ipET.getText().toString().trim()).apply();
                sharedPreferences.edit().putString(Constants.PORT,portET.getText().toString().trim()).apply();
                Toast.makeText(SettingActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
