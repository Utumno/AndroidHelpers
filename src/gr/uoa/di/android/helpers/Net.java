package gr.uoa.di.android.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

/**
 * Needed manifest permissions :
 *
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *
 * Test functions :
 *
 * Utils.getMACAddress("wlan0"); Utils.getMACAddress("eth0");
 * Utils.getIPAddress(true); // IPv4 Utils.getIPAddress(false); // IPv6
 *
 * @see <a href= "http://stackoverflow.com/questions/6064510>How to get
 *      ip-address of the device</a >
 */
public final class Net {

	private Net() {}

	/**
	 * Returns MAC address of the given interface name. Use only in GINGERBREAD
	 * and above builds
	 *
	 * @param interfaceName
	 *            eth0, wlan0 or NULL=use first interface
	 * @return mac address or empty string
	 * @throws SocketException
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static String getMACAddress(String interfaceName)
			throws SocketException {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			throw new IllegalStateException("You can use getMACAddress() only "
				+ "after API " + Build.VERSION_CODES.GINGERBREAD);
		}
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface
				.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			if (interfaceName != null) {
				if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
			}
			byte[] mac = intf.getHardwareAddress();
			if (mac == null) return "";
			StringBuilder buf = new StringBuilder();
			for (int idx = 0; idx < mac.length; idx++)
				buf.append(String.format("%02X:", mac[idx]));
			if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
			return buf.toString();
		}
		return "";
		/*
		 * try { // this is so Linux hack return
		 * loadFileAsString("/sys/class/net/" +interfaceName +
		 * "/address").toUpperCase().trim(); } catch (IOException ex) { return
		 * null; }
		 */
	}

	/**
	 * Get IP address from first non-localhost interface
	 *
	 * @param useIPv4
	 *            true=return ipv4, false=return ipv6
	 * @return address or empty string
	 * @throws SocketException
	 */
	public static String getIPAddress(boolean useIPv4) throws SocketException {
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface
				.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
			for (InetAddress addr : addrs) {
				if (!addr.isLoopbackAddress()) {
					String sAddr = addr.getHostAddress();
					boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
					if (useIPv4) {
						if (isIPv4) return sAddr;
					} else {
						if (!isIPv4) {
							// drop ip6 port suffix
							int delim = sAddr.indexOf('%');
							return delim < 0 ? sAddr : sAddr
									.substring(0, delim);
						}
					}
				}
			}
		}
		return "";
	}

	public static boolean hasWifiConnection(Context ctx) {
		ConnectivityManager connec = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnected();
	}

	public static String getCurrentSsid(Context ctx) {
		String ssid = null;
		if (hasWifiConnection(ctx)) {
			WifiManager wm = (WifiManager) ctx
					.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wm.getConnectionInfo();
			if (connectionInfo != null) {
				ssid = connectionInfo.getSSID();
				if (ssid != null && "".equals(ssid.trim())) ssid = null;
			}
		}
		return ssid;
	}

	// =========================================================================
	// helpers
	// =========================================================================
	/**
	 * Load UTF8withBOM or any ansi text file. It drops the BOM from UTF8 files
	 * if present
	 *
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static String loadFileAsString(String filename, String charsetName)
			throws IOException {
		final int BUFLEN = 1024;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				filename), BUFLEN);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			byte[] bytes = new byte[BUFLEN];
			boolean isUTF8 = false;
			for (int read, count = 0; (read = is.read(bytes)) != -1;) {
				if (count == 0 && bytes[0] == (byte) 0xEF
					&& bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
					isUTF8 = true;
					baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
				} else {
					baos.write(bytes, 0, read);
				}
				count += read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8")
					: new String(baos.toByteArray(), charsetName);
		} finally {
			try {
				is.close();
			} catch (IOException ex) {}
		}
	}

	/**
	 * Convert byte array to hex string
	 *
	 * @param bytes
	 * @return
	 */
	private static String bytesToHex(byte[] bytes) {
		StringBuilder sbuf = new StringBuilder();
		for (int idx = 0; idx < bytes.length; idx++) {
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10) sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase(Locale.US));
		}
		return sbuf.toString();
	}

	/**
	 * Get utf8 byte array.
	 *
	 * @param str
	 *            should be "UTF-8" encoded
	 * @return array or null if UnsupportedEncodingException was thrown
	 */
	private static byte[] getUTF8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException ex) {
			return null;
		}
	}
}
