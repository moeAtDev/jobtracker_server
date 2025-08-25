package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.model.User;
import com.mAtiehDev.jobtracker.repository.UserRepository;
import com.mAtiehDev.jobtracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
//@RequestMapping("/api/users")
//@CrossOrigin(origins = "*") // Allow frontend access
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUser());
	}

    
    @GetMapping("/users/{userId}")
	public ResponseEntity<User> getUsersById(@PathVariable("userId") String userId) {
		return ResponseEntity.ok(userService.getUserById(userId));
	}
	
	@PostMapping("/users")
	public ResponseEntity<User> createUser(@RequestBody User user) {
		return ResponseEntity.ok(userService.addUser(user));
	}
	
	
	@PatchMapping("/users/{userId}")
	public ResponseEntity<User> updateUser(@RequestBody User user,@PathVariable("userId") String userId) {
		User usrObj=userService.getUserById(userId);
		if(usrObj!=null) {
			usrObj.setEmailAddress(user.getEmailAddress());
			usrObj.setFirstName(user.getFirstName());
			usrObj.setLastName(user.getLastName());
			usrObj.setPassword(user.getPassword());
			usrObj.setUserId(user.getUserId());
			usrObj.setUserName(user.getUserName());
		}
		return ResponseEntity.ok(userService.updateUser(usrObj));
	}
	
	@DeleteMapping("/users/{userId}")
	public ResponseEntity<String> deleteUser(@PathVariable("userId") String userId) {
		
		User usrObj=userService.getUserById(userId);
		String deleteMsg=null;
		if(usrObj!=null) {
			deleteMsg=userService.deleteUser(usrObj);
		}
		return ResponseEntity.ok(deleteMsg);
	}
   
}
