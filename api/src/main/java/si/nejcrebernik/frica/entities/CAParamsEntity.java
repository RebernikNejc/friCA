package si.nejcrebernik.frica.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "ca_params")
public class CAParamsEntity {

    @Id
    @GeneratedValue
    private Integer id;

    private Integer keySize;
    private Integer validDays;

    private Boolean country;
    private Boolean state;
    private Boolean locality;
    private Boolean orzanization;
    private Boolean organizationalUnit;
    private Boolean commonName;
    private Boolean email;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getKeySize() {
        return keySize;
    }

    public void setKeySize(Integer keySize) {
        this.keySize = keySize;
    }

    public Integer getValidDays() {
        return validDays;
    }

    public void setValidDays(Integer validDays) {
        this.validDays = validDays;
    }

    public Boolean getCountry() {
        return country;
    }

    public void setCountry(Boolean country) {
        this.country = country;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public Boolean getLocality() {
        return locality;
    }

    public void setLocality(Boolean locality) {
        this.locality = locality;
    }

    public Boolean getOrzanization() {
        return orzanization;
    }

    public void setOrzanization(Boolean orzanization) {
        this.orzanization = orzanization;
    }

    public Boolean getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(Boolean organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public Boolean getCommonName() {
        return commonName;
    }

    public void setCommonName(Boolean commonName) {
        this.commonName = commonName;
    }

    public Boolean getEmail() {
        return email;
    }

    public void setEmail(Boolean email) {
        this.email = email;
    }
}
