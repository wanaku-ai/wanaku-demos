package com.example.employee.resource;

import com.example.employee.dto.*;
import com.example.employee.entity.CompensationChange;
import com.example.employee.entity.Employee;
import com.example.employee.entity.PerformanceReview;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Path("/employee")
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @GET
    @Path("/{id}/information")
    @Transactional
    public Response getEmployeeInformation(@PathParam("id") Long id) {
        Employee employee = Employee.findById(id);

        if (employee == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        EmployeeInformationDTO dto = new EmployeeInformationDTO(
            employee.id,
            employee.name,
            employee.level,
            employee.daysInLevel
        );

        return Response.ok(dto).build();
    }

    @GET
    @Path("/{id}/reviews")
    @Transactional
    public Response getEmployeeReviews(@PathParam("id") Long id) {
        Employee employee = Employee.findById(id);

        if (employee == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<ReviewDTO> reviewDTOs = PerformanceReview
            .find("employee.id = ?1 order by year, period", id)
            .stream()
            .map(review -> {
                PerformanceReview pr = (PerformanceReview) review;
                return new ReviewDTO(pr.year, pr.period, pr.rating, pr.description);
            })
            .collect(Collectors.toList());

        EmployeeReviewsDTO dto = new EmployeeReviewsDTO(
            employee.id,
            employee.name,
            reviewDTOs
        );

        return Response.ok(dto).build();
    }

    @GET
    @Path("/{id}/compensation")
    @Transactional
    public Response getEmployeeCompensation(@PathParam("id") Long id) {
        Employee employee = Employee.findById(id);

        if (employee == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<SalaryChangeDTO> changes = CompensationChange
            .find("employee.id = ?1 order by year, period", id)
            .stream()
            .map(change -> {
                CompensationChange cc = (CompensationChange) change;
                return new SalaryChangeDTO(cc.year, cc.period, cc.adjustedTo, cc.currency);
            })
            .collect(Collectors.toList());

        SalaryDTO salaryDTO = new SalaryDTO(
            employee.currentSalary,
            employee.currency,
            changes
        );

        EmployeeCompensationDTO dto = new EmployeeCompensationDTO(
            employee.id,
            employee.name,
            salaryDTO
        );

        return Response.ok(dto).build();
    }
}
