package com.teamworkload.filter;

import com.teamworkload.service.SysPermissionService;
import com.teamworkload.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysPermissionService sysPermissionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, SysPermissionService sysPermissionService) {
        this.jwtUtil = jwtUtil;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);
            
            // 获取用户权限
            List<String> permissionCodes = sysPermissionService.getUserPermissionCodes(userId);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // 添加角色权限
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            
            // 添加功能权限
            for (String permissionCode : permissionCodes) {
                authorities.add(new SimpleGrantedAuthority(permissionCode));
            }
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
