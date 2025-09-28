package hhsc.kangnasi.xyz.usts_campus_services_android_container.controller;

import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hhsc.kangnasi.xyz.simplespringmvc.api.mvc.annotations.GetMapping;
import hhsc.kangnasi.xyz.simplespringmvc.api.mvc.annotations.RequestMapping;
import hhsc.kangnasi.xyz.simplespringmvc.api.mvc.annotations.RestController;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequestMapping("/ip")
@RestController
public class IpController {

    private final String campusIp="10.160.63.9";

    private final String campusUrl="http://10.160.63.9";

    @GetMapping("/get")
    public String getIp(){
        try {
            InetAddress inet = InetAddress.getByName(campusIp);
            boolean status = inet.isReachable(6000); // 5秒超时
            if (true) {
                // 1) 初次 GET：携带常见头，设置超时与重定向
                Connection conn = Jsoup.connect(campusUrl)
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Safari")
                        .timeout(8000)            // 8s 超时
                        .followRedirects(true)
                        .ignoreHttpErrors(true);  // 即使 40x/50x 也取到响应体
                Connection.Response resp = conn.execute();
                String doc = resp.parse().toString();
                Pattern pattern = Pattern.compile("(10\\.160(?:\\.\\d+){2})");
                Matcher matcher = pattern.matcher(doc);
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    return "";
                }
            } else {
                return "";
            }
        } catch (Exception e) {
            String error=e.getMessage();
            Log.d("获取ip错误", error);
            return "";
        }
    }
}
