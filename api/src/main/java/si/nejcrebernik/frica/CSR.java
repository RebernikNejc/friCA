package si.nejcrebernik.frica;

import com.fasterxml.jackson.annotation.JsonInclude;
import si.nejcrebernik.frica.entities.StatusEntity;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CSR {

    private Integer id;
    private String email;
    private String name;
    private String surname;
    private String country;
    private String enrollmentId;
    private StatusEntity status;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public StatusEntity getStatus() {
        return status;
    }

    public void setStatus(StatusEntity status) {
        this.status = status;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public void setEncryptedToken(String encryptedToken) {
        this.encryptedToken = encryptedToken;
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
