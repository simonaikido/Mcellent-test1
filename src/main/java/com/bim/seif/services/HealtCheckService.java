package com.bim.seif.services;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HealtCheckService {

   // @Autowired
    //AuthRepository authRepository;

    public String methodX(String var1){
       // com.bim.sief.models.User user = authRepository.findByName(var1);
        return "Ok-";// + user.getName();
    }
}
