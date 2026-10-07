package com.babyshophub.service;

import com.babyshophub.dto.*;
import com.babyshophub.entity.Address;
import com.babyshophub.entity.User;
import com.babyshophub.repository.AddressRepository;
import com.babyshophub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class AccountService {
    private static final String UPLOAD_DIR = "uploads/profile-images/";
    private final UserRepository users;
    private final AddressRepository addresses;
    public AccountService(UserRepository users, AddressRepository addresses) { this.users = users; this.addresses = addresses; }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) { return profile(user(email)); }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = user(email);
        user.setName(request.getName()); user.setPhoneNumber(request.getPhoneNumber()); user.setDob(request.getDob());
        return profile(users.save(user));
    }

    @Transactional
    public ProfileResponse updateProfileImage(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile image is required");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be 5 MB or smaller");
        }
        String extension = switch (String.valueOf(file.getContentType()).toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are supported");
        };
        try (java.io.InputStream input = file.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid image");
            }
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded image");
        }

        String storedUrl;
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String fileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
            }
            try (java.io.InputStream input = file.getInputStream()) {
                Files.copy(input, filePath);
            }
            storedUrl = "/uploads/profile-images/" + fileName;
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image file", e);
        }

        User user = user(email);
        user.setProfileImageUrl(storedUrl);
        return profile(users.save(user));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String email) { return addresses.findAllByUserEmailOrderByAddressId(email).stream().map(this::address).toList(); }

    @Transactional
    public AddressResponse addAddress(String email, AddressRequest request) {
        User user = user(email); Address address = new Address(); address.setUser(user); apply(address, request, email); return address(addresses.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(String email, Long id, AddressRequest request) {
        Address address = ownedAddress(email, id); apply(address, request, email); return address(addresses.save(address));
    }

    @Transactional
    public void deleteAddress(String email, Long id) { addresses.delete(ownedAddress(email, id)); }

    private User user(String email) { return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")); }
    private Address ownedAddress(String email, Long id) { return addresses.findByAddressIdAndUserEmail(id, email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found")); }
    private ProfileResponse profile(User user) { return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(), user.getDob(), user.getProfileImageUrl()); }
    private AddressResponse address(Address a) { return new AddressResponse(a.getAddressId(), a.getLabel(), a.getLine1(), a.getCity(), a.getState(), a.getCountry(), a.getPostalCode(), a.isDefault()); }
    private void apply(Address address, AddressRequest request, String email) {
        address.setLabel(request.getLabel()); address.setLine1(request.getLine1()); address.setCity(request.getCity()); address.setState(request.getState());
        address.setCountry(request.getCountry()); address.setPostalCode(request.getPostalCode()); address.setDefault(request.isDefault());
        if (request.isDefault()) addresses.findAllByUserEmailOrderByAddressId(email).stream().filter(a -> !a.getAddressId().equals(address.getAddressId())).forEach(a -> a.setDefault(false));
    }
}
