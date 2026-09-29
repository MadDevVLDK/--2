package ru.library.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<LibraryUser> getAllUsers() { return userRepository.findAll(); }

    public LibraryUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public LibraryUser createUser(LibraryUser user) { return userRepository.save(user); }

    public LibraryUser updateUser(Long id, LibraryUser update) {
        LibraryUser user = getUser(id);
        user.setName(update.getName());
        user.setEmail(update.getEmail());
        user.setActive(update.isActive());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) { userRepository.deleteById(id); }
}