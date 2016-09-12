package com.dujun.djimage;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.boqii.android.framework.image.internal.processor.BlurPostprocessor;
import com.boqii.android.framework.image.internal.processor.ShadowPostprocessor;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;

/**
 * 抽象ImageView，方便以后替换底层库。需要动态加载的所有原本采用android.widget.ImageView的地方都用本类替换。
 * <p/>（只需要显示静态资源的ImageView可以仍使用android.widget.ImageView）
 * <p/>
 * <p/> 当前采用Fresco框架，由于Fresco的限制，必须继承SimpleDraweeView。
 * <p/> 如果以后采用其他底层库，可以将此类直接继承android.widget.ImageView或者其他特殊的类。
 * Created by JinYan on 2016/7/29.
 */
public class BqImageView extends SimpleDraweeView {

    private static final float NO_RATIO = 0F;

    private static final ImageView.ScaleType[] sScaleTypeArray = {
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_CENTER,
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.CENTER,
            ImageView.ScaleType.CENTER_CROP,
            ImageView.ScaleType.CENTER_INSIDE
    };

    /**
     * 原图scaleType;
     */
    private ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_CROP;

    /**
     * 占位图片resId
     */
    private int placeholdId = 0;

    /**
     * 占位图片ScaleType
     */
    private ImageView.ScaleType placeholdScaleType = ImageView.ScaleType.CENTER_CROP;

    /**
     * 固定长宽比例
     */
    private float ratio = NO_RATIO;

    /**
     * 是否显示为圆形
     */
    private boolean asCircle = false;

    /**
     * 是否显示为圆角矩形
     */
    private boolean asRoundRect = false;

    /**
     * 圆角矩形圆角半径
     */
    private int roundRectRadius = 0;

    /**
     * 圆形或者圆角时边框颜色
     */
    private int borderColor = 0;

    /**
     * 圆形或者圆角时边框宽度
     */
    private int borderWidth = 0;

    /**
     * resize宽度。如果图片大小超出视图很多建议使用resize,以减少内存使用。
     * 参考: http://fresco-cn.org/docs/resizing-rotating.html
     */
    private int resizeWidth = 0;

    /**
     * resize高度。如果图片大小超出视图很多建议使用resize,以减少内存使用。
     * 参考: http://fresco-cn.org/docs/resizing-rotating.html
     */
    private int resizeHeight = 0;

    /**
     * 支持的图片图片处理效果。
     */
    private int processor = BqImage.Processors.NONE;

    private OnImageLoadedListener onImageLoadedListener;

    private BqUriIntercepter uriIntercepter;

    private GenericDraweeHierarchy hierarchy;

    public BqImageView(Context context) {
        this(context, null);
    }

