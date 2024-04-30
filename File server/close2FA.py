import socket
import mysql.connector

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
        # Nhận dữ liệu từ client
        data = client_socket.recv(1024).decode()
        print(data)
        print(f'Độ dài dữ liệu: {len(data)}')
        # Xử lý dữ liệu nhận được
        cursor.execute(f"UPDATE infor SET 2FA = '{0}', key_shared = '' WHERE username = '{data.strip()}'")
        db_connection.commit()
        result = cursor.rowcount
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
    server_socket.bind(('103.146.22.207', 5150))  # Chỉ định IP và cổng lắng nghe
    server_socket.listen(5)  # Lắng nghe tối đa 5 kết nối đồng thời

    print("Server is listening...")

    while True:
        client_socket, client_address = server_socket.accept()  # Chấp nhận kết nối từ client
        print("Connection from:", client_address)
        handle_client(client_socket)  # Xử lý yêu cầu từ client

if __name__ == "__main__":
    main()
