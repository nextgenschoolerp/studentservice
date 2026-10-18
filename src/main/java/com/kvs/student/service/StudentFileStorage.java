package com.kvs.student.service;

import com.kvs.core.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Local file vault for student photos and documents: {base-path}/{tenant}/{studentId}/{random}.{ext}.
 * Checks the size, the declared type and the file signature before anything is written.
 */
@Component
@Slf4j
public class StudentFileStorage {

    /** Photos: JPEG or PNG. */
    public static final Map<String, String> IMAGE_TYPES = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    /** Documents: PDF, JPEG or PNG. */
    public static final Map<String, String> DOCUMENT_TYPES = Map.of("application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");

    private final Path basePath;

    public StudentFileStorage(@Value("${student.file-vault.base-path:./student-vault}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    public StoredFile store(String tenantId, UUID studentId, MultipartFile file, Map<String, String> allowedTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Uploaded file exceeds the maximum size of " + (maxBytes / (1024 * 1024)) + " MB");
        }
        String extension = allowedTypes.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Unsupported file type " + file.getContentType() + ". Allowed types: " + allowedTypes.keySet());
        }
        if (!hasMatchingSignature(file, file.getContentType())) {
            throw new BadRequestException("File content does not match the declared type " + file.getContentType());
        }
        String tenantSegment = tenantId.replaceAll("[^A-Za-z0-9_-]", "_");
        Path relative = Path.of(tenantSegment, studentId.toString(), UUID.randomUUID() + extension);
        Path target = basePath.resolve(relative).normalize();
        if (!target.startsWith(basePath)) {
            throw new BadRequestException("Invalid file storage path");
        }
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(relative.toString().replace('\\', '/'), HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Reads a stored file; null when it is missing on disk. */
    public byte[] read(String relativePath) {
        Path target = basePath.resolve(relativePath).normalize();
        if (!target.startsWith(basePath) || !Files.exists(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the stored file", e);
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null) {
            return;
        }
        try {
            Path target = basePath.resolve(relativePath).normalize();
            if (target.startsWith(basePath)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete superseded file {}: {}", relativePath, e.getMessage());
        }
    }

    private boolean hasMatchingSignature(MultipartFile file, String contentType) {
        byte[] expected = switch (contentType) {
            case "application/pdf" -> new byte[]{'%', 'P', 'D', 'F'};
            case "image/jpeg" -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            case "image/png" -> new byte[]{(byte) 0x89, 'P', 'N', 'G'};
            default -> null;
        };
        if (expected == null) {
            return false;
        }
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(expected.length), expected);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded file", e);
        }
    }

    public record StoredFile(String relativePath, String checksumSha256) {
    }
}