    public BqImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        resizeWidth = BqImage.defaultGlobalResizeWidth;
        resizeHeight = BqImage.defaultGlobalResizeHeight;

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BqImageView);

            if (a.hasValue(R.styleable.BqImageView_bqScaleType)) {
                int index = a.getInt(R.styleable.BqImageView_bqScaleType, 0);
                scaleType = sScaleTypeArray[index];
            }

            placeholdId = a.getResourceId(R.styleable.BqImageView_bqPlaceholder, placeholdId);

            if (a.hasValue(R.styleable.BqImageView_bqPlaceholderScaleType)) {
                int index = a.getInt(R.styleable.BqImageView_bqPlaceholderScaleType, 0);
                placeholdScaleType = sScaleTypeArray[index];
            }

            ratio = a.getFloat(R.styleable.BqImageView_bqRatio, NO_RATIO);
            ratio(ratio);

            asCircle = a.getBoolean(R.styleable.BqImageView_bqAsCircle, asCircle);

            asRoundRect = a.getBoolean(R.styleable.BqImageView_bqAsRoundRect, asRoundRect);
            roundRectRadius = a.getDimensionPixelSize(R.styleable.BqImageView_bqRoundRectRadius, roundRectRadius);

            borderColor = a.getColor(R.styleable.BqImageView_bqBorderColor, borderColor);

            borderWidth = a.getDimensionPixelSize(R.styleable.BqImageView_bqBorderWidth, borderWidth);

            resizeWidth = a.getDimensionPixelSize(R.styleable.BqImageView_bqResizeWidth, resizeWidth);
            resizeHeight = a.getDimensionPixelSize(R.styleable.BqImageView_bqResizeHeight, resizeHeight);

            processor = a.getInt(R.styleable.BqImageView_bqProcessor, processor);

            a.recycle();
        }

        GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(getResources());
        builder.setActualImageScaleType(convertToFrescoScaleType(scaleType));
        if (placeholdId != 0) {
            builder.setPlaceholderImage(placeholdId, convertToFrescoScaleType(placeholdScaleType));
        }

        if (asCircle || asRoundRect) {
            RoundingParams roundingParams;
            if (asCircle) {
                roundingParams = RoundingParams.asCircle();
            } else {
                roundingParams = RoundingParams.fromCornersRadius(roundRectRadius);
            }

            if (borderWidth > 0) {
                roundingParams.setBorder(borderColor, borderWidth);
            }

            builder.setRoundingParams(roundingParams);
        }

        hierarchy = builder.build();
        setHierarchy(hierarchy);
    }


    public BqImageView scaleType(ImageView.ScaleType scaleType) {
        this.scaleType = scaleType;
        hierarchy.setActualImageScaleType(convertToFrescoScaleType(scaleType));
        return this;
    }

    public BqImageView placeholder(int resId) {
        placeholdId = resId;
        hierarchy.setPlaceholderImage(resId);
        return this;
    }

    public BqImageView placeholder(int resId, ImageView.ScaleType scaleType) {
        placeholdId = resId;
        placeholdScaleType = scaleType;
        hierarchy.setProgressBarImage(resId, convertToFrescoScaleType(scaleType));
        return this;
    }

    public BqImageView ratio(float ratio) {
        this.ratio = ratio;
        if (ratio > NO_RATIO) {
            setAspectRatio(ratio);
        }
        return this;
    }

    public BqImageView asCircle() {
        asCircle = true;
        hierarchy.setRoundingParams(RoundingParams.asCircle());
        return this;
    }

    public BqImageView asRoundRect(int radius) {
        asRoundRect = true;
        roundRectRadius = radius;
        hierarchy.setRoundingParams(RoundingParams.fromCornersRadius(radius));
        return this;
    }

    public BqImageView border(int color, int width) {
        borderColor = color;
        borderWidth = width;
        RoundingParams roundingParams = hierarchy.getRoundingParams();
        roundingParams.setBorder(color, width);
        return this;
    }

    /**
     * resize高度。如果图片大小超出视图很多建议使用resize,以减少内存使用。
     * 参考: http://fresco-cn.org/docs/resizing-rotating.html
     */
    public BqImageView suggestResize(int width, int height) {
        resizeWidth = width;
        resizeHeight = height;
        return this;
    }

    /**
     * 设置图片效果
     *
     * @param processor
     * @return
     */
    public BqImageView processor(int processor) {
        this.processor = processor;
        return this;
    }

    /**
     * 设置当前uri请求拦截器。
     *
     * @param uriIntercepter
     * @return
     */
    public BqImageView uriInterpreter(BqUriIntercepter uriIntercepter) {
        this.uriIntercepter = uriIntercepter;
        return this;
    }

    public BqImageView listerner(OnImageLoadedListener listener) {
        this.onImageLoadedListener = listener;
        return this;
    }

    private void loadUri(Uri uri) {
        if (uri == null) {
            loadRes(R.drawable.bq_image_empty_image);
            return;
        }
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri);
        if (resizeWidth > 0 && resizeHeight > 0) {
            imageRequestBuilder.setResizeOptions(new ResizeOptions(resizeWidth, resizeHeight));
        }
        if (processor > BqImage.Processors.NONE) {
            BasePostprocessor basePostprocessor = getPostprocessor(getContext(), processor);
            if (basePostprocessor != null) {
                imageRequestBuilder.setPostprocessor(basePostprocessor);
            }
        }
        imageRequestBuilder.setAutoRotateEnabled(true);
        ImageRequest request = imageRequestBuilder.build();

        PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                .setOldController(getController())
                .setAutoPlayAnimations(true)
                .setImageRequest(request);
        if (onImageLoadedListener != null) {
            builder.setControllerListener(new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    onImageLoadedListener.onImageSet(imageInfo == null ? 0 : imageInfo.getWidth(),
                            imageInfo == null ? 0 : imageInfo.getHeight());
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    onImageLoadedListener.onImageFail(throwable);
                }
            });
        }

        DraweeController controller = builder.build();
        setController(controller);
    }

    public void load(String uri) {
        if (TextUtils.isEmpty(uri)) {
            loadUri(null);
            return;
        }

        if (uriIntercepter != null) {
            uri = uriIntercepter.intercept(this, uri);
        } else if (BqImage.getGlobalIntercepter() != null) {
            uri = BqImage.getGlobalIntercepter().intercept(this, uri);
        }

        loadUri(Uri.parse(uri));
    }

    public void loadFile(File file) {
        if (file == null) {
            return;
        }
        loadUri(Uri.fromFile(file));
    }

    public void loadFile(String path) {
        if (path == null) {
            return;
        }
        loadFile(new File(path));
    }

    public void loadRes(int resId) {
        Uri uri = new Uri.Builder()
                .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                .path(String.valueOf(resId))
                .build();
        loadUri(uri);
    }

    private static ScalingUtils.ScaleType convertToFrescoScaleType(ImageView.ScaleType scaleType) {
        switch (scaleType) {
            case FIT_XY:
                return ScalingUtils.ScaleType.FIT_XY;
            case FIT_START:
                return ScalingUtils.ScaleType.FIT_START;
            case FIT_CENTER:
                return ScalingUtils.ScaleType.FIT_CENTER;
            case FIT_END:
                return ScalingUtils.ScaleType.FIT_END;
            case CENTER:
                return ScalingUtils.ScaleType.CENTER;
            case CENTER_CROP:
                return ScalingUtils.ScaleType.CENTER_CROP;
            case CENTER_INSIDE:
                return ScalingUtils.ScaleType.CENTER_INSIDE;
            case MATRIX:
                throw new RuntimeException("fresco doesn't support scale type 'MATRIX'");
            default:
                throw new RuntimeException("unsupported scale type: " + scaleType);
        }
    }

    private static BasePostprocessor getPostprocessor(Context context, int type) {
        switch (type) {
            case BqImage.Processors.BLUR:
                return new BlurPostprocessor(context);
            case BqImage.Processors.SHADOW:
                return new ShadowPostprocessor();
        }
        return null;
    }
}
