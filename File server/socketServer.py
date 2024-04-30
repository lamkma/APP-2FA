import socket
import mysql.connector
import pyotp
import gmpy2
import random
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import binascii
def handle_client(client_socket):
    try:
        # Kết nối đến cơ sở dữ liệu MySQL
        db_connection = mysql.connector.connect(
            host="localhost",
            user="thanhdc",
            password="thanhdc",
            database="inforusers"
        )
        cursor = db_connection.cursor()
        result = ''
        secret_key_shared = ''
        OTP_decrypted = ''
        # Nhận dữ liệu từ client
        data = client_socket.recv(1024).decode()
        print(f"Dữ liệu nhận được từ client: {data}")
        # Xử lý dữ liệu nhận được (ví dụ: truy vấn SQL)
        query = data.split()
        print(query)
        if query[0] == "SELECT":
            cursor.execute(f"SELECT * FROM infor WHERE username = '{query[1]}'")
            result = cursor.fetchone()
            if (result == None):
                result = "0"
            else:
                result = "1"
        elif query[0] == "INSERT":
            cursor.execute(f"INSERT INTO infor(username,password) values('{query[1]}','{query[2]}')")
            db_connection.commit()
            result = cursor.rowcount
        elif query[0] == "SELECTLOGIN":
            cursor.execute(f"SELECT 2FA FROM infor WHERE username = '{query[1]}' AND password = '{query[2]}'")
            result = cursor.fetchall()
            print(result)
        elif query[0] == "Verify2FA":
            cursor.execute(f"SELECT key_shared FROM infor WHERE username = '{query[2]}'")
            result = cursor.fetchone()
            print(result[0])
            print(len(result[0]))
            # Tạo một đối tượng TOTP với secret key
            totp = pyotp.TOTP(result[0])
            # Xác minh mã OTP
            if totp.verify(query[1]):
                result = "Mã code hợp lệ"
            else:
                result = 'Mã code không đúng'
        elif query[0] == "REQUEST_B":
            # Số nguyên tố lớn p công khai
            p_str = "100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000118000000080101811009000118101080000000811000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001"
            p = gmpy2.mpz(p_str)
            # Số nguyên tố gốc g công khai
            g = gmpy2.mpz(2)
            # Bên B chọn số ngẫu nhiên b riêng tư
            # Tạo số B ngẫu nhiên
            first_digit = str(random.randint(1, 9))

            # Tạo một số ngẫu nhiên có 99 chữ số sau đó
            random_number = first_digit + ''.join([str(random.randint(0, 9)) for _ in range(99)])
            b = gmpy2.mpz(str(random_number))
            print(f"b là: {b}")
            # Bên B tính B = g^b mod p
            B = gmpy2.powmod(g, b, p)
            print(f"B là: {B}")
            # Bên B tính s = A^b mod p
            s = gmpy2.powmod(gmpy2.mpz(query[1].strip()), b, p)
            print(f"s là: {s}")
            secret_key_shared = s
            # Chuyển key shared sang dạng hexa
            hexa_format = hex(secret_key_shared)[2:]
            # Lấy 16 ký tự tương đương 128 bit làm khóa bí mật
            secret_key_shared = hexa_format[:32]
            print(f"Khóa bí mật: {secret_key_shared}")
            print(len(secret_key_shared))
            with open('secretKeyEncryptDecrypt', 'w') as file:
                # Ghi khóa bí mật vào file
                file.write(secret_key_shared)
            result = B
        elif query[0] == "ENCRYPTED":
            hex_string = query[1]
            # Đọc khóa bí mật từ file
            with open('secretKeyEncryptDecrypt', 'r') as file:
                dong_dau_tien = file.readline()
                secret_key_shared = dong_dau_tien.strip()
            # Chuyển đổi chuỗi hexa thành dữ liệu byte
            encrypted_data = binascii.unhexlify(hex_string)

            # Khóa bí mật
            aes_key_hex = secret_key_shared
            aes_key_bytes = bytes.fromhex(aes_key_hex)
            print(aes_key_hex)
            aes_key = AES.new(aes_key_bytes, AES.MODE_ECB)

            try:
                # Giải mã dữ liệu
                decrypted_data = unpad(aes_key.decrypt(encrypted_data), AES.block_size)
                OTP_decrypted = decrypted_data.decode('utf-8')
                with open('OTPDcrypted', 'w') as file:
                    # Ghi mã OTP đã được giải mã nhận được từ client
                    file.write(OTP_decrypted)
                # Hiển thị dữ liệu đã giải mã
                print("Decrypted data:", decrypted_data.decode('utf-8'))

            except Exception as e:
                print("Error:", e)
            result = "DoneDecrypted"
        elif query[0] == "REQUEST_STATUS_OTP":
            cursor.execute(f"SELECT key_shared FROM infor WHERE username = '{query[1]}'")
            getkeyshare = cursor.fetchone()
            print(getkeyshare[0])
            # Tạo một đối tượng TOTP với secret key
            totp = pyotp.TOTP(getkeyshare[0])
            # Đọc mã OTP đã được giải mã từ file
            with open('OTPDcrypted', 'r') as file:
                dong_dau_tien = file.readline()
                OTP_decrypted = dong_dau_tien.strip()
            # Giả sử mã OTP từ người dùng
            user_provided_otp = OTP_decrypted
            # Xác minh mã OTP
            if totp.verify(user_provided_otp.strip()):
                result = "Correct OTP"
            else:
                result = 'Mã code không đúng'
        # Gửi kết quả cho client
        client_socket.send(str(result).encode())
        # Đóng kết nối cơ sở dữ liệu và socket
        cursor.close()
        db_connection.close()
        client_socket.close()
    except Exception as e:
        print("Error:", e)

def main():
    # Khởi tạo socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('103.146.22.207', 8888))  # Chỉ định IP và cổng lắng nghe
    server_socket.listen(5)  # Lắng nghe tối đa 5 kết nối đồng thời

    print("Server is listening...")

    while True:
        client_socket, client_address = server_socket.accept()  # Chấp nhận kết nối từ client
        print("Connection from:", client_address)
        handle_client(client_socket)  # Xử lý yêu cầu từ client

if __name__ == "__main__":
    main()

