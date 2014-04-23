package com.crmlytics.dynamicshard.datastore;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Hashing;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class RedisDataStore {
	private static final Logger logger = LoggerFactory.getLogger(RedisDataStore.class);
	private static final String defaultConfigFileName = "\redis.yml";

	private ShardedJedisPool redisPool;
	
	public RedisDataStore() {
		this(readConfig());
	}
	
	public RedisDataStore(List<JedisShardInfo> shards)
	{
		redisPool = new ShardedJedisPool(new JedisPoolConfig(), shards , Hashing.MURMUR_HASH, ShardedJedis.DEFAULT_KEY_TAG_PATTERN);
		logger.info("Redis Service is up. Connected to "+shards.size()+" shards.");
	}
	
	private static class ShardConfig {
		public String host;
		public int port;
		public int timeout;
		public int weight;
		
		@Override
		public String toString() {
			return "Host:"+host+",port:"+port+",timeout:"+timeout+",weight+"+weight;
		}
		
		public JedisShardInfo getJedisShardInfoObj() {
			return new JedisShardInfo(host, port, timeout,weight);
		}
	}
	
	private static List<JedisShardInfo> readConfig() {
		List<JedisShardInfo> shards = new LinkedList<JedisShardInfo>();
		YamlReader reader;
		try {
			reader = new YamlReader(new FileReader(defaultConfigFileName));
		} catch (FileNotFoundException fnfe) {
			throw new RuntimeException(fnfe);
		}
		
		reader.getConfig().setClassTag("redis",ShardConfig.class);
		
		logger.info("Reading redis shard config from:"+defaultConfigFileName);
		ShardConfig shard;
		try {
			do {
				shard = reader.read(ShardConfig.class);
				if(shard != null) {
					logger.info("Read shard with value:"+shard);
					shards.add(shard.getJedisShardInfoObj());
				}
			} while(shard != null);
		} catch (YamlException ye) {
			throw new RuntimeException(ye);
		}
		logger.info("Redius shard config read successfully. Loaded "+shards.size()+" shard info");
		return shards;
	}
	
	public static void main(String args[]) {
		readConfig();
	}
	
}

