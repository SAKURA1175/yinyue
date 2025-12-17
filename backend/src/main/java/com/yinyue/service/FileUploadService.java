package com.yinyue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // 从配置文件里读取我们要在哪里存文件。
    // 如果配置文件没写，默认就存在当前目录下的 "uploads" 文件夹里。
    @Value("${app.storage.local-path:./uploads}")
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

        // 1. 准备目录
        // 拼凑出目标文件夹的路径，比如 "./uploads/audio"
        Path uploadDir = Paths.get(uploadPath, type);
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
        Path filePath = uploadDir.resolve(uniqueFilename);
        // 把文件的内容（字节流）写到硬盘上
        Files.write(filePath, file.getBytes());

        // 4. 告诉外面保存好了，路径在这里
        return filePath.toString();
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
}
