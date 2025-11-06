package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.model.User;
import com.app.game.tetris.users_service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
}
