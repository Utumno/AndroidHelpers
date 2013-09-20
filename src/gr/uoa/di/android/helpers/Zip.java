package gr.uoa.di.android.helpers;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public final class Zip {

	private Zip() {}

	// FIXME lock !
	public static ZipFile zipFolder(final String dirToZip,
			final String destination) throws CompressException {
		// http://stackoverflow.com/questions/13395218/android-compressing-folder
		// TODO parameters etc
		try {
			ZipFile zipfile = new ZipFile(destination);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			synchronized (FileIO.FILE_STORE_LOCK) {
				zipfile.addFolder(dirToZip, parameters);
			}
			return zipfile;
		} catch (ZipException e) {
			throw new CompressException("Failed to compress file ", e);
		}
	}

	public static final class CompressException extends Exception {

		private static final long serialVersionUID = -2914380925512148442L;

		public CompressException(String msg, Throwable t) {
			super(msg, t);
		}
	}
}
