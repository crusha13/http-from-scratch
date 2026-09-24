import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Main {
    static void main(String[] args) {
        final int server_listen_port = 8080;
        try (
                ServerSocket server_socket = new ServerSocket(server_listen_port);
        ) {
            while (true) {
                try (
                        Socket client_socket = server_socket.accept();
                ) {
                    new ClientHandler(client_socket).handle();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class HttpRequestParser {

}

class ClientHandler {
    private final Socket client_socket;

    public ClientHandler(Socket client_socket) {
        this.client_socket = client_socket;
    }

    public void handle() throws IOException {
        try(
            client_socket;
            PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
        ) {
            String body = "<!DOCTYPE HTML><html><head><title>Hello</title></head><body><h1>Hello</h1></body></html>";
            HttpResponse response = new HttpResponse("HTTP/1.1", "200 OK", "text/html", body.length(), body);
            response.writeTo(client_socket.getOutputStream());
        }
    }
}

class HttpResponse {
    private String version;
    private String code;
    private String content_type;
    private int content_length;
    private String body;

    public HttpResponse(String version, String code, String content_type, int content_length, String body) {
        this.version = version;
        this.code = code;
        this.content_type = content_type;
        this.content_length = content_length;
        this.body = body;
    }

    public HttpResponse(String version, String code, String content_type, int content_length) {
        this.version = version;
        this.code = code;
        this.content_type = content_type;
        this.content_length = content_length;
    }

    public HttpResponse(String version, String code, String content_type) {
        this.version = version;
        this.code = code;
        this.content_type = content_type;
    }

    public void writeTo(OutputStream output_stream) {
        PrintWriter out = new PrintWriter(output_stream, true);
        out.print(version + " " + code + "\r\n" +
                  "Content-Type: " + content_type + "\r\n" +
                  "Content-Length: " + String.valueOf(content_length) + "\r\n\r\n" +
                  body
        );
        out.flush();
    }
}