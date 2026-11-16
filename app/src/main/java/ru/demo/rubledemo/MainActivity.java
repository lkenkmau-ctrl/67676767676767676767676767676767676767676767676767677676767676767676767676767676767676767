package ru.demo.rubledemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String PREFS = "wallet_demo";
    private static final String BALANCE_KEY = "balance";

    private static final int BG = Color.rgb(17, 18, 21);
    private static final int SURFACE = Color.rgb(29, 30, 34);
    private static final int SURFACE_LIGHT = Color.rgb(39, 40, 45);
    private static final int ORANGE = Color.rgb(255, 143, 45);
    private static final int WHITE = Color.rgb(247, 247, 245);
    private static final int MUTED = Color.rgb(151, 151, 157);
    private static final int GREEN = Color.rgb(70, 207, 144);
    private static final int RED = Color.rgb(255, 100, 100);

    private static final int HOME = 0;
    private static final int USER = 1;
    private static final int SELLER_ENTRY = 2;
    private static final int PAYMENT = 3;
    private static final int RESULT = 4;

    private long balance;
    private long pendingPaymentAmount;
    private String amountDigits = "";
    private boolean addingFunds = true;
    private int screen = HOME;
    private LinearLayout root;
    private TextView amountView;
    private TextView balanceView;
    private TextView userNotice;
    private TextView operationAdd;
    private TextView operationSubtract;
    private boolean paymentAttempted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        applyImmersiveMode();

        balance = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(BALANCE_KEY, 2500L);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        showHome(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void resetScreen(int newScreen) {
        screen = newScreen;
        root.removeAllViews();
        root.setAlpha(1f);
        root.setPadding(dp(22), dp(14), dp(22), dp(17));
    }

    private void showHome(boolean animate) {
        resetScreen(HOME);

        TextView brand = label("ТЕРМИНАЛ  •  DEMO", 12, ORANGE, true);
        brand.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(-1, -2);
        brandParams.topMargin = dp(25);
        root.addView(brand, brandParams);

        TextView title = label("Добро\nпожаловать", 40, WHITE, true);
        title.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(17);
        root.addView(title, titleParams);

        TextView subtitle = label("Выберите режим работы", 16, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        root.addView(subtitle, subtitleParams);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        root.addView(modeCard("01", "Режим пользователя", "Баланс и управление средствами", "›", () -> {
            addingFunds = true;
            amountDigits = "";
            showUser(true);
        }));
        root.addView(modeCard("02", "Режим продавца", "Ввод суммы и оплата", "›", () -> {
            amountDigits = "";
            showSellerEntry(true);
        }));

        TextView footnote = label("Учебный терминал · реальные платежи не проводятся", 11, MUTED, false);
        footnote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footnoteParams = new LinearLayout.LayoutParams(-1, -2);
        footnoteParams.topMargin = dp(22);
        footnoteParams.bottomMargin = dp(8);
        root.addView(footnote, footnoteParams);
        if (animate) fadeIn();
    }

    private View modeCard(String number, String title, String description, String arrow, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(17), dp(19), dp(17), dp(19));
        card.setBackground(rounded(SURFACE, 20));
        card.setClickable(true);
        card.setFocusable(true);

        TextView badge = label(number, 13, ORANGE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(Color.rgb(57, 39, 27), 12));
        card.addView(badge, new LinearLayout.LayoutParams(dp(43), dp(43)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.leftMargin = dp(14);
        card.addView(texts, textParams);
        texts.addView(label(title, 16, WHITE, true));
        TextView desc = label(description, 12, MUTED, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(5);
        texts.addView(desc, descParams);

        TextView arrowView = label(arrow, 29, ORANGE, false);
        arrowView.setGravity(Gravity.CENTER);
        card.addView(arrowView, new LinearLayout.LayoutParams(dp(32), -2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        card.setOnClickListener(v -> action.run());
        card.setLayoutParams(params);
        return card;
    }

    private void showUser(boolean animate) {
        resetScreen(USER);
        root.addView(topBar("РЕЖИМ ПОЛЬЗОВАТЕЛЯ", () -> showHome(true)));

        LinearLayout balanceCard = new LinearLayout(this);
        balanceCard.setOrientation(LinearLayout.VERTICAL);
        balanceCard.setPadding(dp(18), dp(13), dp(18), dp(13));
        balanceCard.setBackground(rounded(SURFACE, 17));
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(-1, -2);
        balanceParams.topMargin = dp(12);
        root.addView(balanceCard, balanceParams);
        balanceCard.addView(label("ДОСТУПНЫЙ БАЛАНС", 10, MUTED, true));
        balanceView = label(formatMoney(balance) + " ₽", 28, WHITE, true);
        LinearLayout.LayoutParams balanceTextParams = new LinearLayout.LayoutParams(-1, -2);
        balanceTextParams.topMargin = dp(3);
        balanceCard.addView(balanceView, balanceTextParams);

        LinearLayout amountPanel = new LinearLayout(this);
        amountPanel.setOrientation(LinearLayout.HORIZONTAL);
        amountPanel.setGravity(Gravity.CENTER_VERTICAL);
        amountPanel.setPadding(dp(16), dp(4), dp(16), dp(4));
        amountPanel.setBackground(rounded(SURFACE, 15));
        LinearLayout.LayoutParams amountPanelParams = new LinearLayout.LayoutParams(-1, dp(65));
        amountPanelParams.topMargin = dp(12);
        root.addView(amountPanel, amountPanelParams);
        LinearLayout amountTexts = new LinearLayout(this);
        amountTexts.setOrientation(LinearLayout.VERTICAL);
        amountPanel.addView(amountTexts, new LinearLayout.LayoutParams(0, -2, 1));
        amountTexts.addView(label("СУММА ОПЕРАЦИИ", 10, MUTED, true));
        amountView = label("0 ₽", 24, WHITE, true);
        amountView.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams amountValueParams = new LinearLayout.LayoutParams(-1, -2);
        amountValueParams.topMargin = dp(2);
        amountTexts.addView(amountView, amountValueParams);
        TextView clear = label("⌫", 24, ORANGE, true);
        clear.setGravity(Gravity.CENTER);
        amountPanel.addView(clear, new LinearLayout.LayoutParams(dp(42), -1));
        clear.setOnClickListener(v -> deleteDigit());

        LinearLayout operations = new LinearLayout(this);
        operations.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams operationsParams = new LinearLayout.LayoutParams(-1, dp(43));
        operationsParams.topMargin = dp(10);
        root.addView(operations, operationsParams);
        operationAdd = operationChoice("＋  Пополнить", true);
        operationSubtract = operationChoice("−  Списать", false);
        operations.addView(operationAdd, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams subtractParams = new LinearLayout.LayoutParams(0, -1, 1);
        subtractParams.leftMargin = dp(9);
        operations.addView(operationSubtract, subtractParams);
        operationAdd.setOnClickListener(v -> setOperation(true));
        operationSubtract.setOnClickListener(v -> setOperation(false));

        userNotice = label("Введи сумму на цифровой панели", 12, MUTED, false);
        userNotice.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, dp(29));
        noticeParams.topMargin = dp(4);
        root.addView(userNotice, noticeParams);

        root.addView(keypad(), new LinearLayout.LayoutParams(-1, 0, 1));
        TextView action = actionButton("ПОДТВЕРДИТЬ ОПЕРАЦИЮ", ORANGE, WHITE);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(56));
        actionParams.topMargin = dp(10);
        root.addView(action, actionParams);
        action.setOnClickListener(v -> applyUserOperation());
        if (animate) fadeIn();
    }

    private void showSellerEntry(boolean animate) {
        resetScreen(SELLER_ENTRY);
        root.addView(topBar("РЕЖИМ ПРОДАВЦА", () -> showHome(true)));

        TextView title = label("Сумма\nпокупки", 32, WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(12);
        root.addView(title, titleParams);

        LinearLayout amountPanel = new LinearLayout(this);
        amountPanel.setOrientation(LinearLayout.VERTICAL);
        amountPanel.setGravity(Gravity.CENTER_VERTICAL);
        amountPanel.setPadding(dp(17), dp(9), dp(17), dp(9));
        amountPanel.setBackground(rounded(SURFACE, 16));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, dp(78));
        amountParams.topMargin = dp(12);
        root.addView(amountPanel, amountParams);
        amountPanel.addView(label("К ОПЛАТЕ", 10, MUTED, true));
        amountView = label("0 ₽", 30, WHITE, true);
        LinearLayout.LayoutParams amountTextParams = new LinearLayout.LayoutParams(-1, -2);
        amountTextParams.topMargin = dp(2);
        amountPanel.addView(amountView, amountTextParams);

        root.addView(keypad(), new LinearLayout.LayoutParams(-1, 0, 1));
        TextView start = actionButton("НАЧАТЬ ОПЛАТУ   →", ORANGE, WHITE);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(58));
        startParams.topMargin = dp(12);
        root.addView(start, startParams);
        start.setOnClickListener(v -> beginPayment());

        TextView balanceHint = label("На балансе: " + formatMoney(balance) + " ₽", 12, MUTED, false);
        balanceHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, dp(27));
        hintParams.topMargin = dp(4);
        root.addView(balanceHint, hintParams);
        if (animate) fadeIn();
    }

    private void showPayment(boolean animate) {
        resetScreen(PAYMENT);
        root.addView(topBar("ОПЛАТА", () -> showSellerEntry(true)));

        TextView price = label(formatMoney(pendingPaymentAmount) + " ₽", 38, WHITE, true);
        price.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(-1, -2);
        priceParams.topMargin = dp(23);
        root.addView(price, priceParams);
        TextView caption = label("К ОПЛАТЕ", 11, MUTED, true);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(3);
        root.addView(caption, captionParams);

        FrameLayout touchArea = new FrameLayout(this);
        LinearLayout.LayoutParams areaParams = new LinearLayout.LayoutParams(-1, 0, 1);
        areaParams.topMargin = dp(15);
        areaParams.bottomMargin = dp(7);
        root.addView(touchArea, areaParams);

        FrameLayout circle = new FrameLayout(this);
        FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(dp(204), dp(204), Gravity.CENTER);
        touchArea.addView(circle, circleParams);

        View haloOuter = new View(this);
        haloOuter.setBackground(circleBorder(Color.rgb(103, 60, 31), dp(102), dp(1)));
        circle.addView(haloOuter, new FrameLayout.LayoutParams(-1, -1));
        View haloInner = new View(this);
        haloInner.setBackground(circleBorder(Color.rgb(150, 78, 30), dp(90), dp(1)));
        FrameLayout.LayoutParams innerHaloParams = new FrameLayout.LayoutParams(dp(174), dp(174), Gravity.CENTER);
        circle.addView(haloInner, innerHaloParams);

        TextView tapTarget = label("●", 54, ORANGE, true);
        tapTarget.setGravity(Gravity.CENTER);
        tapTarget.setBackground(circleFill(Color.rgb(51, 36, 27), dp(78)));
        FrameLayout.LayoutParams targetParams = new FrameLayout.LayoutParams(dp(145), dp(145), Gravity.CENTER);
        circle.addView(tapTarget, targetParams);
        tapTarget.setOnClickListener(v -> processPayment(tapTarget));
        circle.setOnClickListener(v -> processPayment(tapTarget));
        startPulse(haloOuter, 1f, 1.12f, 1200);
        startPulse(haloInner, 1f, 1.09f, 1000);

        TextView instruction = label("ПРИЛОЖИТЕ ПАЛЕЦ", 18, WHITE, true);
        instruction.setGravity(Gravity.CENTER);
        root.addView(instruction, new LinearLayout.LayoutParams(-1, -2));
        TextView subInstruction = label("Коснитесь круга для имитации оплаты", 12, MUTED, false);
        subInstruction.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(7);
        subParams.bottomMargin = dp(14);
        root.addView(subInstruction, subParams);
        if (animate) fadeIn();
    }

    private void processPayment(TextView tapTarget) {
        if (paymentAttempted) return;
        paymentAttempted = true;
        tapTarget.animate().scaleX(0.82f).scaleY(0.82f).alpha(0.7f).setDuration(120)
                .withEndAction(() -> tapTarget.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180).start()).start();
        if (balance < pendingPaymentAmount) {
            tapTarget.postDelayed(() -> showResult(false), 250);
            return;
        }
        tapTarget.setText("✓");
        tapTarget.setTextSize(54);
        tapTarget.setTextColor(GREEN);
        tapTarget.setBackground(circleFill(Color.rgb(27, 56, 43), dp(78)));
        tapTarget.postDelayed(() -> {
            balance -= pendingPaymentAmount;
            saveBalance();
            showResult(true);
        }, 360);
    }

    private void showResult(boolean success) {
        resetScreen(RESULT);
        root.addView(topBar(success ? "ПОКУПКА" : "ОПЛАТА", () -> showHome(true)));

        FrameLayout resultArea = new FrameLayout(this);
        root.addView(resultArea, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout resultContent = new LinearLayout(this);
        resultContent.setOrientation(LinearLayout.VERTICAL);
        resultContent.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -1);
        resultArea.addView(resultContent, contentParams);

        TextView icon = label(success ? "✓" : "×", 68, success ? GREEN : RED, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(circleFill(success ? Color.rgb(27, 56, 43) : Color.rgb(62, 32, 34), dp(72)));
        resultContent.addView(icon, new LinearLayout.LayoutParams(dp(144), dp(144)));
        icon.setScaleX(0.35f);
        icon.setScaleY(0.35f);
        icon.setAlpha(0f);
        icon.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        TextView message = label(success ? "Успешно оплачено!" : "Недостаточно средств", 25, success ? GREEN : RED, true);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(24);
        resultContent.addView(message, messageParams);

        TextView details = label(success
                ? "− " + formatMoney(pendingPaymentAmount) + " ₽\nНовый баланс: " + formatMoney(balance) + " ₽"
                : "Сумма покупки: " + formatMoney(pendingPaymentAmount) + " ₽\nНа балансе: " + formatMoney(balance) + " ₽",
                15, MUTED, false);
        details.setGravity(Gravity.CENTER);
        details.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = dp(12);
        resultContent.addView(details, detailsParams);

        if (success) {
            ConfettiView confetti = new ConfettiView(this);
            resultArea.addView(confetti, new FrameLayout.LayoutParams(-1, -1));
            confetti.start();
        }

        TextView again = actionButton(success ? "НОВАЯ ПОКУПКА" : "ИЗМЕНИТЬ СУММУ", ORANGE, WHITE);
        LinearLayout.LayoutParams againParams = new LinearLayout.LayoutParams(-1, dp(57));
        againParams.bottomMargin = dp(10);
        root.addView(again, againParams);
        again.setOnClickListener(v -> {
            paymentAttempted = false;
            amountDigits = "";
            showSellerEntry(true);
        });

        TextView home = label("НА ГЛАВНЫЙ ЭКРАН", 13, MUTED, true);
        home.setGravity(Gravity.CENTER);
        root.addView(home, new LinearLayout.LayoutParams(-1, dp(42)));
        home.setOnClickListener(v -> {
            paymentAttempted = false;
            showHome(true);
        });
        if (success) icon.postDelayed(() -> icon.animate().rotation(360f).setDuration(600).start(), 250);
    }

    private LinearLayout topBar(String title, Runnable backAction) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = label("‹", 34, ORANGE, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(rounded(SURFACE, 14));
        bar.addView(back, new LinearLayout.LayoutParams(dp(45), dp(43)));
        back.setOnClickListener(v -> backAction.run());
        TextView heading = label(title, 12, MUTED, true);
        heading.setLetterSpacing(0.11f);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -1, 1);
        headingParams.leftMargin = dp(12);
        bar.addView(heading, headingParams);
        return bar;
    }

    private LinearLayout keypad() {
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setPadding(0, dp(5), 0, dp(3));
        String[][] digits = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"C", "0", "⌫"}};
        for (String[] rowDigits : digits) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, 0, 1);
            rowParams.topMargin = dp(4);
            rowParams.bottomMargin = dp(4);
            pad.addView(row, rowParams);
            for (int i = 0; i < rowDigits.length; i++) {
                String digit = rowDigits[i];
                TextView key = label(digit, digit.length() > 1 ? 21 : 24,
                        (digit.equals("⌫") || digit.equals("C")) ? ORANGE : WHITE, true);
                key.setGravity(Gravity.CENTER);
                key.setBackground(rounded(SURFACE, 15));
                key.setClickable(true);
                key.setFocusable(true);
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, -1, 1);
                if (i > 0) keyParams.leftMargin = dp(9);
                row.addView(key, keyParams);
                key.setOnClickListener(v -> {
                    if (digit.equals("⌫")) deleteDigit();
                    else if (digit.equals("C")) {
                        amountDigits = "";
                        updateAmountView();
                    } else enterDigit(digit);
                });
            }
        }
        return pad;
    }

    private TextView operationChoice(String text, boolean selected) {
        TextView choice = label(text, 13, selected ? BG : MUTED, true);
        choice.setGravity(Gravity.CENTER);
        choice.setBackground(rounded(selected ? ORANGE : SURFACE, 13));
        return choice;
    }

    private void setOperation(boolean add) {
        addingFunds = add;
        operationAdd.setTextColor(add ? BG : MUTED);
        operationAdd.setBackground(rounded(add ? ORANGE : SURFACE, 13));
        operationSubtract.setTextColor(add ? MUTED : BG);
        operationSubtract.setBackground(rounded(add ? SURFACE : ORANGE, 13));
        userNotice.setText("Введи сумму на цифровой панели");
        userNotice.setTextColor(MUTED);
    }

    private void enterDigit(String digit) {
        if (amountDigits.length() >= 12) return;
        if (amountDigits.equals("0")) amountDigits = digit;
        else amountDigits += digit;
        updateAmountView();
    }

    private void deleteDigit() {
        if (!amountDigits.isEmpty()) amountDigits = amountDigits.substring(0, amountDigits.length() - 1);
        updateAmountView();
    }

    private void updateAmountView() {
        if (amountView == null) return;
        String display = amountDigits.isEmpty() ? "0" : amountDigits;
        amountView.setText(formatMoney(Long.parseLong(display)) + " ₽");
    }

    private void applyUserOperation() {
        long amount = amountDigits.isEmpty() ? 0 : Long.parseLong(amountDigits);
        if (amount <= 0) {
            userNotice.setText("Сначала введи сумму");
            userNotice.setTextColor(ORANGE);
            return;
        }
        if (addingFunds) {
            if (Long.MAX_VALUE - balance < amount) {
                userNotice.setText("Сумма слишком большая");
                userNotice.setTextColor(RED);
                return;
            }
            balance += amount;
            userNotice.setText("Баланс успешно пополнен  ✓");
            userNotice.setTextColor(GREEN);
        } else {
            if (amount > balance) {
                userNotice.setText("Недостаточно средств");
                userNotice.setTextColor(RED);
                return;
            }
            balance -= amount;
            userNotice.setText("Сумма успешно списана  ✓");
            userNotice.setTextColor(GREEN);
        }
        saveBalance();
        balanceView.setText(formatMoney(balance) + " ₽");
        amountDigits = "";
        updateAmountView();
    }

    private void beginPayment() {
        long amount = amountDigits.isEmpty() ? 0 : Long.parseLong(amountDigits);
        if (amount <= 0) {
            amountView.setTextColor(ORANGE);
            amountView.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110)
                    .withEndAction(() -> amountView.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
            return;
        }
        pendingPaymentAmount = amount;
        paymentAttempted = false;
        showPayment(true);
    }

    private void saveBalance() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(BALANCE_KEY, balance).apply();
    }

    private String formatMoney(long value) {
        return NumberFormat.getIntegerInstance(new Locale("ru", "RU")).format(value);
    }

    private TextView actionButton(String text, int backgroundColor, int textColor) {
        TextView button = label(text, 14, textColor, true);
        button.setGravity(Gravity.CENTER);
        button.setLetterSpacing(0.045f);
        button.setBackground(rounded(backgroundColor, 16));
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

    private GradientDrawable circleFill(int color, int radiusDp) {
        GradientDrawable drawable = rounded(color, radiusDp);
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private GradientDrawable circleBorder(int color, int radiusDp, int strokeDp) {
        GradientDrawable drawable = circleFill(Color.TRANSPARENT, radiusDp);
        drawable.setStroke(dp(strokeDp), color);
        return drawable;
    }

    private void startPulse(View view, float from, float to, long duration) {
        view.setScaleX(from);
        view.setScaleY(from);
        view.setAlpha(0.65f);
        view.animate().scaleX(to).scaleY(to).alpha(0.05f).setDuration(duration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (screen != PAYMENT) return;
                        view.setScaleX(from);
                        view.setScaleY(from);
                        view.setAlpha(0.65f);
                        startPulse(view, from, to, duration);
                    }
                }).start();
    }

    private void fadeIn() {
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(220).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (screen == HOME) {
            super.onBackPressed();
        } else if (screen == PAYMENT) {
            paymentAttempted = false;
            showSellerEntry(true);
        } else {
            paymentAttempted = false;
            showHome(true);
        }
    }

    private final class ConfettiView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random(17);
        private final float[] angles = new float[42];
        private final float[] distances = new float[42];
        private final int[] colors = {ORANGE, GREEN, Color.WHITE, Color.rgb(255, 195, 84)};
        private float progress;

        ConfettiView(Activity context) {
            super(context);
            setClickable(false);
            for (int i = 0; i < angles.length; i++) {
                angles[i] = (float) (random.nextFloat() * Math.PI * 2);
                distances[i] = dp(70) + random.nextFloat() * dp(190);
            }
        }

        void start() {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1050);
            animator.addUpdateListener(valueAnimator -> {
                progress = (float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(INVISIBLE);
                }
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(Math.max(0, 255 - (int) (progress * 230)));
            for (int i = 0; i < angles.length; i++) {
                paint.setColor(colors[i % colors.length]);
                float distance = distances[i] * progress;
                float x = centerX + (float) Math.cos(angles[i]) * distance;
                float y = centerY + (float) Math.sin(angles[i]) * distance + dp(70) * progress * progress;
                float size = dp(3) + (i % 3) * dp(1);
                canvas.save();
                canvas.rotate(progress * 520 + i * 31, x, y);
                canvas.drawRoundRect(x - size, y - size / 2, x + size, y + size / 2,
                        size / 3, size / 3, paint);
                canvas.restore();
            }
        }
    }
}
