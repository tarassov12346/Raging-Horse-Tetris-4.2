package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.client.UserClient;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.users_service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsersServiceImpl implements UsersService, UserDetailsService {
    @Autowired
    private UserClient userClient; // Внедряем наш новый клиент

    @Override
    public boolean saveUser(Users newUser) {
        return userClient.saveUser(newUser);
    }

    @Override
    public void deleteUser(Long userId) {
        userClient.deleteUser(userId);
    }

    @Override
    public Users findUserById(Long userId) {
        return userClient.findUserById(userId);
    }

    @Override
    public Users findUserByUserName(String userName) {
        return userClient.findUserByUserName(userName);
    }

    @Override
    public List<Users> getAllUsers() {
        return userClient.getAllUsers();
    }

    @Override
    public boolean isRolesDBEmpty() {
        return userClient.isRolesDBEmpty();
    }

    @Override
    public void prepareRolesDB() {
        userClient.prepareRolesDB();
    }

    @Override
    public void prepareUserDB() {
        userClient.prepareUserDB();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userClient.findUserByUserName(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
