package com.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class App {

  public static void main(String[] args) throws Exception {
    int port = 8080;
    String envPort = System.getenv("PORT");
    if (envPort != null && !envPort.trim().isEmpty()) {
      try { port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    server.createContext("/health", ex -> writeText(ex, 200, "ok"));

    server.createContext("/hello", ex -> {
      String name = getQueryParam(ex.getRequestURI(), "name");
      if (name == null || name.trim().isEmpty()) name = "mundo";
      writeText(ex, 200, hello(name));
    });

    server.start();
    System.out.println("Server running on http://0.0.0.0:" + port);
  }

  static String hello(String name) {
    return "hola " + name;
  }

  static String getQueryParam(URI uri, String key) {
    String q = uri.getRawQuery();
    if (q == null) return null;
    String[] parts = q.split("&");
    for (String p : parts) {
      String[] kv = p.split("=", 2);
      if (kv.length == 2 && kv[0].equals(key)) return kv[1].replace("+", " ");
    }
    return null;
  }

  static void writeText(HttpExchange ex, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }
}
