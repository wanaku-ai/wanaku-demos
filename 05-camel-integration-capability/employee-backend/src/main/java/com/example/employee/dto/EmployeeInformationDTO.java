package com.example.employee.dto;

public class EmployeeInformationDTO {
    public Long id;
    public String name;
    public String level;
    public Integer daysInLevel;

    public EmployeeInformationDTO() {
    }

    public EmployeeInformationDTO(Long id, String name, String level, Integer daysInLevel) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.daysInLevel = daysInLevel;
    }
}
