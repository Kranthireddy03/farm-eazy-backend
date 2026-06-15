package com.farmeazy.service;

import com.farmeazy.dto.DeliveryLocationCreateDto;
import com.farmeazy.dto.DeliveryLocationDto;
import com.farmeazy.dto.LocationAccessStatusDto;
import com.farmeazy.entity.Address;
import com.farmeazy.entity.DeliveryLocation;
import com.farmeazy.entity.Product;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.DeliveryLocationRepository;
import com.farmeazy.util.GeocodeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class DeliveryLocationService {

    private final DeliveryLocationRepository deliveryLocationRepository;

    public DeliveryLocationService(DeliveryLocationRepository deliveryLocationRepository) {
        this.deliveryLocationRepository = deliveryLocationRepository;
    }

    @Cacheable(cacheNames = "deliveryLocationAll", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<DeliveryLocationDto> getAllLocations() {
        return deliveryLocationRepository.findAll().stream().map(this::toDto).toList();
    }

    @Cacheable(cacheNames = "deliveryLocationActive", key = "'active'", unless = "#result == null || #result.isEmpty()")
    public List<DeliveryLocationDto> getActiveLocations() {
        return deliveryLocationRepository.findByActiveTrueOrderByLocationNameAsc().stream().map(this::toDto).toList();
    }

    public DeliveryLocationDto getLocation(Long id) {
        return toDto(getLocationEntity(id));
    }

    public DeliveryLocation getLocationEntity(Long id) {
        return deliveryLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery location not found"));
    }

    public DeliveryLocation getActiveLocationEntity(Long id) {
        return deliveryLocationRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Active delivery location not found"));
    }

    public DeliveryLocationDto createLocation(DeliveryLocationCreateDto request) {
        DeliveryLocation location = new DeliveryLocation();
        apply(location, request);
        return toDto(deliveryLocationRepository.save(location));
    }

    public DeliveryLocationDto updateLocation(Long id, DeliveryLocationCreateDto request) {
        DeliveryLocation location = getLocationEntity(id);
        apply(location, request);
        return toDto(deliveryLocationRepository.save(location));
    }

    public DeliveryLocationDto setActive(Long id, boolean active) {
        DeliveryLocation location = getLocationEntity(id);
        location.setActive(active);
        return toDto(deliveryLocationRepository.save(location));
    }

    public DeliveryLocation resolveDeliveryLocationForSeller(User seller) {
        if (seller == null) {
            return null;
        }

        String postalCode = normalize(seller.getPinCode());
        if (!postalCode.isBlank()) {
            Optional<DeliveryLocation> postalMatch = deliveryLocationRepository.findByPostalCodeIgnoreCaseAndActiveTrue(postalCode)
                .stream()
                .findFirst();
            if (postalMatch.isPresent()) {
                return postalMatch.get();
            }
        }

        String city = normalize(seller.getCity());
        String state = normalize(seller.getState());
        return deliveryLocationRepository.findByActiveTrueOrderByLocationNameAsc().stream()
            .filter(location -> matchesText(location, city, state, postalCode))
            .findFirst()
            .orElse(null);
    }

    public DeliveryLocation resolveDeliveryLocationForProduct(Product product) {
        if (product == null) {
            return null;
        }
        if (product.getDeliveryLocationId() != null) {
            return deliveryLocationRepository.findByIdAndActiveTrue(product.getDeliveryLocationId()).orElse(null);
        }
        return resolveDeliveryLocationForSeller(product.getSeller());
    }

    public boolean isProductDeliverable(Product product, String locationHeader, Address address) {
        DeliveryLocation deliveryLocation = resolveDeliveryLocationForProduct(product);
        if (deliveryLocation == null) {
            return false;
        }
        return matchesLocation(deliveryLocation, locationHeader, address);
    }

    public String getDeliveryFailureMessage(Product product, String locationHeader, Address address) {
        return isProductDeliverable(product, locationHeader, address)
            ? "Deliverable to your location"
            : "Not deliverable to this location at the moment";
    }

    public LocationAccessStatusDto getLocationAccessStatus(String locationHeader) {
        String trimmedHeader = trim(locationHeader);
        if (trimmedHeader == null || trimmedHeader.isBlank()) {
            return new LocationAccessStatusDto(
                false,
                "Location access is required to use user marketplace features",
                null,
                null
            );
        }

        List<DeliveryLocation> activeLocations = deliveryLocationRepository.findByActiveTrueOrderByLocationNameAsc();
        for (DeliveryLocation location : activeLocations) {
            if (matchesLocation(location, trimmedHeader, null)) {
                return new LocationAccessStatusDto(
                    true,
                    "Service is available in your current location",
                    location.getId(),
                    location.getLocationName()
                );
            }
        }

        return new LocationAccessStatusDto(
            false,
            "We are not yet available in your current location",
            null,
            null
        );
    }

    private void apply(DeliveryLocation location, DeliveryLocationCreateDto request) {
        location.setLocationName(trim(request.getLocationName()));
        location.setCity(trim(request.getCity()));
        location.setState(trim(request.getState()));
        location.setPostalCode(trim(request.getPostalCode()));
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRadiusKm(request.getRadiusKm() != null ? request.getRadiusKm() : new BigDecimal("0.00"));
        location.setActive(request.getActive() == null || request.getActive());
        location.setNotes(trim(request.getNotes()));
    }

    private DeliveryLocationDto toDto(DeliveryLocation location) {
        DeliveryLocationDto dto = new DeliveryLocationDto();
        BeanUtils.copyProperties(location, dto);
        return dto;
    }

    private boolean matchesLocation(DeliveryLocation location, String locationHeader, Address address) {
        double[] coordinates = GeocodeUtil.parseCoordinates(locationHeader);
        if (coordinates != null && location.getLatitude() != null && location.getLongitude() != null) {
            double radiusKm = location.getRadiusKm() != null ? location.getRadiusKm().doubleValue() : 0d;
            if (radiusKm > 0d) {
                double distance = haversineDistanceKm(
                    coordinates[0],
                    coordinates[1],
                    location.getLatitude().doubleValue(),
                    location.getLongitude().doubleValue()
                );
                return distance <= radiusKm;
            }
        }

        String postalCode = address != null ? normalize(address.getPostalCode()) : "";
        String city = address != null ? normalize(address.getCity()) : "";
        String state = address != null ? normalize(address.getState()) : "";
        return matchesText(location, city, state, postalCode);
    }

    private boolean matchesText(DeliveryLocation location, String city, String state, String postalCode) {
        String locationPostalCode = normalize(location.getPostalCode());
        String locationCity = normalize(location.getCity());
        String locationState = normalize(location.getState());

        if (!postalCode.isBlank() && !locationPostalCode.isBlank()) {
            return postalCode.equalsIgnoreCase(locationPostalCode);
        }

        if (!city.isBlank() && !state.isBlank() && !locationCity.isBlank() && !locationState.isBlank()) {
            return city.equalsIgnoreCase(locationCity) && state.equalsIgnoreCase(locationState);
        }

        if (!city.isBlank() && !locationCity.isBlank()) {
            return city.equalsIgnoreCase(locationCity);
        }

        if (!state.isBlank() && !locationState.isBlank()) {
            return state.equalsIgnoreCase(locationState);
        }

        return false;
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0088d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}