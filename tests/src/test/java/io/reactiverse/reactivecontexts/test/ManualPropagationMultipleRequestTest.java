package io.reactiverse.reactivecontexts.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.reactiverse.reactivecontexts.core.Context;
import io.reactiverse.reactivecontexts.test.MyContext;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class ManualPropagationMultipleRequestTest {
	
	@BeforeClass
	public static void init() {
		// initialise
		Context.load();
	}

	public void newRequest(String reqId) {
		// seed
		MyContext.init();
		
		MyContext.get().set(reqId);
	}
	
	public void endOfRequest() {
		MyContext.clear();
	}

	@Test
	public void testRunnableOnSingleWorkerThread() throws Throwable {
		testRunnable(Executors.newFixedThreadPool(1));
	}

	@Test
	public void testRunnableOnTwoWorkerThread() throws Throwable {
		testRunnable(Executors.newFixedThreadPool(2));
	}

	private void testRunnable(ExecutorService executor) throws Throwable {
		newRequest("req 1");
		Future<?> task1 = executor.submit(Context.wrap(() -> {
			checkContextCaptured("req 1");
			endOfRequest();
		}));
		
		newRequest("req 2");
		Future<?> task2 = executor.submit(Context.wrap(() -> {
			checkContextCaptured("req 2");
			endOfRequest();
		}));

		task1.get();
		task2.get();
		executor.shutdown();
	}

	@Test
	public void testCompletionStageOnSingleWorkerThread() throws Throwable {
		testCompletionStage(Executors.newFixedThreadPool(1));
	}

	@Test
	public void testCompletionStageOnTwoWorkerThread() throws Throwable {
		testCompletionStage(Executors.newFixedThreadPool(2));
	}

	private void testCompletionStage(ExecutorService executor) throws Throwable {
		CountDownLatch latch = new CountDownLatch(2);

		Throwable[] ret = new Throwable[2];

		newRequest("req 1");
		CompletableFuture<Void> cf1 = Context.wrap(new CompletableFuture<>());
		cf1.handleAsync((v, t) -> {
			try {
				ret[0] = t;
				checkContextCaptured("req 1");
				endOfRequest();
			}catch(Throwable t2) {
				ret[0] = t2;
			}
			latch.countDown();
			return null;
		}, executor);
		
		newRequest("req 2");
		CompletableFuture<Void> cf2 = Context.wrap(new CompletableFuture<>());
		cf2.handleAsync((v, t) -> {
			try {
				ret[1] = t;
				checkContextCaptured("req 2");
				endOfRequest();
			}catch(Throwable t2) {
				ret[1] = t2;
			}
			latch.countDown();
			return null;
		}, executor);

		cf1.complete(null);
		cf2.complete(null);
		latch.await();
		if(ret[0] != null)
			throw ret[0];
		if(ret[1] != null)
			throw ret[1];
		executor.shutdown();
	}

	private void checkContextCaptured(String reqId) {
		Assert.assertEquals(reqId, MyContext.get().getReqId());
	}
}
