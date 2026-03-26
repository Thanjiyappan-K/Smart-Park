package com.smartpark.auth.service;

import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import com.smartpark.user.enums.UserStatus;
import com.smartpark.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional
    public void verifyOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.PARKING_OWNER) {
            throw new RuntimeException("Not a parking owner");
        }

        owner.setStatus(UserStatus.ACTIVE);
        userRepository.save(owner);
    }

    public List<User> getPendingOwners() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.PARKING_OWNER
                        && user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .toList();
    }

    @Transactional
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
    }

    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            user.setStatus(user.getRole() == Role.PARKING_OWNER
                    ? UserStatus.PENDING_VERIFICATION
                    : UserStatus.ACTIVE);
            userRepository.save(user);
        }
    }
}
