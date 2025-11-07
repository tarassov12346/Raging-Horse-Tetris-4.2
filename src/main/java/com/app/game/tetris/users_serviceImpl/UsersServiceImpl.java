package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.model.User;
import com.app.game.tetris.users_service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class UsersServiceImpl implements UsersService {
    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    @Override
    public boolean saveUser(User newUser) {
        return Objects.requireNonNull(restTemplate.postForObject("http://users-service/save", newUser, Boolean.class));
    }

    @Override
    public void deleteUser(Long userId) {
        restTemplate.delete("http://users-service" + "/delete?userId={userId}", userId);
    }

    @Override
    public User findUserById(Long userId) {
        return restTemplate.getForObject("http://users-service" + "/findId?userId={userId}", User.class, userId);
    }

    @Override
    public User findUserByUserName(String userName) {
        return restTemplate.getForObject("http://users-service" + "/findName?userName={userName}", User.class, userName);
    }

    @Override
    public List<User> getAllUsers() {
        ResponseEntity<User[]> response =
                restTemplate.getForEntity("http://users-service/users", User[].class);
        return new ArrayList<>(Arrays.stream(response.getBody()).toList());
    }

    @Override
    public boolean isRolesDBEmpty() {
        return restTemplate.getForObject("http://users-service/isEmpty", Boolean.class);
    }

    @Override
    public void prepareRolesDB() {
        restTemplate.getForObject("http://users-service/prepareRolesDB",Void.class);
    }

    @Override
    public void prepareUserDB() {
        restTemplate.getForObject("http://users-service/prepareUserDB",Void.class);
    }
}
