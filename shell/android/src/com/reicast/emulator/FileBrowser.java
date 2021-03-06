package com.reicast.emulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.comparator.CompositeFileComparator;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.util.FileUtils;

public class FileBrowser extends Fragment {

	Vibrator vib;
	Drawable orig_bg;
	Activity parentActivity;
	boolean ImgBrowse;
	private boolean games;
	OnItemSelectedListener mCallback;

	private SharedPreferences mPrefs;
	private File sdcard = Environment.getExternalStorageDirectory();
	private String home_directory = sdcard + "/dc";
	private String game_directory = sdcard + "/";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		home_directory = mPrefs.getString("home_directory", home_directory);
		game_directory = mPrefs.getString("game_directory", game_directory);

		Bundle b = getArguments();
		if (b != null) {
			ImgBrowse = b.getBoolean("ImgBrowse", true);
			if (games = b.getBoolean("games_entry", false)) {
				if (b.getString("path_entry") != null) {
					home_directory = b.getString("path_entry");
				}
			} else {
				if (b.getString("path_entry") != null) {
					game_directory = b.getString("path_entry");
				}
			}
		}

	}

	// Container Activity must implement this interface
	public interface OnItemSelectedListener {
		public void onGameSelected(Uri uri);

		public void onFolderSelected(Uri uri);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mCallback = (OnItemSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnItemSelectedListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_main, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// setContentView(R.layout.activity_main);
		parentActivity = getActivity();
		try {
			File file = new File(home_directory, "data/buttons.png");
			if (!file.exists()) {
				file.createNewFile();
				OutputStream fo = new FileOutputStream(file);
				InputStream png = parentActivity.getBaseContext().getAssets()
						.open("buttons.png");

				byte[] buffer = new byte[4096];
				int len = 0;
				while ((len = png.read(buffer)) != -1) {
					fo.write(buffer, 0, len);
				}
				fo.close();
				png.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		vib = (Vibrator) parentActivity
				.getSystemService(Context.VIBRATOR_SERVICE);

		/*
		 * OnTouchListener viblist=new OnTouchListener() {
		 * 
		 * public boolean onTouch(View v, MotionEvent event) { if
		 * (event.getActionMasked()==MotionEvent.ACTION_DOWN) vib.vibrate(50);
		 * return false; } };
		 * 
		 * findViewById(R.id.config).setOnTouchListener(viblist);
		 * findViewById(R.id.about).setOnTouchListener(viblist);
		 */

		File home = new File(home_directory);
		if (!home.exists() || !home.isDirectory()) {
			Toast.makeText(getActivity(), "Please configure a home directory",
					Toast.LENGTH_LONG).show();
		}

		if (!ImgBrowse) {
			navigate(sdcard);
		} else {
			generate(ExternalFiles(new File(game_directory)));
		}
	}

	class DirSort implements Comparator<File> {

		// Comparator interface requires defining compare method.
		public int compare(File filea, File fileb) {

			return ((filea.isFile() ? "a" : "b") + filea.getName().toLowerCase(
					Locale.getDefault()))
					.compareTo((fileb.isFile() ? "a" : "b")
							+ fileb.getName().toLowerCase(Locale.getDefault()));
		}
	}

	private List<File> ExternalFiles(File baseDirectory) {
		// allows the input of a base directory for storage selection
		final List<File> tFileList = new ArrayList<File>();
		Resources resources = getResources();
		// array of valid image file extensions
		String[] mediaTypes = resources.getStringArray(R.array.images);
		FilenameFilter[] filter = new FilenameFilter[mediaTypes.length];

		int i = 0;
		for (final String type : mediaTypes) {
			filter[i] = new FilenameFilter() {

				public boolean accept(File dir, String name) {
					if (dir.getName().startsWith(".") || name.startsWith(".")) {
						return false;
					} else {
						return StringUtils.endsWithIgnoreCase(name, "." + type);
					}
				}

			};
			i++;
		}

		FileUtils fileUtils = new FileUtils();
		File[] allMatchingFiles = fileUtils.listFilesAsArray(baseDirectory,
				filter, -1);
		for (File mediaFile : allMatchingFiles) {
			tFileList.add(mediaFile);
		}

		@SuppressWarnings("unchecked")
		CompositeFileComparator comparator = new CompositeFileComparator(
				SizeFileComparator.SIZE_REVERSE,
				LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		comparator.sort(tFileList);

		return tFileList;
	}

	private void createListHeader(String header_text, View view, boolean hasBios) {
		LinearLayout list_header = new LinearLayout(parentActivity);
		LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		int margin_top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		listParams.setMargins(0, margin_top, 0, 0);
		list_header.setLayoutParams(listParams);

		ImageView list_icon = new ImageView(parentActivity);
		LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
		imageParams.width = size;
		imageParams.height = size;
		int margin_left_right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
		int margin_top_bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
		imageParams.setMargins(margin_left_right, margin_top_bottom, margin_left_right, margin_top_bottom);
		imageParams.gravity=Gravity.CENTER_VERTICAL;
		list_icon.setLayoutParams(imageParams);
		list_icon.setScaleType(ScaleType.FIT_CENTER);
		list_icon.setImageResource(R.drawable.open_folder);
		list_header.addView(list_icon);

		TextView list_text = new TextView(parentActivity);
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		textParams.gravity=Gravity.CENTER_VERTICAL;
		textParams.weight=1;
		list_text.setLayoutParams(textParams);
		list_text.setTextAppearance(parentActivity,
				android.R.style.TextAppearance_Large);
		list_text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		list_text.setText(header_text);
		list_header.addView(list_text);

		if (hasBios) {
			final View childview = parentActivity.getLayoutInflater().inflate(
					R.layout.bios_list_item, null, false);

			((TextView) childview.findViewById(R.id.item_name))
					.setText(getString(R.string.boot_bios));

			childview.setTag(null);

			orig_bg = childview.getBackground();

			childview.findViewById(R.id.childview).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							File f = (File) view.getTag();
							vib.vibrate(50);
							mCallback.onGameSelected(f != null ? Uri
									.fromFile(f) : Uri.EMPTY);
							vib.vibrate(250);
						}
					});

			childview.findViewById(R.id.childview).setOnTouchListener(
					new OnTouchListener() {
						@SuppressWarnings("deprecation")
						public boolean onTouch(View view, MotionEvent arg1) {
							if (arg1.getActionMasked() == MotionEvent.ACTION_DOWN) {
								view.setBackgroundColor(0xFF4F3FFF);
							} else if (arg1.getActionMasked() == MotionEvent.ACTION_CANCEL
									|| arg1.getActionMasked() == MotionEvent.ACTION_UP) {
								view.setBackgroundDrawable(orig_bg);
							}

							return false;

						}
					});
			((ViewGroup) view).addView(childview);
		}

		((ViewGroup) view).addView(list_header);

	}

	void generate(List<File> list) {
		LinearLayout v = (LinearLayout) parentActivity
				.findViewById(R.id.game_list);
		v.removeAllViews();

		String heading = parentActivity.getString(R.string.games_listing);
		createListHeader(heading, v, true);

		for (int i = 0; i < list.size(); i++) {
			final View childview = parentActivity.getLayoutInflater().inflate(
					R.layout.app_list_item, null, false);

			((TextView) childview.findViewById(R.id.item_name)).setText(list
					.get(i).getName());

			((ImageView) childview.findViewById(R.id.item_icon))
					.setImageResource(list.get(i) == null ? R.drawable.config
							: list.get(i).isDirectory() ? R.drawable.open_folder
									: list.get(i).getName()
											.toLowerCase(Locale.getDefault())
											.endsWith(".gdi") ? R.drawable.gdi
											: list.get(i)
													.getName()
													.toLowerCase(
															Locale.getDefault())
													.endsWith(".cdi") ? R.drawable.cdi
													: list.get(i)
															.getName()
															.toLowerCase(
																	Locale.getDefault())
															.endsWith(".chd") ? R.drawable.chd
															: R.drawable.disk_unknown);

			childview.setTag(list.get(i));

			orig_bg = childview.getBackground();
			
			final File game = list.get(i);

			// vw.findViewById(R.id.childview).setBackgroundColor(0xFFFFFFFF);

			childview.findViewById(R.id.childview).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							vib.vibrate(50);
							mCallback.onGameSelected(game != null ? Uri
									.fromFile(game) : Uri.EMPTY);
							vib.vibrate(250);
						}
					});

			childview.findViewById(R.id.childview).setOnTouchListener(
					new OnTouchListener() {
						@SuppressWarnings("deprecation")
						public boolean onTouch(View view, MotionEvent arg1) {
							if (arg1.getActionMasked() == MotionEvent.ACTION_DOWN) {
								view.setBackgroundColor(0xFF4F3FFF);
							} else if (arg1.getActionMasked() == MotionEvent.ACTION_CANCEL
									|| arg1.getActionMasked() == MotionEvent.ACTION_UP) {
								view.setBackgroundDrawable(orig_bg);
							}

							return false;

						}
					});
			v.addView(childview);
		}
	}

	void navigate(final File root_sd) {
		LinearLayout v = (LinearLayout) parentActivity
				.findViewById(R.id.game_list);
		v.removeAllViews();

		ArrayList<File> list = new ArrayList<File>();

		final String heading = root_sd.getAbsolutePath();
		createListHeader(heading, v, false);

		File flist[] = root_sd.listFiles();

		File parent = root_sd.getParentFile();

		list.add(null);

		if (parent != null)
			list.add(parent);

		Arrays.sort(flist, new DirSort());

		for (int i = 0; i < flist.length; i++)
			list.add(flist[i]);

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != null && !list.get(i).isDirectory())
				continue;
			final View childview = parentActivity.getLayoutInflater().inflate(
					R.layout.app_list_item, null, false);

			if (list.get(i) == null) {
				((TextView) childview.findViewById(R.id.item_name))
						.setText(getString(R.string.folder_select));
			} else if (list.get(i) == parent)
				((TextView) childview.findViewById(R.id.item_name))
						.setText("..");
			else
				((TextView) childview.findViewById(R.id.item_name))
						.setText(list.get(i).getName());

			((ImageView) childview.findViewById(R.id.item_icon))
					.setImageResource(list.get(i) == null ? R.drawable.config
							: list.get(i).isDirectory() ? R.drawable.open_folder
									: R.drawable.disk_unknown);

			childview.setTag(list.get(i));
			final File item = list.get(i);

			orig_bg = childview.getBackground();

			// vw.findViewById(R.id.childview).setBackgroundColor(0xFFFFFFFF);

			childview.findViewById(R.id.childview).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							if (item != null && item.isDirectory()) {
								navigate(item);
								ScrollView sv = (ScrollView) parentActivity
										.findViewById(R.id.game_scroller);
								sv.scrollTo(0, 0);
								vib.vibrate(50);
							} else if (view.getTag() == null) {
								vib.vibrate(50);

								mCallback.onFolderSelected(Uri
										.fromFile(new File(heading)));
								vib.vibrate(250);

								if (games) {
									game_directory = heading;
									mPrefs.edit()
											.putString("game_directory",
													heading).commit();
								} else {
									home_directory = heading;
									mPrefs.edit()
											.putString("home_directory",
													heading).commit();
									File data_directory = new File(heading,
											"data");
									if (!data_directory.exists()
											|| !data_directory.isDirectory()) {
										data_directory.mkdirs();
									}
									JNIdc.config(heading);
								}
							}
						}
					});

			childview.findViewById(R.id.childview).setOnTouchListener(
					new OnTouchListener() {
						@SuppressWarnings("deprecation")
						public boolean onTouch(View view, MotionEvent arg1) {
							if (arg1.getActionMasked() == MotionEvent.ACTION_DOWN) {
								view.setBackgroundColor(0xFF4F3FFF);
							} else if (arg1.getActionMasked() == MotionEvent.ACTION_CANCEL
									|| arg1.getActionMasked() == MotionEvent.ACTION_UP) {
								view.setBackgroundDrawable(orig_bg);
							}

							return false;

						}
					});
			v.addView(childview);
		}
	}
}
