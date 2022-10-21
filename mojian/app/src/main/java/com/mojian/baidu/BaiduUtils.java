package com.mojian.baidu;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

/**
 * Author: pengmutian
 * Date: 2022/10/21 13:15
 * Description: BaiduUtils
 */
public class BaiduUtils {

    private static final String TAG = "BaiduUtils";


    /**
     * 重要提示代码中所需工具类
     * FileUtil,Base64Util,HttpUtil,GsonUtils请从
     * https://ai.baidu.com/file/658A35ABAB2D404FBF903F64D47C1F72
     * https://ai.baidu.com/file/C8D81F3301E24D2892968F09AE1AD6E2
     * https://ai.baidu.com/file/544D677F5D4E4F17B4122FBD60DB82B3
     * https://ai.baidu.com/file/470B3ACCA3FE43788B5A963BF0B625F3
     * 下载
     */
    public static JsonRootBean dish(Bitmap bitmap) {
        if (TextUtils.isEmpty(token)) {
            token = AuthService.getAuth();
        }
        Log.d(TAG, "token " + token);
        // 请求url
        String url = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish";
        try {
            // 本地文件路径
//                String filePath = "[本地文件路径]";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imgData = baos.toByteArray();

//                byte[] imgData = FileUtil.readFileByBytes(filePath);
            String imgStr = Base64Util.encode(imgData);
            String imgParam = URLEncoder.encode(imgStr, "UTF-8");

            String param = "image=" + imgParam + "&top_num=" + 5 + "&scenes=dishs";

            // 注意这里仅为了简化编码每一次请求都去获取access_token，线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
            String accessToken = token;

            String result = HttpUtil.post(url, accessToken, param);
            Log.e(TAG, result);
            JsonRootBean jsonRootBean = new Gson().fromJson(result, JsonRootBean.class);
            return jsonRootBean;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "test", e);
        }
        return null;
    }

    private static String token = "";
}
