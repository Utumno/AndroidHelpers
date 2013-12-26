package gr.uoa.di.android.helpers.files;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public final class ExternalStorage {

	private ExternalStorage() {}

	/**
	 * Creates the File instance where the data is persisted in *external
	 * storage* along with user files (debugging purposes *but not in
	 * production*). The files will NOT be removed on application uninstall - so
	 * use this while installing and uninstalling to keep the logs around. If
	 * the external folder is not available or if the directory can not be
	 * created an exception is thrown forcing the caller to deal with it.
	 * External storage must be used sparingly
	 *
	 * @param rootDir
	 *            the directory the file will be created relative to the
	 *            directory returned depending on the value of
	 *            {@code dirInPublicStorage} - do not give an absolute path
	 * @param filename
	 * @param dirInPublicStorage
	 *            specifies the root relative to which the {@code rootDir}/
	 *            {@code filename} path is relative. If this is one of
	 *            DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES,
	 *            DIRECTORY_ALARMS, DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES,
	 *            DIRECTORY_MOVIES, DIRECTORY_DOWNLOADS, or DIRECTORY_DCIM then
	 *            the relevant directory is returned (as per the docs of
	 *            {@code getExternalStoragePublicDirectory()}). If it is null
	 *            then the root of external storage is returned as per
	 *            {@code getExternalStorageDirectory()}. See the section
	 *            "Accessing files on external storage" in {@link http
	 *            ://developer.android.com/guide/topics/data/data-storage.html}
	 * @return the File instance corresponding to the rootPath/filename relative
	 *         to External Files dir for the application storage or null on
	 *         failure
	 * @throws IOException
	 *             if the external storage is unavailable or the dir can't be
	 *             created
	 */
	public static File fileExternalPublicStorage(final String rootDir,
			final String filename, final String dirInPublicStorage)
			throws IOException {
		// create a File object for the parent directory
		if (isExternalStoragePresent()) {
			File logdir;
			if (dirInPublicStorage != null) {
				logdir = new File(Environment
					.getExternalStoragePublicDirectory(dirInPublicStorage)
					.getAbsolutePath()
					+ File.separator + rootDir);
			} else {
				logdir = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + File.separator + rootDir);
			}
			// have the object build the directory structure, if needed.
			if (FileUtils.createDir(logdir)) {
				// create a File *instance* for the output file - can't fail
				return new File(logdir, filename);
			}
			throw new IOException("Can not create folder "
				+ logdir.getAbsolutePath());
		}
		throw new IOException("External storage not present or not writable");
	}

	/**
	 * Wrapper around {@link Environment#getExternalStorageState()}.
	 *
	 * @return true if External Storage is both available and writable
	 * @see <a
	 *      href=http://developer.android.com/guide/topics/data/data-storage.html
	 *      #filesExternal>Using the External Storage</a>
	 */
	private static boolean isExternalStoragePresent() {
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}
		return (externalStorageAvailable) && (externalStorageWriteable);
	}

	// @formatter:off
	/**
	 * Creates the File instance where the data is persisted in *external
	 * storage* private to the application (meaning the files will be removed on
	 * application uninstall). If the external folder is not available or if the
	 * directory can not be created an exception is thrown forcing the caller to
	 * deal with it. External storage must be used sparingly
	 *
	 * @param ctx
	 *            a context belonging to the application
	 * @param rootDir
	 *            the root directory of the file to be created relative to
	 *            getExternalFilesDir(null) - do not give an absolute path
	 * @param filename
	 * @return the File instance corresponding to the rootPath/filename relative
	 *         to External Files dir for the application storage or null on
	 *         failure
	 * @throws IOException
	 *             if the external storage is unavailable or the dir can't be
	 *             created
	 */
	/*public static File fileExternalApplicationStorage(final Context ctx,
			final String rootDir, final String filename) throws IOException {
		// create a File object for the parent directory
		if (isExternalStoragePresent()) {
			File logdir = new File(ctx.getExternalFilesDir(null)
				.getAbsolutePath() + File.separator + rootDir);
			// have the object build the directory structure, if needed.
			if (FileUtils.createDirExternal(logdir)) {
				// Log.w(TAG, "logdir : " + logdir.getAbsolutePath());
				// create a *File object* for the output file - can't fail
				return new File(logdir, filename);
			}
			throw new IOException("Can not create folder "
				+ logdir.getAbsolutePath());
		}
		throw new IOException("External storage not present or not writable");
	}*/

	/**
	 * Copies the inputFilename (an absolute path) to destinationFilename (a
	 * filename) in *external public storage* in the directory destinationDir
	 * relative to the root of external public storage
	 *
	 * @param inputFilename
	 *            must be an *absolute path* to a file in internal storage
	 * @param destinationDir
	 * @param destinationFilename
	 * @throws FileNotFoundException
	 *             if either the destination or the inputFileName strings do not
	 *             point to an existent path
	 * @throws IOException
	 *             if the copying operation fails
	 */
	/*public static void copyFileFromInternalToExternalStorage(
			String inputFilename, String destinationDir,
			String destinationFilename) throws IOException,
			FileNotFoundException {
		FileInputStream fis = new FileInputStream(inputFilename);
		try {
			File dest = fileExternalPublicStorage(destinationDir,
				destinationFilename, null);
			OutputStream output = new FileOutputStream(dest);
			try {
				// transfer bytes from the inputfile to the outputfile
				byte[] buffer = new byte[1024];
				int length;
				while ((length = fis.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
			} finally {
				FileUtils.close(output);
			}
		} finally {
			FileUtils.close(fis);
		}
	}*/
}
