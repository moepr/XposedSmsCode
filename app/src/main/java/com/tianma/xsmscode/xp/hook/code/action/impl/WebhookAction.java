package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.xp.hook.code.action.RunnableAction;

import java.io.IOException;

import de.robv.android.xposed.XSharedPreferences;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * webhook回调
 */
public class WebhookAction extends RunnableAction {

    // 主线程Handler
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    // OkHttpClient 类内复用（低频请求足够，无需单例）
    private final OkHttpClient mOkHttpClient = new OkHttpClient();

    public WebhookAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, XSharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        String webhook = xsp.getString(PrefConst.KEY_WEBHOOK, "");
        if (webhook !=null && !webhook.isEmpty()) {
            // 替换短信验证码占位符
            webhook = webhook.replace("#smscode#", mSmsMsg.getSmsCode());
            // 网络请求放到子线程执行，避免主线程崩溃
            String finalWebhook = webhook;
            new Thread(() -> {
                HttpUrl httpUrl = HttpUrl.parse(finalWebhook);
                if (httpUrl == null) {
                    showToast("Webhook地址格式错误");
                    return;
                }
                Request request = new Request.Builder().url(httpUrl).build();
                try (Response response = mOkHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String result = response.body() != null ? response.body().string() : "无返回内容";
                        showToast("接口返回：" + result);
                    } else {
                        showToast("请求失败，状态码：" + response.code());
                    }
                } catch (IOException e) {
                    showToast("请求异常：" + (e.getMessage() != null ? e.getMessage() : "网络错误"));
                }
            }).start();
        }
        return null;
    }

    /**
     * 封装Toast方法，确保在主线程显示
     */
    private void showToast(String msg) {
        mMainHandler.post(() -> {
            Context safeContext = mPhoneContext.getApplicationContext();
            Toast.makeText(safeContext, msg, Toast.LENGTH_LONG).show();
        });
    }
}
