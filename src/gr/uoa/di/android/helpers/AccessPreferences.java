package gr.uoa.di.android.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AccessPreferences {

	private static final List<Class<?>> CLASSES = new ArrayList<Class<?>>();
	static {
		CLASSES.add(String.class);
		CLASSES.add(Boolean.class);
		CLASSES.add(Integer.class);
		CLASSES.add(Long.class);
		CLASSES.add(Float.class);
		CLASSES.add(Set.class);
	}

	private AccessPreferences() {}

	private static SharedPreferences prefs;

	private static SharedPreferences getPrefs(Context ctx) {
		// synchronized is not really needed as the same instance of
		// SharedPreferences will be returned AFAIC but better safe than sorry
		if (prefs == null) synchronized (AccessPreferences.class) {
			if (prefs == null) {
				prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			}
		}
		return prefs;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <T> void put(Context ctx, String key, T value) {
		final Editor ed = getPrefs(ctx).edit();
		if (value == null) {
			// commit it as that is exactly what the API does - can be retrieved
			// as anything but if you give get() a default non null value it
			// will give this default value back
			ed.putString(key, null);
		} else if (value instanceof String) ed.putString(key, (String) value);
		else if (value instanceof Boolean) ed.putBoolean(key, (Boolean) value);
		// TODO : IS THE ORDER OF FLOAT, INTEGER AND LONG CORRECT ?
		else if (value instanceof Integer) ed.putInt(key, (Integer) value);
		else if (value instanceof Long) ed.putLong(key, (Long) value);
		else if (value instanceof Float) ed.putFloat(key, (Float) value);
		else if (value instanceof Set) {
			if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				throw new IllegalArgumentException(
						"You can add sets in the preferences only after API "
							+ Build.VERSION_CODES.HONEYCOMB);
			}
			// The given set does not contain strings only --> TODO : not my
			// problem ? probably cause the one who filled it made the mistake
			// Set<?> set = (Set<?>) value;
			// if (!set.isEmpty()) {
			// for (Object object : set) {
			// if (!(object instanceof String))
			// throw new IllegalArgumentException(
			// "The given set does not contain strings only");
			// }
			// }
			@SuppressWarnings({ "unchecked", "unused" })
			Editor dummyVariable = ed.putStringSet(key, (Set<String>) value);
		} else throw new IllegalArgumentException("The given value : " + value
			+ " cannot be persisted");
		ed.commit();
	}

	@SuppressWarnings("unchecked")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <T> T get(Context ctx, String key, T defaultValue) {
		// if the value provided as defaultValue is null I can't get its class
		if (defaultValue == null) {
			// if the key (which can very well be null btw) !exist I return null
			// which is both the default value provided and what Android would
			// do (as in return the default value) (TODO: test)
			if (!getPrefs(ctx).contains(key)) return null;
			// if the key does exist I get the value and..
			final Object value = getPrefs(ctx).getAll().get(key);
			// ..if null I return null
			if (value == null) return null;
			// ..if not null I get the class of the non null value. Problem is
			// that as far as the type system is concerned T is of the type the
			// variable that is to receive the default value is. So :
			// String s = AccessPreferences.retrieve(this, "key", null);
			// if the value stored in "key" is not a String for instance
			// `"key" --> true` or `"key" --> 1.2` a ClassCastException will
			// occur _in the assignment_ after retrieve returns
			// TODO : is it my problem ? This :
			// SharedPreferences p =
			// PreferenceManager.getDefaultSharedPreferences(ctx);
			// int i = p.getInt(KEY_FOR_STRING, 7);
			// results in a class cast exception as well !
			final Class<?> clazz = value.getClass();
			// TODO : IS THE ORDER OF FLOAT, INTEGER AND LONG CORRECT in CLASSES
			for (Class<?> cls : CLASSES) {
				if (clazz.isAssignableFrom(cls)) {
					try {
						// I can't directly cast to T as value may be boolean
						// for instance
						return (T) clazz.cast(value);
					} catch (ClassCastException e) { // won't work see :
						// http://stackoverflow.com/questions/186917/how-do-i-catch-classcastexception
						// basically the (T) clazz.cast(value); line is
						// translated to (Object) clazz.cast(value); which won't
						// fail ever - the CCE is thrown in the assignment (T t
						// =) String s = AccessPreferences.retrieve(this, "key",
						// null); which is compiled as
						// (String)AccessPreferences.retrieve(this, "key",
						// null); and retrieve returns an Integer for instance
						String msg = "Value : " + value + " stored for key : "
							+ key
							+ " is not assignable to variable of given type.";
						throw new IllegalStateException(msg, e);
					}
				}
			}
			// that's really Illegal State I guess
			throw new IllegalStateException("Unknown class for value :\n\t"
				+ value + "\nstored in preferences");
		} else if (defaultValue instanceof String) return (T) getPrefs(ctx)
				.getString(key, (String) defaultValue);
		else if (defaultValue instanceof Boolean) return (T) (Boolean) getPrefs(
			ctx).getBoolean(key, (Boolean) defaultValue);
		// TODO : IS THE ORDER OF FLOAT, INTEGER AND LONG CORRECT ?
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
			// The given set does not contain strings only --> TODO : not my
			// problem ? probably cause the one who filled it made the mistake
			// Set<?> set = (Set<?>) defaultValue;
			// if (!set.isEmpty()) {
			// for (Object object : set) {
			// if (!(object instanceof String))
			// throw new IllegalArgumentException(
			// "The given set does not contain strings only");
			// }
			// }
			return (T) getPrefs(ctx).getStringSet(key,
				(Set<String>) defaultValue);
		} else throw new IllegalArgumentException(defaultValue
			+ " cannot be persisted in SharedPreferences");
	}
}
