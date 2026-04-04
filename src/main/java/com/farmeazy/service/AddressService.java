package com.farmeazy.service;

import com.farmeazy.dto.AddressDto;
import com.farmeazy.entity.Address;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.AddressRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressService.class);

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private void syncUserProfileLocationFromAddress(User user, Address address) {
        if (user == null || address == null) {
            return;
        }

        user.setAddress(address.getAddressLine1());
        user.setCity(address.getCity());
        user.setState(address.getState());
        user.setPinCode(address.getPostalCode());
        userRepository.save(user);
    }

    private void syncUserProfileLocationFromDefaultAddress(User user) {
        if (user == null) {
            return;
        }

        addressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(address -> syncUserProfileLocationFromAddress(user, address));
    }

    /**
     * Create new address for user
     */
    public AddressDto createAddress(User user, AddressDto addressDto) {
        Address address = new Address();
        address.setUser(user);
        address.setFullName(addressDto.getFullName());
        address.setPhoneNumber(addressDto.getPhoneNumber());
        address.setEmail(addressDto.getEmail());
        address.setAddressLine1(addressDto.getAddressLine1());
        address.setAddressLine2(addressDto.getAddressLine2());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry() != null ? addressDto.getCountry() : "India");
        address.setDefault(addressDto.getIsDefault() != null ? addressDto.getIsDefault() : false);

        // If this is the first address, make it default
        List<Address> existingAddresses = addressRepository.findByUserOrderByCreatedAtDesc(user);
        if (existingAddresses.isEmpty()) {
            address.setDefault(true);
        }

        Address saved = addressRepository.save(address);
        if (Boolean.TRUE.equals(saved.getDefault())) {
            syncUserProfileLocationFromAddress(user, saved);
        }
        log.info("Address created for user {}: {}", user.getId(), saved.getId());
        return convertToDto(saved);
    }

    /**
     * Get all addresses for user
     */
    public List<AddressDto> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get specific address
     */
    public AddressDto getAddress(User user, Long addressId) {
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return convertToDto(address);
    }

    /**
     * Update address
     */
    public AddressDto updateAddress(User user, Long addressId, AddressDto addressDto) {
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setFullName(addressDto.getFullName());
        address.setPhoneNumber(addressDto.getPhoneNumber());
        address.setEmail(addressDto.getEmail());
        address.setAddressLine1(addressDto.getAddressLine1());
        address.setAddressLine2(addressDto.getAddressLine2());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry());
        address.setDefault(addressDto.getIsDefault());

        Address updated = addressRepository.save(address);
        if (Boolean.TRUE.equals(updated.getDefault())) {
            syncUserProfileLocationFromAddress(user, updated);
        }
        log.info("Address updated for user {}: {}", user.getId(), addressId);
        return convertToDto(updated);
    }

    /**
     * Delete address
     */
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        addressRepository.delete(address);
        syncUserProfileLocationFromDefaultAddress(user);
        log.info("Address deleted for user {}: {}", user.getId(), addressId);
    }

    /**
     * Set default address
     */
    public void setDefaultAddress(User user, Long addressId) {
        // Remove default flag from all addresses
        List<Address> addresses = addressRepository.findByUserOrderByCreatedAtDesc(user);
        addresses.forEach(addr -> addr.setDefault(false));
        addressRepository.saveAll(addresses);

        // Set as default
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        address.setDefault(true);
        Address saved = addressRepository.save(address);
        syncUserProfileLocationFromAddress(user, saved);
        
        log.info("Default address set for user {}: {}", user.getId(), addressId);
    }

    /**
     * Get default address for user
     */
    public AddressDto getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrue(user)
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * Convert Address entity to DTO
     */
    public AddressDto convertToDto(Address address) {
        AddressDto dto = new AddressDto();
        dto.setId(address.getId());
        dto.setFullName(address.getFullName());
        dto.setPhoneNumber(address.getPhoneNumber());
        dto.setEmail(address.getEmail());
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        dto.setIsDefault(address.getDefault());
        return dto;
    }
}
