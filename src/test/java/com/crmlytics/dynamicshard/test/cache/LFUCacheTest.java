package com.crmlytics.dynamicshard.test.cache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

import org.junit.Test;

import com.crmlytics.dynamicshard.cache.Cache;
import com.crmlytics.dynamicshard.cache.LFUCache;

public class LFUCacheTest extends TestCase {
	
	@Test
	public void testPutAndGet() {
		Cache<Integer,String> nameCache = new LFUCache<Integer, String>(5);
		
		nameCache.put(1, "test1");
		assertEquals(true,"test1".equals(nameCache.get(1)));
		
		nameCache.put(2, "test2");
		nameCache.put(2, "test21");
		assertEquals(true,"test21".equals(nameCache.get(2)));
		
		nameCache.put(3, "test3");
		assertEquals(true,"test3".equals(nameCache.get(3)));
		
		nameCache.put(4, "test4");
		assertEquals(true,"test4".equals(nameCache.get(4)));
		
		nameCache.put(5, "test5");
		assertEquals(true,"test5".equals(nameCache.get(5)));
		
		nameCache.put(6, "test6");
		assertEquals(true,"test6".equals(nameCache.get(6)));
		
		assertEquals(true,nameCache.get(1)==null);
		assertEquals(5,nameCache.size());
		
		nameCache.put(7, "test7");
		assertEquals(true,"test7".equals(nameCache.get(7)));
		
		assertEquals(true,nameCache.get(1)==null);
		assertEquals(true,nameCache.get(3)==null);
		assertEquals(true,"test21".equals(nameCache.get(2)));
		assertEquals(5,nameCache.size());
	}
	
	@Test
	public void testCleanup() {
		Cache<Integer,String> nameCache = new LFUCache<Integer, String>(5,3);
		
		nameCache.put(1, "test1");
		assertEquals(true,"test1".equals(nameCache.get(1)));
		
		nameCache.put(2, "test2");
		nameCache.put(2, "test21");
		assertEquals(true,"test21".equals(nameCache.get(2)));
		
		nameCache.put(3, "test3");
		assertEquals(true,"test3".equals(nameCache.get(3)));
		
		nameCache.put(4, "test4");
		assertEquals(true,"test4".equals(nameCache.get(4)));
		
		nameCache.put(5, "test5");
		assertEquals(true,"test5".equals(nameCache.get(5)));
		assertEquals(5,nameCache.size());
		
		nameCache.put(6, "test6");
		assertEquals(3,nameCache.size());
		
		assertEquals(true,nameCache.get(1)==null);
		assertEquals(3,nameCache.size());
		assertEquals(true,nameCache.get(3)==null);
		assertEquals(3,nameCache.size());
		assertEquals(true,nameCache.get(4)==null);
		assertEquals(3,nameCache.size());
		
		nameCache.put(7, "test7");
		nameCache.put(8, "test8");
		assertEquals(5,nameCache.size());
		
		nameCache.doCleanUp(1);
		assertEquals(4,nameCache.size());
		assertEquals(true,nameCache.get(6)==null);
	}
	
	@Test
	public void testSyncUnderLoad() {
		ExecutorService exe = Executors.newFixedThreadPool(100);
		
		Cache<Integer,String> nameCache = new LFUCache<Integer,String>(10,3);
	}

}
