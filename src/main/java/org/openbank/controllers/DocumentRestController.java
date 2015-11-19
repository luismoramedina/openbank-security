package org.openbank.controllers;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Date;

/**
 * @author luismoramedina
 */
@RestController
public class DocumentRestController {

    private static final String LATCH_APP_ID = "XwdV4gNMLDGM7a9wiAiY";
    private static final String LATCH_SECRET = "48ujUr3kBpnF8DXQWxpWKzkWuu8pBCZXg8kE2w4N";
    private static final String LATCH_OPERATION_ID_DOCUMENT = "m2dTR23x67PF3jgCsU3q";
    private static final String LATCH_OPERATION_ID_CONFIRM = "AdeGmfs9yKVkLi2rWKnX";
    private static String base64file;

    private static String accountId = "Ujsqpn6xCZgVKarJ36hYmjY2zdFnJgmGMWHK6cGMeLXP6racvjTpXqJGnURQVJuk";

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

    @RequestMapping ( value = "/confirm", produces = "application/json" )
    public
    @ResponseBody
    ResponseEntity<String> confirm(
            @RequestParam ( "confirmation_text" )
            String confirm) {

        if (!checkLatchStatus(accountId, LATCH_OPERATION_ID_CONFIRM)) {
            return new ResponseEntity<String>("{\"result\":\"Operation locked by Latch\"}", HttpStatus.FORBIDDEN);
        }

        if (confirm.equalsIgnoreCase("ok")) {
            return new ResponseEntity<String>("{\"result\":\"Document sign is OK\"}", HttpStatus.OK);
        }
        else {
            return new ResponseEntity<String>("{\"result\":\"Document rejected\"}", HttpStatus.OK);
        }
    }

    @RequestMapping ( value = "/document", produces = "application/json" )
    public ResponseEntity<String> document(
            @RequestParam ( "id" )
            String documentId) {

        if (!checkLatchStatus(accountId, LATCH_OPERATION_ID_DOCUMENT)) {
            return new ResponseEntity<String>("{\"result\":\"Operation locked by Latch\"}", HttpStatus.FORBIDDEN);
        }

        String time = new Date().toString();
        //        return "{\"data\" : \"this is the " + time + "\"}";

        return new ResponseEntity<String>("{\"data\" : \"" + base64file + "\", \"id\" : \"" + documentId + "\"}", HttpStatus.OK);
    }

    /**
     * Upload single file
     */
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public
    @ResponseBody
    String uploadFileHandler(@RequestParam("name") String name, @RequestParam("file") MultipartFile file) {

        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();

                // Creating the directory to store file
                String rootPath = System.getProperty("catalina.home");
                File dir = new File(rootPath + File.separator + "tmpFiles");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Create the file on server
                File serverFile = new File(dir.getAbsolutePath() + File.separator + name);
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
                stream.write(bytes);
                stream.close();

                System.out.println("Server File Location=" + serverFile.getAbsolutePath());

                return "You successfully uploaded file=" + name;
            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + name + " because the file was empty.";
        }
    }

    @RequestMapping(value = "/pair", produces = "application/json")
    public String pairDevice(@RequestParam("token") String token) {
        accountId = pair(token);
        System.out.println("AccountId:" + accountId);
        return "{\"accountId\" : \"" + accountId + "\"}";
    }

    @RequestMapping(value = "/unpair", produces = "application/json")
    public String unpairDevice() {
        return unpair();
    }

    private boolean checkLatchStatus(String accountId, String operationId) {
        boolean isAllowed = true;
        if (!accountId.isEmpty()) {
            LatchApp latch = new LatchApp(LATCH_APP_ID, LATCH_SECRET);
            LatchResponse latchResponse = latch.status(accountId, operationId);
            if (latchResponse != null && latchResponse.getData() != null) {
                String status = latchResponse.getData().get("operations").getAsJsonObject().get(operationId).getAsJsonObject().get("status").getAsString();
                if (status.equals("off")) {
                    isAllowed = false;
                }
            }
        }
        return isAllowed;
    }

    public String pair(String token) {
        LatchApp latch = new LatchApp(LATCH_APP_ID, LATCH_SECRET);
        LatchResponse latchResponse = latch.pair(token);
        if (latchResponse != null) {
            if (latchResponse.getData() != null) {
                return accountId = latchResponse.getData().get("accountId").getAsString();
            }
            else {
                return null;
            }
        }
        return null;
    }


    public String unpair() {
        if (accountId == null){
            return "{\"result\":\"no pair application\"}";
        }
        LatchApp latch = new LatchApp(LATCH_APP_ID, LATCH_SECRET);
        LatchResponse latchResponse = latch.unpair(accountId);
        if (latchResponse != null && latchResponse.getError() == null) {
            String json = latchResponse.toJSON().toString();
            return "{\"result\":\"ok unpair\"}";
        }
        else {
            return "{\"result\":\"ko unpair\"}";
        }
    }

}
