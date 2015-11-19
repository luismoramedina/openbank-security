package org.openbank.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @author luismoramedina
 */
@RestController
public class DocumentRestController {
    @RequestMapping(value = "/document", produces = "application/json")
    public String doSomething() {
        String time = new Date().toString();
        return "{\"data\" : \"this is the " + time + "\"}";
    }
}
