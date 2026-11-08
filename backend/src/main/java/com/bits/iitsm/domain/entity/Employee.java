package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    private String department;
    private String officeLocation;

    protected Employee() {}

    public Employee(String name, String email, String passwordHash, String department, String officeLocation) {
        super(name, email, passwordHash, Role.EMPLOYEE);
        this.department = department;
        this.officeLocation = officeLocation;
    }

    @Override
    public String getDashboardRoute() {
        return "/employee/portal";
    }

    public String getDepartment() { return department; }
    public String getOfficeLocation() { return officeLocation; }
}
