package org.openbank.controllers;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
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

    @RequestMapping(value = "/confirm", method = RequestMethod.POST)
    public
    @ResponseBody
    String confirm(@RequestParam("confirmation_text") String confirm) {
        if (confirm.equalsIgnoreCase("ok")) {
            return "Document sign is OK";
        } else {
            return "Document rejected";
        }
    }

    @RequestMapping(value = "/document", produces = "application/json")
    public String doSomething(@RequestParam("id") String documentId) {
        String time = new Date().toString();
        //        return "{\"data\" : \"this is the " + time + "\"}";

        return "{\"data\" : \"" + base64file + "\", \"id\" : \"" + documentId + "\"}";
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
}
