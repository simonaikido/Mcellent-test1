package com.bim.seif.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OtpService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setHashValue(String hashName,String key, String value,long timeout, TimeUnit timeUnit) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.put(hashName,key,value);
        redisTemplate.expire(hashName,timeout, timeUnit);
    }

    public Long incrementHashValue(String hashName,String key,long timeout, TimeUnit timeUnit) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        Long intentos = hashOperations.increment(hashName, key, 1);
        redisTemplate.expire(hashName,timeout, timeUnit);
        return intentos;
    }

    public void deleteHashValue(String hashName,String key) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(hashName,key);
    }

    public Map<String, Object> getUserData(String hashName) {

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        return hashOperations.entries(hashName);
    }


    public void setValue( String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setValueWithExpiration(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    public Boolean deleteKey(String key) {
        return redisTemplate.delete(key);
    }

    public void refreshTimeToken(String key,int minuts) {
        log.info("Refrescando tiempo de expiración para clave: {} a {} minutos", key, minuts);
        redisTemplate.expire(key, minuts, TimeUnit.MINUTES);
    }
}
