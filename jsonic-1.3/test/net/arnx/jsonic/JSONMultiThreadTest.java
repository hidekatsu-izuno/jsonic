package net.arnx.jsonic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.arnx.jsonic.JSONTest.JSONTester;

import org.junit.Test;

public class JSONMultiThreadTest {
	@Test
	public void testMultiThread() throws Exception {
		ExecutorService service = new ThreadPoolExecutor(50, 50,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		List<JSONTester> list = new ArrayList<JSONTester>();
		for (int i = 0; i < 1000; i++) {
			list.add(new JSONTester());
		}
		List<Future<Object>> results = service.invokeAll(list);

		service.shutdown();
		service.awaitTermination(60 * 1000, TimeUnit.MILLISECONDS);

		for (Future<Object> future : results) {
			future.get();
		}
	}
}
