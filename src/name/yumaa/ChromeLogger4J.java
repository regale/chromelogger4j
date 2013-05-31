package name.yumaa;

/**
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * 
 * http://sam.zoy.org/wtfpl/COPYING
 * 
 * for more details.
 */

import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.*;
import it.sauronsoftware.base64.Base64;

/**
 * Server Side ChromeLogger4J debugger class
 * 
 * This class is for debugging and logging Java variables to the chrome console.
 * It is server side library for Chrome Logger, developed by Craig Campbell
 * You can find it (and installation instructions) here -> http://www.chromelogger.com
 * 
 * This class uses
 * Base64 library -> http://www.sauronsoftware.it/projects/javabase64/
 * JSON.simple library -> http://code.google.com/p/json-simple/
 *
 * @author Didenko Victor
 */
@SuppressWarnings("unchecked")
public class ChromeLogger4J {
	
	/**
	 * library version
	 */
	public static final String VERSION = "2.0";

	/**
	 * Name of header we use for transfer data
	 */
	public static final String HEADER_NAME = "X-ChromeLogger-Data";
	
	public static final String LOG = ""; // according to http://craig.is/writing/chrome-logger/techspecs we don't need to send "log"
	public static final String WARN = "warn";
	public static final String ERROR = "error";
	public static final String GROUP = "group";
	public static final String INFO = "info";
	public static final String GROUP_END = "groupEnd";
	public static final String GROUP_COLLAPSED = "groupCollapsed";
	
	// different settings
	
	/**
	 * Max recursion depth (default = 2)
	 */
	public int depth = 2;
	
	/**
	 * Store stack trace or not (it takes much memory!) (default = false)
	 */
	public boolean stack = false;
	
	/**
	 * Add properties with NULL values or not (default = false)
	 */
	public boolean addnull = false;
	
	/**
	 * Use reflection for objects of unknown class or not (default = false)
	 */
	public boolean reflect = false;
	
	/**
	 * If we use reflection - should we add fields or not (default = true)
	 */
	public boolean reflectfields = true;
	
	/**
	 * If we use reflection - should we add array with all methods or not (default = false)
	 */
	public boolean reflectmethods = false;
	
	/**
	 * If we use reflection - should we add private fields/methods or not (default = true)
	 */
	public boolean reflectprivate = true;
	
	/**
	 * If we use reflection - should we add static fields/methods or not (default = false)
	 */
	public boolean reflectstatic = false;
	
	// private
	
	/**
	 * HttpServletResponse object - where headers will be sent
	 */
	private HttpServletResponse response = null;
	
	/**
	 * Resulting JSON data
	 */
	private JSONObject json = new JSONObject();
	
	/**
	 * Collection of back trace messages
	 */
	private HashSet<String> backtraces = new HashSet<String>();
	
	/**
	 * Collection of processed objects, to avoid recursions
	 */
	private ArrayList<Object> processed;

	// this class name
	private final String className;

	/**
	 * Class initializer
	 */
	{
		// add initial values into _json
		json.put("version", VERSION);
		JSONArray columns = new JSONArray();
		columns.add("log");
		columns.add("backtrace");
		columns.add("type");
		json.put("columns", columns);
		json.put("rows", new JSONArray());

		// get this class name
		className = this.getClass().getName();
	}
	
	/**
	 * Logs a variable to the console
	 * @param args    variables to log
	 */
	public void log(Object ... args) {
		_log(LOG, args);
	}
	
	/**
	 * Logs a variable to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void log(Object arg) {
		log(new Object[] { arg });
	}
	
	/**
	 * Logs a variable to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void log(Object arg1, Object arg2) {
		log(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Logs a warning to the console
	 * @param args    variables to log
	 */
	public void warn(Object ... args) {
		_log(WARN, args);
	}
	
	/**
	 * Logs a warning to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void warn(Object arg) {
		warn(new Object[] { arg });
	}
	
	/**
	 * Logs a warning to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void warn(Object arg1, Object arg2) {
		warn(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Logs an error to the console
	 * @param args    variables to log
	 */
	public void error(Object ... args) {
		_log(ERROR, args);
	}
	
	/**
	 * Logs an error to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void error(Object arg) {
		error(new Object[] { arg });
	}
	
	/**
	 * Logs an error to the console
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void error(Object arg1, Object arg2) {
		error(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Sends a group log
	 * @param args    variables to log
	 */
	@SuppressWarnings("unused")
	public void group(Object ... args) {
		_log(GROUP, args);
	}
	
	/**
	 * Sends a group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void group(Object arg) {
		group(new Object[] { arg });
	}
	
	/**
	 * Sends a group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void group(Object arg1, Object arg2) {
		group(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Sends an info log
	 * @param args    variable2 to log
	 */
	public void info(Object ... args) {
		_log(INFO, args);
	}
	
