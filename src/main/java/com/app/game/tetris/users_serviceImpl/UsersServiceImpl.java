package com.app.game.tetris.users_serviceImpl;

import com.app.game.tetris.model.Roles;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.users_service.UsersService;
import com.app.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsersServiceImpl implements UsersService {

    @GrpcClient("users-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @Override
    public void deleteUser(Long userId) {
        try {
            userStub.deleteUser(IdRequest.newBuilder().setId(userId).build());
            log.info("🗑 Пользователь с ID {} успешно удален через gRPC", userId);
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при удалении пользователя {}: {}", userId, e.getStatus());
        }
    }

    @Override
    public Users findUserById(Long userId) {
        try {
            UserResponse response = userStub.findById(IdRequest.newBuilder().setId(userId).build());
            return response.getExists() ? mapToAppModel(response.getUser()) : null;
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при поиске по ID {}: {}", userId, e.getStatus());
            return null;
        }
    }

    @Override
    public Users findUserByUserName(String userName) {
        try {
            UserResponse response = userStub.findByUsername(SearchRequest.newBuilder().setValue(userName).build());
            return response.getExists() ? mapToAppModel(response.getUser()) : null;
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при поиске по имени {}: {}", userName, e.getStatus());
            return null;
        }
    }

    @Override
    public List<Users> getAllUsers() {
        try {
            UserListResponse response = userStub.getAllUsers(UserEmpty.newBuilder().build());

            List<Users> users = response.getUsersList().stream()
                    .map(this::mapToAppModel)
                    .toList();

            log.info("🎯 Из gRPC-канала users-service успешно извлечено {} профилей", users.size());
            return users;
        } catch (StatusRuntimeException e) {
            log.error("❌ Ошибка gRPC при получении списка пользователей: {}", e.getStatus());
            return List.of();
        }
    }

    // --- Оптимизированный безопасный маппер ---
    private Users mapToAppModel(UserMsg msg) {
        Users user = new Users();
        user.setId(msg.getId());
        user.setUsername(msg.getUsername());

        // 🔥 ЗАЩИТА ДАННЫХ: Пароли обнуляем, игровому движку они не нужны по стандарту PCI-DSS / OWASP
        user.setPassword(null);
        user.setPasswordConfirm(null);

        // Безопасный маппинг сета ролей для админки
        Set<Roles> roles = msg.getRolesList().stream()
                .map(r -> new Roles(r.getId(), r.getName()))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        return user;
    }

    // 🔥 ОПТИМИЗАЦИЯ: Неиспользуемый метод mapToMsg удален (Dead Code Cleanup)
}
