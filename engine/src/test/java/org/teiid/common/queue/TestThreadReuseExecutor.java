/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.common.queue;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.junit.After;
import org.junit.Test;
import org.teiid.adminapi.impl.WorkerPoolStatisticsMetadata;
import org.teiid.dqp.internal.process.FutureWork;
import org.teiid.dqp.internal.process.ThreadReuseExecutor;

/**
 */
public class TestThreadReuseExecutor {
	
	ThreadReuseExecutor pool = null;
	
	@After public void tearDown() {
		if (pool != null) {
			pool.shutdownNow();
		}
	}
	
    @Test public void testQueuing() throws Exception {
        final long SINGLE_WAIT = 50;
        final int WORK_ITEMS = 10;
        final int MAX_THREADS = 5;

        pool = new ThreadReuseExecutor("test", MAX_THREADS); //$NON-NLS-1$
        
        for(int i=0; i<WORK_ITEMS; i++) {
            pool.execute(new FakeWorkItem(SINGLE_WAIT));
        }
        
        pool.shutdown();        
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        assertTrue(pool.isTerminated());
        WorkerPoolStatisticsMetadata stats = pool.getStats();
        assertEquals(10, stats.getTotalCompleted());
        assertEquals("Expected threads to be maxed out", MAX_THREADS, stats.getHighestActiveThreads()); //$NON-NLS-1$
    }

    @Test public void testThreadReuse() throws Exception {
        final long SINGLE_WAIT = 50;
        final long NUM_THREADS = 5;

        pool = new ThreadReuseExecutor("test", 5); //$NON-NLS-1$
        
        for(int i=0; i<NUM_THREADS; i++) {            
        	pool.execute(new FakeWorkItem(SINGLE_WAIT));
            
            try {
                Thread.sleep(SINGLE_WAIT*3);
            } catch(InterruptedException e) {                
            }
        }
        
        pool.shutdown();                
        
        WorkerPoolStatisticsMetadata stats = pool.getStats();
        assertTrue("Expected approximately 1 thread for serial execution", stats.getHighestActiveThreads() <= 2); //$NON-NLS-1$
        
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }
    
    @Test(expected=RejectedExecutionException.class) public void testShutdown() throws Exception {
    	pool = new ThreadReuseExecutor("test", 5); //$NON-NLS-1$
        pool.shutdown();
    	pool.execute(new FakeWorkItem(1));
    }
    
    @Test public void testFailingWork() throws Exception {
    	pool = new ThreadReuseExecutor("test", 5); //$NON-NLS-1$
    	final Semaphore signal = new Semaphore(1);
    	pool.execute(new Work() {
    		@Override
    		public void run() {
    			signal.release();
    			throw new RuntimeException();
    		}
    		
    		@Override
    		public void release() {
    			
    		}
    	});
    	assertTrue(signal.tryAcquire(2, TimeUnit.SECONDS));
    }
    
    @Test public void testPriorities() throws Exception {
    	pool = new ThreadReuseExecutor("test", 1); //$NON-NLS-1$
    	FutureWork<Boolean> work1 = new FutureWork<Boolean>(new Callable<Boolean>() {
    		public Boolean call() throws Exception {
    			synchronized (pool) {
    				while (pool.getSubmittedCount() < 4) {
    					pool.wait();
    				}
				}
    			return true;
    		}
		}, 0);
    	final ConcurrentLinkedQueue<Integer> order = new ConcurrentLinkedQueue<Integer>();
    	FutureWork<Boolean> work2 = new FutureWork<Boolean>(new Callable<Boolean>() {
    		public Boolean call() throws Exception {
    			order.add(2);
    			return true;
    		}
		}, 2);
    	FutureWork<Boolean> work3 = new FutureWork<Boolean>(new Callable<Boolean>() {
    		public Boolean call() throws Exception {
    			order.add(3);
    			return false;
    		}
		}, 1);
    	Thread.sleep(20); //ensure a later timestamp
    	FutureWork<Boolean> work4 = new FutureWork<Boolean>(new Callable<Boolean>() {
    		public Boolean call() throws Exception {
    			order.add(4);
    			return false;
    		}
		}, 2);
    	pool.execute(work1);
    	pool.execute(work2);
    	pool.execute(work3);
    	pool.execute(work4);
    	synchronized (pool) {
        	pool.notifyAll();
		}
    	work1.get();
    	work2.get();
    	work3.get();
    	work4.get();
    	assertEquals(Integer.valueOf(3), order.remove());
    	assertEquals(Integer.valueOf(2), order.remove());
    	assertEquals(Integer.valueOf(4), order.remove());
    }
        
}
