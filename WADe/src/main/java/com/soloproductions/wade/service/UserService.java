package com.soloproductions.wade.service;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Service responsible for user registration, authentication, and lookup operations.
 */
@Service
public class UserService
{
    /** Repository for user persistence and retrieval. */
    private final UserRepository userRepository;

    /** Password encoder used for hashing and verification. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(UserService.class);

    /**
     * Creates a user service with required repository dependency.
     *
     * @param   userRepository
     *          user repository dependency
     */
    @Autowired
    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Registers a new user after validating username uniqueness.
     *
     * @param   username
     *          desired username
     * @param   password
     *          raw password to hash before persistence
     * @param   email
     *          user email address
     *
     * @return  persisted user instance
     */
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

    /**
     * Authenticates a user using username and password.
     *
     * @param   username
     *          username to authenticate
     * @param   password
     *          raw password to validate
     *
     * @return  authenticated user entity
     */
    public User login(String username, String password)
    {
        User user = userRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword()))
        {
            throw new IllegalArgumentException("Invalid username or password");
        }
        LOG.debug("The user {} has successfully logged in.", username);
        return user;
    }

    /**
     * Finds a user by username.
     *
     * @param   username
     *          username to search for
     *
     * @return  matching user or {@code null} if not found
     */
    public User findUserByUsername(String username)
    {
        return userRepository.findByUsername(username);
    }
}
