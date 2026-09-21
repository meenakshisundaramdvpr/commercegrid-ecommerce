package com.commercegrid.auth.service;

import com.commercegrid.auth.dto.*;
import com.commercegrid.auth.entity.Admin;

public interface AdminAuthService {

    AdminResponse createAdmin(AdminCreateRequest request);
    Admin getAdminByEmail(String email);
    AdminLoginResponse login(AdminLoginRequest adminLoginRequest);
    AdminResponse updateAdminStatus(Long adminId, AdminStatusUpdateRequest request, String requestingAdminEmail);
}