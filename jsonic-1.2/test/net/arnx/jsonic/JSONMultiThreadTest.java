package net.arnx.jsonic;

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
	public void testMultiThread() throws Exception {
		ExecutorService service = Executors.newCachedThreadPool();
		
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
