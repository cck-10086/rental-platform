package com.rental.service;

import com.rental.dto.LoginDTO;
import com.rental.dto.LoginResultDTO;
import com.rental.dto.RegisterDTO;
import com.rental.dto.UpdateProfileDTO;
import com.rental.entity.User;

public interface UserService {
    void register(RegisterDTO dto);
    LoginResultDTO login(LoginDTO dto);
    User getCurrentUser();
    void updateProfile(UpdateProfileDTO dto);
}
