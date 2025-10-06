package hhsc.kangnasi.xyz.usts_campus_services_android_container.pages.index;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import hhsc.kangnasi.xyz.simplespringmvc.ApiServerActivity;
import hhsc.kangnasi.xyz.simplespringmvc.util.ApiPortStore;
import hhsc.kangnasi.xyz.simplespringmvc.web.JsBridge;
import hhsc.kangnasi.xyz.usts_campus_services_android_container.R;

public class IndexActivityPage extends ApiServerActivity {

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
        WebView webView = findViewById(R.id.webview);
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
        webView.addJavascriptInterface(new JsBridge(this), "AndroidApi");
        // 设置 WebViewClient 拦截跳转/加载
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebView.setWebContentsDebuggingEnabled(true);
        webView.loadUrl("file:///android_asset/index.html");
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
}