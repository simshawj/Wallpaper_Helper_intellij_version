
/*
 * Copyright (C) 2014 James Simshaw
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.  This
 * is available in the License.txt file included with the source code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.jamessimshaw.wallpaperhelper;

import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.os.Handler;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import java.io.FileNotFoundException;

public class Main_Service extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new Wallpaper_Helper_Engine();
    }

    private class Wallpaper_Helper_Engine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private int mWidth;
        private int mHeight;
        private Bitmap mLandscapeBitmap, mPortraitBitmap;
        private SharedPreferences mPreferences;

        private final Handler mHandler = new Handler();
        private final Runnable mRunner = new Runnable() {
            @Override
            public void run() {
                setWallpaper();
            }
        };

        public Wallpaper_Helper_Engine() {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(Main_Service.this);
            mPreferences.registerOnSharedPreferenceChangeListener(this);
            loadImages();
        }

        private void instructions(Canvas drawArea) {
            if(isVisible()) {
                TextPaint mTextPaint = new TextPaint();
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.instructionFontSize));
                mTextPaint.setAntiAlias(true);
                StaticLayout mTextLayout;
                if (mHeight > mWidth) {
                    mTextLayout = new StaticLayout(getResources().getString(R.string.instructionsPortrait),
                            mTextPaint, drawArea.getWidth() - 200, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
                } else {
                    mTextLayout = new StaticLayout(getResources().getString(R.string.instructionsLandscape),
                            mTextPaint, mWidth - 200, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
                }
                drawArea.translate(100,mHeight/2);
                mTextLayout.draw(drawArea);
            }
        }

        private void loadImages() {
            String mLandscape = mPreferences.getString("landscape", "Failed");
            String mPortrait = mPreferences.getString("portrait", "Failed");
            try {
                mPortraitBitmap = BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(Uri.parse(mPortrait)));
            } catch (FileNotFoundException e) {
                mLandscapeBitmap = null;
            }
            try {
                mLandscapeBitmap = BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(Uri.parse(mLandscape)));
            } catch (FileNotFoundException e) {
                mLandscapeBitmap = null;
            }
            mHandler.post(mRunner);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (isVisible()) {
                mHandler.post(mRunner);
            } else {
                mHandler.removeCallbacks(mRunner);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            this.mHeight = height;
            this.mWidth = width;
            super.onSurfaceChanged(holder, format, width, height);
            mHandler.post(mRunner);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mHandler.removeCallbacks(mRunner);
        }

        private void setWallpaper() {
            if(isVisible()) {
                SurfaceHolder mHolder = getSurfaceHolder();
                Canvas mDrawArea = mHolder.lockCanvas();

                if (mDrawArea != null) {
                    Rect mScreenRect = new Rect(0, 0, mWidth, mHeight);
                    if (mWidth > mHeight) {
                        if(mLandscapeBitmap != null) {
                            mDrawArea.drawBitmap(mLandscapeBitmap, null, mScreenRect, null);
                        } else {
                            instructions(mDrawArea);
                        }
                    } else {
                        if(mPortraitBitmap != null) {
                            mDrawArea.drawBitmap(mPortraitBitmap, null, mScreenRect, null);
                        } else {
                            instructions(mDrawArea);
                        }
                    }
                    mHolder.unlockCanvasAndPost(mDrawArea);
                } else {
                    //TODO: Error, canvas not locked
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            loadImages();
        }
    }
}