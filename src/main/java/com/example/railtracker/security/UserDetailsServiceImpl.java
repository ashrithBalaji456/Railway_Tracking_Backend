package com.example.railtracker.security;

import com.example.railtracker.entity.User;
import com.example.railtracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new UsernameNotFoundException("Username or email cannot be empty");
        }
        String cleanIdentifier = usernameOrEmail.trim();
        User user = userRepository.findByUsernameIgnoreCase(cleanIdentifier)
                .or(() -> userRepository.findByEmailIgnoreCase(cleanIdentifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + cleanIdentifier));

        return UserDetailsImpl.build(user);
    }
}
