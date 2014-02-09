package gr.uoa.di.android.helpers;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around SharedPreferences. See <a
 * href="http://stackoverflow.com/questions/19610569/">here</a> for a discussion
 * of some points.
 */
public final class AccessPreferences {

	private static final List<Class<?>> CLASSES = new ArrayList<Class<?>>();
	private static SharedPreferences prefs; // cache
	static {
		CLASSES.add(String.class);
		CLASSES.add(Boolean.class);
		CLASSES.add(Integer.class);
		CLASSES.add(Long.class);
		CLASSES.add(Float.class);
		CLASSES.add(Set.class);
	}

	private AccessPreferences() {}

	private static SharedPreferences getPrefs(Context ctx) {
		// synchronized is really needed or volatile is all I need (visibility)
		// the same instance of SharedPreferences will be returned AFAIC
		SharedPreferences result = prefs;
		if (result == null)
			synchronized (AccessPreferences.class) {
				result = prefs;
				if (result == null) {
					result = prefs = PreferenceManager
						.getDefaultSharedPreferences(ctx);
				}
			}
		return result;
	}

	/**
	 * Wrapper around {@link android.content.SharedPreferences.Editor}
	 * {@code put()} methods. Null keys are not permitted. Attempts to insert a
	 * null key will throw NullPointerException. Will call
	 * {@link android.content.SharedPreferences.Editor#apply()} in Gingerbread
	 * and above instead of commit. If you want to check the return value call
	 * {@link #commit(Context, String, Object)}. When you call this method from
	 * different threads the order of the operations is unspecified - you have
	 * to synchronize externally if the order concerns you (especially for the
	 * same key). If you want to put a long you must explicitly declare it
	 * otherwise Java will interpret it as an Integer resulting in a
	 * {@link ClassCastException} when you try to retrieve it (on get()
	 * invocation). So :
	 *
	 * <pre>
	 * put(ctx, LONG_KEY, 0); // you just persisted an Integer
	 * get(ctx, LONG_KEY, 0L); // CCE here
	 * put(ctx, LONG_KEY, 0L); // Correct, always specify you want a Long
	 * get(ctx, LONG_KEY, 0L); // OK
	 * </pre>
	 *
	 * You will get an {@link IllegalArgumentException} if the value is not an
	 * instance of String, Boolean, Integer, Long, Float or Set<String> (see
	 * below). This includes specifying a Double mistakenly thinking you
	 * specified a Float. So :
	 *
	 * <pre>
	 * put(ctx, FLOAT_KEY, 0.0); // IllegalArgumentException, 0.0 it's a Double
	 * put(ctx, FLOAT_KEY, 0.0F); // Correct, always specify you want a Float
	 * </pre>
	 *
	 * You will also get an IllegalArgumentException if you are trying to add a
	 * Set<String> before API 11 (HONEYCOMB). You **can** persist a {@link Set}
	 * that does not contain Strings using this method, but you are recommended
	 * not to do so. It is untested and the Android API expects a Set<String>.
	 * You can actually do so in the framework also but you will have raw and
	 * unchecked warnings. Here you get no warnings - you've been warned. TODO :
	 * clarify/test this behavior
	 *
	 * Finally, adding null values is supported - but keep in mind that:
	 * <ol>
	 * <li>you will get a NullPointerException if you put a null Boolean, Long,
	 * Float or Integer and you then get() it and assign it to a primitive
	 * (boolean, long, float or int). This is *not* how the prefs framework
	 * works - it will immediately throw NullPointerException (which is better).
	 * TODO : simulate this behavior</li>
	 *
	 * <li>you can put a null String or Set - but you will not get() null back
	 * unless you specify a null default. For non null default you will get this
	 * default back. This is in tune with the prefs framework</li>
	 * </ol>
	 *
	 * @param ctx
	 *            the context the Shared preferences belong to
	 * @param key
	 *            the preference's key, must not be {@code null}
	 * @param value
	 *            an instance of String, Boolean, Integer, Long, Float or
	 *            Set<String> (for API >= HONEYCOMB)
	 * @throws IllegalArgumentException
	 *             if the value is not an instance of String, Boolean, Integer,
	 *             Long, Float or Set<String> (including the case when you
	 *             specify a double thinking you specified a float, see above)
	 *             OR if you try to add a Set<String> _before_ HONEYCOMB API
	 * @throws NullPointerException
	 *             if key is {@code null}
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static <T> void put(final Context ctx, final String key,
			final T value) {
		final Editor ed = _put(ctx, key, value);
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			ed.apply();
		else ed.commit();
	}

	/**
	 * As {@link #put(Context, String, Object)} but will call
	 * {@link android.content.SharedPreferences.Editor#commit()} in all API
	 * versions. See {@link #put(Context, String, Object)} for detailed usage
	 * notes.
	 *
	 * @param ctx
	 *            the context the Shared preferences belong to
	 * @param key
	 *            the preference's key, must not be {@code null}
	 * @param value
	 *            an instance of String, Boolean, Integer, Long, Float or
	 *            Set<String> (for API >= HONEYCOMB)
	 * @return true if the commit succeeded, false if not
	 * @throws IllegalArgumentException
	 *             if the value is not an instance of String, Boolean, Integer,
	 *             Long, Float or Set<String> (including the case when you
	 *             specify a double thinking you specified a float, see put())
	 *             OR if you try to add a Set<String> _before_ HONEYCOMB API
	 * @throws NullPointerException
	 *             if key is {@code null}
	 */
	public static <T> boolean commit(final Context ctx, final String key,
			final T value) {
		return _put(ctx, key, value).commit();
	}

