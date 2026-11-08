package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMINISTRATOR")
public class Administrator extends User {

    private String securityClearance;

    protected Administrator() {}

    public Administrator(String name, String email, String passwordHash, String securityClearance) {
        super(name, email, passwordHash, Role.ADMINISTRATOR);
        this.securityClearance = securityClearance;
    }

    @Override
    public String getDashboardRoute() {
        return "/admin/command-center";
    }

    public String getSecurityClearance() { return securityClearance; }
}
