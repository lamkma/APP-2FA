package com.example.app_2fa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class LoginActivity extends AppCompatActivity {

    private EditText edt_username;
    private EditText edt_password;
    private Button btn_Register;
    private Button btn_Login;
    private ExecutorService executor;
    String tach;
    private Context mContext;

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        edt_username = findViewById(R.id.edt_username);
        edt_password = findViewById(R.id.edt_password);
        btn_Register = findViewById(R.id.btn_registry);
        btn_Login = findViewById(R.id.btn_login);


        btn_Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = edt_username.getText().toString().trim();
                String password = edt_password.getText().toString().trim();
                Client_Socket client = new Client_Socket();
                Future<String> futureResponse = client.sendMessage("SELECT "+username);

                try {
                    // Lấy kết quả từ tác vụ bất đồng bộ
                    String response = futureResponse.get();
                    //Toast.makeText(LoginActivity.this,"Phản hồi Registry server"+response,Toast.LENGTH_LONG).show();

                    if(response.equals("0")){
                        Client_Socket client1 = new Client_Socket();
                        Future<String> registerTK = client1.sendMessage("INSERT "+username+" "+password);
                        // Lấy kết quả từ tác vụ bất đồng bộ
                        String response_registerTK = registerTK.get();
                        System.out.println("Server response: " + response_registerTK);
                        if(response_registerTK.equals("1")){
                            Toast.makeText(LoginActivity.this,"Đăng ký thành công",Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(LoginActivity.this,"Có lỗi xảy ra khi đăng ký tài khoản",Toast.LENGTH_LONG).show();
                        }
                    }
                    else if(response.equals("1")){
                        Toast.makeText(LoginActivity.this,"Tài khoản đã tồn tại",Toast.LENGTH_LONG).show();
                    }
                    // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                    client.shutdown();
                }catch (Exception e){
                    System.err.println("Error resgister: "+e);
                }

            }
        });

        btn_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edt_username.getText().toString().trim();
                String password = edt_password.getText().toString().trim();
                Client_Socket client = new Client_Socket();
                Future<String> Response_login = client.sendMessage("SELECTLOGIN "+username+" "+password);


                try {
                    // Lấy kết quả từ tác vụ bất đồng bộ
                    String response = Response_login.get();

                   // Toast.makeText(LoginActivity.this,"Phản hồi login server"+response,Toast.LENGTH_LONG).show();

                    if(response.equals("[]")){
                        Toast.makeText(LoginActivity.this,"Tài khoản hoặc mật khẩu sai",Toast.LENGTH_LONG).show();
                    }
                    else if(response.equals("[(1,)]")||response.equals("[(0,)]")){
                        // Tách lấy trạng thái 2FA
                        tach = String.valueOf(response.charAt(2));
                        System.out.println("Status 2FA"+String.valueOf(response.charAt(2)));
                        if(String.valueOf(response.charAt(2)).equals("0")){
                            // Đăng nhập thành công, chuyển sang HomeActivity
                            Intent intent = new Intent(LoginActivity.this, activity_2fa.class);
                            intent.putExtra("username", username);
                            intent.putExtra("status2FA", tach);
                            startActivity(intent);
                            Toast.makeText(LoginActivity.this,"Đăng nhập thành công",Toast.LENGTH_LONG).show();
                        }
                        else{

                            showPopup();

                        }

                    }
                    else{

                        Toast.makeText(LoginActivity.this,response,Toast.LENGTH_LONG).show();
                    }
                    // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                    client.shutdown();
                }catch (Exception e){
                    System.err.println("Error Login: "+e);
                }

            }
        });
    }

    private void showPopup() {
        mContext = this;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Nhập mã xác thực 2FA");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_verify2fa, null);
        alertDialogBuilder.setView(dialogView);


        final EditText edt_code = dialogView.findViewById(R.id.edt_Code);

        alertDialogBuilder.setPositiveButton("Xác nhận", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Xử lý sự kiện khi người dùng click OK
                String code_verify = edt_code.getText().toString();
                String username = edt_username.getText().toString().trim();
                // Do something with the input text

                DiffieHelman clientDH = new DiffieHelman(code_verify,username);
                String getcheckverify = clientDH.getResponseVerifyOTP();
                Toast.makeText(LoginActivity.this, getcheckverify, Toast.LENGTH_LONG).show();
                if(getcheckverify != null) {
                    if(getcheckverify.equals("Correct OTP")) {
                        // Đăng nhập thành công, chuyển sang HomeActivity
                        Intent intent = new Intent(LoginActivity.this, activity_2fa.class);
                        intent.putExtra("username", username);
                        intent.putExtra("status2FA", tach);
                        startActivity(intent);
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_LONG).show();
                    }
                }
                else if(getcheckverify == null){
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập lại", Toast.LENGTH_LONG).show();
                }
            }
        });
        alertDialogBuilder.setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Xử lý sự kiện khi người dùng click Cancel
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        // Áp dụng style cho tiêu đề của dialog
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogWindowTitle;

        alertDialog.show();
    }

}

