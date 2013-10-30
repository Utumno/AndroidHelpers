package gr.uoa.di.android.helpers;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;

public final class Zip {

	private Zip() {}

	public static ZipFile zipFolderLocked(final String dirToZip,
			final String destination, Object lock) throws CompressException {
		// http://stackoverflow.com/questions/13395218/android-compressing-folder
		// TODO parameters etc
		try {
			ZipFile zipfile = new ZipFile(destination);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			synchronized (lock) {
				zipfile.addFolder(dirToZip, parameters);
			}
			return zipfile;
		} catch (ZipException e) {
			throw new CompressException("Failed to compress file ", e);
		}
	}

	public static ZipFile zipFolder(final String dirToZip,
			final String destination) throws CompressException {
		// http://stackoverflow.com/questions/13395218/android-compressing-folder
		// TODO parameters etc
		try {
			ZipFile zipfile = new ZipFile(destination);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			zipfile.addFolder(dirToZip, parameters);
			return zipfile;
		} catch (ZipException e) {
			throw new CompressException("Failed to compress file ", e);
		}
	}

	/**
	 * Unzip the fileToUnzip to the destPath
	 *
	 * @param fileToUnzip
	 *            a zip file
	 * @param destPath
	 *            must be an absolute path to a directory
	 * @return a file instance representing a directory where the files were
	 *         unzipped
	 * @throws CompressException
	 *             if fileToUnzip or destPath are incalid
	 */
	public static File unZipFolder(final File fileToUnzip, final String destPath)
			throws CompressException {
		try {
			// http://stackoverflow.com/questions/14353956/
			ZipFile zipfile = new ZipFile(fileToUnzip);
			zipfile.extractAll(destPath);
			return new File(destPath);
		} catch (ZipException e) {
			throw new CompressException("Failed to unzip file ", e);
		}
	}

	public static final class CompressException extends Exception {

		private static final long serialVersionUID = -2914380925512148442L;

		public CompressException(String msg, Throwable t) {
			super(msg, t);
		}
	}
}
