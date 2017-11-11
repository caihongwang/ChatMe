/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2009 The Android Open Source Project
 */

package com.csipsimple.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.chatme.R;
import com.csipsimple.utils.Log;

public class HorizontalQuickActionWindow extends PopupWindow implements KeyEvent.Callback {
    private static final String THIS_FILE = "HorizontalQuickActionWindow";
    private final Resources resources;
    private final LayoutInflater inflater;

    View contentView;

    private int screenWidth, shadowWidth;

    private ImageView arrowUp, arrowDown;

    private ViewGroup track;
    private Animation trackAnim;

    private View anchorView;
    private Rect anchorRect;
    
    public HorizontalQuickActionWindow(Context aContext) {
        this(aContext, (View) null);
    }
    
    public HorizontalQuickActionWindow(Context aContext, AttributeSet attrs) {
        this(aContext, (View) null);
    }
    
    public HorizontalQuickActionWindow(Context aContext, AttributeSet attrs, int defStyle) {
        this(aContext, (View) null);
    }

    public HorizontalQuickActionWindow(Context aContext, View aView) {
        super(aContext);

        // Window and anchor init
        WindowManager wm = (WindowManager) aContext.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
        anchorView = aView;

        setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resources = aContext.getResources();
        shadowWidth = resources.getDimensionPixelSize(R.dimen.quickaction_shadow_horiz);
        setWidth(screenWidth + shadowWidth + shadowWidth);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // Set self properties
        setBackgroundDrawable(new ColorDrawable(0));
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);

        // Inflate
        inflater = ((Activity) aContext).getLayoutInflater();
        setContentView(R.layout.quickaction);

        // Init ui elements
        arrowUp = (ImageView) contentView.findViewById(R.id.arrow_up);
        arrowDown = (ImageView) contentView.findViewById(R.id.arrow_down);
        track = (ViewGroup) contentView.findViewById(R.id.quickaction);

        // Animation
        trackAnim = AnimationUtils.loadAnimation(aContext, R.anim.quickaction);
        trackAnim.setInterpolator(new Interpolator() {
            public float getInterpolation(float t) {
                // Pushes past the target area, then snaps back into place.
                // Equation for graphing: 1.2-((x*1.6)-1.1)^2
                final float inner = (t * 1.55f) - 1.1f;
                return 1.2f - inner * inner;
            }
        });
    }

    private void setContentView(int resId) {
        contentView = inflater.inflate(resId, null);
        super.setContentView(contentView);
    }

    public void setAnchor(Rect rect) {
        anchorRect = rect;
    }

    /**
     * Show the correct call-out arrow based on a {@link R.id} reference.
     */
    private void showArrow(int whichArrow, int requestedX) {
        final View showArrow = (whichArrow == R.id.arrow_up) ? arrowUp : arrowDown;
        final View hideArrow = (whichArrow == R.id.arrow_up) ? arrowDown : arrowUp;

        final int arrowWidth = arrowUp.getMeasuredWidth();

        showArrow.setVisibility(View.VISIBLE);
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) showArrow
                .getLayoutParams();
        param.leftMargin = requestedX - arrowWidth / 2;

        hideArrow.setVisibility(View.INVISIBLE);
    }

    public void addItem(Drawable drawable, String text, OnClickListener l) {
        QuickActionItem view = (QuickActionItem) inflater.inflate(R.layout.quickaction_item, track,
                false);
        view.setImageDrawable(drawable);
        view.setText(text);
        view.setOnClickListener(l);

        final int index = track.getChildCount() - 1;
        track.addView(view, index);
    }

    public void addItem(Bitmap drawable, String text, OnClickListener l) {

    }

    public void addItem(int drawable, String text, OnClickListener l) {
        addItem(resources.getDrawable(drawable), text, l);
    }

    public void addItem(Drawable drawable, int resid, OnClickListener l) {
        addItem(drawable, resources.getString(resid), l);
    }

    public void addItem(int drawable, int resid, OnClickListener l) {
        addItem(resources.getDrawable(drawable), resources.getText(resid).toString(), l);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }

        return false;
    }

    private void onBackPressed() {
        dismiss();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    public void show() {
        show(anchorRect.centerX());
    }

    /**
     * Start showing a dialog for the given element pointing towards the given
     * location.
     */
    public void show(int requestedX) {
        if (anchorRect == null) {
            Log.e(THIS_FILE, "Anchor not defined : Impossible to show the window");
            return;
        }
        super.showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, 0);

        // Calculate properly to position the popup the correctly based on
        // height of popup
        if (isShowing()) {
            int x, y, windowAnimations;
            this.getContentView().measure(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            final int blockHeight = this.getContentView().getMeasuredHeight();

            x = -shadowWidth;

            // Log.d("HorizontalQuickActionWindow",
            // "blockHeight: "+blockHeight);

            if (anchorRect.top > blockHeight) {
                // Show downwards callout when enough room, aligning bottom
                // block
                // edge with top of anchor area, and adjusting to inset arrow.
                showArrow(R.id.arrow_down, requestedX);
                y = anchorRect.top - blockHeight;
                windowAnimations = R.style.QuickActionAboveAnimation;

            } else {
                // Otherwise show upwards callout, aligning block top with
                // bottom of
                // anchor area, and adjusting to inset arrow.
                showArrow(R.id.arrow_up, requestedX);
                y = anchorRect.bottom;
                windowAnimations = R.style.QuickActionBelowAnimation;
            }

            // Log.d("HorizontalQuickActionWindow",
            // "X: "+x+"; Y: "+y+"; Width: "+anchorRect.width()+"; Center: "+anchorRect.centerX());

            setAnimationStyle(windowAnimations);
            track.startAnimation(trackAnim);
            this.update(x, y, -1, -1);
        }
    }

    /* @Override *//* No tag for android 1.5 compat */
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public void removeAllItems() {
        track.removeViews(1, track.getChildCount() - 2);
    }
}