	/**
	 * Sends an info log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void info(Object arg) {
		info(new Object[] { arg });
	}
	
	/**
	 * Sends an info log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void info(Object arg1, Object arg2) {
		info(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Sends a collapsed group log
	 * @param args    variables to log
	 */
	@SuppressWarnings("unused")
	public void groupCollapsed(Object ... args) {
		_log(GROUP_COLLAPSED, args);
	}
	
	/**
	 * Sends a collapsed group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void groupCollapsed(Object arg) {
		groupCollapsed(new Object[] { arg });
	}
	
	/**
	 * Sends a collapsed group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void groupCollapsed(Object arg1, Object arg2) {
		groupCollapsed(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Ends a group log
	 * @param args    variables to log
	 */
	public void groupEnd(Object ... args) {
		_log(GROUP_END, args);
	}
	
	/**
	 * Ends a group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg    variable to log
	 */
	@SuppressWarnings("unused")
	public void groupEnd(Object arg) {
		groupEnd(new Object[] { arg });
	}
	
	/**
	 * Ends a group log
	 * Method for BeanShell, because it don't understand syntax sugar like ... in arguments :(
	 * @param arg1    variable to log
	 * @param arg2    variable to log
	 */
	@SuppressWarnings("unused")
	public void groupEnd(Object arg1, Object arg2) {
		groupEnd(new Object[] { arg1, arg2 });
	}
	
	/**
	 * Internal logging call
	 * @param type    log type
	 * @param args    variables to log
	 */
	private void _log(String type, Object ... args) {
		// nothing passed in, don't do anything
		if (args.length == 0 && !GROUP_END.equals(type))
			return;
		
		JSONArray log = new JSONArray();
		for (Object arg : args) {
			processed = new ArrayList<Object>();
			try {
				arg = convert(arg, 0);
			} catch (Exception e) {
				arg = e.toString();
			}
			log.add(arg);
		}

		StringBuilder backtrace_message = new StringBuilder(this.stack ? "" : "stack->false");
		if (this.stack) {
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			for (int i = 0, j = st.length, count = 0; i < j && count < 5; i++) {
				// skip this class calls, native methods and unknown sources
				if (this.className.equals(st[i].getClassName()) || st[i].getFileName() == null || st[i].getLineNumber() < 0)
					continue;

				count++; // count output rows -> print out maximum 5
				backtrace_message.append(st[i].toString());
				if (i+1 < j && count < 5)
					backtrace_message.append("\n");
			}
		}

		addRow(log, backtrace_message.toString(), type);
		writeHeader(json);
	}
	
	/**
	 * Converts an object to a better format for logging
	 * @param object    variable to conver
	 * @param depth     recursion depth
	 * @return converted object, ready to put to JSON
	 */
	private Object convert(Object object, int depth) {
		// *** return simple types as is ***
		if (object == null || object instanceof String || object instanceof Number || object instanceof Boolean)
			return object;
		
		// *** other simple types ***
		
		if (object instanceof Character || object instanceof StringBuffer || object instanceof StringBuilder ||
			object instanceof Currency || object instanceof Date || object instanceof Locale)
			return object.toString();
		
		if (object instanceof Calendar)
			return ((Calendar) object).getTime().toString();
		
		if (object instanceof SimpleDateFormat)
			return ((SimpleDateFormat) object).toPattern();
		
		// check recursion depth
		if (depth > this.depth)
			return "d>" + this.depth;
		
		// mark this object as processed so we don't convert it twice and it
		// also avoid recursion when objects refer to each other
		processed.add(object);
		
		// *** not so simple types, but we can foreach it ***
		
		if (object instanceof Map) {
			JSONObject jobject = new JSONObject();
			for (Object key : ((Map<Object,Object>) object).keySet()) {
				Object value = ((Map<Object,Object>) object).get(key);
				addValue(jobject, key.toString(), value, depth);
			}
			return jobject;
		}
		
		if (object instanceof Collection) {
			JSONArray jobject = new JSONArray();
			for (Object value : (Collection<Object>) object)
				addValue(jobject, value, depth);
			return jobject;
		}
		
		if (object instanceof Iterable) {
			JSONArray jobject = new JSONArray();
			for (Object value : (Iterable<Object>) object)
				addValue(jobject, value, depth);
			return jobject;
		}
		
		if (object instanceof Object[]) {
			JSONArray jobject = new JSONArray();
			for (Object value : (Object[]) object)
				addValue(jobject, value, depth);
			return jobject;
		}
		
		// *** object of unknown type ***
		
		JSONObject jobject = new JSONObject();
		
		Class<?> cls = object.getClass();
		jobject.put("___class_name", cls.getName()); // add the class name
		jobject.put("___toString()", object.toString()); // and to string representation
		
		if (!this.reflect)
			return jobject;
		
		// get all properties using reflection
		if (this.reflectfields) {
			try {
				for (Field field : cls.getDeclaredFields()) {
					Boolean access = field.isAccessible();
					field.setAccessible(true);
					
					int mod = field.getModifiers();
					String key = getKey(mod, field.getName());
					Object value;
					try {
						value = field.get(object);
					} catch (Exception e) {
						value = e.toString();
					}
					
					field.setAccessible(access);
					
					if (!this.reflectprivate && (Modifier.isPrivate(mod) || Modifier.isProtected(mod)))
						continue;
					if (!this.reflectstatic && Modifier.isStatic(mod))
						continue;
					
					addValue(jobject, key, value, depth);
				}
			} catch (SecurityException e) {}
		}
		
		// get all methods using reflection
		if (this.reflectmethods) {
			try {
				JSONObject methods = new JSONObject();
				for (Method method : cls.getDeclaredMethods()) {
					Boolean access = method.isAccessible();
					method.setAccessible(true);
					
					Class<?>[] params = method.getParameterTypes();
					StringBuilder parameters = new StringBuilder("");
					for (int i = 0, j = params.length; i < j; i++) {
						parameters.append(params[i].getName());
						if (i+1 < j)
							parameters.append(", ");
					}
					int mod = method.getModifiers();
					String key = getKey(mod, method.getName() + "(" + parameters.toString() + ")");
					String value = method.getReturnType().getName();
					
					method.setAccessible(access);
					
					if (!this.reflectprivate && (Modifier.isPrivate(mod) || Modifier.isProtected(mod)))
						continue;
					if (!this.reflectstatic && Modifier.isStatic(mod))
						continue;
					
					methods.put(key, value);
				}
				jobject.put("___methods", methods);
			} catch (SecurityException e) {}
		}
		
		return jobject;
	}
	
