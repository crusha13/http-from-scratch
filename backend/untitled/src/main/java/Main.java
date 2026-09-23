import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    static void main(String[] args) {
        final int server_listen_port = 8080;
        try (
                ServerSocket server_socket = new ServerSocket(server_listen_port);
        ) {
            while (true) {
                try (
                        Socket client_socket = server_socket.accept();
                        PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                ) {
                    String response = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <title>Hello client</title>
                            </head>
                            <body>
                                <h1>Hello, client!</h1>
                            </body>
                            </html>
                            """;
                    out.println("HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "Content-Length: " + response.length() + "\r\n\r\n" + response);
                    out.println();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}