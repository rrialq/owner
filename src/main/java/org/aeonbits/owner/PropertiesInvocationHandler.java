/*
 * Copyright (c) 2012, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner;


import org.aeonbits.owner.Config.DefaultValue;
import org.aeonbits.owner.Config.Key;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.aeonbits.owner.Config.DisableableFeature.PARAMETER_FORMATTING;
import static org.aeonbits.owner.Config.DisableableFeature.VARIABLE_EXPANSION;
import static org.aeonbits.owner.Converters.convert;
import static org.aeonbits.owner.PropertiesMapper.key;
import static org.aeonbits.owner.Util.isFeatureDisabled;
import static org.aeonbits.owner.Util.unsupported;

/**
 * This {@link InvocationHandler} receives method calls from the delegate instantiated by {@link ConfigFactory} and maps
 * it to a property value from a property file, or a {@link DefaultValue} specified in method annotation.
 * <p/>
 * The {@link Key} annotation can be used to override default mapping between method names and property names.
 * <p/>
 * Automatic conversion is handled between the property value and the return type expected by the method of the
 * delegate.
 *
 * @author Luigi R. Viggiano
 */
class PropertiesInvocationHandler implements InvocationHandler {
    private final Properties properties;
    private final StrSubstitutor substitutor;
    private static final Method listPrintStream = getMethod(Properties.class, "list", PrintStream.class);
    private static final Method listPrintWriter = getMethod(Properties.class, "list", PrintWriter.class);

    private static Method getMethod(Class<?> aClass, String name, Class<?>... args) {
        try {
            return aClass.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            // this shouldn't happen, btw we handle the case in which the delegate method is not available...
            // so, it's fine.
            return null;
        }
    }

    PropertiesInvocationHandler(Properties properties) {
        this.properties = properties;
        this.substitutor = new StrSubstitutor(properties);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
        Method proxyMethod;
        if (null != (proxyMethod = proxyMethod(method)))
            return delegate(properties, proxyMethod, args);
        return resolveProperty(method, args);
    }

    private Object resolveProperty(Method method, Object... args) {
        String key = key(method);
        String value = properties.getProperty(key);
        Class<?> returnType = method.getReturnType();
        if (Config.class.isAssignableFrom(returnType))
            return resolveNestedConfig(method, args);
        if (value == null)
            return null;
        return convert(method, returnType, format(method, expandVariables(method, value), args));
    }

    private Object resolveNestedConfig(Method method, Object[] args) {

        Map<?,?>[] imports = null;

        if (args == null || args.length < 1)
            imports = new Map<?, ?>[0];
        else if (args.length == 1 && args[0] != null && Map[].class.isAssignableFrom(args[0].getClass()))
            imports = (Map<?,?>[])args[0];
        else {
            imports = new Map<?, ?>[args.length];
            try {
                System.arraycopy(args, 0, imports, 0, args.length);
            } catch (ArrayStoreException ex) {
                throw unsupported(ex, "Unsupported args '%s' for method '%s'", Arrays.asList(args),
                        method.toGenericString());
            }
        }

        return ConfigFactory.create((Class<? extends Config>) method.getReturnType(), imports);
    }

    private String format(Method method, String format, Object... args) {
        if (isFeatureDisabled(method, PARAMETER_FORMATTING))
            return format;
        return String.format(format, args);
    }

    private String expandVariables(Method method, String value) {
        if (isFeatureDisabled(method, VARIABLE_EXPANSION))
            return value;
        return substitutor.replace(value);
    }

    private Object delegate(Object target, Method method, Object... args) throws InvocationTargetException,
            IllegalAccessException {
        return method.invoke(target, args);
    }

    private Method proxyMethod(Method method) {
        if (matches(listPrintStream, method))
            return listPrintStream;
        if (matches(listPrintWriter, method))
            return listPrintWriter;
        return null;
    }

    private boolean matches(Method proxied, Method proxy) {
        return proxied != null && proxied.getName().equals(proxy.getName())
                && Arrays.equals(proxied.getParameterTypes(), proxy.getParameterTypes());
    }

}
