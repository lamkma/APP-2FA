import socket
import pyotp
def handle_client(client_socket):
    try:
        # Nhận dữ liệu từ client
        data = client_socket.recv(1024).decode()
        # Tạo một base32 encoded secret key. Trong thực tế, bạn sẽ cần lưu trữ
        # key này và chia sẻ nó với người dùng (thông qua QR code, ví dụ) để họ
        # có thể thêm vào trong ứng dụng Google Authenticator của họ.
        secret = pyotp.random_base32()
        print(f"Secret: {secret}")
        # Ghi key vào file tạm
        with open('temp2FA', 'w') as file:
            # Ghi một chuỗi vào file
            file.write(secret)
        # Gửi kết quả cho client
        client_socket.send(str(secret).encode())
        client_socket.close()
    except Exception as e:
        print("Error:", e)

def main():
    # Khởi tạo socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('103.146.22.207', 5050))  # Chỉ định IP và cổng lắng nghe
    server_socket.listen(5)  # Lắng nghe tối đa 5 kết nối đồng thời

    print("Server is listening...")

    while True:
        client_socket, client_address = server_socket.accept()  # Chấp nhận kết nối từ client
        print("Connection from:", client_address)
        handle_client(client_socket)  # Xử lý yêu cầu từ client

if __name__ == "__main__":
    main()
