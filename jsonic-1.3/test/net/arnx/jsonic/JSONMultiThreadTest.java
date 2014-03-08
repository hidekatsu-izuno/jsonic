package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.arnx.jsonic.JSONTest.JSONTester;

import org.junit.Test;

public class JSONMultiThreadTest {
	@Test
	public void testClassLoader() throws Exception {
		ClassLoader cl = new URLClassLoader(new URL[] {
				new URL("file://./lib/jsp-api.jar")
		});
		
		ClassLoader backup = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(cl);
			
			List<String> list = new ArrayList<String>();
			
			assertEquals("[]", JSON.encode(list));
			assertEquals(list, JSON.decode("[]"));
			
		} finally {
			Thread.currentThread().setContextClassLoader(backup);
		}
	}
	
	@Test
	public void testMultiThread() throws Exception {
		ExecutorService service = Executors.newFixedThreadPool(50);
		
		List<JSONTester> list = new ArrayList<JSONTester>();
		for (int i = 0; i < 1000; i++) {
			list.add(new JSONTester());
		}
		List<Future<Object>> results = service.invokeAll(list);
		
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);
		
		for (Future<Object> future : results) {
			future.get();
		}
	}
}
