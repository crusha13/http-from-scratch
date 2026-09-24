import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    static void main(String[] args) {
        final int server_listen_port = 8080;
        Router router = new Router();
        router.addRoute("GET", "/", new StaticFileHandler("index.html"));
        try (
                ServerSocket server_socket = new ServerSocket(server_listen_port);
        ) {
            while (true) {
                try (
                    Socket client_socket = server_socket.accept();
                ) {
                    new ClientHandler(client_socket, router).handle();
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
    public HttpRequest parse(BufferedReader in) throws IOException {
        String request_line = in.readLine();
        String[] request_parts = request_line.split(" ");
        String method = request_parts[0];
        String path = request_parts[1];
        String version = request_parts[2];

        Map<String, String> headers = new HashMap<String, String>();
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            int colonIndex = line.indexOf(":");
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring((colonIndex + 1)).trim();
            headers.put(key, value);
        }

        String body = null;
        if (headers.containsKey("Content-Length")) {
            int content_length = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[content_length];
            in.read(bodyChars, 0, content_length);
            body = new String(bodyChars);
        }

        return new HttpRequest(method, path, version, headers, body);
    }
}

record HttpRequest (
    String method,
    String path,
    String version,
    Map<String, String> headers,
    String body
    ) {}

class ClientHandler {
    private final Socket client_socket;
    private final Router router;

    public ClientHandler(Socket client_socket, Router router) {
        this.client_socket = client_socket;
        this.router = router;
    }

    public void handle() throws IOException {
        try(
            client_socket;
            PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
        ) {
            HttpRequest request = new HttpRequestParser().parse(in);
            HttpResponse response = router.route(request);
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

interface RequestHandler {
    public HttpResponse handle(HttpRequest request);
}

class Router {
    private Map<RouteKey, RequestHandler> routes = new HashMap<RouteKey, RequestHandler>();

    public void addRoute(String method, String path, RequestHandler handler) {
        RouteKey key = new RouteKey(method, path);
        routes.put(key, handler);
    }

    public HttpResponse route(HttpRequest request) {
        RouteKey key = new RouteKey(request.method(), request.path());

        RequestHandler handler = routes.get(key);
        if (handler == null) {
            return new HttpResponse("HTTP/1.1", "404 Not Found", "text/html", 0, "");
        }
        return handler.handle(request);
    }
}

record RouteKey(String method, String path){}

class StaticFileHandler implements RequestHandler {
    private String file_path;
    private static final Path BASE_DIR = Paths.get("frontend").toAbsolutePath().normalize();
    static { System.out.println("BASE_DIR = " + BASE_DIR); }

    public StaticFileHandler(String file_path) {
        this.file_path = file_path;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        if (file_path.startsWith("/")) {
            file_path = file_path.substring(1);
        }

        Path target_path = BASE_DIR.resolve(file_path);

        try{
            Path real_target_path = target_path.toRealPath(LinkOption.NOFOLLOW_LINKS);

            if (!real_target_path.startsWith(BASE_DIR)) {
                String text = "<html><head><title>Forbidden</title></head><body><h1>403 Forbidden</h1><p>You don't have access to this page</p></body></html>";
                return new HttpResponse("HTTP/1.1", "403 Forbidden", "text/html", text.length(), text);
            }
            if (!Files.exists(real_target_path)) {
                String text = "<html><head><title>Not Found</title></head><body><h1>404 Not Found</h1><p>The page you're looking for doesn't exist</p></body></html>";
                return new HttpResponse("HTTP/1.1", "404 Not Found", "text/html", text.length(), text);
            }
            if (!Files.isRegularFile(real_target_path)) {
                String text = "<html><head><title>Not Found</title></head><body><h1>404 Not Found</h1><p>The page you're looking for doesn't exist</p></body></html>";
                return new HttpResponse("HTTP/1.1", "404 Not Found", "text/html", text.length(), text);
            }

            String body = Files.readString(real_target_path);
            return new HttpResponse("HTTP/1.1", "200 OK", "text/html", body.length(), body);

        } catch (IOException e) {
            String text = "<html><head><title>Not Found</title></head><body><h1>404 Not Found</h1><p>The page you're looking for doesn't exist</p></body></html>";
            return new HttpResponse("HTTP/1.1", "404 Not Found", "text/html", text.length(), text);
        }
    }
}