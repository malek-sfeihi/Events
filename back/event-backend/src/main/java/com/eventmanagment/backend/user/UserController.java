package com.eventmanagment.backend.user;

import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.media.LocalMediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final LocalMediaStorageService mediaStorageService;

    @PostMapping("/me/photo")
    public ResponseEntity<UserPhotoResponse> uploadMyPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        User user = userRepository.findByEmail(authentication.getName().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String photoUrl = mediaStorageService.storeImage(file, "users");
        user.setPhotoUrl(photoUrl);
        userRepository.save(user);
        return ResponseEntity.ok(new UserPhotoResponse(photoUrl));
    }

    public record UserPhotoResponse(String photoUrl) {}
}
