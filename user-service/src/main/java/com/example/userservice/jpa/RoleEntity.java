package com.example.userservice.jpa;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "roles")
public class RoleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 예: ROLE_USER, ROLE_ADMIN 등
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
