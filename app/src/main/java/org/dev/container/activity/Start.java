package org.dev.container.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.dev.container.databinding.LayoutStartBinding;
import org.dev.container.util.ArchiveUtil;
import org.dev.container.util.PermissionUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Start extends AppCompatActivity {

    private static final String UBUNTU_URL = "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-arm64.tar.gz";
    private static final String ARCHIVE_NAME = "ubuntu-base-26.04-base-arm64.tar.gz";

    private LayoutStartBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> appSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                updatePermissionStatus();
                checkAndProceedWorkflow();
            });

    private final ActivityResultLauncher<Intent> manageStorageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                updatePermissionStatus();
                checkAndProceedWorkflow();
            });

    private final ActivityResultLauncher<Intent> installPackagesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                updatePermissionStatus();
                checkAndProceedWorkflow();
            });

    private final ActivityResultLauncher<String[]> runtimePermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                updatePermissionStatus();
                checkAndProceedWorkflow();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LayoutStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updatePermissionStatus();
        initListeners();

        // 启动时自动检查是否已具备完整环境
        checkAndProceedWorkflow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean hasStorage = PermissionUtil.hasStoragePermission(this);
        boolean hasInstall = PermissionUtil.hasInstallPermission(this);
        boolean hasNotif = PermissionUtil.hasNotificationPermission(this);

        binding.tvPermStorageStatus.setText(hasStorage ? "• 全文件管理权限: [已授权]" : "• 全文件管理权限: [未授权]");
        binding.tvPermInstallStatus.setText(hasInstall ? "• APK 安装权限: [已授权]" : "• APK 安装权限: [未授权]");
        binding.tvPermNotifStatus.setText(hasNotif ? "• 通知权限: [已授权]" : "• 通知权限: [未授权]");
    }

    private boolean isContainerInstalled() {
        File rootfsDir = new File(getFilesDir(), "rootfs");
        File etcDir = new File(rootfsDir, "etc");
        File binDir = new File(rootfsDir, "bin");
        return rootfsDir.exists() && etcDir.exists() && binDir.exists();
    }

    private void checkAndProceedWorkflow() {
        // 1. 检查权限
        if (!PermissionUtil.isAllPermissionsGranted(this)) {
            return;
        }

        // 2. 检查容器是否已部署
        if (isContainerInstalled()) {
            log("检测到完整权限且 Ubuntu 容器已就绪，正在进入系统...");
            navigateToHome();
        }
    }

    private void requestNextPermission() {
        if (!PermissionUtil.hasNotificationPermission(this)) {
            PermissionUtil.requestNotificationPermission(this, appSettingsLauncher, runtimePermLauncher);
            return;
        }

        if (!PermissionUtil.hasStoragePermission(this)) {
            PermissionUtil.requestStoragePermission(this, manageStorageLauncher, runtimePermLauncher);
            return;
        }

        if (!PermissionUtil.hasInstallPermission(this)) {
            PermissionUtil.requestInstallPermission(this, installPackagesLauncher);
            return;
        }

        Toast.makeText(this, "所有核心权限均已授予！", Toast.LENGTH_SHORT).show();
        checkAndProceedWorkflow();
    }

    private void initListeners() {
        binding.btnGrantPermissions.setOnClickListener(v -> requestNextPermission());

        binding.btnStartDeploy.setOnClickListener(v -> {
            if (!PermissionUtil.isAllPermissionsGranted(this)) {
                Toast.makeText(this, "请先授予必要权限再进行部署", Toast.LENGTH_SHORT).show();
                requestNextPermission();
                return;
            }

            binding.btnStartDeploy.setEnabled(false);
            binding.btnGrantPermissions.setEnabled(false);
            executor.execute(this::executeDownloadAndDeploy);
        });
    }

    private void navigateToHome() {
        mainHandler.postDelayed(() -> {
            Intent intent = new Intent(Start.this, Home.class);
            startActivity(intent);
            finish();
        }, 800);
    }

    private void log(String message) {
        runOnUiThread(() -> {
            String current = binding.tvLogConsole.getText().toString();
            binding.tvLogConsole.setText(message + "\n" + current);
            binding.scrollLog.scrollTo(0, 0);
        });
    }

    private void setDeployStatus(String status, int progress, boolean indeterminate) {
        runOnUiThread(() -> {
            binding.tvDeployStatus.setText(status);
            binding.progressDeploy.setIndeterminate(indeterminate);
            if (!indeterminate) {
                binding.progressDeploy.setProgress(progress);
            }
        });
    }

    private void executeDownloadAndDeploy() {
        File cacheDir = getExternalFilesDir(null);
        if (cacheDir == null) {
            cacheDir = getFilesDir();
        }
        File targetFile = new File(cacheDir, ARCHIVE_NAME);
        File rootfsDir = new File(getFilesDir(), "rootfs");

        try {
            // 第一步：下载
            log("开始连接镜像服务器: " + UBUNTU_URL);
            setDeployStatus("状态: 正在下载 Ubuntu 镜像...", 0, false);

            HttpURLConnection conn = (HttpURLConnection) new URL(UBUNTU_URL).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP 连接失败，响应码: " + responseCode);
            }

            long fileLength = conn.getContentLengthLong();
            log(String.format("连接成功，文件大小: %.2f MB", fileLength / (1024.0 * 1024.0)));

            try (InputStream input = new BufferedInputStream(conn.getInputStream());
                 OutputStream output = new FileOutputStream(targetFile)) {

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                long lastLogTime = System.currentTimeMillis();

                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);

                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        runOnUiThread(() -> binding.progressDeploy.setProgress(progress));

                        if (System.currentTimeMillis() - lastLogTime > 1500) {
                            log(String.format("下载进度: %d%% (%.2f / %.2f MB)",
                                    progress,
                                    total / (1024.0 * 1024.0),
                                    fileLength / (1024.0 * 1024.0)));
                            lastLogTime = System.currentTimeMillis();
                        }
                    }
                }
                output.flush();
            }

            log("下载完成: " + targetFile.getAbsolutePath());

            // 第二步：部署与解压
            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs();
            }

            setDeployStatus("状态: 正在解压部署 Rootfs...", 0, true);
            log("开始解压容器至: " + rootfsDir.getAbsolutePath());

            ArchiveUtil.extractTarGz(targetFile, rootfsDir, (message, count) -> {
                log(message);
            });

            log("容器环境部署完成！");
            setDeployStatus("状态: 部署成功，准备进入系统...", 100, false);

            runOnUiThread(() -> {
                Toast.makeText(this, "Ubuntu 26.04 部署成功", Toast.LENGTH_SHORT).show();
            });

            // 第三步：完成所有流程后跳转到 Home 并关闭 Start 页面
            navigateToHome();

        } catch (Exception e) {
            log("部署发生错误: " + e.getMessage());
            setDeployStatus("状态: 部署失败", 0, false);
            runOnUiThread(() -> {
                binding.btnStartDeploy.setEnabled(true);
                binding.btnGrantPermissions.setEnabled(true);
                Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
