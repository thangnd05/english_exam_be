//package com.example.test.Config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//
////Giúp Spring Security lấy thông tin tài khoản từ database.
//@Service
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRespo userRespo;
//
//    @Autowired
//    public CustomUserDetailsService(UserRespo userRespo) {
//        this.userRespo = userRespo;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        // Tìm người dùng trong cơ sở dữ liệu
//        Users user = userRespo.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        // Lấy danh sách vai trò của người dùng (giả sử bạn có cách lấy vai trò)
//        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole())); // Thêm tiền tố ROLE_ vào vai trò
//
//        // Trả về đối tượng UserDetails với username, password và danh sách vai trò
//        return new User(user.getUsername(), user.getPassword(), authorities);
//    }
//}
