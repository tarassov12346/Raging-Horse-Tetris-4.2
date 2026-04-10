package com.app.game.tetris.dto;

import java.io.Serializable;

public record UserResponseDTO(Long id,
                              String username,
                              String roles, // Строка ролей через ";"
                              boolean isAdmin // Можно добавить флаг для удобства фронта
)  implements Serializable{ }
