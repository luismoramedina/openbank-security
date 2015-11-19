package org.openbank.controllers;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * @author luismoramedina
 */
@RestController
public class DocumentRestController {

    private static String base64file;

    static {
        InputStream resourceAsStream = DocumentRestController.class.getResourceAsStream("/docs/a-file-to-sign.txt");
        try {
            byte[] b = new byte[resourceAsStream.available()];
            resourceAsStream.read(b);
            resourceAsStream.close();
            base64file = Base64.encodeBase64String(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @RequestMapping(value = "/document", produces = "application/json")
    public String doSomething() {
        String time = new Date().toString();
//        return "{\"data\" : \"this is the " + time + "\"}";
        return "{\"data\" : \"" + base64file + "\"}";
    }
}
