package gr.uoa.di.android.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Wake the radio up. You must provide a latch on which the wake methods await. */
public final class WifiWaker {

	private final static String TAG = WifiWaker.class.getSimpleName();
	private BroadcastReceiver mConnectionReceiver;
	private final CountDownLatch latch_;

	/**
	 * Provide a latch with count 1 to have the methods of this class wait upon.
	 *
	 * @param latch
	 *            the latch on which the methods wait - must have count one
	 */
	public WifiWaker(CountDownLatch latch) {
		if (latch.getCount() != 1)
			throw new IllegalArgumentException("The latch must have count 1");
		this.latch_ = latch;
	}

	/**
	 * Tries to connect to wifi only if wifi is initially enabled. Will wait on
	 * the class latch (with count one) till a connection is established or the
	 * latch times out - in which case it returns false. DO NOT CALL THIS ON THE
	 * MAIN THREAD. It registers a receiver which will receive the connection
	 * event and will count the latch down, so the main thread must no block on
	 * the latch for onReceive () to run. Anyway the main thread must not block.
	 *
	 * @param ctx
	 *            the Context used to get the services and register the receiver
	 * @param latchTimeout
	 *            the timeout of the latch in milliseconds
	 * @return true if wifi connected, false otherwise
	 */
	public boolean wakeWifiUpIfEnabled(Context ctx, long latchTimeout) {
		WifiManager wm = (WifiManager) ctx
			.getSystemService(Context.WIFI_SERVICE);
		final int wifiState = wm.getWifiState();
		if (!wm.isWifiEnabled() || wifiState == WifiManager.WIFI_STATE_DISABLED
			|| wifiState == WifiManager.WIFI_STATE_DISABLING) {
			// Make sure the Wi-Fi is enabled, required for some devices when
			// enable WiFi does not occur immediately
			// ******************* do not enable if not enabled ****************
			// ******************* d("!_wifiManager.isWifiEnabled()");
			// ******************* wm.setWifiEnabled(true);
			// ******************* return false instead ****************
			return false;
		}
		if (!Net.isWifiConnectedOrConnecting(ctx)) {
			d("Wifi is NOT Connected Or Connecting - "
				+ "wake it up and wait till is up");
			// Do not wait for the OS to initiate a reconnect to a Wi-Fi router
			wm.pingSupplicant();
			// if (wifiState == WifiManager.WIFI_STATE_ENABLED) { // DONT !!!!!!
			// try {
			// // Brute force methods required for some devices
			// _wifiManager.setWifiEnabled(false);
			// _wifiManager.setWifiEnabled(true);
			// } catch (SecurityException e) {
			// // Catching exception which should not occur on most
			// // devices. OS bug details at :
			// // https://code.google.com/p/android/issues/detail?id=22036
			// }
			// }
			// ////////////////// commented those out :
			// _wifiManager.disconnect();
			// _wifiManager.startScan();
			wm.reassociate();
			wm.reconnect();
			try {
				mConnectionReceiver = new WifiConnectionMonitor(latch_);
				startMonitoringConnection(ctx);
				w("I wait");
				// If the current count is zero then await returns immediately
				// with the value true. So no worries if the receiver
				// immediately counts down the latch
				final boolean await = latch_.await(latchTimeout,
					TimeUnit.MILLISECONDS);
				w("Woke up");
				// await should be false if latch timed out
				return await;
			} catch (InterruptedException e) {
				w("Interrupted while waiting for connection", e);
				return false;
			} finally {
				stopMonitoringConnection(ctx);
			}
		}
		return true;
	}

	private final static class WifiConnectionMonitor extends BroadcastReceiver {

		private final CountDownLatch latch_;
		private final static String TAG = BroadcastReceiver.class
			.getSimpleName();

		WifiConnectionMonitor(CountDownLatch latch) {
			super();
			this.latch_ = latch;
		}