	/**
	 * Returns a nicely formatted key of the field or method name
	 * @param mod    .getModifiers()
	 * @param end    key ending
	 * @return class member keys, converted to string
	 */
	private String getKey(int mod, String end) {
		StringBuilder key = new StringBuilder("");
		if (Modifier.isPublic(mod))
			key.append("public ");
		if (Modifier.isPrivate(mod))
			key.append("private ");
		if (Modifier.isProtected(mod))
			key.append("protected ");
		if (Modifier.isStatic(mod))
			key.append("static ");
		if (Modifier.isTransient(mod))
			key.append("transient ");
		if (Modifier.isVolatile(mod))
			key.append("volatile ");
		if (Modifier.isFinal(mod))
			key.append("final ");
		if (Modifier.isSynchronized(mod))
			key.append("synchronized ");
		return key.append(end).toString();
	}
	
	private Object processValue(Object object, Object value, int depth) {
		boolean isProcessed = false;
		for (Object o : processed) // don't use .contains() because it tries to cast value type via .equals()
			if (value == o) {      // just check links
				isProcessed = true;
				break;
			}
		
		if (value == object || isProcessed)
			value = "r->" + value.toString();
		else
			try {
				value = convert(value, depth + 1);
			} catch (Exception e) {
				value = e.toString();
			}
		
		return value;
	}
	
	private void addValue(JSONObject object, String key, Object value, int depth) {
		value = processValue(object, value, depth);
		if (value != null || this.addnull)
			object.put(key, value);
	}
	
	private void addValue(JSONArray object, Object value, int depth) {
		value = processValue(object, value, depth);
		if (value != null || this.addnull)
			object.add(value);
	}
	
	/**
	 * Adds a value to the data array
	 * @param log          log data
	 * @param backtrace    backtrace data
	 * @param type         log type
	 */
	private void addRow(JSONArray log, String backtrace, String type) {
		if (backtraces.contains(backtrace))
			backtrace = null;
		else if (backtrace != null)
			backtraces.add(backtrace);
		
		JSONArray row = new JSONArray();
		row.add(log);
		row.add(backtrace);
		row.add(type);
		
		((JSONArray) json.get("rows")).add(row);
	}
	
	/**
	 * Add header to HTTP response
	 * @param data    response JSON data
	 */
	private void writeHeader(JSONObject data) {
		String encodedData = Base64.encode(data.toJSONString(), "UTF-8").replaceAll("\\n", "");
		//FIXME if there is long header it rises "header full: java.lang.ArrayIndexOutOfBoundsException: 16384" uncatchable exception
		response.setHeader(HEADER_NAME, encodedData);
	}
	
	/**
	 * Constructor
	 * @param response    HTTP response
	 */
	public ChromeLogger4J(HttpServletResponse response) {
		this.response = response;
	}
	
	/**
	 * Print about, in case class will be executed
	 * @param args    command line arguments, if class was executed inside of console
	 */
	public static void main(String[] args) {
		System.out.println("/**\n" +
			" * Server Side ChromeLogger4J debugger class\n" +
			" * \n" +
			" * This class is for debugging and logging Java variables to the chrome console.\n" +
			" * It is server side library for Chrome Logger, developed by Craig Campbell\n" +
			" * You can find it (and installation instructions) here -> http://www.chromelogger.com\n" +
			" * \n" +
			" * This class uses\n" +
			" * Base64 library -> http://www.sauronsoftware.it/projects/javabase64/\n" +
			" * JSON.simple library -> http://code.google.com/p/json-simple/\n" +
			" * \n" +
			" * @author Didenko Victor\n" +
			" */"
		);
	}

}
