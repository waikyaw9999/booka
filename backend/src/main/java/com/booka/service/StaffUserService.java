package com.booka.service;

import com.booka.domain.entity.User;
import com.booka.domain.enums.UserRole;
import com.booka.dto.AuthDtos.CreateStaffRequest;
import com.booka.dto.AuthDtos.ResetStaffPasswordRequest;
import com.booka.dto.AuthDtos.StaffMemberResponse;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.UserRepository;
import com.booka.security.BookaUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelService hotelService;

    public List<StaffMemberResponse> listStaff(BookaUserDetails actor) {
        requireManager(actor);
        Long hotelId = hotelService.requireStaffHotelId(actor.getHotelId());
        return userRepository.findByHotelIdOrderByRoleAscFullNameAsc(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StaffMemberResponse inviteStaff(BookaUserDetails actor, CreateStaffRequest request) {
        requireManager(actor);
        Long hotelId = hotelService.requireStaffHotelId(actor.getHotelId());
        hotelService.findHotel(hotelId);

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already registered");
        }

        User staff = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(UserRole.STAFF)
                .hotelId(hotelId)
                .active(true)
                .build();
        return toResponse(userRepository.save(staff));
    }

    @Transactional
    public StaffMemberResponse setActive(BookaUserDetails actor, Long userId, boolean active) {
        requireManager(actor);
        User target = requireSameHotelStaff(actor, userId);
        if (target.getId().equals(actor.getId())) {
            throw new BusinessException("You cannot deactivate your own account");
        }
        if (target.getRole() == UserRole.ADMIN && !active) {
            throw new BusinessException("Deactivate other managers via support for now");
        }
        target.setActive(active);
        return toResponse(userRepository.save(target));
    }

    @Transactional
    public StaffMemberResponse resetPassword(
            BookaUserDetails actor, Long userId, ResetStaffPasswordRequest request) {
        requireManager(actor);
        User target = requireSameHotelStaff(actor, userId);
        target.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return toResponse(userRepository.save(target));
    }

    private User requireSameHotelStaff(BookaUserDetails actor, Long userId) {
        Long hotelId = hotelService.requireStaffHotelId(actor.getHotelId());
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getHotelId() == null || !hotelId.equals(target.getHotelId())) {
            throw new BusinessException("User does not belong to your property");
        }
        if (target.getRole() != UserRole.STAFF && target.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Not a property team account");
        }
        return target;
    }

    private void requireManager(BookaUserDetails actor) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Only property managers can manage team accounts");
        }
    }

    private StaffMemberResponse toResponse(User user) {
        return new StaffMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getHotelId(),
                user.isActive());
    }
}
