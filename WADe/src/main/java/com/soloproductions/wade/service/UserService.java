package com.soloproductions.wade.service;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class UserService
{

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User registerUser(String username, String password, String email)
    {
        if (userRepository.findByUsername(username) != null)
        {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User login(String username, String password)
    {
        User user = userRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword()))
        {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return user;
    }

    public User findUserByUsername(String username)
    {
        return userRepository.findByUsername(username);
    }
}
