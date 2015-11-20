package org.openbank.controllers;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author luismoramedina
 */
@RestController
public class DocumentRestController {

    private static final String PATH_TO_KEYSTORE = "/openbank.jks";
    private static final String KEYSTORE_PASSWORD = "123123123";
    private static final String KEY_ALIAS_IN_KEYSTORE = "openbank-server";
    private static final String SIGNATUREALGO = "SHA1withRSA";

    private static final String LATCH_APP_ID = "XwdV4gNMLDGM7a9wiAiY";
    private static final String LATCH_SECRET = "48ujUr3kBpnF8DXQWxpWKzkWuu8pBCZXg8kE2w4N";
    private static final String LATCH_OPERATION_ID_DOCUMENT = "m2dTR23x67PF3jgCsU3q";
    private static final String LATCH_OPERATION_ID_CONFIRM = "AdeGmfs9yKVkLi2rWKnX";
    private static String base64file;
    private static byte[] fileBytes;

    private static String accountId = "Ujsqpn6xCZgVKarJ36hYmjY2zdFnJgmGMWHK6cGMeLXP6racvjTpXqJGnURQVJuk";

    static {
        InputStream resourceAsStream = DocumentRestController.class.getResourceAsStream("/docs/a-file-to-sign.txt");
        try {
            byte[] b = new byte[resourceAsStream.available()];
            resourceAsStream.read(b);
            resourceAsStream.close();
            base64file = Base64.encodeBase64String(b);
            fileBytes = b;
        } catch (IOException e) {
            e.printStackTrace();
        }

        Security.addProvider(new BouncyCastleProvider());
    }

    @RequestMapping ( value = "/confirm", produces = "application/json" )
    public
    @ResponseBody
    ResponseEntity<String> confirm(
            @RequestParam ( "confirmation_text" )
            String confirm) {

        if (!checkLatchStatus(accountId, LATCH_OPERATION_ID_CONFIRM)) {
            return new ResponseEntity<String>("{\"result\":\"Operation locked by Latch\"}", HttpStatus.UNAUTHORIZED);
        }

        if (confirm.equalsIgnoreCase("ok")) {
            return new ResponseEntity<String>("{\"result\":\"Document sign is OK\"}", HttpStatus.OK);
        }
        else {
            return new ResponseEntity<String>("{\"result\":\"Document rejected\"}", HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/document", produces = "application/json")
    public ResponseEntity<String> document(@RequestParam("id") String documentId) throws Exception {
//        if (!checkLatchStatus(accountId, LATCH_OPERATION_ID_DOCUMENT)) {
//            return new ResponseEntity<String>("{\"result\":\"Operation locked by Latch\"}", HttpStatus.UNAUTHORIZED);
//        }
        String signedB64Document = signDocument(fileBytes);

        return new ResponseEntity<String>("{\"data\" : \"" + signedB64Document + "\", \"id\" : \"" + documentId + "\"}", HttpStatus.OK);
    }

    private KeyStore loadKeyStore() throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        InputStream is = DocumentRestController.class.getResourceAsStream(PATH_TO_KEYSTORE);
        keystore.load(is, KEYSTORE_PASSWORD.toCharArray());
        return keystore;
    }

    private CMSSignedDataGenerator setupProvider(KeyStore keystore) throws Exception {
        Certificate[] certchain = keystore.getCertificateChain(KEY_ALIAS_IN_KEYSTORE);

        final List<Certificate> certlist = new ArrayList<Certificate>();

        for (int i = 0, length = certchain == null ? 0 : certchain.length; i < length; i++) {
            certlist.add(certchain[i]);
        }

        Store certstore = new JcaCertStore(certlist);

        Certificate cert = keystore.getCertificate(KEY_ALIAS_IN_KEYSTORE);

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATUREALGO).setProvider("BC").
                build((PrivateKey) (keystore.getKey(KEY_ALIAS_IN_KEYSTORE, KEYSTORE_PASSWORD.toCharArray())));

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

        generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").
                build()).build(signer, (X509Certificate) cert));

        generator.addCertificates(certstore);

        return generator;
    }

    byte[] signPkcs7(final byte[] content, final CMSSignedDataGenerator generator) throws Exception {
        CMSTypedData cmsdata = new CMSProcessableByteArray(content);
        CMSSignedData signeddata = generator.generate(cmsdata, true);
        return signeddata.getEncoded();
    }

    private String signDocument(byte[] bytesDocument) throws Exception {
        KeyStore keystore = loadKeyStore();
        CMSSignedDataGenerator signatureGenerator = setupProvider(keystore);
        byte[] signedBytes = signPkcs7(bytesDocument, signatureGenerator);
        return Base64.encodeBase64String(signedBytes);
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

    /**
     * @param token this code is given to the user by the application
     */
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