		// http://stackoverflow.com/a/4504110/281545
		// http://stackoverflow.com/questions/5276032/
		@Override
		public void onReceive(Context context, Intent in) {
			String action = in.getAction();
			if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				NetworkInfo networkInfo = in
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				d("NETWORK_STATE_CHANGED_ACTION :" + networkInfo);
				if (networkInfo.isConnected()) {
					d("Wifi is connected!");
					latch_.countDown(); // COUNT DOWN HERE
				}
			} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				@SuppressWarnings("deprecation")
				final String extraNetInfo = ConnectivityManager.EXTRA_NETWORK_INFO;
				NetworkInfo networkInfo = in.getParcelableExtra(extraNetInfo);
				// boolean noConnectivity = intent.getBooleanExtra(
				// ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
				/* && !networkInfo.isConnected() */) {
					d("CONNECTIVITY_ACTION - " + networkInfo);
				}
			} else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION
				.equals(action)) {
				d("SUPPLICANT_CONNECTION_CHANGE_ACTION - "
					+ "EXTRA_SUPPLICANT_CONNECTED :"
					+ in.getBooleanExtra(
						WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
			} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
				.equals(action)) {
				d("SUPPLICANT_STATE_CHANGED_ACTION - EXTRA_NEW_STATE: "
					+ in.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)
					+ " - EXTRA_SUPPLICANT_ERROR: "
					+ in.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1));
			} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
				d("WIFI_STATE_CHANGED_ACTION - EXTRA_WIFI_STATE: "
					+ wifiState(in, WifiManager.EXTRA_WIFI_STATE)
					+ " - EXTRA_PREVIOUS_WIFI_STATE: "
					+ wifiState(in, WifiManager.EXTRA_PREVIOUS_WIFI_STATE));
			}
		}

		/**
		 * Returns a string describing the state of the Wifi. Meant to be used
		 * with an intent received by a BroadcastReceiver whose action is
		 * WifiManager.WIFI_STATE_CHANGED_ACTION
		 *
		 * @param in
		 *            an intent received by a BR whose action is
		 *            WifiManager.WIFI_STATE_CHANGED_ACTION
		 * @param key
		 *            must be either WifiManager.EXTRA_WIFI_STATE OR
		 *            WifiManager.EXTRA_PREVIOUS_WIFI_STATE. NOTHING else
		 * @return a string for the state instead of an int
		 */
		private static String wifiState(Intent in, String key) {
			switch (in.getIntExtra(key, -1)) {
			case WifiManager.WIFI_STATE_DISABLED:
				return "WIFI_STATE_DISABLED";
			case WifiManager.WIFI_STATE_DISABLING:
				return "WIFI_STATE_DISABLING";
			case WifiManager.WIFI_STATE_ENABLED:
				return "WIFI_STATE_ENABLED";
			case WifiManager.WIFI_STATE_ENABLING:
				return "WIFI_STATE_ENABLING";
			case WifiManager.WIFI_STATE_UNKNOWN:
				return "WIFI_STATE_UNKNOWN";
			}
			throw new RuntimeException("Forgot Wifi State");
		}

		private static void d(String string) {
			Log.d(TAG, string);
		}
	}

	private synchronized void startMonitoringConnection(Context ctx) {
		IntentFilter aFilter = new IntentFilter(
			ConnectivityManager.CONNECTIVITY_ACTION);
		// aFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		aFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		// aFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		// aFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); // !!!!
		aFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		aFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		aFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		ctx.registerReceiver(mConnectionReceiver, aFilter);
	}

	private synchronized void stopMonitoringConnection(Context ctx) {
		ctx.unregisterReceiver(mConnectionReceiver);
	}

	// helpers
	private static void w(String string) {
		Log.w(TAG, string);
	}

	private static void w(String string, Throwable e) {
		Log.w(TAG, string, e);
	}

	private static void d(String string) {
		Log.d(TAG, string);
	}
}
