package com.blinq.models.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.blinq.models.Platform;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactInfo {

    private String familyName;
    private String fullName;
    private String givenName;

    public String getFamilyName() {
        return familyName;
    }

    @JsonProperty("familyName")
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFullName() {
        return fullName;
    }

    @JsonProperty("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGivenName() {
        return givenName;
    }

    @JsonProperty("givenName")
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }


}
