/**
 * 
 */
package org.matsim.contrib.smartcity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.ReflectionUtils;

import com.google.inject.Injector;

/**
 * Class with methods that help the instantiation of class
 *  
 * @author Filippo Muzzini
 *
 */
public class InstantationUtils {
	
	/**
	 * The class is searched in this package
	 */
	private static final String DEFAULT_PACKAGE = "org.matsim.contrib.smartcity";
	
	public static HashMap<Class<?>, String> getParamName(String className) {
		Class<?> cl = getClassForName(className);
		Constructor<?> con = getMinConstructor(cl);
		HashMap<Class<?>, String> res = new HashMap<Class<?>, String>();
		for (Parameter param : con.getParameters()) {
			res.put(param.getType(), param.getName());
		}
		
		return res;
	}
	
	/**
	 * Instantiate the class defined in the name using the constructor with less number
	 * of parameters
	 * If the name don't contains the package, the default package is used.
	 * 
	 * @param inj injector
	 * @param name name of class
	 * @return instantiated class object
	 */
	public static <T> T instantiateForName(Injector inj, String name) {
		Class<T> objectClass = getClassForName(name);		
		return instantiateClass(inj, objectClass);
	}
	
	/**
	 * Return the Class using specified name
	 * @param name
	 * @return class
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClassForName(String name){
		String className = foundClassName(name);
		Class<T> objectClass = null;
		try {
			objectClass = (Class<T>) Class.forName(className);
		} catch (ClassNotFoundException e1) {
			System.err.println("Class "+className+" not found");
			e1.printStackTrace();
		}
		
		return objectClass;
	}
	
	/**
	 * Instantiate the class defined in the name using the constructor with less number
	 * of parameters and use the params specified. The non specified params are fell using
	 * injector.
	 * If the name don't contains the package, the default package is used.
	 * 
	 * @param inj injector
	 * @param name name of class
	 * @param params specified params
	 * @return instantiated object
	 */
	public static <T> T instantiateForNameWithParams(Injector inj, String name, Object ... params) {
		HashMap<Class<?>, Object> paramsType = new HashMap<Class<?>, Object>();
		for (Object param : params) {
			if (param != null)
				paramsType.put(param.getClass(), param);
		}
		
		Class<T> cl = getClassForName(name);
		Constructor<T> constructor = getMinConstructor(cl);
		Object[] paramsObj = new Object[constructor.getParameterTypes().length];
		int i = 0;
		for (Class<?> type : constructor.getParameterTypes()) {
			Object obj = paramsType.get(type);
			if (obj == null) {
				for (Class<?> t : paramsType.keySet()) {
					Set<Class<?>> s = ReflectionUtils.getAllSuperTypes(t);
					if (s.contains(type)) {
						obj = paramsType.get(t);
						break;
					}
				}
			}
			obj = obj != null ? obj : inj.getInstance(type);
			paramsObj[i] = obj;
			i++;
		}
				
		return instantiateClassWithConstructorAndParams(inj, constructor, paramsObj);
	}
	
	/**
	 * Instantiate the class defined in the name using the constructor with less number
	 * of parameters and use the params specified. The non specified params are fell using
	 * injector.
	 * If the name don't contains the package, the default package is used.
	 * 
	 * @param inj injector
	 * @param name name of class
	 * @param params specified params
	 * @return instantiated object
	 */
//	public static <T> T instantiateForNameWithParams(Injector inj, String name, Map<String, Object> params) {
//		Class<T> cl = getClassForName(name);
//		Constructor<T> constructor = getMinConstructor(cl);
//		Object[] paramsObj = new Object[constructor.getParameterTypes().length];
//		int i = 0;
//		for (Parameter param : constructor.getParameters()) {
//			Object obj = params.get(param.getName());
//			obj = obj != null ? obj : inj.getInstance(param.getType());
//			paramsObj[i] = obj;
//			i++;
//		}
//
//		return instantiateClassWithConstructorAndParams(inj, constructor, paramsObj);
//	}
	

	/**
	 * Instantiate the class using the constructor with less number of parameters
	 * 
	 * @param inj injector
	 * @param cl class
	 * @return instantiated class object
	 */
	public static <T> T instantiateClass(Injector inj, Class<T> cl) {
		Constructor<T> constructor = getMinConstructor(cl);
		return instantiateClassWithConstructor(inj, constructor);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getMinConstructor(Class<T> cl){
		Constructor<T>[] constrs = (Constructor<T>[]) cl.getConstructors();
		int min = Integer.MAX_VALUE;
		Constructor<T> constructor = null;
		for (Constructor<T> c : constrs) {
			int n = c.getParameterTypes().length;
			if (n <= min) {
				min = n;
				constructor = c;
			}
		}
		
		return constructor;
	}
	
	/**
	 * Instantiate the class using specified constructor
	 * 
	 * @param inj injector
	 * @param constructor constructor
	 * @return instantiated class object
	 */
	public static <T> T instantiateClassWithConstructor(Injector inj, Constructor<T> constructor) {
		Class<?>[] params = constructor.getParameterTypes();
		Object[] objectsParams = getParams(inj, params);
		
		return instantiateClassWithConstructorAndParams(inj, constructor, objectsParams);
	}
	
	private static <T> T instantiateClassWithConstructorAndParams(Injector inj, Constructor<T> constructor, Object[] params) {
		T res = null;
		try {
			res = constructor.newInstance(params);
			inj.injectMembers(res);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	/**
	 * Instantiate the params specified in params usig injector
	 * 
	 * @param inj injector
	 * @param params list of parmams' class
	 * @return instantiated parmas' object
	 */
	private static Object[] getParams(Injector inj, Class<?>[] params){
		int n = params.length;
		Object[] res = new Object[n];
		for (int i=0; i<n; i++) {
			res[i] = inj.getInstance(params[i]);
		}
		
		return res;
	}
	
	/**
	 * Determinate if class name have specified the package or no.
	 * If no return the class name with default package.
	 * @param name name of class
	 * @return name of class with package
	 */
	public static String foundClassName(String name) {
		try {
			Class.forName(name);
			return name;
		} catch (ClassNotFoundException e1) {
			return DEFAULT_PACKAGE+"."+name;
		}
	}

}
