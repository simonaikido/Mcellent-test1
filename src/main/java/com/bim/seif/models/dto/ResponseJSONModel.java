package com.bim.seif.models.dto;

import lombok.Data;
@Data
public class ResponseJSONModel {
    private Boolean admin = false;
    private String msg = "";
    private String JWT = "";
    private String role ="";
    private String otp = "";
    private String apiKey = "";
}
