package com.sportify.field.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CloudinaryService {

    @ConfigProperty(name = "cloudinary.cloud-name")
    Optional<String> cloudName;

    @ConfigProperty(name = "cloudinary.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "cloudinary.api-secret")
    Optional<String> apiSecret;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadFieldImage(FileUpload fileUpload) {
        String configuredCloudName = cloudName.filter(value -> !value.isBlank()).orElse(null);
        String configuredApiKey = apiKey.filter(value -> !value.isBlank()).orElse(null);
        String configuredApiSecret = apiSecret.filter(value -> !value.isBlank()).orElse(null);

        if (configuredCloudName == null || configuredApiKey == null || configuredApiSecret == null) {
            throw ServiceException.badRequest("Cloudinary configuration is missing");
        }

        if (fileUpload == null || fileUpload.uploadedFile() == null) {
            throw ServiceException.badRequest("Image file is required");
        }

        String contentType = fileUpload.contentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw ServiceException.badRequest("Only image files are allowed");
        }

        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String folder = "sportify/fields";
            String publicId = folder + "/" + UUID.randomUUID();
            String signature = sha1("folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + configuredApiSecret);
            String boundary = "----SportifyBoundary" + UUID.randomUUID();

            byte[] body = multipartBody(boundary, fileUpload, configuredApiKey, publicId, folder, timestamp, signature);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudinary.com/v1_1/" + configuredCloudName + "/image/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw ServiceException.badRequest("Cloudinary upload failed: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("secure_url").asText();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.badRequest("Cloudinary upload failed: " + e.getMessage());
        }
    }

    private byte[] multipartBody(String boundary, FileUpload fileUpload, String configuredApiKey, String publicId,
                                 String folder, long timestamp, String signature) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        addFormField(output, boundary, "api_key", configuredApiKey);
        addFormField(output, boundary, "timestamp", String.valueOf(timestamp));
        addFormField(output, boundary, "folder", folder);
        addFormField(output, boundary, "public_id", publicId);
        addFormField(output, boundary, "signature", signature);
        addFileField(output, boundary, fileUpload);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void addFormField(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void addFileField(ByteArrayOutputStream output, String boundary, FileUpload fileUpload) throws IOException {
        String fileName = fileUpload.fileName() != null ? fileUpload.fileName() : "field-image";
        String contentType = fileUpload.contentType() != null ? fileUpload.contentType() : "application/octet-stream";
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(Files.readAllBytes(fileUpload.uploadedFile()));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sha1(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
