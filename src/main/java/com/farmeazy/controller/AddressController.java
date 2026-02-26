package com.farmeazy.controller;

import com.farmeazy.dto.AddressDto;
import com.farmeazy.entity.User;
import com.farmeazy.service.AddressService;
import com.farmeazy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto addressDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        AddressDto createdAddress = addressService.createAddress(user, addressDto);
        return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AddressDto>> getUserAddresses(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<AddressDto> addresses = addressService.getUserAddresses(user);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto addressDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        AddressDto updatedAddress = addressService.updateAddress(user, id, addressDto);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        addressService.deleteAddress(user, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        addressService.setDefaultAddress(user, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
