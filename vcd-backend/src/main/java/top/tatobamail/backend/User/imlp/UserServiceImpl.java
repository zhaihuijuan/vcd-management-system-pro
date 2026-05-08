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
        // 禁止使用内置管理员用户名
        if (BuiltinAdmin.USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("\u5e97\u5458\u540d\u4e0d\u80fd\u91cd\u590d");
        }
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("\u5e97\u5458\u540d\u4e0d\u80fd\u91cd\u590d");
        }
        // 校验密码：长度>=6，且同时包含字母和数字
        String pwd = user.getPassword();
        if (pwd == null || pwd.length() < 6) {
            throw new RuntimeException("\u5bc6\u7801\u957f\u5ea6\u5fc5\u987b\u5927\u4e8e\u7b49\u4e8e6\u4f4d");
        }
        boolean hasLetter = pwd.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = pwd.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new RuntimeException("\u5bc6\u7801\u5fc5\u987b\u540c\u65f6\u5305\u542b\u5b57\u6bcd\u548c\u6570\u5b57");
        }
        user.setPassword(passwordEncoder.encode(pwd));
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
                .orElseThrow(() -> new RuntimeException("\u7528\u6237\u4e0d\u5b58\u5728"));
        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            throw new RuntimeException("\u5bc6\u7801\u9519\u8bef");
        }
        return existingUser;
    }
}