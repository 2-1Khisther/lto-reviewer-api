package com.lto.api.entity;

import jakarta.persistence.*;

// Maps to tbl_content_domains (LAWS, SIGNS, DEFENSIVE, PENALTIES).
@Entity
@Table(name = "tbl_content_domains")
public class ContentDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer domainId;

    private String domainCode;
    private String domainName;
    private String description;
    private Boolean isActive = true;

    public Integer getDomainId() { return domainId; }
    public String getDomainCode() { return domainCode; }
    public String getDomainName() { return domainName; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
}
