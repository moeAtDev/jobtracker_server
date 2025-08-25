package com.mAtiehDev.jobtracker.service;

import java.util.List;

import com.mAtiehDev.jobtracker.model.User;

public interface UserService {
	List<User> getAllUser();
	User getUserById(String userId);
	User addUser(User user);
	User updateUser(User user);
	String deleteUser(User user);
	User login(String emailOrUsername, String password);
	
	

}
