package com.example.userservice.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.RoleEntity;
import com.example.userservice.jpa.RoleRepository;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        //무작위로 생성된 고유한 문자열 ID를 반환
        userDto.setUserId(UUID.randomUUID().toString());
        
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        //UserDto를 UserEntity로 매핑
        UserEntity userEntity = mapper.map(userDto, UserEntity.class);
        userEntity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd())); //비밀번호 암호화

        // 기본 ROLE_USER 할당 (만약 다른 역할을 추가할 수 있다면 이를 확장)
        RoleEntity role = roleRepository.findByName("ROLE_USER");
        if(role == null) {
            role = new RoleEntity();
            role.setName("ROLE_USER");
            roleRepository.save(role);
        }
        userEntity.setRoles(Set.of(role));

        userRepository.save(userEntity); //DB에 저장

        UserDto returnUserDto = mapper.map(userEntity, UserDto.class);
        return returnUserDto;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        
        UserEntity userEntity = userRepository.findByUserId(userId);
        
        if(userEntity == null) {
            throw new EntityNotFoundException("User not found");
        }
        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);
        
        return userDto;
    }

    @Override
    public Iterable<UserEntity> getUserByAll() {
        return userRepository.findAll();
    }

    @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            UserEntity userEntity = userRepository.findByEmail(username);

            if (userEntity == null) {
                throw new UsernameNotFoundException(username);
            }
            
            List<GrantedAuthority> authorities = userEntity.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
            
            return new User(userEntity.getEmail(), userEntity.getEncryptedPwd(), authorities);
    }


    @Override
    public UserDto getUserDetailsByEmail(String email) {
        
        UserEntity userEntity = userRepository.findByEmail(email);

        if(userEntity==null) {
            throw new UsernameNotFoundException(email);
        }
        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);
        return userDto;
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) {
        // 1. 사용자 조회
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new EntityNotFoundException("User not found");
        }

        // 2. 기존 비밀번호가 올바른지 확인 (암호화된 비밀번호와 비교)
        if (!passwordEncoder.matches(oldPassword, userEntity.getEncryptedPwd())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        
        // 3. 새 비밀번호가 기존 비밀번호와 동일한지 검사
        if (passwordEncoder.matches(newPassword, userEntity.getEncryptedPwd())) {
            throw new IllegalArgumentException("New password must be different from the old password");
        }
        
        // 4. 새 비밀번호의 최소 길이 검사 (추가적인 복잡도 검사는 필요에 따라 적용)
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        // 5. 새 비밀번호 암호화 및 저장
        userEntity.setEncryptedPwd(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
    }

}
