package com.crmlytics.dynamicshard.cache;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LRUCache<K,V> implements Cache<K,V> {
	private static Logger logger = LoggerFactory.getLogger(LRUCache.class);
	
	private static final int DEFAULT_CLEANUP_SIZE = 1;
	
	private Node<K,V> head;
	private Node<K,V> tail;
	private int size;
	
	private final int maxSize;
	private final HashMap<K,Node<K,V>> map;
	private final int cleanUpSize;
	
	public LRUCache(int maxSize) {
		this(maxSize,DEFAULT_CLEANUP_SIZE);
	}
	
	public LRUCache(int maxSize, int cleanUpSize) {
		this.maxSize = maxSize;
		this.size = 0;
		this.cleanUpSize = cleanUpSize;
		this.head = null;
		this.tail = null;
		this.map = new HashMap<K, Node<K,V>>(maxSize);
	}
	
	public synchronized void put(K key, V value) {
		Node<K,V> node = map.get(key);
		if(node == null) {
			if(size + 1 > maxSize) {
				doCleanUp();
			}
			
			node = new Node<K, V>(key, value);
			map.put(key, node);
			
			if(head != null) {
				head.addBefore(node);
				head = node;
			} else {
				head = node;
				tail = node;
			}
			
			size++;
		} else {
			if(node == tail && node == head) {
				logger.debug("Only one element present. node:{}",node);
			} else {
				if(node == tail) {
					tail = tail.prev;
				}
				node.deleteNode();
				head.addBefore(node);
				head = node;
			}
			node.value = value;
		}
	}

	public synchronized V get(K key) {
		Node<K,V> node = map.get(key);
		if(node != null) {
			if(node != head) {
				if(node == tail) {
					tail = node.prev;
				}
				node.deleteNode();
				head.addBefore(node);
				head = node;
			} 
			return node.value;
		}
		return null;
	}
	
	public void doCleanUp() {
		doCleanUp(cleanUpSize);
	}
	
	public synchronized void doCleanUp(int num) {
		while(num>0 && size > 0) {
			Node<K,V> nodeToBeDeleted = tail;
			tail = tail.prev;
			
			nodeToBeDeleted.deleteNode();
			
			map.remove(nodeToBeDeleted.key);
			
			size--;
			num--;
		}
	}
	
	public synchronized int size() {
		return size;
	}
	
	private static class Node<K,V> {
		private Node<K,V> prev;
		private Node<K,V> next;
		private K key;
		private V value;
		
		public Node(K key, V value) {
			this(key,value,null,null);
		}
		
		public Node(K key, V value, Node<K,V> next, Node<K,V> prev) {
			this.key = key;
			this.value = value;
			this.next = next;
			this.prev = prev;
		}
		
		public void addBefore(Node<K,V> newNode) {
			if(newNode != null) {
				newNode.next = this;
				newNode.prev = this.prev;
				if(this.prev != null) {
					this.prev.next = newNode;
				}
				this.prev = newNode;
				logger.debug("Added new node:{} before node:{}",newNode,this);
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
			return "key:"+this.key+", value:"+this.value;
		}
	}
}
