package com.bim.seif.services;

import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.utils.JSONUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.bim.seif.models.dto.UserRedisJSONModel;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserRedisJSONModel userRedisJSONModel = new UserRedisJSONModel();

    public void setValue(String key, String value) {

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

    public void refreshTimeSession(String key,int minuts) {
        redisTemplate.expire(key, minuts, TimeUnit.MINUTES);
    }

    public void guardarUsuarioRedis(EmpleadoDto empleado, String role, int attempts, String jwt, String codeAuth, String apiKey,int minutes) {

        String userRedisManageJSON = JSONUtils.userRedisManage(role, attempts, jwt, codeAuth,apiKey);
        guardarValorLlaveRedis(empleado.getEmail(), userRedisManageJSON, minutes);
    }

    public void guardarValorLlaveRedis(String key, String value, int minutes) {
        setValueWithExpiration(key, value, minutes, TimeUnit.MINUTES);
    }

    public String obtenerCodigoAuthRedis(EmpleadoDto Empleado) {

        userRedisJSONModel = new UserRedisJSONModel();
        String userRedisJSON = getValue(Empleado.getEmail());
        userRedisJSONModel = JSONUtils.covertJSONToUserRedisJSONModel(userRedisJSON, userRedisJSONModel);
        return userRedisJSONModel.getCodeAuth();
    }

//    public void borrarOtp(Empleado Empleado,int expiration){
//        guardarUsuarioRedis(Empleado, Empleado.getRol().name(), 0, "", "", "", expiration);
//    }

}
