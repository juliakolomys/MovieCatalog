package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final String rootDirectory;

    public StaticFileHandler(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(rootDirectory, path.substring(1));


            System.out.println("Requested file: " + file.getAbsolutePath());

        if (!file.exists() || !file.isFile()) {
            String error = "404 Not Found: " + path;
            exchange.sendResponseHeaders(404, error.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(error.getBytes());
            }
            return;
        }

        byte[] response = Files.readAllBytes(file.toPath());

        String contentType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", contentType);

        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
        if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (fileName.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }
}
