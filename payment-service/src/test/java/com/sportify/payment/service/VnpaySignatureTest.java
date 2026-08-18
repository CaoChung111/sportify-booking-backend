package com.sportify.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho logic bao mat thanh toan VNPay trong PaymentService.
 * Kiem tra ham ma hoa HMAC-SHA512 va xac thuc chu ky so (Signature Verification).
 *
 * Logic goc: PaymentService.hmacSHA512(), verifyVnpaySignature(), encodeVnpay()
 */
@DisplayName("PaymentService - Kiem thu bao mat chu ky VNPay HMAC-SHA512")
public class VnpaySignatureTest {

    private static final String TEST_SECRET = "SPORTIFYHASHSECRET2024";

    // ---- Replicate logic tu PaymentService.java ----

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.US_ASCII), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }

    private String encodeVnpay(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private boolean verifyVnpaySignature(Map<String, String> params, String receivedHash, String secret) {
        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");
        String signData = sorted.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isBlank())
            .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
            .collect(Collectors.joining("&"));
        String computed = hmacSHA512(secret, signData);
        return computed.equalsIgnoreCase(receivedHash);
    }

    // ---- Test Cases ----

    @Nested
    @DisplayName("1. Ham bam HMAC-SHA512")
    class HmacTests {

        @Test
        @DisplayName("Ket qua bam co do dai 128 ky tu hex (512 bit)")
        void hashLength_is128Hex() {
            String hash = hmacSHA512("key", "data");
            assertEquals(128, hash.length());
        }

        @Test
        @DisplayName("Bam cung key va data cho ket qua giong nhau (deterministic)")
        void sameInput_sameOutput() {
            String h1 = hmacSHA512("secret", "payment_data_123");
            String h2 = hmacSHA512("secret", "payment_data_123");
            assertEquals(h1, h2);
        }

        @Test
        @DisplayName("Thay doi secret key => ket qua bam khac")
        void differentKey_differentHash() {
            String h1 = hmacSHA512("key_A", "same_data");
            String h2 = hmacSHA512("key_B", "same_data");
            assertNotEquals(h1, h2);
        }

        @Test
        @DisplayName("Thay doi du lieu dau vao => ket qua bam khac")
        void differentData_differentHash() {
            String h1 = hmacSHA512("same_key", "amount=100000");
            String h2 = hmacSHA512("same_key", "amount=200000");
            assertNotEquals(h1, h2);
        }

        @Test
        @DisplayName("Ket qua bam luon la chu thuong (lowercase hex)")
        void hash_isLowercase() {
            String hash = hmacSHA512("key", "data");
            assertEquals(hash, hash.toLowerCase());
        }
    }

    @Nested
    @DisplayName("2. Xac thuc chu ky VNPay Callback")
    class SignatureVerificationTests {

        private Map<String, String> createSampleParams() {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Amount", "15000000");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", "SPORTIFY1");
            params.put("vnp_TxnRef", "SPF1001692345678");
            params.put("vnp_ResponseCode", "00");
            return params;
        }

        @Test
        @DisplayName("Chu ky hop le => xac thuc thanh cong (true)")
        void validSignature_returnsTrue() {
            Map<String, String> params = createSampleParams();
            // Tinh hash dung
            String signData = params.entrySet().stream()
                .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
                .collect(Collectors.joining("&"));
            String validHash = hmacSHA512(TEST_SECRET, signData);
            params.put("vnp_SecureHash", validHash);

            assertTrue(verifyVnpaySignature(params, validHash, TEST_SECRET));
        }

        @Test
        @DisplayName("Chu ky bi gia mao => xac thuc that bai (false)")
        void tamperedHash_returnsFalse() {
            Map<String, String> params = createSampleParams();
            String fakeHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890" +
                              "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            params.put("vnp_SecureHash", fakeHash);

            assertFalse(verifyVnpaySignature(params, fakeHash, TEST_SECRET));
        }

        @Test
        @DisplayName("So tien bi thay doi => chu ky khong khop (chong gia mao giao dich)")
        void tamperedAmount_returnsFalse() {
            Map<String, String> params = createSampleParams();
            // Tinh hash voi so tien goc (15,000,000)
            String signData = params.entrySet().stream()
                .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
                .collect(Collectors.joining("&"));
            String validHash = hmacSHA512(TEST_SECRET, signData);
            // Sua so tien thanh 1,000,000 (gia mao)
            params.put("vnp_Amount", "1000000");
            params.put("vnp_SecureHash", validHash);

            assertFalse(verifyVnpaySignature(params, validHash, TEST_SECRET));
        }

        @Test
        @DisplayName("vnp_SecureHash va vnp_SecureHashType bi loai bo truoc khi bam")
        void secureHashFields_excludedFromSigning() {
            Map<String, String> params = createSampleParams();
            String signData = params.entrySet().stream()
                .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
                .collect(Collectors.joining("&"));
            String validHash = hmacSHA512(TEST_SECRET, signData);

            // Them ca 2 truong bao mat vao params
            params.put("vnp_SecureHash", validHash);
            params.put("vnp_SecureHashType", "SHA512");

            // Verify van thanh cong vi 2 truong nay bi loai truoc khi bam
            assertTrue(verifyVnpaySignature(params, validHash, TEST_SECRET));
        }

        @Test
        @DisplayName("Tham so duoc sap xep theo thu tu ASCII truoc khi bam")
        void params_sortedAlphabetically() {
            // Tao params khong theo thu tu
            Map<String, String> unsorted = new TreeMap<>();
            unsorted.put("vnp_TxnRef", "SPF100");
            unsorted.put("vnp_Amount", "100000");
            // TreeMap tu dong sap xep -> vnp_Amount truoc vnp_TxnRef

            String signData = "vnp_Amount=100000&vnp_TxnRef=SPF100";
            String expectedHash = hmacSHA512(TEST_SECRET, signData);

            assertTrue(verifyVnpaySignature(unsorted, expectedHash, TEST_SECRET));
        }
    }
}
