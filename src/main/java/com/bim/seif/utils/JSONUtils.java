package com.bim.seif.utils;

import com.bim.seif.models.dto.ResponseJSONModel;
import com.bim.seif.models.dto.UserRedisJSONModel;
import com.google.gson.Gson;
import lombok.Data;
@Data
public class JSONUtils {


    static ResponseJSONModel response = new ResponseJSONModel();
    static UserRedisJSONModel userRedis = new UserRedisJSONModel();
    static public String responseGeneratorJSON(Boolean admin, String msg, String JWT,String codeAuth, String role,String apiKey) {

        response.setAdmin(admin);
        response.setMsg(msg);
        response.setJWT(JWT);
        response.setRole(role);
        response.setOtp(codeAuth);
        response.setApiKey(apiKey);
        Gson gson = new Gson();
        return gson.toJson(response);
    }

    static public String userRedisManage(String role, int attempts, String JWT, String codeAuth, String apiKey) {

        userRedis.setRole(role);
        userRedis.setAttempts(attempts);
        userRedis.setJwt(JWT);
        userRedis.setCodeAuth(codeAuth);
        Gson gson = new Gson();
        return gson.toJson(userRedis);
    }


    static public String GeneratorJSON(Boolean admin, String msg, String JWT, String role) {
        response.setAdmin(admin);
        response.setMsg(msg);
        response.setJWT(JWT);
        response.setRole(role);
        Gson gson = new Gson();
        return gson.toJson(response);
    }

    static public String covertObjectToJSON(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    static public UserRedisJSONModel covertJSONToUserRedisJSONModel(String strJSON,UserRedisJSONModel userRedis) {
        Gson gson = new Gson();
        return gson.fromJson(strJSON,userRedis.getClass());
    }
}