package ru.demo.rubledemo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "wallet_demo";
    private static final String BALANCE_KEY = "balance";
    private static final int INK = Color.rgb(29, 39, 69);
    private static final int MUTED = Color.rgb(130, 140, 159);
    private static final int PURPLE = Color.rgb(112, 91, 235);
    private static final int GREEN = Color.rgb(31, 163, 113);
    private static final int RED = Color.rgb(214, 78, 91);

    private long balance;
    private int currentMode = 0;
    private EditText userAmount;
    private EditText sellerAmount;
    private TextView balanceValue;
    private TextView resultMessage;
    private LinearLayout content;
    private LinearLayout tabUser;
    private LinearLayout tabSeller;
    private TextView userTabText;
    private TextView sellerTabText;
    private boolean paymentFinished;
    private boolean paymentStarted;
    private long pendingPaymentAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(245, 246, 250));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        balance = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(BALANCE_KEY, 2500L);
        buildScreen();
        showMode(0);
    }

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(245, 246, 250));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(16), dp(22), dp(10));

        TextView brand = label("МАЛЕНЬКИЙ БАНК", 11, MUTED, true);
        brand.setLetterSpacing(0.14f);
        header.addView(brand);
        TextView title = label("Мой кошелёк", 28, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(5);
        header.addView(title, titleParams);

        LinearLayout balanceCard = new LinearLayout(this);
        balanceCard.setOrientation(LinearLayout.VERTICAL);
        balanceCard.setPadding(dp(21), dp(18), dp(21), dp(19));
        balanceCard.setBackground(rounded(Color.rgb(39, 44, 82), dp(22)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(17);
        header.addView(balanceCard, cardParams);
        TextView balanceCaption = label("ДОСТУПНЫЙ БАЛАНС", 11, Color.rgb(204, 205, 229), true);
        balanceCaption.setLetterSpacing(0.08f);
        balanceCard.addView(balanceCaption);
        balanceValue = label("", 35, Color.WHITE, true);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
        valueParams.topMargin = dp(5);
        balanceCard.addView(balanceValue, valueParams);
        TextView demoLabel = label("Демо-кошелёк · реальные деньги не используются", 11, Color.rgb(204, 205, 229), false);
        LinearLayout.LayoutParams demoParams = new LinearLayout.LayoutParams(-1, -2);
        demoParams.topMargin = dp(4);
        balanceCard.addView(demoLabel, demoParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(5), dp(5), dp(5), dp(5));
        tabs.setBackground(rounded(Color.rgb(233, 235, 243), dp(15)));
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, dp(54));
        tabsParams.topMargin = dp(17);
        header.addView(tabs, tabsParams);

        tabUser = new LinearLayout(this);
        tabSeller = new LinearLayout(this);
        userTabText = label("Мой баланс", 14, INK, true);
        sellerTabText = label("Оплата", 14, MUTED, true);
        setupTab(tabUser, userTabText);
        setupTab(tabSeller, sellerTabText);
        tabs.addView(tabUser, new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(tabSeller, new LinearLayout.LayoutParams(0, -1, 1));
        tabUser.setOnClickListener(v -> showMode(0));
        tabSeller.setOnClickListener(v -> showMode(1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(13), dp(22), dp(26));
        scroll.addView(content);
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        refreshBalance();
    }

    private void setupTab(LinearLayout tab, TextView text) {
        tab.setGravity(Gravity.CENTER);
        tab.addView(text);
        tab.setBackground(rounded(Color.TRANSPARENT, dp(12)));
    }

    private void showMode(int mode) {
        currentMode = mode;
        boolean user = mode == 0;
        tabUser.setBackground(rounded(user ? Color.WHITE : Color.TRANSPARENT, dp(12)));
        tabSeller.setBackground(rounded(user ? Color.TRANSPARENT : Color.WHITE, dp(12)));
        userTabText.setTextColor(user ? INK : MUTED);
        sellerTabText.setTextColor(user ? MUTED : INK);
        content.removeAllViews();
        if (user) showUserMode(); else showSellerMode();
    }

    private void showUserMode() {
        content.addView(label("Управление балансом", 20, INK, true));
        addGap(content, 5);
        content.addView(label("Добавляй учебные деньги или списывай их.", 14, MUTED, false));

        LinearLayout card = panel();
        addTop(content, card, 18);
        card.addView(label("Сумма операции", 14, INK, true));
        userAmount = amountInput("Например, 500");
        addTop(card, userAmount, 11);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        addTop(card, buttons, 13);
        Button add = actionButton("＋  Добавить", GREEN);
        Button subtract = actionButton("−  Списать", Color.rgb(247, 238, 239));
        subtract.setTextColor(RED);
        buttons.addView(add, new LinearLayout.LayoutParams(0, dp(51), 1));
        LinearLayout.LayoutParams subtractParams = new LinearLayout.LayoutParams(0, dp(51), 1);
        subtractParams.leftMargin = dp(10);
        buttons.addView(subtract, subtractParams);
        add.setOnClickListener(v -> changeBalance(true));
        subtract.setOnClickListener(v -> changeBalance(false));

        LinearLayout hint = panel();
        addTop(content, hint, 14);
        hint.setOrientation(LinearLayout.HORIZONTAL);
        TextView icon = label("✦", 20, PURPLE, true);
        hint.addView(icon, new LinearLayout.LayoutParams(dp(34), -2));
        TextView text = label("Баланс сохраняется автоматически и доступен в режиме оплаты.", 13, MUTED, false);
        hint.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void showSellerMode() {
        content.addView(label("Режим продавца", 20, INK, true));
        addGap(content, 5);
        content.addView(label("Укажи сумму покупки и запусти оплату.", 14, MUTED, false));

        LinearLayout card = panel();
        addTop(content, card, 18);
        card.addView(label("Сумма покупки, ₽", 14, INK, true));
        sellerAmount = amountInput("Например, 350");
        addTop(card, sellerAmount, 11);
        Button start = actionButton("Начать оплату   →", PURPLE);
        addTop(card, start, 14);
        start.setOnClickListener(v -> startPayment());

        LinearLayout paymentCard = new LinearLayout(this);
        paymentCard.setOrientation(LinearLayout.VERTICAL);
        paymentCard.setGravity(Gravity.CENTER_HORIZONTAL);
        paymentCard.setPadding(dp(20), dp(21), dp(20), dp(22));
        paymentCard.setBackground(rounded(Color.WHITE, dp(21)));
        addTop(content, paymentCard, 14);

        TextView touchIcon = label("👆", 43, INK, false);
        touchIcon.setGravity(Gravity.CENTER);
        paymentCard.addView(touchIcon, new LinearLayout.LayoutParams(-1, dp(66)));
        TextView instruction = label("Приложите палец на экран,\nчтобы оплатить", 17, INK, true);
        instruction.setGravity(Gravity.CENTER);
        addTop(paymentCard, instruction, 8);
        TextView explanation = label("Коснитесь кнопки — это имитация оплаты", 12, MUTED, false);
        explanation.setGravity(Gravity.CENTER);
        addTop(paymentCard, explanation, 7);
        Button pay = actionButton("Коснуться и оплатить", Color.rgb(235, 232, 255));
        pay.setTextColor(PURPLE);
        addTop(paymentCard, pay, 15);
        pay.setOnClickListener(v -> processPayment());

        resultMessage = label("Сначала укажи сумму и нажми «Начать оплату».", 13, MUTED, true);
        resultMessage.setGravity(Gravity.CENTER);
        addTop(paymentCard, resultMessage, 13);
    }

    private void changeBalance(boolean adding) {
        Long amount = readAmount(userAmount);
        if (amount == null) return;
        if (!adding && amount > balance) {
            Toast.makeText(this, "Недостаточно средств", Toast.LENGTH_SHORT).show();
            return;
        }
        if (adding && Long.MAX_VALUE - balance < amount) {
            Toast.makeText(this, "Сумма слишком большая", Toast.LENGTH_SHORT).show();
            return;
        }
        balance += adding ? amount : -amount;
        saveBalance();
        refreshBalance();
        userAmount.setText("");
        Toast.makeText(this, adding ? "Баланс пополнен" : "Сумма списана", Toast.LENGTH_SHORT).show();
        hideKeyboard(userAmount);
    }

    private void startPayment() {
        Long amount = readAmount(sellerAmount);
        if (amount == null) return;
        paymentFinished = false;
        paymentStarted = true;
        pendingPaymentAmount = amount;
        resultMessage.setText("Сумма к оплате: " + formatMoney(amount) + " ₽. Коснитесь кнопки ниже.");
        resultMessage.setTextColor(MUTED);
        hideKeyboard(sellerAmount);
    }

    private void processPayment() {
        if (paymentFinished) {
            resultMessage.setText("Начни новую оплату, чтобы продолжить.");
            return;
        }
        if (!paymentStarted) {
            resultMessage.setText("Сначала укажи сумму и нажми «Начать оплату».");
            resultMessage.setTextColor(RED);
            return;
        }
        long amount = pendingPaymentAmount;
        if (balance >= amount) {
            balance -= amount;
            saveBalance();
            refreshBalance();
            resultMessage.setText("✓  Успешно оплачено!\n− " + formatMoney(amount) + " ₽");
            resultMessage.setTextColor(GREEN);
            paymentFinished = true;
            paymentStarted = false;
        } else {
            resultMessage.setText("Недостаточно средств");
            resultMessage.setTextColor(RED);
        }
    }

    private Long readAmount(EditText input) {
        String value = input.getText().toString().trim().replace(" ", "");
        if (value.isEmpty()) {
            Toast.makeText(this, "Введи сумму в рублях", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            long amount = Long.parseLong(value);
            if (amount <= 0) {
                Toast.makeText(this, "Сумма должна быть больше нуля", Toast.LENGTH_SHORT).show();
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Проверь сумму", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void refreshBalance() {
        if (balanceValue != null) balanceValue.setText(formatMoney(balance) + " ₽");
    }

    private void saveBalance() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(BALANCE_KEY, balance).apply();
    }

    private String formatMoney(long value) {
        return NumberFormat.getIntegerInstance(new Locale("ru", "RU")).format(value);
    }

    private EditText amountInput(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(dp(15), 0, dp(15), 0);
        input.setBackground(rounded(Color.rgb(245, 246, 250), dp(13)));
        input.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(53)));
        return input;
    }

    private Button actionButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(color, dp(14)));
        return button;
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(17), dp(17), dp(17), dp(17));
        layout.setBackground(rounded(Color.WHITE, dp(19)));
        return layout;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void addTop(LinearLayout parent, View child, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        parent.addView(child, params);
    }

    private void addGap(LinearLayout parent, int height) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }
}
