package top.tatobamail.backend.User.imlp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.tatobamail.backend.User.BuiltinAdmin;
import top.tatobamail.backend.User.User;
import top.tatobamail.backend.User.UserRepository;
import top.tatobamail.backend.User.UserRole;
import top.tatobamail.backend.User.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user) {
        if (BuiltinAdmin.USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("该用户名不可用");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setUsername(user.getUsername());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User login(User user) {
        if (BuiltinAdmin.matches(user.getUsername(), user.getPassword())) {
            User builtin = new User();
            builtin.setId(0L);
            builtin.setUsername(BuiltinAdmin.USERNAME);
            builtin.setRole(UserRole.ADMIN);
            LocalDateTime now = LocalDateTime.now();
            builtin.setCreatedAt(now);
            builtin.setUpdatedAt(now);
            return builtin;
        }

        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return existingUser;
    }
}
