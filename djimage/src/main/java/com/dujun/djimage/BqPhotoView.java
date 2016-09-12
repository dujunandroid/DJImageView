package com.dujun.djimage;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.View;

import com.boqii.android.framework.image.internal.photoview.PhotoDraweeView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Created by jinyan on 16/8/23.
 */
public class BqPhotoView extends PhotoDraweeView {

    public static interface OnPhotoTapListener {
        void onPhotoTap(BqPhotoView bqPhotoView);
    }

    public static interface OnPhotoLongClickListener {
        boolean onPhotoLongClick(BqPhotoView bqPhotoView);
    }

    private OnPhotoTapListener onPhotoTapListener;
    private OnPhotoLongClickListener onPhotoLongClickListener;
    private OnImageLoadedListener onImageLoadedListener;


    public BqPhotoView(Context context) {
        super(context);
    }

    public BqPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void load(String uri) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri))
                .setResizeOptions(new ResizeOptions(dm.widthPixels/2, dm.heightPixels/2))
                .setAutoRotateEnabled(true)
                .build();
        PipelineDraweeControllerBuilder controller = Fresco.newDraweeControllerBuilder();
        controller.setImageRequest(imageRequest);
        controller.setOldController(getController());
        controller.setControllerListener(new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                super.onFinalImageSet(id, imageInfo, animatable);
                if (imageInfo == null) {
                    return;
                }
                update(imageInfo.getWidth(), imageInfo.getHeight());
                if (onImageLoadedListener != null) {
                    onImageLoadedListener.onImageSet(imageInfo.getWidth(), imageInfo.getHeight());
                }
            }

            @Override
            public void onFailure(String id, Throwable throwable) {
                super.onFailure(id, throwable);
                if (onImageLoadedListener != null) {
                    onImageLoadedListener.onImageFail(throwable);
                }
            }
        });
        setController(controller.build());
    }

    @Override
    public void setPhotoUri(Uri uri) {
        throw new RuntimeException("请勿调用此方法");
    }

    @Override
    public void setPhotoUri(Uri uri, @Nullable Context context) {
        throw new RuntimeException("请勿调用此方法");
    }

    public void setOnPhotoTapListener(final OnPhotoTapListener onPhotoTapListener) {
        this.onPhotoTapListener = onPhotoTapListener;
        super.setOnPhotoTapListener(new com.boqii.android.framework.image.internal.photoview.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                if (BqPhotoView.this.onPhotoTapListener != null) {
                    BqPhotoView.this.onPhotoTapListener.onPhotoTap(BqPhotoView.this);
                }
            }
        });
    }

    public void setOnPhotoLongClickListener(OnPhotoLongClickListener onPhotoLongClickListener) {
        this.onPhotoLongClickListener = onPhotoLongClickListener;
        super.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (BqPhotoView.this.onPhotoLongClickListener != null) {
                    return BqPhotoView.this.onPhotoLongClickListener.onPhotoLongClick(BqPhotoView.this);
                }
                return false;
            }
        });
    }

    public void setOnImageLoadedListener(OnImageLoadedListener onImageLoadedListener) {
        this.onImageLoadedListener = onImageLoadedListener;
    }

    @Override
    public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener listener) {
        throw new RuntimeException("请勿调用此方法");
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener listener) {
        throw new RuntimeException("请勿调用此方法");
    }

    @Override
    public void setOnPhotoTapListener(com.boqii.android.framework.image.internal.photoview.OnPhotoTapListener listener) {
        throw new RuntimeException("请勿调用此方法");
    }
}
