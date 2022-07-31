package com.yurch.explorer;

import android.content.Context;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

class MyLinearLayoutManager extends LinearLayoutManager {
	public MyLinearLayoutManager(Context context) {
		super(context);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		// https://issuetracker.google.com/issues/37030377
		// https://stackoverflow.com/a/33822747/10941348
		try {
			super.onLayoutChildren(recycler, state);
		} catch (IndexOutOfBoundsException e) {
			Log.e("MyLinearLayoutManager", "meet a IOOBE in RecyclerView");
		}
	}
}