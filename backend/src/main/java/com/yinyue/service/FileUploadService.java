package com.yinyue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ======================================================================================
 * 类名：FileUploadService (文件搬运工)
 * 
 * 作用：专门负责处理文件的上传、保存和删除。
 * 
 * 当用户在网页上点击“上传”按钮时，文件会先发给 Controller，
 * 然后 Controller 会把文件交给这个 Service。
 * 这个 Service 就像是一个负责任的图书管理员，它会把文件收下来，
 * 给它改个不重复的名字（防止重名冲突），然后整整齐齐地放到书架（硬盘文件夹）上。
 * ======================================================================================
 */
@Service
public class FileUploadService {

    private static final Map<String, List<String>> ALLOWED_EXTENSIONS = Map.of(
            "audio", List.of("mp3", "wav", "m4a", "flac", "ogg", "aac"),
            "image", List.of("png", "jpg", "jpeg", "webp")
    );

    private static final Map<String, List<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "audio", List.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave", "audio/mp4", "audio/aac", "audio/flac", "audio/x-flac", "audio/ogg"),
            "image", List.of("image/png", "image/jpeg", "image/webp")
    );

    // 从配置文件里读取我们要在哪里存文件。
    // 如果配置文件没写，默认就存在当前目录下的 "uploads" 文件夹里。
    @Value("${app.storage.local-path:./runtime/uploads}")
    private String uploadPath;

    /**
     * 方法名：uploadFile (上传文件)
     * 
     * 作用：把用户传过来的文件保存到硬盘上。
     * 
     * @param file 用户传过来的文件对象
     * @param type 文件类型 (比如 "audio" 表示音频，"image" 表示图片)，我们会根据类型把它们分开放。
     * @return 保存后的文件路径（告诉外面我把文件放哪了）
     */
    public String uploadFile(MultipartFile file, String type) throws IOException {
        // 先检查一下是不是传了个寂寞（空文件）
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String normalizedType = type.toLowerCase(Locale.ROOT);
        byte[] fileBytes = file.getBytes();
        validateFile(file, normalizedType, fileBytes);

        // 1. 准备目录
        // 拼凑出目标文件夹的路径，比如 "./uploads/audio"
        Path uploadDir = getUploadRoot().resolve(normalizedType).normalize();
        // 如果这个文件夹不存在，就创建一个。Files.createDirectories 很聪明，多级目录也能一次创建好。
        Files.createDirectories(uploadDir);

        // 2. 给文件起个新名字
        // 为什么要改名？因为如果两个用户都上传了 "love.mp3"，后上传的就会把前面的覆盖掉。
        // 所以我们要用 UUID (通用唯一识别码) 生成一个世界上独一无二的乱码名字，比如 "550e8400-e29b-41d4...mp3"。
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename); // 获取后缀名 (.mp3)
        String uniqueFilename = UUID.randomUUID() + "." + fileExtension;

        // 3. 真正开始保存
        // 算出文件的完整路径：目录 + 新文件名
        Path filePath = uploadDir.resolve(uniqueFilename).normalize();
        // 把文件的内容（字节流）写到硬盘上
        Files.write(filePath, fileBytes);

        // 4. 告诉外面保存好了，路径在这里
        return filePath.toAbsolutePath().normalize().toString();
    }

    /**
     * 方法名：getFileExtension (获取后缀名)
     * 
     * 作用：从文件名里提取出后缀，比如从 "song.mp3" 提取出 "mp3"。
     * 
     * @param filename 文件名
     * @return 后缀名（小写）
     */
    private String getFileExtension(String filename) {
        // 如果文件名不为空，而且里面有点号 "."
        if (filename != null && filename.contains(".")) {
            // 截取最后一个点号后面的所有字符，并转成小写
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        // 如果实在找不到后缀，就叫它 "unknown" (未知)
        return "unknown";
    }

    /**
     * 方法名：deleteFile (删除文件)
     * 
     * 作用：当数据库里的记录被删除时，顺手把硬盘上的文件也删掉，节省空间。
     * 
     * @param filePath 文件的完整路径
     */
    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        // 先看看文件还在不在，在的话再删
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * 方法名：fileExists (检查文件是否存在)
     * 
     * 作用：在处理文件之前，先确认一下文件是不是真的在那里，防止报错。
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public Path getUploadRoot() {
        return Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    public String normalizeManagedPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        Path normalized = Paths.get(filePath).toAbsolutePath().normalize();
        if (!normalized.startsWith(getUploadRoot())) {
            throw new IllegalArgumentException("不允许访问上传目录之外的文件");
        }
        return normalized.toString();
    }

    public Path resolveManagedFile(String filePath) {
        Path normalized = Paths.get(normalizeManagedPath(filePath));
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException("音频文件不存在: " + normalized);
        }
        return normalized;
    }

    private void validateFile(MultipartFile file, String type, byte[] fileBytes) {
        if (!ALLOWED_EXTENSIONS.containsKey(type)) {
            throw new IllegalArgumentException("不支持的文件类型分类: " + type);
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.get(type).contains(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.get(type).contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("不支持的文件 Content-Type: " + contentType);
        }

        if (!matchesMagicHeader(type, extension, fileBytes)) {
            throw new IllegalArgumentException("文件内容与扩展名不匹配");
        }
    }

    private boolean matchesMagicHeader(String type, String extension, byte[] fileBytes) {
        if (fileBytes.length < 12) {
            return false;
        }

        if ("audio".equals(type)) {
            return switch (extension) {
                case "mp3" -> startsWith(fileBytes, "ID3") || (fileBytes[0] & 0xFF) == 0xFF;
                case "wav" -> startsWith(fileBytes, "RIFF") && containsAt(fileBytes, "WAVE", 8);
                case "flac" -> startsWith(fileBytes, "fLaC");
                case "ogg" -> startsWith(fileBytes, "OggS");
                case "m4a", "aac" -> containsAt(fileBytes, "ftyp", 4);
                default -> false;
            };
        }

        return switch (extension) {
            case "png" -> startsWith(fileBytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            case "jpg", "jpeg" -> startsWith(fileBytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "webp" -> startsWith(fileBytes, "RIFF") && containsAt(fileBytes, "WEBP", 8);
            default -> false;
        };
    }

    private boolean startsWith(byte[] fileBytes, String signature) {
        return startsWith(fileBytes, signature.getBytes(StandardCharsets.US_ASCII));
    }

    private boolean startsWith(byte[] fileBytes, byte[] signature) {
        if (fileBytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (fileBytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAt(byte[] fileBytes, String signature, int offset) {
        byte[] signatureBytes = signature.getBytes(StandardCharsets.US_ASCII);
        if (fileBytes.length < offset + signatureBytes.length) {
            return false;
        }

        for (int i = 0; i < signatureBytes.length; i++) {
            if (fileBytes[offset + i] != signatureBytes[i]) {
                return false;
            }
        }
        return true;
    }
}
