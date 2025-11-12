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
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
    }


    private Long id;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public Set<Roles> getRoles() {
        return roles;
    }


    private String username;


    private String password;


    private String passwordConfirm;


    private Set<Roles> roles;
}
