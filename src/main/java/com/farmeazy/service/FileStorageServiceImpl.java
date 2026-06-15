package com.farmeazy.service;

import com.farmeazy.config.FileStorageProperties;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "mp4", "webm",
        "pdf", "txt", "csv", "doc", "docx", "xls", "xlsx"
    );
    private static final List<String> DANGEROUS_PREFIXES = List.of(
        "application/x-msdownload", "application/x-sh", "application/x-executable"
    );
    private static final Map<String, List<String>> ALLOWED_MIME_TYPES = createAllowedMimeTypeMap();
    private static final String OBJECT_KEY_PREFIX = "attachments";

    private final S3Client s3Client;
    private final String bucketName;
    private final String uploadDir;
    private final Tika tika;

    @Autowired
    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties, S3Client s3Client,
                                  org.springframework.core.env.Environment environment) {
        this.s3Client = s3Client;
        this.bucketName = environment.getProperty("aws.s3.bucket-name", "farmeazy-prod-attachments");
        this.uploadDir = fileStorageProperties.getUploadDir();
        this.tika = new Tika();
    }

    @Override
    public String store(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty files are not allowed");
        }
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (originalName.contains("..")) {
            throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + originalName);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds max size of 5MB");
        }

        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        String declaredContentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        for (String prefix : DANGEROUS_PREFIXES) {
            if (declaredContentType.startsWith(prefix)) {
                throw new IllegalArgumentException("Dangerous file content type is not allowed");
            }
        }

        try {
            byte[] bytes = file.getBytes();
            String detectedContentType = tika.detect(bytes, originalName).toLowerCase(Locale.ROOT);

            for (String prefix : DANGEROUS_PREFIXES) {
                if (detectedContentType.startsWith(prefix)) {
                    throw new IllegalArgumentException("Dangerous file content type is not allowed");
                }
            }

            if (!isAllowedMimeType(extension, detectedContentType)) {
                throw new IllegalArgumentException("Uploaded file content does not match extension: " + extension);
            }

            String objectKey = buildS3Key(extension);
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(detectedContentType)
                    .contentLength((long) bytes.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
            logger.info("S3_UPLOAD_SUCCESS key={} size={} originalName={}", objectKey, bytes.length, originalName);
            return encodeKey(objectKey);
        } catch (IOException ex) {
            logger.error("S3_UPLOAD_FAILURE originalName={}", originalName, ex);
            throw new RuntimeException("Could not store file " + originalName + ". Please try again!", ex);
        } catch (S3Exception ex) {
            logger.error("S3_UPLOAD_FAILURE key generation failed originalName={}", originalName, ex);
            throw new RuntimeException("Could not store file " + originalName + ". AWS S3 upload failure.", ex);
        }
    }

    private boolean isAllowedMimeType(String extension, String detectedContentType) {
        List<String> allowed = ALLOWED_MIME_TYPES.get(extension);
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        for (String allowedType : allowed) {
            if (detectedContentType.equals(allowedType) || detectedContentType.startsWith(allowedType + ";")
                    || allowedType.endsWith("/*") && detectedContentType.startsWith(allowedType.replace("/*", "/"))) {
                return true;
            }
        }
        return false;
    }

    private String buildS3Key(String extension) {
        LocalDate now = LocalDate.now();
        return String.format("%s/%04d/%02d/%s.%s",
                OBJECT_KEY_PREFIX,
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID(),
                extension);
    }

    private static Map<String, List<String>> createAllowedMimeTypeMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("jpg", List.of("image/jpeg"));
        map.put("jpeg", List.of("image/jpeg"));
        map.put("png", List.of("image/png"));
        map.put("gif", List.of("image/gif"));
        map.put("webp", List.of("image/webp"));
        map.put("mp4", List.of("video/mp4"));
        map.put("webm", List.of("video/webm"));
        map.put("pdf", List.of("application/pdf"));
        map.put("txt", List.of("text/plain"));
        map.put("csv", List.of("text/csv", "application/csv", "text/plain"));
        map.put("doc", List.of("application/msword"));
        map.put("docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        map.put("xls", List.of("application/vnd.ms-excel"));
        map.put("xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return Map.copyOf(map);
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String encodeKey(String objectKey) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeKey(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public Stream<Path> loadAll() {
        throw new UnsupportedOperationException("S3 storage does not support listing all files through Path API");
    }

    @Override
    public Path load(String filename) {
        throw new UnsupportedOperationException("Direct Path access is not supported for S3-backed storage");
    }

    @Override
    public Resource loadAsResource(String filename) {
        String objectKey = decodeKey(filename);
        boolean usingArrow = true;

        if (objectKey == null) {
            // If the filename was passed as a raw S3 key or legacy file name, use it directly.
            objectKey = filename;
            usingArrow = objectKey.contains("/");
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
            logger.info("S3_DOWNLOAD_SUCCESS key={}", objectKey);
            String filenameOnly = Paths.get(objectKey).getFileName().toString();
            return new S3Resource(s3Object, filenameOnly, s3Object.response().contentLength());
        } catch (S3Exception ex) {
            if (!usingArrow) {
                // fallback to local storage for legacy attachments if present
                Path localPath = Paths.get(uploadDir, filename);
                if (Files.exists(localPath)) {
                    try {
                        logger.info("LOCAL_FALLBACK_DOWNLOAD key={}", filename);
                        return new InputStreamResource(Files.newInputStream(localPath)) {
                            @Override
                            public String getFilename() {
                                return localPath.getFileName().toString();
                            }
                        };
                    } catch (IOException ioEx) {
                        logger.error("LOCAL_FALLBACK_FAILURE key={}", filename, ioEx);
                    }
                }
            }
            logger.error("S3_DOWNLOAD_FAILURE key={}", objectKey, ex);
            throw new RuntimeException("Could not read file: " + filename, ex);
        }
    }

    @Override
    public void deleteAll() {
        logger.warn("deleteAll() called on S3-backed FileStorageService; operation is a no-op");
    }

    @Override
    public void init() {
        logger.info("init() called on S3-backed FileStorageService; no local storage initialization required");
    }

    private static class S3Resource extends InputStreamResource {
        private final String filename;
        private final long contentLength;

        public S3Resource(InputStream inputStream, String filename, long contentLength) {
            super(inputStream);
            this.filename = filename;
            this.contentLength = contentLength;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }
    }
}
