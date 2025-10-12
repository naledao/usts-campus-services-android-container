package hhsc.kangnasi.xyz.usts_campus_services_android_container.pages.index;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

import hhsc.kangnasi.xyz.simplespringmvc.ApiServerActivity;
import hhsc.kangnasi.xyz.simplespringmvc.util.ApiPortStore;
import hhsc.kangnasi.xyz.simplespringmvc.web.JsBridge;
import hhsc.kangnasi.xyz.usts_campus_services_android_container.R;

public class IndexActivityPage extends ApiServerActivity {

    private static final String UPDATE_CHECK_URL = "http://hhsc.kangnasi.xyz:25051/down/aSK4UOb9U2mS.txt"; // 设置为版本检查接口地址（仅返回版本号）
    private static final String APK_DOWNLOAD_URL = "http://hhsc.kangnasi.xyz/usts.apk"; // 固定的APK下载直链
    private static final int REQ_UNKNOWN_APP_SOURCES = 1001;
    private File pendingApkFile = null;
    private AlertDialog downloadDialog = null;
    private ProgressBar downloadProgressBar = null;
    private TextView downloadProgressText = null;
    private volatile boolean isDownloading = false;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.index_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.index_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String port=String.valueOf(ApiPortStore.getPort(this));
        String ip=getDeviceIp();
        Log.d("我的port", port);
        Log.d("我的ip", ip);


