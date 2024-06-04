package com.example.app_2fa;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Future;


public class activity_2fa extends AppCompatActivity {


    Button btn_verify;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch btn_switch;
    LinearLayout layout_2FA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_2fa);
        btn_switch = findViewById(R.id.switch_btn_2FA);
        btn_verify = findViewById(R.id.btn_verify);
        EditText edt_code_verify2fa = findViewById(R.id.edt_code_enable2FA);
        layout_2FA = findViewById(R.id.Form2FA);
//        layout_2FA.setVisibility(View.GONE);
        EditText secret_key = findViewById(R.id.edt_SecretKey);
        TextView show_username = findViewById(R.id.show_username);
        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        String getUsername = intent.getStringExtra("username");
        String getUstatus2FA = intent.getStringExtra("status2FA");
        if(getUstatus2FA.equals("1")){
            btn_switch.setChecked(true);
        }
        else{
            btn_switch.setChecked(false);
        }

        show_username.setText(getUsername);

        //xử lí khi thay đổi switch bật 2fa
        btn_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layout_2FA.setVisibility(View.VISIBLE);
                    Client_Socket client = new Client_Socket();
                    Future<String> futureResponse = client.sendMessageRequest2FA("request 2FA");
                    try {
                        // Lấy kết quả từ tác vụ bất đồng bộ
                        String response = futureResponse.get();
                        System.out.println("Server response request 2FA: " + response);
                        secret_key.setText(response);
                        // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                        client.shutdown();
                    } catch (Exception e) {
                        System.err.println("Error bật switch 2fa: "+e);
                    }
                }else {
                    layout_2FA.setVisibility(View.GONE);
                    Client_Socket client = new Client_Socket();
                    Future<String> futureResponse = client.sendMessageClose2FA(getUsername);
                    try {
                        // Lấy kết quả từ tác vụ bất đồng bộ
                        String response = futureResponse.get();
                        System.out.println("Server response close 2FA: " + response);
                        if(response.equals("1")){
                            // Nút switch được tắt
                            Toast.makeText(activity_2fa.this, "2FA is OFF", Toast.LENGTH_SHORT).show();
                        }
                        // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                        client.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //xử lí xác minh 2fa khi có secret Key
        btn_verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = edt_code_verify2fa.getText().toString().trim();
                Client_Socket client = new Client_Socket();
                Future<String> futureResponse = client.sendMessageVerify2FA(getUsername+"$"+code);
                try {
                    // Lấy kết quả từ tác vụ bất đồng bộ
                    String response = futureResponse.get();
                    System.out.println("Server response verify 2FA: " + response);
                    if(response.equals("1")){
                        layout_2FA.setVisibility(View.GONE);
                        Toast.makeText(activity_2fa.this, "2-FA đã được bật", Toast.LENGTH_SHORT).show();
                    }
                    // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                    client.shutdown();
                } catch (Exception e) {
                    System.err.println("Error verify bật 2fa: "+e);
                }
            }
        });
    }

}