package com.mAtiehDev.jobtracker.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mAtiehDev.jobtracker.model.User;
import com.mAtiehDev.jobtracker.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Override
	public List<User> getAllUser() {
		return userRepository.findAll();
	}

	@Override
	public User getUserById(String userId) {
		return userRepository.findById(userId).get();
	}

	@Override
	public User addUser(User user) {
	    String generatedId = idGenerator.generateId("user_2025_", "user_id_seq");
	    user.setUserId(generatedId);
	    return userRepository.save(user);
	}

	@Override
	public User updateUser(User user) {
		return userRepository.save(user);
	}

	@Override
	public String deleteUser(User user) {
		userRepository.delete(user);
		return "user with user ID "+user.getUserId()+" is deleted successfully";
	}
	
	@Override
	public User login(String emailOrUsername, String password) {
	    // Try to find user by email and password
	    Optional<User> userOpt = userRepository.findByEmailAddressAndPassword(emailOrUsername, password);

	    if(userOpt.isPresent()) {
	        return userOpt.get();
	    }
	    
	    // If not found by email, try username and password
	    userOpt = userRepository.findByUserNameAndPassword(emailOrUsername, password);
	    return userOpt.orElse(null);
	}


}
