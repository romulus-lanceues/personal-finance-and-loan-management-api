package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.LoanRequest;
import com.lancea.personal_finance_loan_api.dto.response.*;
import com.lancea.personal_finance_loan_api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Loan creation and actions")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @Operation(
            summary = "Creates a Loan",
            description = "Creates a loan for the user and its amortization schedules ",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @ApiResponses({
            @ApiResponse (
                    responseCode = "201",
                    description = "The loan and its schedules are created",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request body failed validation or JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User or account not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })

    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(
            @Parameter(description = "The request that contains the loan info")
            @Valid @RequestBody LoanRequest loanRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){

        LoanResponse response = loanService.createLoan(loanRequest, jwt);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.loanId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Get user loans",
            description = "Retrieves a paginated list of existing loans for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Retrieving the loan was successful",
                    content = @Content(schema = @Schema(implementation = PagedLoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })

    @GetMapping
    public ResponseEntity<PagedLoanResponse> getUserLoans(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "disbursedAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanService.getUserLoans(pageable,jwt));
    }

    @Operation(
            summary = "Get loan by ID",
            description = "Retrieves detailed information for a specific loan belonging to the user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Retrieving the loans was successful",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Loan was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
    })

    @GetMapping("/{loanId}")
    ResponseEntity<LoanResponse> getLoanById(
            @Parameter(description = "ID of the loan that needs to be retrieved", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID loanId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanService.getLoanById(loanId, jwt));
    }

    @Operation(
            summary = "Compare two existing loans",
            description = "Compares terms, interest, and monthly obligations between two existing loans.",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "The loans were compared successfully",
                    content = @Content(schema = @Schema(implementation = LoanComparisonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Loan was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })

    @GetMapping("/compare")
    ResponseEntity<LoanComparisonResponse> compareLoans(
            @Parameter(description = "ID of the first loan", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam UUID loanAId,
            @Parameter(description = "ID of the second loan", example = "4ba85f64-5717-4562-b3fc-2c963f66afa7")
            @RequestParam UUID loanBId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(loanService.compareLoan(loanAId, loanBId, jwt));
    }

    @Operation(
            summary = "Simulate extra payment on loan",
            description = "Simulates remaining amortization schedule and computes interest savings if an extra payment is applied.",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "The estimation was successful",
                    content = @Content(schema = @Schema(implementation = LoanSimulationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Extra amount is non-positive, or target installment is already paid" ,
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Loan or target schedule installment number not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
    })

    @PostMapping("/{loanId}/simulate")
    ResponseEntity<LoanSimulationResponse> simulatePayment(
            @Parameter(description = "ID of the loan to be simulated", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID loanId,
            @Parameter(description = "The schedule number of where the user would like the simulation to start", example = "1")
            @RequestParam int paymentNumber,
            @Parameter(description = "The extra amount the user would like to pay on this schedule", example = "500.00")
            @RequestParam BigDecimal extraAmount,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {


        return ResponseEntity.ok(loanService.simulatePayment(loanId, paymentNumber, extraAmount, jwt));
    }
}
