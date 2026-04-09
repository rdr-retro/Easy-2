package com.rdr.easy2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoodScannerActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 4301;

    private VolumeOverlayController volumeOverlayController;
    private LauncherThemePalette palette;
    private Locale locale;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private View rootView;
    private TextView titleView;
    private TextView introView;
    private PreviewView previewView;
    private TextView previewHintView;
    private View resultCardView;
    private TextView resultTitleView;
    private TextView resultValueView;
    private TextView resultDetailView;
    private TextView resultCodeView;
    private TextView scanNowButtonView;
    private TextView closeButtonView;

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private boolean analyzingFrame;
    private boolean scanLocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_scanner);
        volumeOverlayController = new VolumeOverlayController(this);
        locale = resolveLocale();
        cameraExecutor = Executors.newSingleThreadExecutor();
        barcodeScanner = BarcodeScanning.getClient(createBarcodeScannerOptions());

        bindViews();
        scanNowButtonView.setOnClickListener(view -> handlePrimaryAction());
        closeButtonView.setOnClickListener(view -> finish());

        applyThemePalette();
        enableFullscreen();
        prepareScannerFlow();
    }

    @Override
    protected void onDestroy() {
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (volumeOverlayController != null) {
            volumeOverlayController.release();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreen();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (volumeOverlayController != null && volumeOverlayController.handleKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }

        if (hasCameraPermission()) {
            prepareScannerFlow();
        } else {
            showPermissionState();
        }
    }

    private void bindViews() {
        rootView = findViewById(R.id.food_scanner_root);
        titleView = findViewById(R.id.food_scanner_title_view);
        introView = findViewById(R.id.food_scanner_intro_view);
        previewView = findViewById(R.id.food_scanner_preview_view);
        previewHintView = findViewById(R.id.food_scanner_preview_hint);
        resultCardView = findViewById(R.id.food_scanner_result_card);
        resultTitleView = findViewById(R.id.food_scanner_result_title);
        resultValueView = findViewById(R.id.food_scanner_result_value);
        resultDetailView = findViewById(R.id.food_scanner_result_detail);
        resultCodeView = findViewById(R.id.food_scanner_result_code);
        scanNowButtonView = findViewById(R.id.food_scanner_scan_button);
        closeButtonView = findViewById(R.id.food_scanner_close_button);
    }

    private BarcodeScannerOptions createBarcodeScannerOptions() {
        return new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_PDF417
                )
                .build();
    }

    private void handlePrimaryAction() {
        if (!hasCameraPermission()) {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
            return;
        }

        scanLocked = false;
        showIdleState();
        startCamera();
    }

    private void prepareScannerFlow() {
        if (!hasCameraPermission()) {
            showPermissionState();
            return;
        }

        scanLocked = false;
        showLoadingState();
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases();
                showIdleState();
            } catch (Exception exception) {
                showFailureState();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
        );
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (scanLocked || analyzingFrame) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        analyzingFrame = true;
        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> handleDetectedBarcodes(barcodes))
                .addOnFailureListener(exception -> {
                })
                .addOnCompleteListener(task -> {
                    analyzingFrame = false;
                    imageProxy.close();
                });
    }

    private void handleDetectedBarcodes(List<Barcode> barcodes) {
        if (barcodes == null || barcodes.isEmpty() || scanLocked) {
            return;
        }

        Barcode selectedBarcode = null;
        for (Barcode barcode : barcodes) {
            if (barcode != null && barcode.getRawValue() != null && !barcode.getRawValue().trim().isEmpty()) {
                selectedBarcode = barcode;
                break;
            }
        }

        if (selectedBarcode == null) {
            return;
        }

        scanLocked = true;
        String rawValue = selectedBarcode.getRawValue();
        Gs1DateParser.ParsedDate parsedDate = Gs1DateParser.parse(rawValue);

        runOnUiThread(() -> {
            if (parsedDate == null) {
                showNoDateState(rawValue);
            } else {
                showDateState(parsedDate, rawValue);
            }
        });
    }

    private void showDateState(Gs1DateParser.ParsedDate parsedDate, String rawValue) {
        String formattedDate = formatParsedDate(parsedDate);
        resultTitleView.setText(R.string.food_scanner_found_title);
        resultValueView.setText(parsedDate.getType() == Gs1DateParser.ParsedDate.Type.EXPIRATION
                ? getString(R.string.food_scanner_expiration_label, formattedDate)
                : getString(R.string.food_scanner_best_before_label, formattedDate));
        resultDetailView.setText(R.string.food_scanner_intro);
        resultCodeView.setText(getString(R.string.food_scanner_code_prefix, sanitizeCode(rawValue)));
        scanNowButtonView.setText(R.string.food_scanner_scan_again);
    }

    private void showPermissionState() {
        resultTitleView.setText(R.string.food_scanner_permission_title);
        resultValueView.setText("");
        resultDetailView.setText(R.string.food_scanner_permission_detail);
        resultCodeView.setText("");
        previewHintView.setText(R.string.food_scanner_preview_hint);
        scanNowButtonView.setText(R.string.food_scanner_permission_button);
    }

    private void showIdleState() {
        resultTitleView.setText(R.string.food_scanner_idle_title);
        resultValueView.setText("");
        resultDetailView.setText(R.string.food_scanner_idle_detail);
        resultCodeView.setText("");
        previewHintView.setText(R.string.food_scanner_preview_hint);
        scanNowButtonView.setText(R.string.food_scanner_scan_again);
    }

    private void showLoadingState() {
        resultTitleView.setText(R.string.food_scanner_opening);
        resultValueView.setText("");
        resultDetailView.setText(R.string.food_scanner_intro);
        resultCodeView.setText("");
        previewHintView.setText(R.string.food_scanner_preview_hint);
        scanNowButtonView.setText(R.string.food_scanner_scan_now);
    }

    private void showNoDateState(String rawValue) {
        resultTitleView.setText(R.string.food_scanner_missing_title);
        resultValueView.setText(R.string.food_scanner_missing_detail);
        resultDetailView.setText(R.string.food_scanner_common_barcode_detail);
        resultCodeView.setText(getString(R.string.food_scanner_code_prefix, sanitizeCode(rawValue)));
        scanNowButtonView.setText(R.string.food_scanner_scan_again);
    }

    private void showFailureState() {
        resultTitleView.setText(R.string.food_scanner_failed);
        resultValueView.setText("");
        resultDetailView.setText(R.string.food_scanner_intro);
        resultCodeView.setText("");
        scanNowButtonView.setText(R.string.food_scanner_scan_again);
    }

    private String formatParsedDate(Gs1DateParser.ParsedDate parsedDate) {
        if (parsedDate.getExactDate() != null) {
            return parsedDate.getExactDate().format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            );
        }

        return getString(
                R.string.food_scanner_month_only,
                String.format(locale, "%02d", parsedDate.getMonthOnlyDate().getMonthValue()),
                String.valueOf(parsedDate.getMonthOnlyDate().getYear())
        );
    }

    private String sanitizeCode(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "--";
        }
        return rawValue.replace('\u001d', ' ').trim();
    }

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller == null) {
            return;
        }

        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void applyThemePalette() {
        palette = LauncherThemePalette.fromPreferences(this);

        rootView.setBackgroundColor(palette.getBackgroundColor());
        titleView.setTextColor(palette.getHeadingColor());
        introView.setTextColor(palette.getBodyTextColor());
        previewHintView.setTextColor(Color.WHITE);
        resultTitleView.setTextColor(palette.getHeadingColor());
        resultValueView.setTextColor(palette.getPrimaryColor());
        resultDetailView.setTextColor(palette.getBodyTextColor());
        resultCodeView.setTextColor(palette.getBodyTextColor());
        resultCardView.setBackground(createRoundedBackground(
                palette.getSetupContactFillColor(),
                palette.getSetupContactStrokeColor(),
                28,
                2
        ));

        scanNowButtonView.setTextColor(Color.WHITE);
        scanNowButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_save_button));
        closeButtonView.setTextColor(Color.WHITE);
        closeButtonView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_close_button));

        if (volumeOverlayController != null) {
            volumeOverlayController.applyTheme(palette);
        }
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int cornerRadiusDp,
            int strokeWidthDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpToPx(cornerRadiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dpToPx(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private Locale resolveLocale() {
        LocaleList locales = getResources().getConfiguration().getLocales();
        return locales.isEmpty() ? Locale.getDefault() : locales.get(0);
    }

    private int dpToPx(int dpValue) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        ));
    }
}
