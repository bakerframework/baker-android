/**
 * Copyright (c) 2013-2014. Francisco Contreras, Holland Salazar.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the Baker Framework nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
package com.baker.abaker.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.baker.abaker.R;

public class FlowLayout extends ViewGroup {
	private int mHorizontalSpacing;
	private int mVerticalSpacing;
	private Paint mPaint;

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
		try {
			mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0);
			mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0);
		} finally {
			a.recycle();
		}
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(0xffff0000);
		mPaint.setStrokeWidth(2.0f);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingRight();
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		boolean growHeight = widthMode != MeasureSpec.UNSPECIFIED;

		int width = 0;
		int height = getPaddingTop();

		int currentWidth = getPaddingLeft();
		int currentHeight = 0;

		boolean breakLine = false;
		boolean newLine = false;
		int spacing = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
		    spacing = mHorizontalSpacing;
			if (lp.horizontalSpacing > 0) {
				spacing = lp.horizontalSpacing;
			}

			if (growHeight && (breakLine || currentWidth + child.getMeasuredWidth() > widthSize)) {
				height += currentHeight + mVerticalSpacing;
				currentHeight = 0;
				width = Math.max(width, currentWidth - spacing);
				
				currentWidth = getPaddingLeft();
				newLine = true;
			} else {
				newLine = false;
			}

			lp.x = currentWidth;
			lp.y = height;

			currentWidth += child.getMeasuredWidth() + spacing;
			currentHeight = Math.max(currentHeight, child.getMeasuredHeight());
			
			breakLine = lp.breakLine;
		}

		height += currentHeight;
		width = Math.max(width, currentWidth - spacing);

        width += getPaddingRight();
		height += getPaddingBottom();

		setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), lp.y + child.getMeasuredHeight());
		}
	}
	
	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		boolean more = super.drawChild(canvas, child, drawingTime);
		LayoutParams lp = (LayoutParams) child.getLayoutParams();
		if (lp.horizontalSpacing > 0) {
			float x = child.getRight();
			float y = child.getTop() + child.getHeight() / 2.0f;
			canvas.drawLine(x, y - 4.0f, x, y + 4.0f, mPaint);
			canvas.drawLine(x, y, x + lp.horizontalSpacing, y, mPaint);
			canvas.drawLine(x + lp.horizontalSpacing, y - 4.0f, x + lp.horizontalSpacing, y + 4.0f, mPaint);
		}
		if (lp.breakLine) {
			float x = child.getRight();
			float y = child.getTop() + child.getHeight() / 2.0f;
			canvas.drawLine(x, y, x, y + 6.0f, mPaint);
			canvas.drawLine(x, y + 6.0f, x + 6.0f, y + 6.0f, mPaint);
		}
		return more;
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	
	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p.width, p.height);
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		int x;
		int y;
		
		public int horizontalSpacing;
		public boolean breakLine;

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams);
			try {
				horizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing, 0);
				breakLine = a.getBoolean(R.styleable.FlowLayout_LayoutParams_layout_breakLine, false);
			} finally {
				a.recycle();
			}
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}
	}
}
