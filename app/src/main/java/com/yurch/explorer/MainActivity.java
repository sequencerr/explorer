package com.yurch.explorer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
	private static final String MAIN_PERMISSION               = Manifest.permission.READ_EXTERNAL_STORAGE;
	private static final String STARTING_DIR_INSTANCE_KEY     = "STARTING_DIR_SAVE_INSTANCE_KEY";
	private static final String SORTING_METHOD_INSTANCE_KEY   = "SORTING_METHOD_INSTANCE_KEY";
	private static final int    EXTERNAL_STORAGE_REQUEST_CODE = 0;
	private static final File   ROOT_DIR_PATHNAME             = Environment.getExternalStorageDirectory();

	private TextView mPathDisplayView, mTextCreateFolder, mTextCreateTextFile, mTextCreateFile;
	private FloatingActionButton mFabMain, mFabCreateFolder, mFabCreateTextFile, mFabCreateFile;
	private RecyclerView     mRecycler;
	private MainFilesAdapter mFilesListAdapter;
	private ImageButton      mBtnGoBack;
	private boolean          isFabsEnabled = true;
	private SortingMethod    mCurrentSortingMethod;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mFabMain            = findViewById(R.id.fab_main);
		mFabCreateFolder    = findViewById(R.id.fab_create_folder);
		mTextCreateFolder   = findViewById(R.id.text_create_folder);
		mFabCreateTextFile  = findViewById(R.id.fab_create_text_file);
		mTextCreateTextFile = findViewById(R.id.text_create_text_file);
		mFabCreateFile      = findViewById(R.id.fab_create_file);
		mTextCreateFile     = findViewById(R.id.text_create_file);
		mPathDisplayView    = findViewById(R.id.dir_path_display_text_view);
		mRecycler           = findViewById(R.id.recycler);
		mBtnGoBack          = findViewById(R.id.btn_go_back);

		toggleFabs();
		mFabMain.setOnClickListener(v -> toggleFabs());
		mFabCreateFolder.setOnClickListener(new CreateFolderFABListener());
		mFabCreateFile.setOnClickListener(new CreateFileFABListener());
		mFabCreateTextFile.setOnClickListener(new CreateTextFileFABListener());

		mPathDisplayView.setMovementMethod(new ScrollingMovementMethod());

		mRecycler.setLayoutManager(new MyLinearLayoutManager(this));
		mRecycler.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
			@Override
			public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
				return hideFabsOnBackgroundTouch(e);
			}

			@Override
			public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
			}

			@Override
			public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

			}
		});

		// onCreate() -> if granted — enable, if not -> checkAndRequestPermission() -> onRequestPermissionsResult() -> if granted — enable, if not -> checkAndRequestPermission() -> ...
		if (checkIfPermissionGranted(MAIN_PERMISSION)) {
			if (savedInstanceState != null) {
				createAndSetAdapterIntoRecycler(savedInstanceState.getString(STARTING_DIR_INSTANCE_KEY));
				mCurrentSortingMethod = (SortingMethod) savedInstanceState.getSerializable(SORTING_METHOD_INSTANCE_KEY);
				if (mCurrentSortingMethod != null) changeSortingMethod(mCurrentSortingMethod);
			}
			else {
				createAndSetAdapterIntoRecycler(null);
			}
		}
		else {
			checkAndRequestPermission(MAIN_PERMISSION);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (checkIfPermissionGranted(MAIN_PERMISSION)) {
			outState.putString(STARTING_DIR_INSTANCE_KEY, mFilesListAdapter.getCurrentDirPath());
			// About enum serialization performance
			// https://stackoverflow.com/questions/3293020/#comment78498783_3293020
			outState.putSerializable(SORTING_METHOD_INSTANCE_KEY, mCurrentSortingMethod);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		int id = item.getItemId();
		// Resource IDs will be non-final by default in Android Gradle Plugin version 8.0, avoid using them in switch case statements
		if (id == R.id.action_disable_sorting) {
			changeSortingMethod(SortingMethod.NONE);
			return true;
		}
		if (id == R.id.action_sort_by_name_desc) {
			changeSortingMethod(SortingMethod.BY_NAME_DESC);
			return true;
		}
		if (id == R.id.action_sort_by_asc) {
			changeSortingMethod(SortingMethod.BY_NAME_ASC);
			return true;
		}
		if (id == R.id.action_sort_by_modification) {
			changeSortingMethod(SortingMethod.BY_MODIFICATION_TIME);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void changeSortingMethod(SortingMethod method) {
		mCurrentSortingMethod = method;
		switch (method) {
		case NONE:
			mFilesListAdapter.setSortingMethod(null);
			return;
		case BY_NAME_DESC:
			mFilesListAdapter.setSortingMethod((f1, f2) -> {
				if (f1.isFile() && !f2.isFile()) return 1;
				if (!f1.isFile() && f2.isFile()) return -1;
				return f1.getName().compareTo(f2.getName());
			});
			return;
		case BY_NAME_ASC:
			mFilesListAdapter.setSortingMethod((f1, f2) -> {
				if (f1.isFile() && !f2.isFile()) return -1;
				if (!f1.isFile() && f2.isFile()) return 1;
				return -f1.getName().compareTo(f2.getName());
			});
			return;
		case BY_MODIFICATION_TIME:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				mFilesListAdapter.setSortingMethod(Comparator.comparingLong(File::lastModified).reversed());
			}
			else {
				mFilesListAdapter.setSortingMethod((f1, f2) -> -Long.compare(f1.lastModified(), f2.lastModified()));
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		hideFabsOnBackgroundTouch(event);

		return super.onTouchEvent(event);
	}

	private boolean hideFabsOnBackgroundTouch(MotionEvent e) {
		if (e.getAction() != MotionEvent.ACTION_DOWN || !isFabsEnabled) return false;

		toggleFabs();
		// to prevent scrolling when hiding buttons change to "true"
		return false;
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode != EXTERNAL_STORAGE_REQUEST_CODE) return;

		for (int i = 0; i < permissions.length; i++) {
			String permission = permissions[i];

			// without this permission app won't have sense. if not granted, ask repeatedly
			if (!permission.equals(MAIN_PERMISSION)) continue;

			if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
				requestPermission(permission);
			}
			else {
				createAndSetAdapterIntoRecycler(null);
			}
		}
	}

	private boolean checkAndRequestPermission(String permission) {
		if (!checkIfPermissionGranted(permission)) return false;

		requestPermission(permission);

		return true;
	}

	private void requestPermission(String permission) {
		// ⬇ user has already granted permission to them during installation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
		requestPermissions(new String[]{ permission }, EXTERNAL_STORAGE_REQUEST_CODE);
	}

	private boolean checkIfPermissionGranted(String permission) {
		return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
	}

	private void createAndSetAdapterIntoRecycler(@Nullable String startingDirPath) {
		File startingDir = startingDirPath == null
		                   ? ROOT_DIR_PATHNAME
		                   : new File(startingDirPath);

		if (!startingDir.isDirectory()) {
			throw new IllegalArgumentException("startingDirPath should follow to directory");
		}

		mFilesListAdapter = new MainFilesAdapter(this, startingDir);
		mFilesListAdapter.setFolderClickListener(this::adapterOnFolderClick);
		mFilesListAdapter.setItemLongClickListener(this::adapterOnItemLongClick);

		mRecycler.setAdapter(mFilesListAdapter);

		mPathDisplayView.setText(startingDir.getAbsolutePath());

		mBtnGoBack.setOnClickListener(v -> {
			File currentDir = mFilesListAdapter.getCurrentDir();
			if (currentDir.getAbsolutePath().equals(ROOT_DIR_PATHNAME.getAbsolutePath())) return;

			File parent = currentDir.getParentFile();
			// won't be null (if ROOT_DIR_PATHNAME set correctly), but anyway...
			if (parent == null) return;

			mFilesListAdapter.updateFilesData(parent);
		});
	}

	protected void adapterOnFolderClick(int position) {
		// when 2 folders clicked with too small delay between clicks
		// have a problem because it opens another directory in opened directory with position of second click position
		if (position < 0 || position > mFilesListAdapter.getItemCount()) return;

		mFilesListAdapter.updateFilesData(mFilesListAdapter.getItem(position));
	}

	protected boolean adapterOnItemLongClick(int position) {
		File file = mFilesListAdapter.getItem(position);

		if (!file.canWrite()) {
			Toast.makeText(this, "Cannot delete this.", Toast.LENGTH_SHORT).show();
			return true;
		}

		// Variable used in lambda expression should be final or effectively final
		AtomicBoolean consumed = new AtomicBoolean(false);
		String        name     = file.getName();
		name = name.length() > 16
		       ? name.substring(0, 16) + "..."
		       : name;

		new AlertDialog.Builder(this)
				.setCancelable(true)
				.setMessage("Do you want do delete \"" + name + "\"?")
				.setPositiveButton("YES", (dialog, id) -> {
					boolean deletedSuccessfully = file.delete();
					if (!deletedSuccessfully) {
						Toast.makeText(this, "Not deleted. Error occurred.", Toast.LENGTH_SHORT).show();
						return;
					}

					mFilesListAdapter.removeFile(file);
					consumed.set(true);
				})
				.setNegativeButton("Cancel", (dialog, id) ->
						dialog.cancel()
				)
				.create()
				.show();

		return consumed.get();
	}

	private void toggleFabs() {
		if (isFabsEnabled) {
			mFabMain.show();
			mFabCreateFolder.hide();
			mTextCreateFolder.setVisibility(View.GONE);
			mFabCreateTextFile.hide();
			mTextCreateTextFile.setVisibility(View.GONE);
			mFabCreateFile.hide();
			mTextCreateFile.setVisibility(View.GONE);
		}
		else {
			mFabMain.hide();
			mFabCreateFolder.show();
			mTextCreateFolder.setVisibility(View.VISIBLE);
			mFabCreateTextFile.show();
			mTextCreateTextFile.setVisibility(View.VISIBLE);
			mFabCreateFile.show();
			mTextCreateFile.setVisibility(View.VISIBLE);
		}
		isFabsEnabled = !isFabsEnabled;
	}

	enum SortingMethod {NONE, BY_NAME_DESC, BY_NAME_ASC, BY_MODIFICATION_TIME}

	public enum FileType {DIRECTORY, FILE}

	class MainFilesAdapter extends FilesAdapter {
		protected MainFilesAdapter(Context context, File filesData) {
			super(context, filesData);
		}

		@Override
		public void updateFilesData(File dirToOpen) {
			super.updateFilesData(dirToOpen);
			mPathDisplayView.setText(mFilesListAdapter.getCurrentDirPath());
			mRecycler.scrollToPosition(0);
		}
	}

	private abstract class CreateSomeFileFABClickListener implements View.OnClickListener {
		private static final String   DEFAULT_DIRECTORY_NAME       = "New folder";
		private static final String   DEFAULT_FILE_NAME            = "New file";
		private static final int      MAX_NAME_LENGTH              = 255; // https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations
		protected final      Toast    mToastFilenameLengthExceeded =
				Toast.makeText(MainActivity.this, "Max file name length is: " + MAX_NAME_LENGTH, Toast.LENGTH_SHORT);
		private final        Toast    mToastFileIsCreatedAlready   =
				Toast.makeText(MainActivity.this, "Already created.", Toast.LENGTH_SHORT);
		private final        Toast    mToastCannotCreateFiles      =
				Toast.makeText(MainActivity.this, "Cannot create files/folders there.", Toast.LENGTH_SHORT);
		private final        Toast    mToastErrorWhenCreating      =
				Toast.makeText(MainActivity.this, "Not created. Error occurred.", Toast.LENGTH_SHORT);
		private final        Toast    mToastInvalidFileName        =
				Toast.makeText(MainActivity.this, "File cannot contain \"/\" (slash) in it's name", Toast.LENGTH_SHORT);
		private final        FileType mFileOrFolder;
		private final        Pattern  INVALID_FIlE_NAME_PATTERN    = Pattern.compile("/");
		private              View     layoutDialogPrompt;
		private              String   mProvidedFilename;

		protected CreateSomeFileFABClickListener(FileType fileOrFolder) {
			mFileOrFolder = fileOrFolder;
		}

		@SuppressLint("InflateParams")
		@Override
		public void onClick(View v) {
			toggleFabs();

			if (!checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) return;

			String currentDirPath = mFilesListAdapter.getCurrentDirPath();

			if (!new File(currentDirPath).canWrite()) {
				mToastCannotCreateFiles.show();
				return;
			}

			changeDialogLayout(LayoutInflater.from(MainActivity.this).inflate(R.layout.prompt, null));
			EditText editTextDialogPrompt = layoutDialogPrompt.findViewById(R.id.filenameDialogInput);
			editTextDialogPrompt.setHint(DEFAULT_DIRECTORY_NAME);

			new AlertDialog.Builder(MainActivity.this)
					.setView(layoutDialogPrompt)
					.setCancelable(true)
					.setMessage("Enter name:")
					.setNegativeButton("Cancel", (dialog, id) ->
							dialog.cancel()
					)
					.setPositiveButton("OK", (dialog, id) -> {
						changeFilenameBeforeCreate(Objects.requireNonNull(editTextDialogPrompt.getText()).toString());

						if (INVALID_FIlE_NAME_PATTERN.matcher(mProvidedFilename).find()) {
							mToastInvalidFileName.show();
							return;
						}
						if (mProvidedFilename.length() > MAX_NAME_LENGTH) {
							mToastFilenameLengthExceeded.show();
							return;
						}

						String filename = mProvidedFilename.isEmpty()
						                  ? mFileOrFolder == FileType.DIRECTORY
						                    ? DEFAULT_DIRECTORY_NAME
						                    : DEFAULT_FILE_NAME
						                  : mProvidedFilename;

						File mNewAbstractFile = new File(currentDirPath, filename);
						if (mFilesListAdapter.contains(mNewAbstractFile)) {
							mToastFileIsCreatedAlready.show();
							return;
						}

						boolean createdSuccessfully = createSomeActualFile(mNewAbstractFile);

						if (!createdSuccessfully) {
							mToastErrorWhenCreating.show();
							return;
						}

						int position = mFilesListAdapter.addFile(mNewAbstractFile);
						mRecycler.scrollToPosition(position);
					})
					.create()
					.show();
		}

		protected void changeDialogLayout(@NonNull View view) {
			layoutDialogPrompt = view;
		}

		protected void changeFilenameBeforeCreate(@NonNull String newFilename) {
			mProvidedFilename = newFilename;
		}

		protected abstract boolean createSomeActualFile(File newAbstractFile);
	}

	private class CreateFolderFABListener extends CreateSomeFileFABClickListener {
		protected CreateFolderFABListener() {
			super(FileType.DIRECTORY);
		}

		@Override
		protected boolean createSomeActualFile(File newAbstractFile) {
			// create directory
			return newAbstractFile.mkdir();
		}
	}

	private class CreateFileFABListener extends CreateSomeFileFABClickListener {
		protected CreateFileFABListener() {
			super(FileType.FILE);
		}

		@Override
		protected boolean createSomeActualFile(File newAbstractFile) {
			boolean createdSuccessfully;
			try {
				createdSuccessfully = newAbstractFile.createNewFile();
			} catch (IOException e) {
				Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				return false;
			}

			return createdSuccessfully;
		}
	}

	private class CreateTextFileFABListener extends CreateFileFABListener {
		@SuppressLint("InflateParams")
		@Override
		protected void changeDialogLayout(@NonNull View view) {
			super.changeDialogLayout(LayoutInflater.from(MainActivity.this).inflate(R.layout.prompt_textfile, null));
		}

		@Override
		protected void changeFilenameBeforeCreate(@NonNull String newFilename) {
			super.changeFilenameBeforeCreate(newFilename + ".txt");
		}
	}
}