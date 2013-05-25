/*
 * Copyright (c) 2013, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner;

import org.aeonbits.owner.NestedConfigTest.MyAppConfig.SystemEnv;
import org.aeonbits.owner.NestedConfigTest.MyAppConfig.WebServer;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Luigi R. Viggiano
 */
public class NestedConfigTest {

    private MyAppConfig cfg;

    public static interface MyAppConfig extends Config {
        @DefaultValue("My Super App")
        public String title();

        public WebServer webServer();
        public static interface WebServer extends Config {
            @DefaultValue("8080")
            public int port();
            @DefaultValue("http://localhost:${port}/myApp")
            public URL url();
        }

        public SystemProperties systemProperties(Properties... imports);
        public static interface SystemProperties extends Config {
            @Key("user.home")
            public String userHome();
        }

        public Env env(Map<String, String> imports);
        public static interface Env extends Config{
            @Key("HOME")
            public String home();
        }

        public SystemEnv sysEnv(Properties system, Map<String, String> env);
        public interface SystemEnv extends Config {
            @Key("user.home")
            public String userHome();

            @Key("HOME")
            public String home();
        }


        public SystemProperties wrongParams(String wrong);

    }

    @Before
    public void before() {
        cfg = ConfigFactory.create(MyAppConfig.class);
    }

    @Test
    public void testNestedConfig() throws Throwable {
        assertEquals("My Super App", cfg.title());

        WebServer web = cfg.webServer();
        assertEquals(8080, web.port());
        assertEquals(new URL("http://localhost:8080/myApp"), web.url());
    }

    @Test
    public void testNestedConfigWithArrayParameters() throws Throwable {
        String expected = System.getProperty("user.home");
        String result = cfg.systemProperties(System.getProperties()).userHome();
        assertEquals(expected, result);
    }

    @Test
    public void testNestedConfigWithSingleParameter() throws Throwable {
        String expected = System.getenv("HOME");
        String result = cfg.env(System.getenv()).home();
        assertEquals(expected, result);
    }

    @Test
    public void testNestedConfigWithMultipleParameters() throws Throwable {
        SystemEnv env = cfg.sysEnv(System.getProperties(), System.getenv());

        String expectedHomeFromSystemProperties = System.getProperty("user.home");
        String result1 = env.userHome();
        assertEquals(expectedHomeFromSystemProperties, result1);

        String expectedHomeFromSystemEnv = System.getenv("HOME");
        String result2 = env.home();
        assertEquals(expectedHomeFromSystemEnv, result2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWrongParam() {
        cfg.wrongParams("foobar");
    }
}
