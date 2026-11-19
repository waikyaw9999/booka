package com.booka.service;

import com.booka.domain.entity.User;
import com.booka.domain.enums.UserRole;
import com.booka.dto.AuthDtos.CreateStaffRequest;
import com.booka.dto.AuthDtos.StaffMemberResponse;
import com.booka.exception.BusinessException;
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
                .build();
        return toResponse(userRepository.save(staff));
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
                user.getHotelId());
    }
}
