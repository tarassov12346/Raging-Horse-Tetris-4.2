package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.model.Users;
import com.app.game.tetris.users_service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UsersServiceImpl implements UsersService, UserDetailsService {
    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    @Override
    public boolean saveUser(Users newUser) {
        return Objects.requireNonNull(restTemplate.postForObject("http://users-service/save", newUser, Boolean.class));
    }

    @Override
    public void deleteUser(Long userId) {
        restTemplate.delete("http://users-service" + "/delete?userId={userId}", userId);
    }

    @Override
    public Users findUserById(Long userId) {
        return restTemplate.getForObject("http://users-service" + "/findId?userId={userId}", Users.class, userId);
    }

    @Override
    public Users findUserByUserName(String userName) {
        return restTemplate.getForObject("http://users-service" + "/findName?userName={userName}", Users.class, userName);
    }

    @Override
    public List<Users> getAllUsers() {
        ResponseEntity<Users[]> response =
                restTemplate.getForEntity("http://users-service/users", Users[].class);
        return new ArrayList<>(Arrays.stream(response.getBody()).toList());
    }

    @Override
    public boolean isRolesDBEmpty() {
        return restTemplate.getForObject("http://users-service/isEmpty", Boolean.class);
    }

    @Override
    public void prepareRolesDB() {
        restTemplate.getForObject("http://users-service/prepareRolesDB", Void.class);
    }

    @Override
    public void prepareUserDB() {
        restTemplate.getForObject("http://users-service/prepareUserDB", Void.class);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users user = restTemplate.getForObject("http://users-service" + "/findName?userName={userName}", Users.class, username);

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))  // Префикс ROLE_ если нужно
                .collect(Collectors.toList());

        return new User(
                user.getUsername(),
                user.getPassword(),
                authorities  // Или пустой список, если authorities не нужны сразу
        );
    }
}
