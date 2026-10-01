package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.net.URLClassLoader;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaClass;
import org.junit.jupiter.api.Test;

public class JSONMultiClassLoaderTest {
    @Test
    public void encodesDynaBeanWithSeparateContextClassLoader() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        URL[] urls = {
            JSON.class.getProtectionDomain().getCodeSource().getLocation(),
            BasicDynaBean.class.getProtectionDomain().getCodeSource().getLocation(),
            org.apache.commons.logging.Log.class.getProtectionDomain().getCodeSource().getLocation()
        };
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
            Class<?> json = loader.loadClass(JSON.class.getName());
            Class<?> dynaClass = loader.loadClass(DynaClass.class.getName());
            Object definition = loader.loadClass(BasicDynaClass.class.getName()).getConstructor().newInstance();
            Object bean = loader.loadClass(BasicDynaBean.class.getName()).getConstructor(dynaClass).newInstance(definition);
            assertSame(loader, json.getClassLoader());
            assertEquals("{}", json.getMethod("encode", Object.class).invoke(null, bean));
        } finally {
            thread.setContextClassLoader(original);
        }
    }
}
