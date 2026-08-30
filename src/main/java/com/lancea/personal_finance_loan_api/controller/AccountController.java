package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.AccountRequest;
import com.lancea.personal_finance_loan_api.dto.request.AccountUpdateRequest;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account creation and other operations" )
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "Create an account",
            description = "Returns an account response with the specified details regarding the created account"
    )

    @ApiResponses({
            @ApiResponse( responseCode = "201", description = "Account was created",
                content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse( responseCode = "404", description = "User account was not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse( responseCode = "400", description = "Request body failed validation or the JWT contains an empty userId claim",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),

    })

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount( @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "The request that contains the account info")
            @Valid @RequestBody AccountRequest accountRequest){

        AccountResponse response = accountService.createAccount(jwt, accountRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Get all user accounts",
            description = "Returns a a list of active accounts of the user"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrieving the accounts was successful",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)))),
            @ApiResponse( responseCode = "400", description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getAllAccounts(jwt));
    }

    @Operation(
            summary = "Get the account using its ID",
            description = "Returns the account information using its ID"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrieving of the account was successful",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)))),
            @ApiResponse( responseCode = "404", description = "Account was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse( responseCode = "400", description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "ID of the account that needs to be retrieved", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getAccountById(accountId, jwt ));

    }

    @Operation(
            summary = "Update a selected account's information",
            description = "Updates the account name and type based on the value the user provided"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account information was updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse( responseCode = "404", description = "Account was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse( responseCode = "400", description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })


    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(

            @Parameter(description = "ID of the account that needs to be updated", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID accountId,
            @Parameter(description = "Contains the updated information regarding the account")
            @Valid @RequestBody AccountUpdateRequest updateRequest,
            @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(accountService.updateAccount(accountId, updateRequest, jwt));

    }

    @Operation(
            summary = "Close an account",
            description = "Closes the provided account as long as it doesn't have a remaining balance"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account information was closed successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse( responseCode = "404", description = "Account was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse( responseCode = "400", description = "Account still has a positive balance or JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @PatchMapping("/{accountId}/close")
    public ResponseEntity<AccountResponse> closeAccount(
            @Parameter(description = "ID of the account that needs to be updated", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt)  {

        return ResponseEntity.ok(accountService.closeAccount(accountId, jwt));
    }

    @Operation(
            summary = "Delete an account",
            description = "Mark the selected account as deleted"
    )

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse( responseCode = "404", description = "Account was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse( responseCode = "400", description = "JWT contains an empty userId claim",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @Parameter(description = "ID of the account that needs to be updated", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt) {

        accountService.deleteAccount(accountId, jwt);
        return ResponseEntity.noContent().build();
    }

}

