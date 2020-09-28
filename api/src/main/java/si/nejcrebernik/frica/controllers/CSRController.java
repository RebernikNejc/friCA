package si.nejcrebernik.frica.controllers;

import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import si.nejcrebernik.frica.CSR;
import si.nejcrebernik.frica.repositories.*;
import si.nejcrebernik.frica.entities.CSREntity;

import java.io.*;
import java.util.Optional;

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
    public @ResponseBody CSR getCSR(@RequestParam("id") Integer id,
                                    @RequestParam("token") String token) {
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
                                         @RequestHeader("enrollmentId") String enrollmentId) throws IOException {
        CSR response = new CSR();

        CSREntity csrEntity = new CSREntity();
        csrEntity.setEmail(email);
        csrEntity.setName(name);
        csrEntity.setSurname(surname);
        csrEntity.setCountryEntity(countryRepository.findById(countryId).get());
        csrEntity.setEnrollmentId(enrollmentId);
        csrEntity.setStatusEntity(statusRepository.findById(requestedId).get());
        String token = RandomString.make(16);
        csrEntity.setToken(token);
        csrRepository.save(csrEntity);

        response.setId(csrEntity.getId());
        response.setToken(token);

        File directory = new File(certsFolder);
        if (!directory.exists()) {
            directory.mkdir();
        }
        String filePath = certsFolder + "/" + csrEntity.getId() + ".csr";
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(csr.getBytes());
        fos.close();

        csrEntity.setFilePath(filePath);
        csrRepository.save(csrEntity);

        return response;
    }

    @GetMapping(path = "/crt", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getCRT(@RequestParam("id") Integer id,
                                       @RequestParam("token") String token) throws IOException {
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
