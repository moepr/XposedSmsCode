package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.content.Context;
import android.os.Bundle;
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

    public WebhookAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, XSharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        String webhook = xsp.getString(PrefConst.KEY_WEBHOOK, "");
        if (webhook !=null && !webhook.isEmpty()) {
            webhook = webhook.replace("#smscode#",mSmsMsg.getSmsCode());
            OkHttpClient okHttpClient = new OkHttpClient();
            HttpUrl httpUrl = HttpUrl.parse(webhook).newBuilder().build();
            Request request = new Request.Builder().url(httpUrl).build(); // GET 方法默认，无需显式写 .get()
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    Toast.makeText(mPhoneContext, "接口返回：" + result, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(mPhoneContext, "请求失败，状态码：" + response.code(), Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(mPhoneContext, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }
}
