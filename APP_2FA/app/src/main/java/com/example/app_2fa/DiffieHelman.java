package com.example.app_2fa;


import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.Key;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DiffieHelman {

    private String responseVerifyOTP;


    public DiffieHelman(String OTP, String username) {
        // Số p và g công khai
        BigInteger p = new BigInteger("100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000118000000080101811009000118101080000000811000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001"); // Số nguyên tố lớn p
        BigInteger g = new BigInteger("2");  // Số nguyên tố gốc g
        // Bên A (client) chọn số ngẫu nhiên a riêng tư
        BigInteger a = new BigInteger(randomNumber100digits());

        // Bên A client tính A = g^a mod p
        BigInteger A = g.modPow(a, p);

        // Yêu cầu server gửi B
        Client_Socket client = new Client_Socket();
        Future<String> futureResponse = client.sendMessage("REQUEST_B " + String.valueOf(A));
        String response = "";
        String status_f2a_response = "";
        try {
            // Lấy kết quả từ tác vụ bất đồng bộ
            response = futureResponse.get();

            // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
            client.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Chuyển kết quả của yêu cầu B từ server sang dạng BigInteger
        BigInteger B = new BigInteger(response);
        // Bên A tính s = B^a mod p
        BigInteger s = B.modPow(a, p);

        // Đã tính được s là khóa chia sẻ bí mật chung, tiến hành mã hóa AES

        // Chuyển đổi khóa chia sẻ thành chuỗi hexa
        String sharedSecretHex = s.toString(16);

        // Chỉ lấy 16 ký tự đầu tiên của chuỗi hexa (tương đương 128 bit)
        String aesKeyHex = sharedSecretHex.substring(0, 32);

        // Chuyển đổi khóa AES từ chuỗi hexa
        byte[] aesKeyBytes = new BigInteger(aesKeyHex, 16).toByteArray();
        Key aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        try {
            // Encrypt with AES
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedData = cipher.doFinal(OTP.getBytes());
            // Convert encryptedData to hexa string
            StringBuilder hexString = new StringBuilder();
            for (byte bb : encryptedData) {
                String hex = Integer.toHexString(0xff & bb);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String encryptedDataHex = hexString.toString();
            // Hiển thị kết quả
            System.out.println("Encrypted data: " + new String(encryptedDataHex));

            if(!(response.equals(""))){
                // Gửi kết quả mã hóa cho server
                Client_Socket client1 = new Client_Socket();
                Future<String> futureResponse1 = client1.sendMessage("ENCRYPTED " + encryptedDataHex);
                try {
                    String responseEncrypted = futureResponse1.get();
                    System.out.println("responseEncrypted: " + responseEncrypted);
                    if (responseEncrypted.equals("DoneDecrypted")) {
                        client1.shutdown();
                        // Lấy kết quả mã code xác minh OTP có đúng hay không

                        Client_Socket client2 = new Client_Socket();
                        Future<String> futureResponse2 = client2.sendMessage("REQUEST_STATUS_OTP " + username + " " + OTP);

                        try {
                            // Lấy kết quả từ tác vụ bất đồng bộ
                            status_f2a_response = futureResponse2.get();
                            System.out.println("Trạng thái OTP: " + status_f2a_response);
                            this.responseVerifyOTP = status_f2a_response;
                            // Sau khi nhận được phản hồi từ máy chủ, shutdown ExecutorService
                            client2.shutdown();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Có vấn đề với việc gửi mã hóa");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getResponseVerifyOTP() {
        return responseVerifyOTP;
    }


    public static String randomNumber100digits() {
        String result = "";
        // Khởi tạo đối tượng Random
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            if (i == 0) {
                int randomNumber = 0;
                do {
                    // Sinh số ngẫu nhiên từ 0 đến 9 bằng phương thức nextInt(10)
                    randomNumber = random.nextInt(10);
                } while (randomNumber == 0);
                result += String.valueOf(randomNumber);
            } else {
                int randomNumber = random.nextInt(10);
                result += String.valueOf(randomNumber);
            }
        }
        return result;
    }
}

