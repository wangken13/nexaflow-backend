package com.vebcoding.trade.file.service;

import com.vebcoding.trade.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

final class FileContentValidator {
    private static final byte[] PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ZIP = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final Set<String> TEXT_EXTENSIONS = Set.of("csv", "txt");

    private FileContentValidator() {
    }

    static void validate(String extension, byte[] prefix) {
        boolean valid = switch (extension) {
            case "pdf" -> startsWith(prefix, PDF);
            case "png" -> startsWith(prefix, PNG);
            case "jpg", "jpeg" -> startsWith(prefix, JPEG);
            case "webp" -> isWebp(prefix);
            case "doc" -> startsWith(prefix, OLE) || startsWith(prefix, "{\\rtf".getBytes(StandardCharsets.US_ASCII));
            case "xls" -> startsWith(prefix, OLE);
            case "docx", "xlsx" -> startsWith(prefix, ZIP);
            default -> TEXT_EXTENSIONS.contains(extension) && isText(prefix);
        };
        if (!valid) {
            throw new BusinessException("文件内容与扩展名不匹配，请上传真实且未损坏的文件");
        }
    }

    private static boolean startsWith(byte[] value, byte[] signature) {
        return value.length >= signature.length
                && Arrays.equals(value, 0, signature.length, signature, 0, signature.length);
    }

    private static boolean isWebp(byte[] value) {
        return value.length >= 12
                && startsWith(value, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(value, 8, 12, "WEBP".getBytes(StandardCharsets.US_ASCII), 0, 4);
    }

    private static boolean isText(byte[] value) {
        for (byte current : value) {
            int unsigned = Byte.toUnsignedInt(current);
            if (unsigned == 0 || (unsigned < 0x20 && unsigned != '\t' && unsigned != '\n' && unsigned != '\r')) {
                return false;
            }
        }
        return true;
    }
}
