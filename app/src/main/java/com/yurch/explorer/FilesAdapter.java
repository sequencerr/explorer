package com.yurch.explorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.BaseViewHolder> {
	private static final int              MAX_FILENAME_LENGTH_DISP = 48;
	private final        LayoutInflater   mInflater;
	public               Comparator<File> mFileComparator;
	File mCurrentDir;
	private ArrayList<File>   mFilesData;
	private ClickListener     mClickListener;
	private LongClickListener mLongClickListener;

	protected FilesAdapter(Context context, File filesData) {
		mInflater = LayoutInflater.from(context);
		updateFilesData(filesData, true);
	}

	@NonNull
	private static <T> ArrayList<T> toArrayList(T[] a) {
		if (a == null) return new ArrayList<>();
		return new ArrayList<>(Arrays.asList(a));
	}

	// inflates the row layout from xml when needed
	// Creates new views (invoked by the layout manager)
	@NonNull
	@Override
	public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView = mInflater.inflate(R.layout.recycler_adapter_row, parent, false);
		switch (viewType) {
		case 0:
			return new FoldersHolder(itemView);
		case 1:
			return new FilesHolder(itemView);
		}
		//noinspection ConstantConditions
		return null;
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).isDirectory()
		       ? 0
		       : 1;
	}

	// binds the data to the TextView in each row
	// Replaces the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
		String name = getItem(position).getName();
		holder.updateFileName(name.length() > MAX_FILENAME_LENGTH_DISP
		                      ? name.substring(0, MAX_FILENAME_LENGTH_DISP) + "..."
		                      : name);
	}

	/**
	 * @return the size of the dataset (invoked by the layout manager)
	 */
	@Override
	public int getItemCount() {
		return mFilesData.size();
	}

	public File getCurrentDir() {
		return mCurrentDir;
	}

	public String getCurrentDirPath() {
		try {
//			Absolute Path: E:\workspace\gfg\..\..\Test.txt
//			Canonical Path: E:\Test.txt
//			while calling getAbsolutePath(), we can see how we get the path, not in a fully relative one but, partial relative path. But while calling getCanonicalPath(), we can see the direct abstract path in the file system
			return mCurrentDir.getCanonicalPath();
		} catch (IOException e) {
			return mCurrentDir.getAbsolutePath();
		}
	}

	@NonNull
	private ArrayList<File> sortItems(ArrayList<File> files) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			files.sort(mFileComparator);
			return files;
		}
		else {
			File[] arrayFiles = files.toArray(new File[0]);
			Arrays.sort(arrayFiles, mFileComparator);
			return toArrayList(arrayFiles);
		}
	}

	public void updateFilesData(File dirToOpen) {
		updateFilesData(dirToOpen, false);
	}

	// ⬇ all dataset will be changed anyway
	@SuppressLint("NotifyDataSetChanged")
	private void updateFilesData(@NonNull File dirToOpen, boolean isInit) {
		ArrayList<File> files = toArrayList(dirToOpen.listFiles());

		for (Iterator<File> it = files.iterator(); it.hasNext(); ) {
			File file = it.next();
			if (!file.canRead() || file.isHidden()) it.remove();
		}

		if (files.isEmpty()) {
			Toast.makeText(getContext(), "This folder is empty.", Toast.LENGTH_SHORT).show();
		}

		mCurrentDir = dirToOpen;
		if (!isInit) {
			mFilesData = getItemCount() > 1 && mFileComparator != null
			             ? sortItems(files)
			             : files;

			notifyDataSetChanged();
		}
		else {
			mFilesData = files;
		}
	}

	/**
	 * @return the position where item was inserted
	 */
	public int addFile(File file) {
		mFilesData.add(file);
		if (getItemCount() > 1 && mFileComparator != null) mFilesData = sortItems(mFilesData);
		int position = mFilesData.indexOf(file);
		notifyItemInserted(position);
		return position;
	}

	public void removeFile(File file) {
		int position = mFilesData.indexOf(file);
		mFilesData.remove(file);
		notifyItemRemoved(position);
	}

	public File getItem(int position) {
		return mFilesData.get(position);
	}

	public boolean contains(File file) {
		for (@SuppressWarnings("StandardVariableNames") File f : mFilesData)
			if (f.getName().equals(file.getName())) return true;
		return false;
	}

	public void setFolderClickListener(ClickListener listener) {
		mClickListener = listener;
	}

	public void setItemLongClickListener(LongClickListener listener) {
		mLongClickListener = listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setSortingMethod(Comparator<File> fileComparator) {
		if (Objects.equals(fileComparator, mFileComparator)) return;
		mFileComparator = fileComparator;

		if (mFileComparator == null) {
			mFilesData = toArrayList(mCurrentDir.listFiles());
			notifyDataSetChanged();
		}
		else if (getItemCount() > 1) {
			mFilesData = sortItems(mFilesData);
			notifyDataSetChanged();
		}
	}

	private Context getContext() {
		return mInflater.getContext();
	}

	private void setColorFilter(@NonNull ImageView imageView, int colorRes) {
		imageView.setColorFilter(
				getContext().getResources().getColor(colorRes), android.graphics.PorterDuff.Mode.SRC_IN);
	}

	interface ClickListener {
		void execute(int position);
	}

	interface LongClickListener {
		boolean execute(int position);
	}

	protected class BaseViewHolder extends RecyclerView.ViewHolder {
		protected final TextView  mTextView;
		protected final ImageView mImageView;

		public BaseViewHolder(@NonNull View itemView) {
			super(itemView);
			// Define click listener for the ViewHolder's View.

			itemView.setOnLongClickListener(v -> {
				if (mLongClickListener == null) return false;
				return mLongClickListener.execute(getAdapterPosition());
			});

			mTextView  = itemView.findViewById(R.id.text_view);
			mImageView = itemView.findViewById(R.id.image);
		}

		public void updateFileName(String s) {
			mTextView.setText(s);
		}
	}

	protected class FoldersHolder extends BaseViewHolder {
		public FoldersHolder(@NonNull View itemView) {
			super(itemView);
			itemView.setOnClickListener(v -> {
				if (mClickListener == null) return;
				mClickListener.execute(getAdapterPosition());
			});
			mImageView.setImageResource(R.drawable.folder);
			setColorFilter(mImageView, R.color.yellow);
		}
	}

	protected class FilesHolder extends BaseViewHolder {
		public FilesHolder(@NonNull View itemView) {
			super(itemView);
			mImageView.setImageResource(R.drawable.file);
			setColorFilter(mImageView, R.color.gray);
		}
	}
}
