package com.crmlytics.dynamicshard.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFUCache<K,V> implements Cache<K, V> {
	private static Logger logger = LoggerFactory.getLogger(LFUCache.class);
	
	private static final int DEFAULT_CLEANUP_SIZE = 1;
	
	// Its the current size of cache
	private AtomicInteger size;
	// Frequency level vs cached elements list 
	private final ConcurrentHashMap<Integer,FrequencyLevelNode<K,V>> levelMap;
	private final ConcurrentHashMap<K,Node<K,V>> map;
	private final int cleanUpSize;
	private final int maxSize;
	
	public LFUCache(int maxSize) {
		this(maxSize,DEFAULT_CLEANUP_SIZE);
	}
	
	public LFUCache(int maxSize, int cleanUpSize) {
		this.maxSize = maxSize;
		this.size = new AtomicInteger(0);
		this.cleanUpSize = cleanUpSize;
		this.levelMap = new ConcurrentHashMap<Integer, FrequencyLevelNode<K,V>>();
		this.levelMap.put(1, new FrequencyLevelNode<K, V>(1));
		this.map = new ConcurrentHashMap<K, Node<K,V>>(this.size.get());;
		logger.debug("LFU cached initalized with maxsize:{} and cleanupSize:{}",maxSize,cleanUpSize);
	}
	
	public void put(K key, V value) {
		Node<K,V> node = map.get(key);
		if(node == null) {
			// Current key value pair is not cached. Action:Cache it
			if(this.size.get() + 1 > maxSize) {
				doCleanUp();
			}
			
			node = new Node<K, V>(key, value);
			map.putIfAbsent(key, node);
			node = map.get(key);
			
			synchronized (node) {
				node.value = value;
				FrequencyLevelNode<K, V> level = this.levelMap.get(FrequencyLevelNode.FIRST_FREQUENCY_LEVEL);
				level.addNode(node);
				
				size.incrementAndGet();
				logger.debug("Added new node{}",node);
			}
		} else {
			synchronized (node) {
				FrequencyLevelNode<K, V> level = this.levelMap.get(node.freqency);
				
				if(level.deleteNode(node)) {
					FrequencyLevelNode<K, V> nextLevel = this.levelMap.get(node.freqency+1);
					if(nextLevel == null) {
						nextLevel = new FrequencyLevelNode<K, V>(node.freqency+1);
						this.levelMap.putIfAbsent(nextLevel.frequency, nextLevel);
						nextLevel = this.levelMap.get(node.freqency+1);
					}
					nextLevel.addNode(node);
					//Update the value
					node.value = value;
				} else {
					logger.debug("The node got cleaned by the time we tried deleting node from freq level(to increase the freq level). Retrying put method call.");
					put(key,value);
				}
			}
		}
		
	}

	public V get(K key) {
		Node<K,V> node = map.get(key);
		if(node != null) {
			synchronized (node) {
				FrequencyLevelNode<K, V> level = this.levelMap.get(node.freqency);
				
				if(level.deleteNode(node)) {
					FrequencyLevelNode<K, V> nextLevel = this.levelMap.get(node.freqency+1);
					if(nextLevel == null) {
						nextLevel = new FrequencyLevelNode<K, V>(node.freqency+1);
						this.levelMap.putIfAbsent(nextLevel.frequency, nextLevel);
					}
					nextLevel = this.levelMap.get(node.freqency+1);
					nextLevel.addNode(node);
					return node.value;
				} else {
					logger.debug("The node got cleaned by the time we tried deleting node from freq level(to increase the freq level). Returning null");
				}
			}
		}
		return null;
	}

	public void doCleanUp() {
		doCleanUp(cleanUpSize);
	}
	
	public synchronized void doCleanUp(int num) {
		logger.debug("Attempting cleanup of {} number of elements",num);
		FrequencyLevelNode<K, V> freqLevel = this.levelMap.get(FrequencyLevelNode.FIRST_FREQUENCY_LEVEL);
		int count = 0;
		while(this.size.get() > 0 && count < num) {
			int numDeleted = freqLevel.doCleanUp(num, map, size);
			logger.debug("Number of cached elements deleted:{} from freqlevel:{}",numDeleted,freqLevel.frequency);
			count += numDeleted;
			if(numDeleted == 0) {
				do {
					freqLevel = levelMap.get(freqLevel.frequency+1);
				} while(freqLevel == null && size.get() > 0);
			}
		}
		
		logger.debug("Successfully cleaned {} number of elements",count);
	}
	
	public int size() {
		return this.size.get();
	}
	
	private static class FrequencyLevelNode<K,V> {
		private static final int FIRST_FREQUENCY_LEVEL = 1;
		private int frequency;
		private Node<K,V> firstNode;
		private Node<K,V> lastNode;
		
		public FrequencyLevelNode(int freqency) {
			this.frequency = freqency;
			this.firstNode = null;
			this.lastNode = null;
		}
		
		public synchronized void addNode(Node<K,V> node) {
			if(node != null) {
				if(this.lastNode == null) {
					this.firstNode = node;
					this.lastNode = node;
				} else {
					this.lastNode.addAfter(node);
					this.lastNode = node;
				}
				node.freqency = this.frequency;
			}
		}
		
		public synchronized boolean deleteNode(Node<K,V> node) {
			if(node != null) {
				if(this.lastNode == node) {
					this.lastNode = node.prev;
				}
				if(this.firstNode == node) {
					this.firstNode = node.next;
				}
				node.deleteNode();
				return true;
			}
			return false;
		}
		
		public synchronized int doCleanUp(int cleanUpSize, Map<K,Node<K,V>> map, AtomicInteger size) {
			int processed = 0;
			while(this.firstNode != null && cleanUpSize > 0) {
				Node<K, V> nodeToBeDeleted = this.firstNode;
				
				deleteNode(nodeToBeDeleted);
				map.remove(nodeToBeDeleted.key);
				cleanUpSize--;
				processed++;
				size.decrementAndGet();
			}
			return processed;
		}
	}
	
	private static class Node<K,V> {
		private Node<K,V> prev;
		private Node<K,V> next;
		private K key;
		private V value;
		private int freqency=1;
		
		public Node(K key, V value) {
			this(key,value,null,null);
		}
		
		public Node(K key, V value, Node<K,V> next, Node<K,V> prev) {
			this.key = key;
			this.value = value;
			this.next = next;
			this.prev = prev;
		}
		
		public void addAfter(Node<K,V> newNode) {
			if(newNode != null) {
				newNode.prev = this;
				newNode.next = this.next;
				if(this.next != null) {
					this.next.prev = newNode;
				}
				this.next = newNode;
				logger.debug("Added new node:{} next to node:{}",newNode,this);
			}
		}
		
		public void deleteNode() {
			if(this.next != null) {
				this.next.prev = this.prev;
			}
			if(this.prev != null) {
				this.prev.next = this.next;
			}
			this.prev = null;
			this.next = null;
			logger.debug("Deleted Node:{}",this);
		}
		
		@Override
		public String toString() {
			return "key:"+this.key+", value:"+this.value+",freq:"+this.freqency;
		}
	}
}