        // Setup WebView and load tester page
        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new JsBridge(this), "AndroidApi");
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setOnTouchListener((v, event) -> event.getPointerCount() > 1);
        // 设置 WebViewClient 拦截跳转/加载
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebView.setWebContentsDebuggingEnabled(true);
        webView.loadUrl("file:///android_asset/index.html");

        if (UPDATE_CHECK_URL != null && !UPDATE_CHECK_URL.isEmpty()) {
            checkForUpdate();
        }
    }

    @Override
    protected int getDesiredApiPort() {
        return 0;
    }

    public static String getDeviceIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress(); // 返回 IPv4
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void checkForUpdate() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPDATE_CHECK_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code == 200) {
                    String resp = readAll(conn.getInputStream());

                    long latestCode = -1L;
                    String latestName = "";
                    try {
                        String trimmed = resp == null ? "" : resp.trim();
                        if (!trimmed.isEmpty() && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
                            JSONObject json = new JSONObject(trimmed);
                            if (json.has("versionCode")) {
                                latestCode = json.optLong("versionCode", -1L);
                            } else if (json.has("version")) {
                                String v = json.optString("version", "");
                                try { latestCode = Long.parseLong(v.replaceAll("[^0-9]", "")); } catch (Exception ignore) {}
                            }
                            latestName = json.optString("versionName", "");
                        } else {
                            // 接口仅返回数字版本号（纯文本）
                            latestCode = Long.parseLong(trimmed.replaceAll("[^0-9]", ""));
                        }
                    } catch (Exception parseEx) {
                        Log.e("Update", "parse version failed: " + resp, parseEx);
                    }

                    long localCode = getLocalVersionCode();
                    Log.d("Update", "local=" + localCode + ", remote=" + latestCode + ", url=" + APK_DOWNLOAD_URL);
                    if (latestCode > localCode && APK_DOWNLOAD_URL != null && !APK_DOWNLOAD_URL.isEmpty()) {
                        long finalLatestCode = latestCode;
                        String finalLatestName = latestName;
                        runOnUiThread(() -> promptUpdate(finalLatestName, finalLatestCode, APK_DOWNLOAD_URL));
                    }
                } else {
                    Log.w("Update", "check http code=" + code);
                }
            } catch (Exception e) {
                Log.e("Update", "check failed", e);
            } finally {
                if (conn != null) conn.disconnect();
                isDownloading = false;
                runOnUiThread(this::dismissDownloadProgressDialog);
            }
        }).start();
    }

    private void promptUpdate(String latestName, long latestCode, String apkUrl) {
        String msg = (latestName == null || latestName.isEmpty()) ?
                ("发现新版本(" + latestCode + ")，是否更新？") :
                ("发现新版本 " + latestName + "，是否更新？");
        new AlertDialog.Builder(this)
                .setTitle("应用更新")
                .setMessage(msg)
                .setNegativeButton("取消", null)
                .setPositiveButton("更新", (d, w) -> downloadAndInstallApk(apkUrl))
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        Toast.makeText(this, "开始下载更新…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection conn = null;
            File outFile = null;
            runOnUiThread(this::showDownloadProgressDialog);
            isDownloading = true;
            try {
                URL url = new URL(apkUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(20000);
                conn.setRequestMethod("GET");
                conn.connect();
                if (conn.getResponseCode() != 200) throw new RuntimeException("HTTP " + conn.getResponseCode());

                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) dir = getFilesDir();
                if (!dir.exists()) dir.mkdirs();
                outFile = new File(dir, "update.apk");
                
                final long totalBytes = conn.getContentLengthLong();
                try (InputStream is = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8 * 1024];
                    int len;
                    long downloaded = 0L;
                    int lastPercent = -1;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        downloaded += len;
                        if (totalBytes > 0) {
                            int percent = (int) (downloaded * 100 / totalBytes);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                long finalDownloaded = downloaded;
                                runOnUiThread(() -> updateDownloadProgress(finalDownloaded, totalBytes));
                            }
                        } else {
                            long finalDownloaded = downloaded;
                            runOnUiThread(() -> updateDownloadProgress(finalDownloaded, -1));
                        }
                    }
                    fos.flush();
                }

                File finalOutFile = outFile;
                runOnUiThread(() -> {
                    Toast.makeText(this, "下载完成，开始安装…", Toast.LENGTH_SHORT).show();
                    installApk(finalOutFile);
                });
            } catch (Exception e) {
                File failedFile = outFile;
                Log.e("Update", "download failed", e);
                runOnUiThread(() -> Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                if (failedFile != null && failedFile.exists()) failedFile.delete();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, "安装包不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean canInstall = getPackageManager().canRequestPackageInstalls();
            if (!canInstall) {
                pendingApkFile = apkFile;
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_UNKNOWN_APP_SOURCES);
                Toast.makeText(this, "请允许从此来源安装后返回继续", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(this, authority, apkFile);
        } else {
            uri = Uri.fromFile(apkFile);
        }

        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (Exception e) {
            Log.e("Update", "install intent failed", e);
            Toast.makeText(this, "无法启动安装程序", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_UNKNOWN_APP_SOURCES && pendingApkFile != null && pendingApkFile.exists()) {
            installApk(pendingApkFile);
            pendingApkFile = null;
        }
    }

    private void showDownloadProgressDialog() {
        if (downloadDialog != null && downloadDialog.isShowing()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null, false);
        downloadProgressBar = view.findViewById(R.id.progress_bar);
        downloadProgressText = view.findViewById(R.id.progress_text);
        if (downloadProgressBar != null) {
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setMax(100);
        }
        if (downloadProgressText != null) downloadProgressText.setText("正在准备下载...");
        downloadDialog = new AlertDialog.Builder(this)
                .setTitle("正在下载更新")
                .setView(view)
                .setCancelable(false)
                .create();
        downloadDialog.show();
    }

    private void updateDownloadProgress(long downloaded, long total) {
        if (downloadProgressBar == null || downloadProgressText == null) return;
        if (total > 0) {
            int percent = (int) (downloaded * 100 / total);
            downloadProgressBar.setIndeterminate(false);
            downloadProgressBar.setProgress(percent);
            String text = String.format("已下载 %d%%", percent);
            downloadProgressText.setText(text);
        } else {
            downloadProgressBar.setIndeterminate(true);
            downloadProgressText.setText("正在下载...");
        }
    }

    private void dismissDownloadProgressDialog() {
        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.dismiss();
        }
        downloadDialog = null;
        downloadProgressBar = null;
        downloadProgressText = null;
    }

    @Override
    public void onBackPressed() {
        if (isDownloading) {
            Toast.makeText(this, "正在下载，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webView != null) {
            WebBackForwardList history = webView.copyBackForwardList();
            if (history != null && history.getCurrentIndex() > 0) {
                webView.goBack();
                return;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }

    private long getLocalVersionCode() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return PackageInfoCompat.getLongVersionCode(pi);
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }

    private String readAll(InputStream in) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
