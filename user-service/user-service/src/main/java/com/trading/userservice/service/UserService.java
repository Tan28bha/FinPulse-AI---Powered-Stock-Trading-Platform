package com.trading.userservice.service;


import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import com.trading.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final String User_Registered_Topic="user.registered";


//    Registe a New trader
    public AuthResponse register(RegisterRequest request){
        log.info("registering user: {}",request.getEmail());

        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException((
                    "Email already registered" + request.getEmail());
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setWalletBalance(
                request.getInitialDeposit() !=null
                ? request.getInitialDeposit()
                : BigDecimal.valueOf(10000));

        User savedUser= userRepository.save(user);
        log.info("User registered: {}",savedUser.getId());

        // Publish user.registered event
        Map<String,Object> userRegisteredEvent= new HashMap<>();
        userRegisteredEvent.put("userId",savedUser.getId());
        userRegisteredEvent.put("email",savedUser.getEmail());
        userRegisteredEvent.put("firstName",savedUser.getFirstName());
        userRegisteredEvent.put("lastName" ,savedUser.getLastName());
        userRegisteredEvent.put("walletBalance" ,savedUser.getWalletBalance());

        kafkaTemplate.send(User_Registered_Topic,savedUser.getId(),userRegisteredEvent);

        String token=generateAccessToken(savedUser.getId(),savedUser.getEmail());

        String refreshToken=generateRefreshToken(savedUser.getId());

        return buildAuthResponse(savedUser,token,refreshToken);

}

// Login

public AuthResponse login(LoginRequest request){
        log.info("Login attempt: {}",request.getEmail());

        User user =userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: "+request.getEmail()));

        if(passwordEncoder.matches(
                request.getPassword(),user.getPassword())){
            throw new RuntimeException("Invalid Credentials");
        }
        log.info("Login successful:{}",user.getId());
        String token=generateAccessToken(User.getId(),User.getEmail());
        String refreshToken=generateRefreshToken(User.getId());

        return buildAuthResponse(user,toke,refreshToken);
    }

//    get user by id called by other services via feign(internal)

    public UserResponse getUserById(String userId){
        User user=userRepository.findById(userId).orElseThrow(() -> new RuntimeException(
                "User not found: "+userId
        ));
        return mapToResponse(user);
    }

    public UserResponse addFundss(String userId,BigDecimal amount){
        User user=userRepository.findById(userId).orElseThrow(() -> new RuntimeException(
                "User not found: "+userId));
        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser= userRepository.save(user);

        log.info("Funds added: {} to user:{}",amount,userId);
        return mapToResponse(user);

    }

    public UserResponse deductFunds(String userId,BigDecimal amount){
        User user=userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: "+ userId));
        if(user.getWalletBalance().compareTo(amount)<0){
            throw new RuntimeException("Insufficient wallet Balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        User savedUser=userRepository.save(user);

        log.info("Funds credited: {} to user: {}",amount,userId);
        return mapToResponse(user);
    }

    private AuthResponse buildAuthResponse(User user,String token,String refreshToken){
        AuthResponse response=new AuthResponse();

    }

    private UserResponse mapToResponse(User user){
        UserResponse response=new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setWalletBalance(user.getWalletBalance());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

}
