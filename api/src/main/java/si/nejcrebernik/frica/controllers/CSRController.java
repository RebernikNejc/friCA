package si.nejcrebernik.frica.controllers;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import si.nejcrebernik.frica.CSR;
import si.nejcrebernik.frica.entities.CAParamsEntity;
import si.nejcrebernik.frica.entities.CSREntity;
import si.nejcrebernik.frica.repositories.CAParamsRepository;
import si.nejcrebernik.frica.repositories.CSRRepository;
import si.nejcrebernik.frica.repositories.CountryRepository;
import si.nejcrebernik.frica.repositories.StatusRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

import static si.nejcrebernik.frica.Constants.*;

@Controller
@RequestMapping(path = "/csr")
public class CSRController {

    @Autowired
    private CSRRepository csrRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CAParamsRepository caParamsRepository;

    @Value("${certs.folder}")
    private String certsFolder;

    @Value("${environment:onprem}")
    private String environment;

    @GetMapping
    public @ResponseBody CSR getCSR(@RequestHeader("id") Integer id,
                                    @RequestHeader("token") String token) {
        Optional<CSREntity> csrEntity = csrRepository.findById(id);
        if (csrEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else {
            if (!csrEntity.get().getToken().equals(token)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            } else {
                CSR csr = new CSR();
                csr.setId(csrEntity.get().getId());
                csr.setName(csrEntity.get().getName());
                csr.setSurname(csrEntity.get().getSurname());
                csr.setStatus(csrEntity.get().getStatusEntity());
                csr.setReceived(csrEntity.get().getReceived());
                csr.setApproved(csrEntity.get().getApproved());
                csr.setDelivered(csrEntity.get().getDelivered());
                csr.setRejected(csrEntity.get().getRejected());
                return csr;
            }
        }
    }

    @GetMapping(path = "/decrypt")
    public @ResponseBody String decrypt(@RequestHeader("data") String token,
                                            @RequestHeader("key") MultipartFile key) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidCipherTextException {
        byte[] tokenb = Base64.getDecoder().decode(token);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        InputStreamReader inputStreamReader = new InputStreamReader(key.getInputStream());
        PemReader pemReader = new PemReader(inputStreamReader);
        PemObject pemObject = pemReader.readPemObject();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        // encrypt token with public key
        PKCS1Encoding pkcs1Encoding = new PKCS1Encoding(new RSAEngine());
        RSAKeyParameters rsaKeyParameters = new RSAKeyParameters(true, rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
        pkcs1Encoding.init(false, rsaKeyParameters);
        // generate token
        //byte[] tokenb = new byte[pkcs1Encoding.getInputBlockSize()];
        //tokenb = token.getBytes();
        // decrypt
        byte[] decrypted = pkcs1Encoding.processBlock(tokenb, 0, tokenb.length);
        return Base64.getEncoder().encodeToString(decrypted);
    }

    @PostMapping
    public @ResponseBody CSR sendCSR(@RequestParam("csr") MultipartFile csr,
                                         @RequestHeader("email") String email,
                                         @RequestHeader("name") String name,
                                         @RequestHeader("surname") String surname) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidCipherTextException {

        // check parameters and required fields
        Optional<CAParamsEntity> params = caParamsRepository.findFirstByOrderByIdDesc();
        if (params.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // get certification request
        JcaPKCS10CertificationRequest request = new JcaPKCS10CertificationRequest(new PemReader(new InputStreamReader(csr.getInputStream())).readPemObject().getContent());
        // get public key and check key length
        RSAPublicKey rsaPublicKey = (RSAPublicKey) request.getPublicKey();
        if (rsaPublicKey.getModulus().bitLength() != params.get().getKeySize()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // check required parameters
        boolean b1 = params.get().getCountry() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_COUNTRY.equals(rdn.getFirst().getType().getId()));
        boolean b2 = params.get().getState() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_STATE.equals(rdn.getFirst().getType().getId()));
        boolean b3 = params.get().getLocality() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_LOCALITY.equals(rdn.getFirst().getType().getId()));
        boolean b4 = params.get().getOrzanization() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_ORGANIZATION.equals(rdn.getFirst().getType().getId()));
//        boolean b5 = params.get().getOrganizationalUnit() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_ORGANIZATIONAL_UNIT.equals(rdn.getFirst().getType().getId()));
        boolean b5 = false;
        boolean b6 = params.get().getCommonName() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_COMMON_NAME.equals(rdn.getFirst().getType().getId()));
        boolean b7 = params.get().getEmail() && Arrays.stream(request.getSubject().getRDNs()).noneMatch(rdn -> CSR_EMAIL.equals(rdn.getFirst().getType().getId()));
        b7 = false;
        if (b1 || b2 || b3 || b4 || b5 || b6 || b7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        CSR response = new CSR();

        CSREntity csrEntity = new CSREntity();
        csrEntity.setEmail(email);
        csrEntity.setName(name);
        csrEntity.setSurname(surname);
        csrEntity.setStatusEntity(statusRepository.findById(1).get());
        csrEntity.setCaParamsEntity(caParamsRepository.findFirstByOrderByIdDesc().get());
        csrRepository.save(csrEntity);

        // encrypt token with public key
        PKCS1Encoding pkcs1Encoding = new PKCS1Encoding(new RSAEngine());
        RSAKeyParameters rsaKeyParameters = new RSAKeyParameters(false, rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
        pkcs1Encoding.init(true, rsaKeyParameters);
        // generate token
        Random r = new Random();
        byte[] token = new byte[pkcs1Encoding.getInputBlockSize()];
        r.nextBytes(token);
        // encrypt
        byte[] encrypted = pkcs1Encoding.processBlock(token, 0, token.length);

        if ("onprem".equals(environment)) { // save to filesystem
            File directory = new File(certsFolder);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String filePath = certsFolder + "/" + csrEntity.getId() + ".csr";
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(csr.getBytes());
            fos.close();
        } else { // save to S3 bucket - only for testing purposes
            S3Client s3Client = S3Client.create();
            s3Client.putObject(PutObjectRequest.builder().bucket("frica").key(csrEntity.getId().toString() + ".csr").build(),
                    RequestBody.fromBytes(csr.getBytes()));
            s3Client.close();
        }

        csrEntity.setToken(Base64.getEncoder().encodeToString(token));
        csrEntity.setEncryptedToken(Base64.getEncoder().encodeToString(encrypted));
        csrEntity.setReceived(LocalDateTime.now());
        csrRepository.save(csrEntity);

        response.setId(csrEntity.getId());
        response.setEncryptedToken(Base64.getEncoder().encodeToString(encrypted));

        return response;
    }

    @PutMapping(path = "/{id}/reject")
    public @ResponseBody CSREntity rejectCSR(@PathVariable Integer id) {
        Optional<CSREntity> optionalCSREntity = csrRepository.findById(id);
        if (optionalCSREntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        CSREntity csrEntity = optionalCSREntity.get();
        csrEntity.setStatusEntity(statusRepository.findById(4).get());
        csrEntity.setRejected(LocalDateTime.now());
        csrRepository.save(csrEntity);

        return csrEntity;
    }

    @PutMapping(path = "/{id}/approve")
    public @ResponseBody CSREntity updateCSR(@PathVariable Integer id) throws IOException, InterruptedException {
        // TODO: authentication

        // get csr entry and ca params
        Optional<CSREntity> optionalCSREntity = csrRepository.findById(id);
        if (optionalCSREntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        CSREntity csrEntity = optionalCSREntity.get();
        CAParamsEntity caParamsEntity = csrEntity.getCaParamsEntity();

        // get csr (from filesystem/aws s3 bucket)
        S3Client s3Client = S3Client.create();
        if (!environment.equals("onprem")) {
            InputStream csr = s3Client.getObject(GetObjectRequest.builder().bucket("frica").key(id.toString() + ".csr").build());
            File f = new File(certsFolder + "/" + id.toString() + ".csr");
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(csr.readAllBytes());
            fos.close();

            InputStream caKey = s3Client.getObject(GetObjectRequest.builder().bucket("frica").key("ca_" + caParamsEntity.getId() + ".key").build());
            f = new File(certsFolder + "/ca_" + caParamsEntity.getId() + ".key");
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            fos = new FileOutputStream(f);
            fos.write(caKey.readAllBytes());
            fos.close();

            InputStream caCrt = s3Client.getObject(GetObjectRequest.builder().bucket("frica").key("ca_" + caParamsEntity.getId() + ".crt").build());
            f = new File(certsFolder + "/ca_" + caParamsEntity.getId() + ".crt");
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            fos = new FileOutputStream(f);
            fos.write(caCrt.readAllBytes());
            fos.close();
        }

        // openssl sign command
        // TODO: ca password environment parameter
        String command = String.format("openssl x509 -req -in %s.csr -CA %s.crt -CAkey %s.key -CAcreateserial -out %s.crt -days %s -passin pass:password",
                certsFolder + "/" + id,
                certsFolder + "/ca_" + caParamsEntity.getId(),
                certsFolder + "/ca_" + caParamsEntity.getId(),
                certsFolder + "/" + id,
                caParamsEntity.getValidDays());
        Process p = Runtime.getRuntime().exec(command);
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // save signed crt to s3
        if (!environment.equals("onprem")) {
            s3Client.putObject(PutObjectRequest.builder().bucket("frica").key(id + ".crt").build(),
                    RequestBody.fromBytes(
                            new FileInputStream(new File(certsFolder + "/" + id + ".crt")).readAllBytes())
            );
            s3Client.close();
        }

        // update status
        csrEntity.setStatusEntity(statusRepository.findById(2).get());
        csrEntity.setApproved(LocalDateTime.now());
        csrRepository.save(csrEntity);

        return csrEntity;
    }

    @GetMapping(path = "/crt", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getCRT(@RequestHeader("id") Integer id,
                                       @RequestHeader("token") String token) throws IOException {
        Optional<CSREntity> csrEntity = csrRepository.findById(id);
        if (!csrEntity.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else {
            if (!csrEntity.get().getToken().equals(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            } else {
                if (!csrEntity.get().getStatusEntity().getId().equals(2)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                } else {
                    FileInputStream fileInputStream = new FileInputStream(certsFolder + "/" + id + ".crt");
                    byte[] file = fileInputStream.readAllBytes();
                    csrEntity.get().setStatusEntity(statusRepository.findById(3).get());
                    csrEntity.get().setDelivered(LocalDateTime.now());
                    csrRepository.save(csrEntity.get());
                    return file;
                }
            }
        }
    }
}