	@SuppressLint("CommitPrefEdits")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static <T> Editor _put(final Context ctx, final String key,
			final T value) {
		if (key == null)
			throw new NullPointerException("Null keys are not permitted");
		final Editor ed = getPrefs(ctx).edit();
		if (value == null) {
			// commit it as that is exactly what the API does (but not for boxed
			// primitives) - can be retrieved as anything but if you give get()
			// a default non null value it will give this default value back
			ed.putString(key, null);
			// btw the signature is given by the compiler as :
			// <Object> void
			// gr.uoa.di.android.helpers.AccessPreferences.put(Context ctx,
			// String key, Object value)
			// if I write AccessPreferences.put(ctx, "some_key", null);
		} else if (value instanceof String) ed.putString(key, (String) value);
		else if (value instanceof Boolean) ed.putBoolean(key, (Boolean) value);
		// while int "is-a" long (will be converted to long) Integer IS NOT a
		// Long (CCE) - so the order of "instanceof" checks does not matter -
		// except for frequency I use the values (so I put String, Boolean and
		// Integer first as I mostly use those preferences)
		else if (value instanceof Integer) ed.putInt(key, (Integer) value);
		else if (value instanceof Long) ed.putLong(key, (Long) value);
		else if (value instanceof Float) ed.putFloat(key, (Float) value);
		else if (value instanceof Set) {
			if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				throw new IllegalArgumentException(
					"You can add sets in the preferences only after API "
						+ Build.VERSION_CODES.HONEYCOMB);
			}
			@SuppressWarnings({ "unchecked", "unused" })
			// this set can contain whatever it wants - don't be fooled by the
			// Set<String> cast
			Editor dummyVariable = ed.putStringSet(key, (Set<String>) value);
		} else throw new IllegalArgumentException("The given value : " + value
			+ " cannot be persisted");
		return ed;
	}

	/**
	 * Wrapper around {@link android.content.SharedPreferences.Editor}
	 * {@code get()} methods. Null keys are not permitted. Attempts to retrieve
	 * a preference with a null key will throw NullPointerException. As far as
	 * the type system is concerned T is of the type the variable that is to
	 * receive the default value is. You will get a {@link ClassCastException}
	 * if you put() in a value of type T and try to get() a value of different
	 * type Y - except if you specify a null default *where you will get the CCE
	 * only if you try to assign the get() return value to a variable of type Y,
	 * _in the assignment_ after get() returns*. So don't do this :
	 *
	 * <pre>
	 * AccessPreferences.put(ctx, BOOLEAN_KEY, DEFAULT_BOOLEAN);
	 * AccessPreferences.get(ctx, BOOLEAN_KEY, DEFAULT_STRING); // CCE !
	 * AccessPreferences.get(ctx, BOOLEAN_KEY, null); // NO CCE !!! (***)
	 * String dummy = AccessPreferences.get(ctx, BOOLEAN_KEY, null); // CCE
	 * </pre>
	 *
	 * This is unlike the Preferences framework where you will get a
	 * ClassCastException even if you specify a default null value:
	 *
	 * <pre>
	 * ed.putBoolean(BOOLEAN_KEY, DEFAULT_BOOLEAN);
	 * ed.commit();
	 * prefs.getString(BOOLEAN_KEY, null); // CCE - unlike AccessPreferences!
	 * prefs.getString(BOOLEAN_KEY, &quot;a string&quot;); // CCE
	 * </pre>
	 *
	 * TODO : correct this (***)
	 *
	 * If you put a Set<?> you will get it out as a set of strings - I am not
	 * entirely clear on this
	 *
	 * @param ctx
	 *            the context the Shared preferences belong to
	 * @param key
	 *            the preference's key, must not be {@code null}
	 * @param defaultValue
	 * @return
	 * @throws ClassCastException
	 *             If you try to get a different type than the one you put in -
	 *             except if you specify a null default (***). For other CCEs
	 *             see the {@link #put(Context, String, Object)} docs
	 * @throws IllegalArgumentException
	 *             if a given default value's type is not among the accepted
	 *             classes for preferences or if a Set is given as default or
	 *             asked for before HONEYCOMB API
	 * @throws IllegalStateException
	 *             if I can't figure out the class of a value retrieved from
	 *             preferences (when default is null)
	 * @throws NullPointerException
	 *             if key is {@code null}
	 */
	@SuppressWarnings("unchecked")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <T> T get(final Context ctx, final String key,
			final T defaultValue) {
		if (key == null)
			throw new NullPointerException("Null keys are not permitted");
		// if the value provided as defaultValue is null I can't get its class
		if (defaultValue == null) {
			// if the key !exist I return null which is both the default value
			// provided and what Android would do (as in return the default
			// value - except if boxed primitive..)
			if (!getPrefs(ctx).contains(key)) return null;
			// if the key does exist I get the value and..
			final Object value = getPrefs(ctx).getAll().get(key);
			// ..if null I return null - here I differ from framework - I return
			// null for boxed primitives
			if (value == null) return null;
			// ..if not null I get the class of the non null value. Here I
			// differ from framework - I do not throw if the (non null) value is
			// not of the type the variable to receive it is - cause I have no
			// way to guess the return value expected ! (***)
			final Class<?> valueClass = value.getClass();
			// the order of "instanceof" checks does not matter - still if I
			// have a long autoboxed as Integer ? - tested in
			// testAPNullDefaultUnboxingLong() and works OK (long 0L is
			// autoboxed as long)
			for (Class<?> cls : CLASSES) {
				if (valueClass.isAssignableFrom(cls)) {
					// try {
					// I can't directly cast to T as value may be boolean
					// for instance
					return (T) valueClass.cast(value);
					// } catch (ClassCastException e) { // won't work see :
					// //
					// http://stackoverflow.com/questions/186917/
					// (how-do-i-catch-classcastexception)
					// // basically the (T) valueClass.cast(value); line is
					// // translated to (Object) valueClass.cast(value); which
					// // won't fail ever - the CCE is thrown in the assignment
					// // (T t =) String s = AccessPreferences.get(this, "key",
					// // null); which is compiled as
					// // (String) AccessPreferences.get(this, "key",
					// // null); and get returns an Integer for instance
					// String msg = "Value : " + value + " stored for key : "
					// + key
					// + " is not assignable to variable of given type.";
					// throw new IllegalStateException(msg, e);
					// }
				}
			}
			// that's really Illegal State I guess
			throw new IllegalStateException("Unknown class for value :\n\t"
				+ value + "\nstored in preferences");
		} else if (defaultValue instanceof String) return (T) getPrefs(ctx)
			.getString(key, (String) defaultValue);
		else if (defaultValue instanceof Boolean) return (T) (Boolean) getPrefs(
			ctx).getBoolean(key, (Boolean) defaultValue);
		// the order should not matter
		else if (defaultValue instanceof Integer) return (T) (Integer) getPrefs(
			ctx).getInt(key, (Integer) defaultValue);
		else if (defaultValue instanceof Long) return (T) (Long) getPrefs(ctx)
			.getLong(key, (Long) defaultValue);
		else if (defaultValue instanceof Float) return (T) (Float) getPrefs(ctx)
			.getFloat(key, (Float) defaultValue);
		else if (defaultValue instanceof Set) {
			if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				throw new IllegalArgumentException(
					"You can add sets in the preferences only after API "
						+ Build.VERSION_CODES.HONEYCOMB);
			}
			// this set can contain whatever it wants - don't be fooled by the
			// Set<String> cast
			return (T) getPrefs(ctx).getStringSet(key,
				(Set<String>) defaultValue);
		} else throw new IllegalArgumentException(defaultValue
			+ " cannot be persisted in SharedPreferences");
	}

	/**
	 * Wraps {@link android.content.SharedPreferences#contains(String)}.
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @param key
	 *            the preference's key, must not be {@code null}
	 * @return true if the preferences contain the given key, false otherwise
	 * @throws NullPointerException
	 *             if key is {@code null}
	 */
	public static boolean contains(Context ctx, String key) {
		if (key == null)
			throw new NullPointerException("Null keys are not permitted");
		return getPrefs(ctx).contains(key);
	}

	/**
	 * Wraps {@link android.content.SharedPreferences#getAll()}. Since you must
	 * not modify the collection returned by this method, or alter any of its
	 * contents, this method returns an <em>unmodifiableMap</em> representing
	 * the preferences.
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @return an <em>unmodifiableMap</em> containing a list of key/value pairs
	 *         representing the preferences
	 * @throws NullPointerException
	 *             as per the docs of getAll() - does not say when
	 */
	public static Map<String, ?> getAll(Context ctx) {
		return Collections.unmodifiableMap(getPrefs(ctx).getAll());
	}

	/**
	 * Wraps {@link android.content.SharedPreferences.Editor#clear()}. See its
	 * docs for clarifications. Calls
	 * {@link android.content.SharedPreferences.Editor#commit()}
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @return true if the preferences were successfully cleared, false
	 *         otherwise
	 */
	public static boolean clear(Context ctx) {
		return getPrefs(ctx).edit().clear().commit();
	}

	/**
	 * Wraps {@link android.content.SharedPreferences.Editor#remove(String)}.
	 * See its docs for clarifications. Calls
	 * {@link android.content.SharedPreferences.Editor#commit()}.
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @param key
	 *            the preference's key, must not be {@code null}
	 * @return true if the key was successfully removed, false otherwise
	 * @throws NullPointerException
	 *             if key is {@code null}
	 */
	public static boolean remove(Context ctx, String key) {
		if (key == null)
			throw new NullPointerException("Null keys are not permitted");
		return getPrefs(ctx).edit().remove(key).commit();
	}

	/**
	 * Wraps
	 * {@link android.content.SharedPreferences#registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener)}
	 * .
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @param lis
	 *            the listener, must not be null
	 * @throws NullPointerException
	 *             if lis is {@code null}
	 */
	public static void registerListener(Context ctx,
			OnSharedPreferenceChangeListener lis) {
		if (lis == null) throw new NullPointerException("Null listener");
		getPrefs(ctx).registerOnSharedPreferenceChangeListener(lis);
	}

	/**
	 * Wraps
	 * {@link android.content.SharedPreferences#unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener)}
	 * .
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @param lis
	 *            the listener, must not be null
	 * @throws NullPointerException
	 *             if lis is {@code null}
	 */
	public static void unregisterListener(Context ctx,
			OnSharedPreferenceChangeListener lis) {
		if (lis == null) throw new NullPointerException("Null listener");
		getPrefs(ctx).unregisterOnSharedPreferenceChangeListener(lis);
	}

	/**
	 * Wraps
	 * {@link android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(SharedPreferences, String)}
	 * .
	 *
	 * @param ctx
	 *            the context the SharedPreferences belong to
	 * @param lis
	 *            the listener, must not be null
	 * @param key
	 *            the key we want to run onSharedPreferenceChanged on, must not
	 *            be null
	 * @throws NullPointerException
	 *             if lis or key is {@code null}
	 */
	public static void callListener(Context ctx,
			OnSharedPreferenceChangeListener lis, String key) {
		if (lis == null) throw new NullPointerException("Null listener");
		if (key == null)
			throw new NullPointerException("Null keys are not permitted");
		lis.onSharedPreferenceChanged(getPrefs(ctx), key);
	}

	/**
	 * Check that the given set contains strings only.
	 *
	 * @param set
	 * @return the set cast to Set<String>
	 */
	@SuppressWarnings("unused")
	private static Set<String> checkSetContainsStrings(Set<?> set) {
		if (!set.isEmpty()) {
			for (Object object : set) {
				if (!(object instanceof String))
					throw new IllegalArgumentException(
						"The given set does not contain strings only");
			}
		}
		@SuppressWarnings("unchecked")
		final Set<String> stringSet = (Set<String>) set;
		return stringSet;
	}
}
