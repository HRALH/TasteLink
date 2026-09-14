package com.tastelink.service.impl;

import com.tastelink.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LocalFileStorageServiceImpl 单测（B1-1）：isOwnedUrl 白名单 + delete 路径穿越防御。
 */
class LocalFileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.getLocal().setBasePath(tempDir.toString());
        // publicBaseUrl/urlPrefix 用默认值 http://localhost:8080 + /static/uploads
        service = new LocalFileStorageServiceImpl(props);
    }

    // ---------- isOwnedUrl ----------

    @Test
    void isOwnedUrl_validUploadUrl_true() {
        assertTrue(service.isOwnedUrl("http://localhost:8080/static/uploads/2026/09/a.jpg"));
    }

    @Test
    void isOwnedUrl_traversalUrl_false() {
        // 前缀匹配但 key 含 ".." 段 —— 路径穿越
        assertFalse(service.isOwnedUrl("http://localhost:8080/static/uploads/../../etc/passwd"));
    }

    @Test
    void isOwnedUrl_foreignUrl_false() {
        assertFalse(service.isOwnedUrl("https://evil.example.com/static/uploads/2026/09/a.jpg"));
    }

    @Test
    void isOwnedUrl_blankUrl_false() {
        assertFalse(service.isOwnedUrl(""));
        assertFalse(service.isOwnedUrl(null));
    }

    // ---------- delete 越界防御 ----------

    @Test
    void delete_traversalKey_doesNotTouchOutsideFile() throws IOException {
        Path outside = tempDir.getParent().resolve("outside-b11-" + System.nanoTime() + ".txt");
        Files.writeString(outside, "do-not-delete");
        try {
            // key 越出 base 目录 → 拒绝删除且不抛异常
            service.delete("../" + outside.getFileName());
            assertTrue(Files.exists(outside), "越界 key 不应删除 base 目录外的文件");
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void delete_normalKey_deletesFile() throws IOException {
        Path dir = tempDir.resolve("2026/09");
        Files.createDirectories(dir);
        Path target = dir.resolve("a.jpg");
        Files.writeString(target, "img");

        service.delete("2026/09/a.jpg");

        assertFalse(Files.exists(target));
    }

    @Test
    void delete_blankKey_isNoOp() {
        service.delete("");
        service.delete(null);
    }
}
