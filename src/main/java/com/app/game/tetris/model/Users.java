package com.app.game.tetris.model;

//admin password:sam; user password:mas; Dunny dun; Oswaldo osw; Tommy tom; Bonny bon; Ira ira; Wolfy wol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    private Long id;
    private String username;
    private String password;
    private String passwordConfirm;
    private Set<Roles> roles;
}
