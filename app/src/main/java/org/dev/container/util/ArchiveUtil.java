package org.dev.container.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * 归档解压工具类
 */
public final class ArchiveUtil {

    private ArchiveUtil() {}

    public interface ProgressListener {
        void onProgress(String message, int count);
    }

    /**
     * 流式提取 tar.gz
     */
    public static void extractTarGz(File tarGzFile, File targetDir, ProgressListener listener) throws Exception {
        File tarFile = new File(targetDir.getParentFile(), "temp_rootfs.tar");
        
        if (listener != null) listener.onProgress("解压 GZIP 流...", 0);
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(tarGzFile));
             FileOutputStream fos = new FileOutputStream(tarFile)) {
            byte[] buffer = new byte[16384];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        if (listener != null) listener.onProgress("提取 TAR 目录树...", 0);
        extractTarArchive(tarFile, targetDir, listener);

        if (tarFile.exists()) {
            tarFile.delete();
        }
    }

    /**
     * 标准 POSIX TAR 解析提取
     */
    public static void extractTarArchive(File tarFile, File targetDir, ProgressListener listener) throws Exception {
        try (FileInputStream fis = new FileInputStream(tarFile)) {
            byte[] header = new byte[512];
            int fileCount = 0;

            while (fis.read(header) == 512) {
                // 检查全零块（归档结尾）
                boolean isZeroBlock = true;
                for (byte b : header) {
                    if (b != 0) {
                        isZeroBlock = false;
                        break;
                    }
                }
                if (isZeroBlock) break;

                // 提取文件名
                String name = new String(header, 0, 100).trim().replace("\0", "");
                if (name.isEmpty()) continue;

                // 提取文件大小（八进制字符）
                String sizeStr = new String(header, 124, 12).trim().replace("\0", "");
                long size = 0;
                if (!sizeStr.isEmpty()) {
                    try {
                        size = Long.parseLong(sizeStr, 8);
                    } catch (NumberFormatException ignored) {}
                }

                // 类型标识 (0/null = regular file, 5 = dir, 2 = symlink)
                byte typeFlag = header[156];
                File outputFile = new File(targetDir, name);

                if (typeFlag == '5' || name.endsWith("/")) {
                    if (!outputFile.exists()) outputFile.mkdirs();
                } else {
                    File parent = outputFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        long remaining = size;
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            int bytesRead = fis.read(buffer, 0, toRead);
                            if (bytesRead == -1) break;
                            fos.write(buffer, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                    }

                    fileCount++;
                    if (listener != null && fileCount % 500 == 0) {
                        listener.onProgress("已提取 " + fileCount + " 个节点...", fileCount);
                    }
                }

                // 512 字节对齐填充跳过
                long padding = (512 - (size % 512)) % 512;
                if (padding > 0) {
                    long skipped = fis.skip(padding);
                    while (skipped < padding) {
                        long s = fis.skip(padding - skipped);
                        if (s <= 0) break;
                        skipped += s;
                    }
                }
            }
            if (listener != null) {
                listener.onProgress("总计提取完成: " + fileCount + " 个节点", fileCount);
            }
        }
    }
}