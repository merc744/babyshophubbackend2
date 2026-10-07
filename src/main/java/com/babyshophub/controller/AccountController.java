package com.babyshophub.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import com.babyshophub.dto.ProfileRequest;
import com.babyshophub.dto.AddressRequest;
import com.babyshophub.dto.AddressResponse;
import com.babyshophub.dto.ChangePasswordRequest;
import com.babyshophub.service.AuthService;
import com.babyshophub.service.AccountService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        return ResponseEntity.ok(accountService.getProfile(principal.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(principal.getName(), request));
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(Principal principal, @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(accountService.updateProfileImage(principal.getName(), image));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(principal.getName(), request));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(Principal principal) {
        return ResponseEntity.ok(accountService.getAddresses(principal.getName()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(Principal principal, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(accountService.addAddress(principal.getName(), request));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(Principal principal, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(accountService.updateAddress(principal.getName(), id, request));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(Principal principal, @PathVariable Long id) {
        accountService.deleteAddress(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
