package org.dev.container.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import org.dev.container.databinding.LayoutHomeBinding;
import org.dev.container.util.DefaultTerminalViewClient;

import java.io.File;

public class Home extends AppCompatActivity implements TerminalSessionClient {

    private LayoutHomeBinding binding;
    private TerminalSession currentSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LayoutHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initTerminal();
        initShortcuts();
    }

    private void initTerminal() {
        // 必须优先初始化字号以创建 TerminalRenderer，避免 onSizeChanged 时 mRenderer 空指针
        int defaultTextSize = (int) (12 * getResources().getDisplayMetrics().scaledDensity);
        binding.terminalView.setTextSize(Math.max(defaultTextSize, 24));

        // 设置 TerminalView 客户端交互代理
        binding.terminalView.setTerminalViewClient(new DefaultTerminalViewClient());

        // 默认工作目录
        File homeDir = getFilesDir();
        String currentWorkingDirectory = homeDir.getAbsolutePath();

        // 默认启动系统 /system/bin/sh
        String executable = "/system/bin/sh";
        String[] arguments = new String[]{"-i"};
        String[] environment = new String[]{
                "TERM=xterm-256color",
                "HOME=" + currentWorkingDirectory,
                "PATH=/system/bin:/system/xbin"
        };

        // 创建并启动终端会话
        currentSession = new TerminalSession(
                executable,
                currentWorkingDirectory,
                arguments,
                environment,
                null,
                this
        );

        // 绑定会话至视图
        binding.terminalView.attachSession(currentSession);
    }

    private void initShortcuts() {
        binding.btnKeyEsc.setOnClickListener(v -> sendAsciiCode(27)); // ESC
        binding.btnKeyTab.setOnClickListener(v -> sendAsciiCode(9));  // TAB
        binding.btnKeyCtrlC.setOnClickListener(v -> sendAsciiCode(3)); // Ctrl+C (SIGINT)
        binding.btnKeyCtrlD.setOnClickListener(v -> sendAsciiCode(4)); // Ctrl+D (EOF)

        binding.btnKeyUp.setOnClickListener(v -> sendString("\033[A"));
        binding.btnKeyDown.setOnClickListener(v -> sendString("\033[B"));
        binding.btnKeyRight.setOnClickListener(v -> sendString("\033[C"));
        binding.btnKeyLeft.setOnClickListener(v -> sendString("\033[D"));
    }

    private void sendAsciiCode(int asciiCode) {
        if (currentSession != null && currentSession.isRunning()) {
            currentSession.write(new byte[]{(byte) asciiCode}, 0, 1);
        }
    }

    private void sendString(String text) {
        if (currentSession != null && currentSession.isRunning()) {
            currentSession.write(text);
        }
    }

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        binding.terminalView.onScreenUpdated();
    }

    @Override
    public void onTitleChanged(TerminalSession changedSession) {
        if (changedSession != null && changedSession.getTitle() != null) {
            binding.toolbar.setTitle(changedSession.getTitle());
        }
    }

    @Override
    public void onSessionFinished(TerminalSession finishedSession) {
        Toast.makeText(this, "终端会话已退出", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCopyTextToClipboard(TerminalSession session, String text) {}

    @Override
    public void onPasteTextFromClipboard(TerminalSession session) {}

    @Override
    public void onBell(TerminalSession session) {}

    @Override
    public void onColorsChanged(TerminalSession session) {}

    @Override
    public void onTerminalCursorStateChange(boolean state) {}

    @Override
    public Integer getTerminalCursorStyle() {
        return null;
    }

    @Override
    public void logError(String tag, String message) {}

    @Override
    public void logWarn(String tag, String message) {}

    @Override
    public void logInfo(String tag, String message) {}

    @Override
    public void logDebug(String tag, String message) {}

    @Override
    public void logVerbose(String tag, String message) {}

    @Override
    public void logStackTraceWithMessage(String tag, String message, Exception e) {}

    @Override
    public void logStackTrace(String tag, Exception e) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentSession != null) {
            currentSession.finishIfRunning();
        }
    }
}
