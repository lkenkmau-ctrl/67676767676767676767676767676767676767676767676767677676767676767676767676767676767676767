package ru.demo.rubledemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    private static final String PREFS = "chat_app";
    private static final String KEY_URL = "supabase_url";
    private static final String KEY_PUBLIC = "supabase_public_key";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "display_name";

    private static final int BACKGROUND = Color.rgb(248, 247, 244);
    private static final int WHITE = Color.WHITE;
    private static final int INK = Color.rgb(34, 38, 48);
    private static final int MUTED = Color.rgb(137, 140, 149);
    private static final int ACCENT = Color.rgb(244, 126, 48);
    private static final int PALE_ACCENT = Color.rgb(255, 239, 225);
    private static final int PALE_GREY = Color.rgb(240, 239, 236);
    private static final int GREEN = Color.rgb(45, 164, 112);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Set<Long> messageIds = new HashSet<>();
    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            if (screen == Screen.CHAT) {
                fetchMessages(false);
                handler.postDelayed(this, 3500);
            }
        }
    };

    private enum Screen { CONFIG, AUTH, CHAT }

    private SharedPreferences prefs;
    private Screen screen;
    private LinearLayout root;
    private EditText urlInput;
    private EditText publicKeyInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText nicknameInput;
    private EditText messageInput;
    private TextView authStatus;
    private TextView chatStatus;
    private TextView emptyMessages;
    private LinearLayout messageList;
    private ScrollView messageScroll;
    private boolean fetchInProgress;
    private boolean initialFetch = true;
    private long lastMessageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        if (configured()) {
            if (hasSession()) showChat(); else showAuth();
        } else {
            showConfig();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (screen == Screen.CHAT) {
            logout();
        } else if (screen == Screen.AUTH) {
            showConfig();
        } else {
            super.onBackPressed();
        }
    }

    private boolean configured() {
        return !prefs.getString(KEY_URL, "").isEmpty()
                && !prefs.getString(KEY_PUBLIC, "").isEmpty();
    }

    private boolean hasSession() {
        return !prefs.getString(KEY_ACCESS, "").isEmpty();
    }

    private void beginScreen(Screen next) {
        handler.removeCallbacks(pollTask);
        screen = next;
        root.removeAllViews();
        root.setPadding(dp(22), dp(18), dp(22), dp(14));
    }

    private void showConfig() {
        beginScreen(Screen.CONFIG);
        addBrand("ВАШЕ ПРОСТРАНСТВО ДЛЯ ОБЩЕНИЯ");
        addTop(root, label("Подключим\nтвой мессенджер", 34, INK, true), 26);
        addTop(root, label("Укажи данные проекта Supabase. Они сохранятся на этом устройстве.", 15, MUTED, false), 11);

        LinearLayout card = panel();
        addTop(root, card, 26);
        card.addView(label("АДРЕС ПРОЕКТА", 11, MUTED, true));
        urlInput = field("https://xxxx.supabase.co", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setText(prefs.getString(KEY_URL, ""));
        addTop(card, urlInput, 8);
        addTop(card, label("ПУБЛИЧНЫЙ КЛЮЧ (ANON / PUBLISHABLE)", 11, MUTED, true), 17);
        publicKeyInput = field("Вставь публичный ключ Supabase", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        publicKeyInput.setText(prefs.getString(KEY_PUBLIC, ""));
        addTop(card, publicKeyInput, 8);

        TextView note = label("Нужны только URL и публичный ключ. Не вставляй сюда service_role или секретный ключ.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        addTop(card, note, 13);
        TextView connect = primaryButton("ПОДКЛЮЧИТЬ SUPABASE   →");
        addTop(root, connect, 18);
        connect.setOnClickListener(v -> saveConfig());
        addTop(root, label("После подключения можно будет зарегистрироваться или войти в общий чат.", 13, MUTED, false), 13);
    }

    private void saveConfig() {
        String url = urlInput.getText().toString().trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        String key = publicKeyInput.getText().toString().trim();
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            toast("Укажи корректный HTTPS-адрес проекта Supabase");
            return;
        }
        if (key.isEmpty() || key.toLowerCase(Locale.ROOT).contains("service_role") || key.toLowerCase(Locale.ROOT).contains("secret")) {
            toast("Нужен публичный anon/publishable ключ, не секретный");
            return;
        }
        prefs.edit().putString(KEY_URL, url).putString(KEY_PUBLIC, key).apply();
        showAuth();
    }

    private void showAuth() {
        beginScreen(Screen.AUTH);
        addBrand("ОБЩАЙСЯ. БУДЬ РЯДОМ.");
        addTop(root, label("Тёплые слова —\nв одном сообщении.", 32, INK, true), 27);
        addTop(root, label("Войди в аккаунт или создай новый, чтобы открыть общий чат.", 14, MUTED, false), 11);

        LinearLayout card = panel();
        addTop(root, card, 24);
        card.addView(label("ТВОЁ ИМЯ В ЧАТЕ", 11, MUTED, true));
        nicknameInput = field("Например, Саша", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nicknameInput.setText(prefs.getString(KEY_NAME, ""));
        addTop(card, nicknameInput, 8);
        addTop(card, label("ЭЛЕКТРОННАЯ ПОЧТА", 11, MUTED, true), 16);
        emailInput = field("you@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(prefs.getString(KEY_EMAIL, ""));
        addTop(card, emailInput, 8);
        addTop(card, label("ПАРОЛЬ", 11, MUTED, true), 16);
        passwordInput = field("Не менее 6 символов", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        addTop(card, passwordInput, 8);

        TextView create = primaryButton("СОЗДАТЬ АККАУНТ");
        addTop(card, create, 19);
        create.setOnClickListener(v -> authenticate(true));
        TextView signIn = secondaryButton("УЖЕ ЕСТЬ АККАУНТ?  ВОЙТИ");
        addTop(card, signIn, 8);
        signIn.setOnClickListener(v -> authenticate(false));

        authStatus = label("", 12, MUTED, false);
        authStatus.setGravity(Gravity.CENTER);
        authStatus.setLineSpacing(dp(3), 1f);
        addTop(root, authStatus, 13);
        TextView change = secondaryButton("НАСТРОИТЬ ДРУГОЙ ПРОЕКТ SUPABASE");
        addTop(root, change, 9);
        change.setOnClickListener(v -> showConfig());
    }

    private void authenticate(boolean signUp) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String nickname = nicknameInput.getText().toString().trim();
        if (nickname.isEmpty()) nickname = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Друг";
        if (!email.contains("@") || email.length() < 5) {
            setAuthStatus("Проверь адрес электронной почты", Color.rgb(192, 68, 57));
            return;
        }
        if (password.length() < 6) {
            setAuthStatus("Пароль должен содержать не менее 6 символов", Color.rgb(192, 68, 57));
            return;
        }
        final String displayName = nickname;
        setAuthStatus(signUp ? "Создаём аккаунт…" : "Выполняем вход…", MUTED);
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            if (signUp) {
                JSONObject metadata = new JSONObject();
                metadata.put("username", displayName);
                body.put("data", metadata);
            }
        } catch (JSONException e) {
            setAuthStatus("Не удалось подготовить запрос", Color.rgb(192, 68, 57));
            return;
        }
        String path = signUp ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password";
        sendRequest("POST", path, body, false, (code, response) -> {
            if (code < 200 || code >= 300) {
                setAuthStatus(explainError(response), Color.rgb(192, 68, 57));
                return;
            }
            try {
                JSONObject json = new JSONObject(response);
                String access = json.optString("access_token", "");
                if (access.isEmpty()) {
                    prefs.edit().putString(KEY_EMAIL, email).putString(KEY_NAME, displayName).apply();
                    setAuthStatus("Аккаунт создан. Подтверди почту по ссылке из письма, затем нажми «Войти».", GREEN);
                    return;
                }
                saveSession(json, email, displayName);
                showChat();
            } catch (JSONException e) {
                setAuthStatus("Сервер вернул неожиданный ответ", Color.rgb(192, 68, 57));
            }
        });
    }

    private void saveSession(JSONObject json, String email, String displayName) throws JSONException {
        String access = json.optString("access_token", "");
        String refresh = json.optString("refresh_token", "");
        JSONObject user = json.optJSONObject("user");
        String userId = user == null ? "" : user.optString("id", "");
        if (access.isEmpty() || userId.isEmpty()) throw new JSONException("Missing session data");
        prefs.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, displayName)
                .apply();
    }

    private void showChat() {
        beginScreen(Screen.CHAT);
        messages.clear();
        messageIds.clear();
        lastMessageId = 0;
        initialFetch = true;
        fetchInProgress = false;

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = label(initials(prefs.getString(KEY_NAME, "Друг")), 17, WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(circle(ACCENT));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -2, 1);
        headingParams.leftMargin = dp(12);
        header.addView(heading, headingParams);
        heading.addView(label("Тёплый чат", 20, INK, true));
        TextView online = label("●  общий разговор", 12, GREEN, false);
        LinearLayout.LayoutParams onlineParams = new LinearLayout.LayoutParams(-1, -2);
        onlineParams.topMargin = dp(3);
        heading.addView(online, onlineParams);
        TextView menu = label("···", 25, MUTED, true);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(rounded(WHITE, 16));
        header.addView(menu, new LinearLayout.LayoutParams(dp(45), dp(43)));
        menu.setOnClickListener(v -> showChatMenu());
        root.addView(header);

        LinearLayout roomBanner = new LinearLayout(this);
        roomBanner.setGravity(Gravity.CENTER_VERTICAL);
        roomBanner.setPadding(dp(13), dp(11), dp(13), dp(11));
        roomBanner.setBackground(rounded(PALE_ACCENT, 15));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, -2);
        bannerParams.topMargin = dp(15);
        root.addView(roomBanner, bannerParams);
        TextView sparkle = label("✦", 20, ACCENT, true);
        roomBanner.addView(sparkle, new LinearLayout.LayoutParams(dp(30), -2));
        LinearLayout bannerText = new LinearLayout(this);
        bannerText.setOrientation(LinearLayout.VERTICAL);
        roomBanner.addView(bannerText);
        bannerText.addView(label("Общий чат", 13, INK, true));
        TextView roomDescription = label("Пиши всем участникам пространства", 11, MUTED, false);
        LinearLayout.LayoutParams roomDescriptionParams = new LinearLayout.LayoutParams(-1, -2);
        roomDescriptionParams.topMargin = dp(2);
        bannerText.addView(roomDescription, roomDescriptionParams);

        FrameLayout conversation = new FrameLayout(this);
        LinearLayout.LayoutParams conversationParams = new LinearLayout.LayoutParams(-1, 0, 1);
        conversationParams.topMargin = dp(12);
        conversationParams.bottomMargin = dp(10);
        root.addView(conversation, conversationParams);
        messageScroll = new ScrollView(this);
        messageScroll.setFillViewport(true);
        messageScroll.setClipToPadding(false);
        messageScroll.setVerticalScrollBarEnabled(false);
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(0, dp(4), 0, dp(6));
        messageScroll.addView(messageList);
        conversation.addView(messageScroll, new FrameLayout.LayoutParams(-1, -1));
        emptyMessages = label("Здесь пока тихо.\nОтправь первое сообщение 👋", 14, MUTED, false);
        emptyMessages.setGravity(Gravity.CENTER);
        conversation.addView(emptyMessages, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(7), dp(6), dp(7), dp(6));
        composer.setBackground(rounded(WHITE, 18));
        composer.setElevation(dp(3));
        messageInput = field("Написать сообщение…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setSingleLine(false);
        messageInput.setMinLines(1);
        messageInput.setMaxLines(4);
        messageInput.setPadding(dp(12), dp(8), dp(8), dp(8));
        messageInput.setBackgroundColor(Color.TRANSPARENT);
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1));
        TextView send = label("↑", 25, WHITE, true);
        send.setGravity(Gravity.CENTER);
        send.setBackground(circle(ACCENT));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(45), dp(45));
        sendParams.leftMargin = dp(5);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> sendMessage());
        root.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        chatStatus = label("Сообщения обновляются автоматически", 10, MUTED, false);
        chatStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(22));
        statusParams.topMargin = dp(3);
        root.addView(chatStatus, statusParams);

        fetchMessages(true);
        handler.postDelayed(pollTask, 3500);
    }

    private void fetchMessages(boolean initial) {
        if (screen != Screen.CHAT || fetchInProgress) return;
        fetchInProgress = true;
        String query;
        if (initial || lastMessageId == 0) {
            query = "/rest/v1/messages?select=id,user_id,sender_name,content,created_at&order=id.desc&limit=60";
        } else {
            query = "/rest/v1/messages?select=id,user_id,sender_name,content,created_at&id=gt." + lastMessageId + "&order=id.asc&limit=100";
        }
        dbRequest("GET", query, null, (code, response) -> {
            fetchInProgress = false;
            if (screen != Screen.CHAT) return;
            if (code < 200 || code >= 300) {
                if (initialFetch) {
                    emptyMessages.setText("Не удалось загрузить сообщения.\nПроверь схему Supabase и подключение.");
                    chatStatus.setText("Ошибка загрузки · проверь настройки базы");
                    chatStatus.setTextColor(Color.rgb(192, 68, 57));
                }
                return;
            }
            try {
                JSONArray rows = new JSONArray(response);
                List<ChatMessage> incoming = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    incoming.add(new ChatMessage(
                            row.optLong("id"),
                            row.optString("user_id"),
                            row.optString("sender_name", "Друг"),
                            row.optString("content", ""),
                            row.optString("created_at", "")));
                }
                if (initialFetch) {
                    for (int i = incoming.size() - 1; i >= 0; i--) addMessage(incoming.get(i));
                    initialFetch = false;
                    renderMessages(true);
                } else if (!incoming.isEmpty()) {
                    for (ChatMessage message : incoming) addMessage(message);
                    renderMessages(true);
                }
                chatStatus.setText("●  Чат синхронизирован");
                chatStatus.setTextColor(GREEN);
            } catch (JSONException e) {
                chatStatus.setText("Не удалось прочитать ответ сервера");
                chatStatus.setTextColor(Color.rgb(192, 68, 57));
            }
        });
    }

    private void addMessage(ChatMessage message) {
        if (message.id <= 0 || !messageIds.add(message.id)) return;
        messages.add(message);
        if (message.id > lastMessageId) lastMessageId = message.id;
    }

    private void renderMessages(boolean scrollToBottom) {
        if (screen != Screen.CHAT) return;
        messageList.removeAllViews();
        emptyMessages.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        for (ChatMessage message : messages) messageList.addView(messageBubble(message));
        if (scrollToBottom) messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
    }

    private View messageBubble(ChatMessage message) {
        boolean own = message.userId.equals(prefs.getString(KEY_USER_ID, ""));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(own ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(5);
        rowParams.bottomMargin = dp(5);
        row.setLayoutParams(rowParams);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(13), dp(9), dp(13), dp(8));
        bubble.setBackground(rounded(own ? PALE_ACCENT : WHITE, 16));
        bubble.setElevation(dp(1));
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(0, -2, 1);
        bubbleParams.weight = 0;
        bubbleParams.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(105), dp(310));
        bubbleParams.gravity = own ? Gravity.END : Gravity.START;
        row.addView(bubble, bubbleParams);

        if (!own) {
            TextView sender = label(message.senderName, 12, ACCENT, true);
            bubble.addView(sender);
        }
        TextView content = label(message.content, 15, INK, false);
        content.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, -2);
        if (!own) contentParams.topMargin = dp(3);
        bubble.addView(content, contentParams);
        TextView time = label(timePart(message.createdAt), 10, MUTED, false);
        time.setGravity(Gravity.END);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = dp(4);
        bubble.addView(time, timeParams);
        return row;
    }

    private void sendMessage() {
        if (messageInput == null) return;
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;
        if (content.length() > 2000) {
            toast("Сообщение слишком длинное (максимум 2000 символов)");
            return;
        }
        messageInput.setEnabled(false);
        JSONObject body = new JSONObject();
        try {
            body.put("user_id", prefs.getString(KEY_USER_ID, ""));
            body.put("sender_name", prefs.getString(KEY_NAME, "Друг"));
            body.put("content", content);
        } catch (JSONException e) {
            messageInput.setEnabled(true);
            toast("Не удалось подготовить сообщение");
            return;
        }
        dbRequest("POST", "/rest/v1/messages", body, (code, response) -> {
            if (messageInput == null || screen != Screen.CHAT) return;
            messageInput.setEnabled(true);
            if (code < 200 || code >= 300) {
                toast(explainError(response));
                return;
            }
            messageInput.setText("");
            hideKeyboard(messageInput);
            fetchMessages(false);
        });
    }

    private void showChatMenu() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Настройки чата")
                .setItems(new String[]{"Изменить проект Supabase", "Выйти из аккаунта"}, (dialog, which) -> {
                    if (which == 0) {
                        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_USER_ID).apply();
                        showConfig();
                    } else {
                        logout();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void logout() {
        handler.removeCallbacks(pollTask);
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_USER_ID).apply();
        showAuth();
    }

    private interface ResultCallback {
        void complete(int code, String response);
    }

    private void dbRequest(String method, String path, JSONObject body, ResultCallback callback) {
        sendRequest(method, path, body, true, callback);
    }

    private void sendRequest(String method, String path, JSONObject body, boolean withSession, ResultCallback callback) {
        String url = prefs.getString(KEY_URL, "") + path;
        String publicKey = prefs.getString(KEY_PUBLIC, "");
        Request.Builder builder = new Request.Builder().url(url)
                .header("apikey", publicKey)
                .header("Accept", "application/json");
        if (withSession) {
            String accessToken = prefs.getString(KEY_ACCESS, "");
            if (!accessToken.isEmpty()) builder.header("Authorization", "Bearer " + accessToken);
        }
        if (body != null) builder.method(method, RequestBody.create(JSON_TYPE, body.toString()));
        else if (method.equals("POST") || method.equals("PATCH") || method.equals("PUT")) {
            builder.method(method, RequestBody.create(JSON_TYPE, "{}"));
        } else builder.method(method, null);

        if (method.equals("POST") && path.startsWith("/rest/v1/")) {
            builder.header("Prefer", "return=representation");
        }
        http.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    if (screen == Screen.CHAT && chatStatus != null) {
                        chatStatus.setText("Нет соединения · повторяем подключение…");
                        chatStatus.setTextColor(Color.rgb(192, 120, 64));
                    } else if (screen == Screen.AUTH && authStatus != null) {
                        setAuthStatus("Не удалось подключиться. Проверь интернет и URL Supabase.", Color.rgb(192, 68, 57));
                    } else {
                        toast("Не удалось подключиться к Supabase");
                    }
                    callback.complete(0, e.getMessage() == null ? "Нет соединения" : e.getMessage());
                });
            }

            @Override public void onResponse(Call call, Response response) {
                int code = response.code();
                if (code == 401 && withSession && !path.startsWith("/auth/v1/")) {
                    String refreshToken = prefs.getString(KEY_REFRESH, "");
                    if (!refreshToken.isEmpty()) {
                        response.close();
                        JSONObject refreshBody = new JSONObject();
                        try {
                            refreshBody.put("refresh_token", refreshToken);
                        } catch (JSONException ignored) { }
                        sendRequest("POST", "/auth/v1/token?grant_type=refresh_token", refreshBody, false,
                                (refreshCode, refreshResponse) -> {
                                    if (refreshCode >= 200 && refreshCode < 300) {
                                        try {
                                            JSONObject session = new JSONObject(refreshResponse);
                                            saveSession(session, prefs.getString(KEY_EMAIL, ""), prefs.getString(KEY_NAME, "Друг"));
                                            sendRequest(method, path, body, true, callback);
                                        } catch (JSONException e) {
                                            callback.complete(401, "Сессия истекла. Войди в аккаунт снова.");
                                        }
                                    } else {
                                        callback.complete(401, "Сессия истекла. Войди в аккаунт снова.");
                                    }
                                });
                        return;
                    }
                }
                String responseText;
                try {
                    responseText = response.body() == null ? "" : response.body().string();
                } catch (java.io.IOException e) {
                    responseText = "Не удалось прочитать ответ сервера";
                }
                response.close();
                String finalText = responseText;
                runOnUiThread(() -> callback.complete(code, finalText));
            }
        });
    }

    private void setAuthStatus(String text, int color) {
        if (authStatus == null) return;
        authStatus.setText(text);
        authStatus.setTextColor(color);
    }

    private String explainError(String response) {
        try {
            JSONObject error = new JSONObject(response);
            String message = error.optString("msg", "");
            if (message.isEmpty()) message = error.optString("message", "");
            if (message.isEmpty()) message = error.optString("error_description", "");
            if (message.isEmpty()) message = error.optString("error", "");
            if (!message.isEmpty()) return humanizeError(message);
        } catch (JSONException ignored) { }
        return response.isEmpty() ? "Запрос не выполнен. Проверь соединение и настройки Supabase." : humanizeError(response);
    }

    private String humanizeError(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("invalid login credentials")) return "Неверная почта или пароль";
        if (lower.contains("email not confirmed")) return "Сначала подтверди почту по ссылке из письма";
        if (lower.contains("user already registered")) return "Этот адрес уже зарегистрирован — нажми «Войти»";
        if (lower.contains("password should be at least")) return "Пароль должен содержать не менее 6 символов";
        if (lower.contains("relation") && lower.contains("does not exist")) return "Таблица чата не найдена. Выполни SQL из supabase/schema.sql";
        if (lower.contains("permission denied") || lower.contains("row-level security")) return "Нет доступа к сообщениям. Проверь SQL-политики Supabase.";
        if (lower.length() > 180) return message.substring(0, 177) + "…";
        return message;
    }

    private String timePart(String timestamp) {
        if (timestamp == null || timestamp.length() < 16) return "";
        return timestamp.substring(11, 16);
    }

    private String initials(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) return "?";
        return clean.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void addBrand(String text) {
        TextView brand = label("✦   " + text, 11, ACCENT, true);
        brand.setLetterSpacing(0.08f);
        root.addView(brand);
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(17), dp(18), dp(17), dp(18));
        panel.setBackground(rounded(WHITE, 20));
        panel.setElevation(dp(2));
        return panel;
    }

    private EditText field(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setPadding(dp(13), 0, dp(13), 0);
        input.setBackground(rounded(BACKGROUND, 13));
        input.setMinimumHeight(dp(51));
        return input;
    }

    private TextView primaryButton(String text) {
        TextView button = label(text, 14, WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(ACCENT, 15));
        button.setPadding(dp(13), dp(15), dp(13), dp(15));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView secondaryButton(String text) {
        TextView button = label(text, 12, INK, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(PALE_GREY, 14));
        button.setPadding(dp(10), dp(13), dp(10), dp(13));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = rounded(color, 50);
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private void addTop(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }

    private void toast(String text) {
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show();
    }

    private static final class ChatMessage {
        final long id;
        final String userId;
        final String senderName;
        final String content;
        final String createdAt;

        ChatMessage(long id, String userId, String senderName, String content, String createdAt) {
            this.id = id;
            this.userId = userId;
            this.senderName = senderName == null || senderName.isEmpty() ? "Друг" : senderName;
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
