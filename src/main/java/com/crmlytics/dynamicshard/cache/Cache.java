package com.crmlytics.dynamicshard.cache;

public interface Cache <K,V> {
	
	public void put(K key, V value);
	public V get(K key);
	
	public void doCleanUp();
	public void doCleanUp(int num);
	public int size();
	
}
