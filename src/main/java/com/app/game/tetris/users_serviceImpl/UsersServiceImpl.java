package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.model.Roles;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.users_service.UsersService;
import com.app.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService{

    @GrpcClient("users-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @Override
    public void deleteUser(Long userId) {
        try {
            userStub.deleteUser(IdRequest.newBuilder().setId(userId).build());
            log.info("🗑 Пользователь с ID {} удален", userId);
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при удалении: {}", e.getStatus());
        }
    }

    @Override
    public Users findUserById(Long userId) {
        try {
            UserResponse response = userStub.findById(IdRequest.newBuilder().setId(userId).build());
            return response.getExists() ? mapToAppModel(response.getUser()) : null;
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при поиске по ID: {}", e.getStatus());
            return null;
        }
    }

    @Override
    public Users findUserByUserName(String userName) {
        try {
            UserResponse response = userStub.findByUsername(SearchRequest.newBuilder().setValue(userName).build());
            return response.getExists() ? mapToAppModel(response.getUser()) : null;
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при поиске по имени: {}", e.getStatus());
            return null;
        }
    }

    @Override
    public List<Users> getAllUsers() {
        try {
            UserListResponse response = userStub.getAllUsers(UserEmpty.newBuilder().build());
            return response.getUsersList().stream()
                    .map(this::mapToAppModel)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при получении списка пользователей: {}", e.getStatus());
            return List.of();
        }
    }

    // --- Мапперы (Конвертация данных) ---
    private Users mapToAppModel(UserMsg msg) {
        Users user = new Users();
        user.setId(msg.getId());
        user.setUsername(msg.getUsername());
        user.setPassword(msg.getPassword());
        user.setPasswordConfirm(msg.getPasswordConfirm());

        Set<Roles> roles = msg.getRolesList().stream()
                .map(r -> new Roles(r.getId(), r.getName()))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        return user;
    }

    private UserMsg mapToMsg(Users user) {
        UserMsg.Builder builder = UserMsg.newBuilder()
                .setUsername(user.getUsername())
                .setPassword(user.getPassword())
                .setPasswordConfirm(user.getPasswordConfirm() != null ? user.getPasswordConfirm() : "");

        if (user.getId() != null) {
            builder.setId(user.getId());
        }
        if (user.getRoles() != null) {
            List<RoleMsg> roleMsgs = user.getRoles().stream()
                    .map(r -> RoleMsg.newBuilder().setId(r.getId()).setName(r.getName()).build())
                    .toList();
            builder.addAllRoles(roleMsgs);
        }
        return builder.build();
    }
}
