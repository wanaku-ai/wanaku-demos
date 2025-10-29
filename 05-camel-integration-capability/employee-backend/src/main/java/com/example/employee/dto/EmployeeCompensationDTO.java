package com.example.employee.dto;

public class EmployeeCompensationDTO {
    public Long id;
    public String name;
    public SalaryDTO salary;

    public EmployeeCompensationDTO() {
    }

    public EmployeeCompensationDTO(Long id, String name, SalaryDTO salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }
}
