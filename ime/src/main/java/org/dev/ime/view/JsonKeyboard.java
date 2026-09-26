package org.dev.ime.view;

import android.content.Context;
import android.util.DisplayMetrics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JsonKeyboard {

    public static class Key {
        public int[] codes;
        public String label, keyOutputText, icon, popupCharacters;
        public int width, height, x, y;
        public boolean isModifier, isRepeatable;

        public Key(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        public boolean contains(int tx, int ty) {
            return tx >= x && tx < (x + width) && ty >= y && ty < (y + height);
        }

        public int distSq(int tx, int ty) {
            int dx = tx - (x + width / 2), dy = ty - (y + height / 2);
            return dx * dx + dy * dy;
        }
    }

    private final List<Key> mKeys = new ArrayList<>();
    private int mTotalWidth, mTotalHeight;

    public JsonKeyboard(Context context, String assetPath) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        this.mTotalWidth = dm.widthPixels;
        try {
            byte[] buf;
            try (InputStream is = context.getAssets().open(assetPath)) {
                buf = new byte[is.available()];
                is.read(buf);
            }
            JSONObject root = new JSONObject(new String(buf, StandardCharsets.UTF_8));
            int defW = parseDim(root.opt("keyWidth"), mTotalWidth, dm, mTotalWidth / 10);
            int defH = parseDim(root.opt("keyHeight"), mTotalWidth, dm, (int) (50 * dm.density));
            int defVGap = parseDim(root.opt("verticalGap"), mTotalWidth, dm, 0);

            JSONArray rows = root.optJSONArray("rows");
            if (rows == null) return;

            int curY = 0;
            for (int r = 0; r < rows.length(); r++) {
                JSONObject row = rows.getJSONObject(r);
                int rH = parseDim(row.opt("keyHeight"), mTotalWidth, dm, defH);
                int rW = parseDim(row.opt("keyWidth"), mTotalWidth, dm, defW);
                int rVGap = parseDim(row.opt("verticalGap"), mTotalWidth, dm, defVGap);
                if (r > 0) curY += rVGap;

                JSONArray keys = row.optJSONArray("keys");
                if (keys != null) {
                    int curX = 0;
                    for (int k = 0; k < keys.length(); k++) {
                        JSONObject kObj = keys.getJSONObject(k);
                        int kW = parseDim(kObj.opt("keyWidth"), mTotalWidth, dm, rW);
                        int kH = parseDim(kObj.opt("keyHeight"), mTotalWidth, dm, rH);
                        curX += parseDim(kObj.opt("horizontalGap"), mTotalWidth, dm, 0);

                        Key key = new Key(curX, curY, kW, kH);
                        JSONArray cArr = kObj.optJSONArray("codes");
                        if (cArr != null) {
                            key.codes = new int[cArr.length()];
                            for (int i = 0; i < cArr.length(); i++) key.codes[i] = cArr.getInt(i);
                        } else {
                            key.codes = kObj.has("codes") ? new int[]{ kObj.getInt("codes") } : new int[0];
                        }

                        key.label = kObj.optString("label", null);
                        key.keyOutputText = kObj.optString("keyOutputText", null);
                        key.icon = kObj.optString("icon", null);
                        key.popupCharacters = kObj.optString("popupCharacters", null);
                        key.isModifier = kObj.optBoolean("isModifier", false);
                        key.isRepeatable = kObj.optBoolean("isRepeatable", false);

                        mKeys.add(key);
                        curX += kW;
                    }
                    curY += rH;
                }
            }
            mTotalHeight = curY;
        } catch (Exception e) {
            throw new RuntimeException("Parse error: " + assetPath, e);
        }
    }

    private int parseDim(Object val, int baseW, DisplayMetrics dm, int def) {
        if (val == null) return def;
        if (val instanceof Number) {
            float n = ((Number) val).floatValue();
            return (n > 0 && n <= 1.0f) ? (int) (n * baseW) : (int) (n * dm.density);
        }
        String s = val.toString().trim();
        if (s.endsWith("%p") || s.endsWith("%")) {
            return (int) (Float.parseFloat(s.replaceAll("[%p]", "")) / 100f * baseW);
        } else if (s.endsWith("dp") || s.endsWith("dip")) {
            return (int) (Float.parseFloat(s.replaceAll("dip|dp", "")) * dm.density);
        } else if (s.endsWith("px")) {
            return Integer.parseInt(s.replace("px", ""));
        }
        return (int) (Float.parseFloat(s) * dm.density);
    }

    public List<Key> getKeys() { return mKeys; }
    public int getHeight() { return mTotalHeight; }
    public int getMinWidth() { return mTotalWidth; }
}