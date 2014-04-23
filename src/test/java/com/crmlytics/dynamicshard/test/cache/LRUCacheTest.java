package com.crmlytics.dynamicshard.test.cache;

import org.junit.Test;

import com.crmlytics.dynamicshard.cache.Cache;
import com.crmlytics.dynamicshard.cache.LRUCache;

import junit.framework.TestCase;

public class LRUCacheTest extends TestCase {
	
	@Test
	public void testPutAndGet() {
		Cache<Integer,String> nameCache = new LRUCache<Integer, String>(5);
		
		nameCache.put(1, "test1");
		assertEquals(true,"test1".equals(nameCache.get(1)));
		nameCache.put(2, "test2");
		assertEquals(true,"test2".equals(nameCache.get(2)));
		nameCache.put(3, "test3");
		assertEquals(true,"test3".equals(nameCache.get(3)));
		nameCache.put(4, "test4");
		assertEquals(true,"test4".equals(nameCache.get(4)));
		nameCache.put(5, "test5");
		assertEquals(true,"test5".equals(nameCache.get(5)));
		nameCache.put(6, "test6");
		assertEquals(true,"test6".equals(nameCache.get(6)));
		assertEquals(5,nameCache.size());
		
		assertEquals(false,"test1".equals(nameCache.get(1)));
		assertEquals(5,nameCache.size());
	}
	
	@Test
	public void testPutAndGetOutOfOrder() {
		Cache<Integer,String> nameCache = new LRUCache<Integer, String>(5);
		
		nameCache.put(1, "test1");
		assertEquals(true,"test1".equals(nameCache.get(1)));
		nameCache.put(2, "test2");
		assertEquals(true,"test2".equals(nameCache.get(2)));
		nameCache.put(3, "test3");
		assertEquals(true,"test3".equals(nameCache.get(3)));
		nameCache.put(4, "test4");
		assertEquals(true,"test4".equals(nameCache.get(4)));
		nameCache.put(5, "test5");
		assertEquals(true,"test5".equals(nameCache.get(5)));
		nameCache.put(6, "test6");
		assertEquals(true,"test6".equals(nameCache.get(6)));
		
		assertEquals(false,"test1".equals(nameCache.get(1)));
		assertEquals(true,"test2".equals(nameCache.get(2)));
		
		nameCache.put(7, "test7");
		assertEquals(true,"test7".equals(nameCache.get(7)));
		assertEquals(false,"test1".equals(nameCache.get(1)));
		assertEquals(true,"test2".equals(nameCache.get(2)));
		assertEquals(false,"test3".equals(nameCache.get(3)));
		assertEquals(5,nameCache.size());
		
	}
	
	@Test
	public void testCleanup() {
		Cache<Integer,String> nameCache = new LRUCache<Integer, String>(5);
		
		nameCache.put(1, "test1");
		assertEquals(true,"test1".equals(nameCache.get(1)));
		nameCache.put(2, "test2");
		assertEquals(true,"test2".equals(nameCache.get(2)));
		nameCache.put(3, "test3");
		assertEquals(true,"test3".equals(nameCache.get(3)));
		nameCache.put(4, "test4");
		assertEquals(true,"test4".equals(nameCache.get(4)));
		nameCache.put(5, "test5");
		assertEquals(true,"test5".equals(nameCache.get(5)));
		nameCache.put(6, "test6");
		assertEquals(true,"test6".equals(nameCache.get(6)));
		
		assertEquals(false,"test1".equals(nameCache.get(1)));
		assertEquals(true,"test2".equals(nameCache.get(2)));
		assertEquals(5,nameCache.size());
		
		nameCache.doCleanUp(3);
		assertEquals(2,nameCache.size());
		
		assertEquals(false,"test3".equals(nameCache.get(3)));
		assertEquals(false,"test4".equals(nameCache.get(4)));
		assertEquals(false,"test5".equals(nameCache.get(5)));
		
		nameCache.put(7, "test7");
		assertEquals(true,"test7".equals(nameCache.get(7)));
		assertEquals(3,nameCache.size());
	}

}
