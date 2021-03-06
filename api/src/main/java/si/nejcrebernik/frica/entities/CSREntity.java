package si.nejcrebernik.frica.entities;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "csr")
public class CSREntity {

    @Id
    @GeneratedValue
    private Integer id;
    @ManyToOne
    private CAParamsEntity caParamsEntity;
    private String email;
    private String name;
    private String surname;
    @ManyToOne
    private CountryEntity countryEntity;
    private String enrollmentId;
    @ManyToOne
    private StatusEntity statusEntity;
    private String token;
    private String encryptedToken;
    private LocalDateTime received;
    private LocalDateTime approved;
    private LocalDateTime delivered;
    private LocalDateTime rejected;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public CountryEntity getCountryEntity() {
        return countryEntity;
    }

    public void setCountryEntity(CountryEntity countryEntity) {
        this.countryEntity = countryEntity;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public StatusEntity getStatusEntity() {
        return statusEntity;
    }

    public void setStatusEntity(StatusEntity statusEntity) {
        this.statusEntity = statusEntity;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public void setEncryptedToken(String encryptedToken) {
        this.encryptedToken = encryptedToken;
    }

    public CAParamsEntity getCaParamsEntity() {
        return caParamsEntity;
    }

    public void setCaParamsEntity(CAParamsEntity caParamsEntity) {
        this.caParamsEntity = caParamsEntity;
    }

    public LocalDateTime getReceived() {
        return received;
    }

    public void setReceived(LocalDateTime received) {
        this.received = received;
    }

    public LocalDateTime getApproved() {
        return approved;
    }

    public void setApproved(LocalDateTime approved) {
        this.approved = approved;
    }

    public LocalDateTime getDelivered() {
        return delivered;
    }

    public void setDelivered(LocalDateTime delivered) {
        this.delivered = delivered;
    }

    public LocalDateTime getRejected() {
        return rejected;
    }

    public void setRejected(LocalDateTime rejected) {
        this.rejected = rejected;
    }
}
