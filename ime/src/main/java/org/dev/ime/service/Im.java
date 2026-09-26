package org.dev.ime.service;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.dev.ime.R;
import org.dev.ime.view.JsonKeyboard;
import org.dev.ime.view.JsonKeyboardView;

public class Im extends InputMethodService implements JsonKeyboardView.OnKeyboardActionListener {

    private JsonKeyboardView mKeyboardView;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        // 1. 加载键盘布局
        View rootView = getLayoutInflater().inflate(R.layout.layout_keyboardview, null);
        mKeyboardView = rootView.findViewById(R.id.jkv);

        // 2. 将主键盘(Qwerty)和副键盘(Number)同时交给 View 托管
        JsonKeyboard qwerty = new JsonKeyboard(this, "keyboard/qwerty.json");
        JsonKeyboard number = new JsonKeyboard(this, "keyboard/number.json");
        mKeyboardView.setKeyboards(qwerty, number);
        mKeyboardView.setOnKeyboardActionListener(this);

        return rootView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (mKeyboardView != null) {
            mKeyboardView.setShifted(false);
            mKeyboardView.resetKeyboard(); // 每次唤起通知 View 重置回主键盘
        }
    }

    // --- JsonKeyboardView.OnKeyboardActionListener 回调实现 ---

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case -5: // DELETE / BACKSPACE
                handleDelete(ic);
                break;

            case -4: // DONE / ENTER
                handleAction(ic);
                break;

            case -1: // SHIFT (View 内部已自闭环处理视觉，Service 无需处理)
            case -2: // 键盘切换 (View 内部已自闭环切换，Service 无需处理)
                break;

            default: // 普通字符输入
                char c = (char) primaryCode;
                ic.commitText(String.valueOf(c), 1);
                break;
        }
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && !TextUtils.isEmpty(text)) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void onPress(int primaryCode) {
        // 可选：按键震动或声音反馈
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    // --- 内部处理逻辑 ---

    private void handleDelete(InputConnection ic) {
        CharSequence selectedText = ic.getSelectedText(0);
        if (TextUtils.isEmpty(selectedText)) {
            // 没有选中文本，删除光标前 1 个字符
            ic.deleteSurroundingText(1, 0);
        } else {
            // 有选中文本，直接清空选中文本
            ic.commitText("", 1);
        }
    }

    private void handleShift() {
        if (mKeyboardView != null) {
            mKeyboardView.setShifted(!mKeyboardView.isShifted());
        }
    }

    private void handleAction(InputConnection ic) {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        int action = editorInfo != null ? (editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION) : EditorInfo.IME_ACTION_NONE;
        
        if (action != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(action);
        } else {
            // 默认回车换行
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }
}