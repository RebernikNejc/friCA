package si.nejcrebernik.frica.controllers;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
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
import si.nejcrebernik.frica.entities.CSREntity;
import si.nejcrebernik.frica.repositories.CSRRepository;
import si.nejcrebernik.frica.repositories.CountryRepository;
import si.nejcrebernik.frica.repositories.StatusRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping(path = "/csr")
public class CSRController {

    @Autowired
    private CSRRepository csrRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Value("${db.requested.id}")
    private Integer requestedId;

    @Value("${db.delivered.id}")
    private Integer deliveredId;

    @Value("${certs.folder}")
    private String certsFolder;

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
                csr.setCountry(csrEntity.get().getCountryEntity().getName());
                csr.setEnrollmentId(csrEntity.get().getEnrollmentId());
                csr.setStatus(csrEntity.get().getStatusEntity().getName());
                return csr;
            }
        }
    }

    @PostMapping
    public @ResponseBody CSR sendCSR(@RequestParam("csr") MultipartFile csr,
                                         @RequestHeader("email") String email,
                                         @RequestHeader("name") String name,
                                         @RequestHeader("surname") String surname,
                                         @RequestHeader("country") Integer countryId,
                                         @RequestHeader("enrollmentId") String enrollmentId) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidCipherTextException {
        CSR response = new CSR();

        CSREntity csrEntity = new CSREntity();
        csrEntity.setEmail(email);
        csrEntity.setName(name);
        csrEntity.setSurname(surname);
        csrEntity.setCountryEntity(countryRepository.findById(countryId).get());
        csrEntity.setEnrollmentId(enrollmentId);
        csrEntity.setStatusEntity(statusRepository.findById(requestedId).get());
        csrRepository.save(csrEntity);

        // get public key
        PemReader pemReader = new PemReader(new InputStreamReader(csr.getInputStream()));
        JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = new JcaPKCS10CertificationRequest(pemReader.readPemObject().getContent());
        RSAPublicKey rsaPublicKey = (RSAPublicKey) jcaPKCS10CertificationRequest.getPublicKey();

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

        // saving to filesystem
        File directory = new File(certsFolder);
        if (!directory.exists()) {
            directory.mkdir();
        }
        String filePath = certsFolder + "/" + csrEntity.getId() + ".csr";
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(csr.getBytes());
        fos.close();

        // saving to S3 bucket
        S3Client s3Client = S3Client.create();
        s3Client.putObject(PutObjectRequest.builder().bucket("frica").key(csrEntity.getId().toString() + ".csr").build(),
                RequestBody.fromBytes(csr.getBytes()));
        s3Client.close();

        csrEntity.setToken(Base64.getEncoder().encodeToString(token));
        csrEntity.setEncryptedToken(Base64.getEncoder().encodeToString(encrypted));
        csrRepository.save(csrEntity);

        response.setId(csrEntity.getId());
        response.setEncryptedToken(Base64.getEncoder().encodeToString(encrypted));

        return response;
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
                    csrEntity.get().setStatusEntity(statusRepository.findById(deliveredId).get());
                    csrRepository.save(csrEntity.get());
                    return file;
                }
            }
        }
    }
}
