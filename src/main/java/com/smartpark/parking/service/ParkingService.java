package com.smartpark.parking.service;

import com.smartpark.parking.dto.*;
import com.smartpark.parking.entity.AdminAction;
import com.smartpark.parking.entity.ParkingAvailability;
import com.smartpark.parking.entity.ParkingImage;
import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.enums.AdminActionType;
import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.enums.VehicleType;
import com.smartpark.parking.exception.ParkingErrorCode;
import com.smartpark.parking.exception.ParkingException;
import com.smartpark.parking.repository.AdminActionRepository;
import com.smartpark.parking.repository.ParkingAvailabilityRepository;
import com.smartpark.parking.repository.ParkingImageRepository;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import com.smartpark.user.enums.UserStatus;
import com.smartpark.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingService implements ParkingSlotUpdateHook {

    private static final double LAT_MIN = -90;
    private static final double LAT_MAX = 90;
    private static final double LON_MIN = -180;
    private static final double LON_MAX = 180;

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingAvailabilityRepository availabilityRepository;
    private final ParkingImageRepository imageRepository;
    private final AdminActionRepository adminActionRepository;
    private final UserRepository userRepository;
    private final ParkingAvailabilityCache availabilityCache;
    private final ParkingDashboardProvider dashboardProvider;

    private void validateCoordinates(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) return;
        double la = lat.doubleValue();
        double lo = lon.doubleValue();
        if (la < LAT_MIN || la > LAT_MAX || lo < LON_MIN || lo > LON_MAX) {
            throw new ParkingException(ParkingErrorCode.INVALID_COORDINATES);
        }
    }

    private void ensureOwner(User user, Long ownerId) {
        if (!user.getRole().equals(Role.PARKING_OWNER) || !user.getId().equals(ownerId)) {
            throw new ParkingException(ParkingErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void ensureOwnerOfParking(User user, ParkingSpace parking) {
        if (!user.getRole().equals(Role.PARKING_OWNER) || !parking.getOwnerId().equals(user.getId())) {
            throw new ParkingException(ParkingErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void ensureAdmin(User user) {
        if (!user.getRole().equals(Role.ADMIN)) {
            throw new ParkingException(ParkingErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private boolean isVisibleToDriver(ParkingSpace p) {
        return VerificationStatus.APPROVED.equals(p.getVerificationStatus())
                && Boolean.TRUE.equals(p.getIsPublished())
                && Boolean.TRUE.equals(p.getIsActive());
    }

    @Transactional
    public ParkingResponse create(User user, CreateParkingRequest req) {
        ensureOwner(user, user.getId());
        validateCoordinates(req.getLatitude(), req.getLongitude());
        if (user.getRole() != Role.PARKING_OWNER) {
            throw new ParkingException(ParkingErrorCode.FORBIDDEN_ACCESS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ParkingException(ParkingErrorCode.FORBIDDEN_ACCESS, "Owner must be verified (ACTIVE)");
        }
        ParkingSpace parking = ParkingSpace.builder()
                .ownerId(user.getId())
                .name(req.getName())
                .address(req.getAddress())
                .city(req.getCity())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .totalSlots(req.getTotalSlots())
                .availableSlots(req.getTotalSlots())
                .pricePerHour(req.getPricePerHour())
                .vehicleType(req.getVehicleType() != null ? req.getVehicleType() : VehicleType.ANY)
                .verificationStatus(VerificationStatus.PENDING)
                .isPublished(false)
                .isActive(false)
                .version(0L)
                .build();
        parking = parkingSpaceRepository.save(parking);
        if (req.getAvailabilityRules() != null && !req.getAvailabilityRules().isEmpty()) {
            for (AvailabilitySlotDto dto : req.getAvailabilityRules()) {
                ParkingAvailability av = ParkingAvailability.builder()
                        .parkingSpace(parking)
                        .dayOfWeek(dto.getDayOfWeek())
                        .openTime(dto.getOpenTime())
                        .closeTime(dto.getCloseTime())
                        .isAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : true)
                        .build();
                availabilityRepository.save(av);
            }
        }
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                String url = req.getImageUrls().get(i);
                ParkingImage img = ParkingImage.builder()
                        .parkingSpace(parking)
                        .imageUrl(url)
                        .isPrimary(i == 0)
                        .build();
                imageRepository.save(img);
            }
        }
        return toResponse(parking, null);
    }

    @Transactional
    public ParkingResponse update(User user, Long parkingId, UpdateParkingRequest req) {
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        ensureOwnerOfParking(user, parking);
        if (req.getLatitude() != null || req.getLongitude() != null) {
            validateCoordinates(
                    req.getLatitude() != null ? req.getLatitude() : parking.getLatitude(),
                    req.getLongitude() != null ? req.getLongitude() : parking.getLongitude());
        }
        if (req.getTotalSlots() != null && req.getTotalSlots() < parking.getTotalSlots()) {
            int minSlots = parking.getTotalSlots() - parking.getAvailableSlots();
            if (req.getTotalSlots() < minSlots) {
                throw new ParkingException(ParkingErrorCode.CANNOT_REDUCE_SLOTS_BELOW_BOOKINGS);
            }
            parking.setAvailableSlots(parking.getAvailableSlots() - (parking.getTotalSlots() - req.getTotalSlots()));
            parking.setTotalSlots(req.getTotalSlots());
        }
        if (req.getName() != null) parking.setName(req.getName());
        if (req.getAddress() != null) parking.setAddress(req.getAddress());
        if (req.getCity() != null) parking.setCity(req.getCity());
        if (req.getLatitude() != null) parking.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) parking.setLongitude(req.getLongitude());
        if (req.getPricePerHour() != null) parking.setPricePerHour(req.getPricePerHour());
        if (req.getVehicleType() != null) parking.setVehicleType(req.getVehicleType());
        if (req.getAvailabilityRules() != null) {
            availabilityRepository.deleteByParkingSpaceId(parking.getId());
            for (AvailabilitySlotDto dto : req.getAvailabilityRules()) {
                ParkingAvailability av = ParkingAvailability.builder()
                        .parkingSpace(parking)
                        .dayOfWeek(dto.getDayOfWeek())
                        .openTime(dto.getOpenTime())
                        .closeTime(dto.getCloseTime())
                        .isAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : true)
                        .build();
                availabilityRepository.save(av);
            }
        }
        if (req.getImageUrls() != null) {
            imageRepository.deleteByParkingSpaceId(parking.getId());
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                String url = req.getImageUrls().get(i);
                ParkingImage img = ParkingImage.builder()
                        .parkingSpace(parking)
                        .imageUrl(url)
                        .isPrimary(i == 0)
                        .build();
                imageRepository.save(img);
            }
        }
        parking = parkingSpaceRepository.save(parking);
        availabilityCache.setAvailableSlots(parking.getId(), parking.getAvailableSlots());
        return toResponse(parking, null);
    }

    public List<ParkingResponse> listByOwner(User user, int page, int size) {
        ensureOwner(user, user.getId());
        Pageable pageable = PageRequest.of(page, size);
        List<ParkingSpace> list = parkingSpaceRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId(), pageable);
        return list.stream().map(p -> toResponse(p, null)).collect(Collectors.toList());
    }

    public long countByOwner(User user) {
        return parkingSpaceRepository.countByOwnerId(user.getId());
    }

    public ParkingResponse getByIdForOwner(User user, Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        return toResponse(parking, null);
    }

    public ParkingResponse getByIdPublic(Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (!isVisibleToDriver(parking)) {
            throw new ParkingException(ParkingErrorCode.PARKING_NOT_ACTIVE);
        }
        Integer cached = availabilityCache.getAvailableSlots(parkingId);
        ParkingResponse resp = toResponse(parking, null);
        if (cached != null) {
            resp.setAvailableSlots(cached);
        }
        return resp;
    }

    @Transactional
    public ParkingResponse activate(User user, Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (parking.getVerificationStatus() != VerificationStatus.APPROVED || !Boolean.TRUE.equals(parking.getIsPublished())) {
            throw new ParkingException(ParkingErrorCode.CANNOT_ACTIVATE_UNAPPROVED);
        }
        parking.setIsActive(true);
        parking = parkingSpaceRepository.save(parking);
        availabilityCache.setAvailableSlots(parking.getId(), parking.getAvailableSlots());
        return toResponse(parking, null);
    }

    @Transactional
    public ParkingResponse deactivate(User user, Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        parking.setIsActive(false);
        parking = parkingSpaceRepository.save(parking);
        availabilityCache.evict(parkingId);
        return toResponse(parking, null);
    }

    // --- Admin ---
    public List<ParkingResponse> listPending(User admin, int page, int size) {
        ensureAdmin(admin);
        Pageable pageable = PageRequest.of(page, size);
        List<ParkingSpace> list = parkingSpaceRepository.findByVerificationStatusOrderByCreatedAtAsc(
                VerificationStatus.PENDING, pageable);
        return list.stream().map(p -> toResponse(p, null)).collect(Collectors.toList());
    }

    public long countPending(User admin) {
        ensureAdmin(admin);
        return parkingSpaceRepository.countByVerificationStatus(VerificationStatus.PENDING);
    }

    @Transactional
    public ParkingResponse approve(User admin, Long parkingId) {
        ensureAdmin(admin);
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (parking.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ParkingException(ParkingErrorCode.NOT_APPROVED);
        }
        parking.setVerificationStatus(VerificationStatus.APPROVED);
        parking.setIsPublished(true);
        parking.setIsActive(false);
        parking = parkingSpaceRepository.save(parking);
        AdminAction action = AdminAction.builder()
                .parkingSpaceId(parkingId)
                .adminId(admin.getId())
                .actionType(AdminActionType.APPROVE)
                .reason(null)
                .build();
        adminActionRepository.save(action);
        return toResponse(parking, null);
    }

    @Transactional
    public ParkingResponse reject(User admin, Long parkingId, String reason) {
        ensureAdmin(admin);
        if (reason == null || reason.isBlank()) {
            throw new ParkingException(ParkingErrorCode.REJECT_REASON_REQUIRED);
        }
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (parking.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ParkingException(ParkingErrorCode.NOT_APPROVED);
        }
        parking.setVerificationStatus(VerificationStatus.REJECTED);
        parking.setIsPublished(false);
        parking.setIsActive(false);
        parking = parkingSpaceRepository.save(parking);
        AdminAction action = AdminAction.builder()
                .parkingSpaceId(parkingId)
                .adminId(admin.getId())
                .actionType(AdminActionType.REJECT)
                .reason(reason)
                .build();
        adminActionRepository.save(action);
        return toResponse(parking, null);
    }

    @Transactional
    public ParkingResponse forceDisable(User admin, Long parkingId, String reason) {
        ensureAdmin(admin);
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        parking.setIsActive(false);
        parking = parkingSpaceRepository.save(parking);
        availabilityCache.evict(parkingId);
        AdminAction action = AdminAction.builder()
                .parkingSpaceId(parkingId)
                .adminId(admin.getId())
                .actionType(AdminActionType.FORCE_DISABLE)
                .reason(reason)
                .build();
        adminActionRepository.save(action);
        return toResponse(parking, null);
    }

    // --- Public search ---
    public ParkingSearchResponse search(ParkingSearchRequest req) {
        validateCoordinates(req.getLatitude(), req.getLongitude());
        double lat = req.getLatitude().doubleValue();
        double lon = req.getLongitude().doubleValue();
        double radiusMeters = req.getRadiusKm().doubleValue() * 1000;
        String vehicleType = req.getVehicleType() != null ? req.getVehicleType().name() : null;
        BigDecimal priceMin = req.getPriceMin();
        BigDecimal priceMax = req.getPriceMax();
        String city = req.getCity();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        List<Object[]> rows = parkingSpaceRepository.findIdsAndDistanceByLocation(
                lat, lon, radiusMeters, vehicleType, priceMin, priceMax, city, pageable);
        long total = parkingSpaceRepository.countSearchByLocationAndFilters(
                lat, lon, radiusMeters, vehicleType, priceMin, priceMax, city);
        List<Long> ids = new ArrayList<>();
        Map<Long, Double> distanceMap = new HashMap<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            Double dist = ((Number) row[1]).doubleValue();
            ids.add(id);
            distanceMap.put(id, dist);
        }
        List<ParkingSpace> parkings = ids.isEmpty() ? Collections.emptyList() : parkingSpaceRepository.findAllById(ids);
        Map<Long, ParkingSpace> byId = parkings.stream().collect(Collectors.toMap(ParkingSpace::getId, p -> p));
        List<ParkingResponse> responses = new ArrayList<>();
        for (Long id : ids) {
            ParkingSpace p = byId.get(id);
            if (p != null) {
                Double dist = distanceMap.get(id);
                Integer cached = availabilityCache.getAvailableSlots(id);
                ParkingResponse r = toResponse(p, dist);
                if (cached != null) r.setAvailableSlots(cached);
                responses.add(r);
            }
        }
        int totalPages = (int) Math.ceil((double) total / req.getSize());
        return ParkingSearchResponse.builder()
                .parkings(responses)
                .page(req.getPage())
                .size(req.getSize())
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    // --- Slot update hook (for booking module) ---
    @Override
    @Transactional
    public boolean reserveSlot(Long parkingId, Long version) {
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (!isVisibleToDriver(parking)) {
            throw new ParkingException(ParkingErrorCode.PARKING_NOT_ACTIVE);
        }
        if (parking.getAvailableSlots() <= 0) {
            throw new ParkingException(ParkingErrorCode.OVERBOOKING);
        }
        if (version != null && !version.equals(parking.getVersion())) {
            throw new ParkingException(ParkingErrorCode.CONCURRENT_UPDATE_FAILED);
        }
        parking.setAvailableSlots(parking.getAvailableSlots() - 1);
        try {
            parking = parkingSpaceRepository.save(parking);
            availabilityCache.setAvailableSlots(parkingId, parking.getAvailableSlots());
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ParkingException(ParkingErrorCode.CONCURRENT_UPDATE_FAILED);
        }
    }

    @Override
    @Transactional
    public void releaseSlot(Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        if (parking.getAvailableSlots() >= parking.getTotalSlots()) {
            log.warn("Release slot called but available_slots already at max for parking {}", parkingId);
            return;
        }
        parking.setAvailableSlots(parking.getAvailableSlots() + 1);
        parking = parkingSpaceRepository.save(parking);
        availabilityCache.setAvailableSlots(parkingId, parking.getAvailableSlots());
    }

    // --- Dashboard ---
    public OwnerDashboardResponse getDashboardForParking(User user, Long parkingId) {
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new ParkingException(ParkingErrorCode.PARKING_NOT_FOUND));
        OwnerDashboardResponse d = dashboardProvider.getDashboardForParking(parkingId, user.getId());
        if (d == null) d = buildStubDashboard(parking);
        return d;
    }

    public List<OwnerDashboardResponse> getDashboardsForOwner(User user) {
        ensureOwner(user, user.getId());
        return dashboardProvider.getDashboardsForOwner(user.getId());
    }

    private OwnerDashboardResponse buildStubDashboard(ParkingSpace p) {
        int total = p.getTotalSlots();
        int available = p.getAvailableSlots();
        int used = total - available;
        BigDecimal utilization = total > 0
                ? BigDecimal.valueOf(used * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return OwnerDashboardResponse.builder()
                .parkingId(p.getId())
                .parkingName(p.getName())
                .totalSlots(total)
                .availableSlots(available)
                .upcomingBookingsCount(0)
                .completedBookingsCount(0)
                .cancelledBookingsCount(0)
                .utilizationPercent(utilization)
                .grossEarnings(BigDecimal.ZERO)
                .fees(BigDecimal.ZERO)
                .netEarnings(BigDecimal.ZERO)
                .recentBookings(Collections.emptyList())
                .build();
    }

    private ParkingResponse toResponse(ParkingSpace p, Double distanceMeters) {
        List<ParkingAvailability> avList = availabilityRepository.findByParkingSpaceIdOrderByDayOfWeek(p.getId());
        List<AvailabilitySlotDto> avDtos = avList.stream()
                .map(a -> AvailabilitySlotDto.builder()
                        .dayOfWeek(a.getDayOfWeek())
                        .openTime(a.getOpenTime())
                        .closeTime(a.getCloseTime())
                        .isAvailable(a.getIsAvailable())
                        .build())
                .collect(Collectors.toList());
        List<ParkingImage> imgs = imageRepository.findByParkingSpaceId(p.getId());
        List<String> urls = imgs.stream().map(ParkingImage::getImageUrl).collect(Collectors.toList());
        String primaryUrl = imgs.stream().filter(ParkingImage::getIsPrimary).findFirst().map(ParkingImage::getImageUrl).orElse(null);
        if (primaryUrl == null && !urls.isEmpty()) primaryUrl = urls.get(0);
        return ParkingResponse.builder()
                .id(p.getId())
                .ownerId(p.getOwnerId())
                .name(p.getName())
                .address(p.getAddress())
                .city(p.getCity())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .totalSlots(p.getTotalSlots())
                .availableSlots(p.getAvailableSlots())
                .pricePerHour(p.getPricePerHour())
                .vehicleType(p.getVehicleType())
                .verificationStatus(p.getVerificationStatus())
                .isPublished(p.getIsPublished())
                .isActive(p.getIsActive())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .distanceMeters(distanceMeters)
                .availabilityRules(avDtos)
                .imageUrls(urls)
                .primaryImageUrl(primaryUrl)
                .build();
    }
}
