package com.yurch.explorer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

// based on https://stackoverflow.com/a/37278154/10941348
//        & https://stackoverflow.com/questions/14195207
public class EditTextFloatingPostfix extends AppCompatEditText {
	private static final String POSTFIX         = ".txt";
	private static final float     POSTFIX_PADDING = 0f;
	private final        TextPaint textPaint       = new TextPaint();

	public EditTextFloatingPostfix(Context context) {
		super(context);
	}

	public EditTextFloatingPostfix(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextFloatingPostfix(
			@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr
	) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int postfixXPosition = (int) (getText() == null
		                              ? getPaddingLeft()
		                              : textPaint.measureText(getText().toString()) + getPaddingLeft());

		canvas.drawText(POSTFIX, Math.max(postfixXPosition, POSTFIX_PADDING), getBaseline(), textPaint);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		textPaint.setColor(Color.LTGRAY);
		textPaint.setTextSize(getTextSize());
		textPaint.setTextAlign(Paint.Align.LEFT);
	}
}
