package com.dms.deverytime.global.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ImageStorageTimeoutTest {
    @Test
    void uploadAndDeleteTimeOutWhenServerDoesNotRespond() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var received = new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/", exchange -> {
            received.incrementAndGet();
            try {
                exchange.getRequestBody().readAllBytes();
                Thread.sleep(2500);
                String response = exchange.getRequestURI().getPath().endsWith("/upload")
                        ? "{\"public_id\":\"posts/test\",\"secure_url\":\"https://example.com/test.png\"}"
                        : "{\"result\":\"ok\"}";
                byte[] body = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        try {
            var cloudinary = new Cloudinary(ObjectUtils.asMap("cloud_name", "test", "api_key", "key",
                    "api_secret", "secret", "secure", false, "timeout", 1,
                    "upload_prefix", "http://127.0.0.1:" + server.getAddress().getPort()));
            var storage = new ImageStorageService(cloudinary);
            var image = new MockMultipartFile("file", "a.png", "image/png",
                    new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            long start = System.nanoTime();
            assertThatThrownBy(() -> storage.uploadReservedPostImage(image, "posts/test"))
                    .isInstanceOf(DeveryTimeException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
            start = System.nanoTime();
            assertThatThrownBy(() -> storage.delete("posts/test"))
                    .isInstanceOf(DeveryTimeException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
            assertThat(received.get()).isEqualTo(2);
            var relaxed = new ImageStorageService(new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "test", "api_key", "key", "api_secret", "secret",
                    "secure", false, "timeout", 5,
                    "upload_prefix", "http://127.0.0.1:" + server.getAddress().getPort())));
            assertThat(relaxed.uploadReservedPostImage(image, "posts/test").profileImagePublicId())
                    .isEqualTo("posts/test");
            assertThatCode(() -> relaxed.delete("posts/test")).doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }
}
