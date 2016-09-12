package com.dujun.djimage;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.File;

/**
 * 图片框架入口。封装底层实现。
 * Created by JinYan on 2016/7/29.
 */
public final class BqImage {

    private static ImageImp imp;

    static int defaultGlobalResizeWidth;
    static int defaultGlobalResizeHeight;

    /**
     * 初始化BqImage,请在Application:onCreate中进行初始化。
     *
     * @param context
     */
    public static void initialize(Context context) {
        initialize(context, null);
    }

    public static void initialize(Context context, Object initParam) {
        if (imp == null) {
            imp = new FrescoImp();
            imp.initialize(context, initParam);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            defaultGlobalResizeWidth = dm.widthPixels / 3;
            defaultGlobalResizeHeight = dm.heightPixels / 3;
        }
    }

    /**
     * 异步加载图片的bitmap。加载操作和callback调用都在后台线程进行。
     *
     * @param uri
     * @param callback
     */
    public static void loadBitmap(String uri, BqImageCallback callback) {
        imp.loadBitmap(uri, callback);
    }

    /**
     * 异步加载图片的bitmap。加载操作和callback调用都在后台线程进行。width和height只是一个建议值,具体返回bitmap大小可能不一致。
     * 参考: http://fresco-cn.org/docs/resizing-rotating.html
     *
     * @param uri
     * @param callback
     */
    public static void loadBitmap(String uri, int width, int height, final BqImageCallback callback) {
        imp.loadBitmap(uri, width, height, callback);
    }


    /******
     * uri 拦截器
     ****/

    private static BqUriIntercepter globalIntercepter = new BqUriIntercepter() {
        @Override
        public String intercept(BqImageView imageView, String uri) {
            /* workaround: 有些uri是文件路径,这里转一下。主要是防止用错。 */
            Uri u = Uri.parse(uri);
            if (TextUtils.isEmpty(u.getScheme())) {
                return Uri.fromFile(new File(uri)).toString();
            }
            return uri;
        }
    };

    /**
     * 设置全局到uri拦截器
     *
     * @param intercepter
     */
    public static void setGlobalUriIntercepter(BqUriIntercepter intercepter) {
        globalIntercepter = intercepter;
    }

    public static BqUriIntercepter getGlobalIntercepter() {
        return globalIntercepter;
    }

    public static void setDefaultGolbalResize(int width, int height) {
        defaultGlobalResizeWidth = width;
        defaultGlobalResizeHeight = height;
    }

    /*******图片效果*******/
    /**
     * bqimage支持的各种效果在这里添加定义。
     */
    public static interface Processors {
        int NONE = 0;
        int BLUR = 1;
        int SHADOW = 2;
    }

}
