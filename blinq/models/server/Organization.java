package com.blinq.models.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Holds the user oragnization details includes:
 * Is primary, Name, Start Date, Title, Is Current.
 * For example:
 * Blinq, 2014-02, CTO, true, true
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization {

    private boolean isPrimary;
    private String name;
    private String startDate;
    private String endDate;
    private String title;
    private boolean isCurrent;

    public boolean isPrimary() {
        return isPrimary;
    }

    @JsonProperty("isPrimary")
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getStartDate() {
        return startDate;
    }

    @JsonProperty("startDate")
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    @JsonProperty("endDate")
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    @JsonProperty("isCurrent")
    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }


}
