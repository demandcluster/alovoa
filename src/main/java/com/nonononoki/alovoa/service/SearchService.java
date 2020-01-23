package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Location;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class SearchService {
	
	private final int SORT_DISTANCE = 1;
	private final int SORT_ACTIVE_DATE = 2;
	

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Autowired
	private AuthService authService;
		
	@Autowired
	private UserRepository userRepo;
	
	@Value("${app.search.max}")
	private int maxResults;
	
	
	@SuppressWarnings("unlikely-arg-type")
	public List<UserDto> search(String latitude, String longitude, int distance, int sort) throws Exception {
		User user = authService.getCurrentUser();
		user.setActiveDate(new Date());
		Location loc = new Location();
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		user.setLastLocation(loc);
		userRepo.saveAndFlush(user);
		
		List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrueAndIntentionNotNullAndLastLocationNotNullAndAgeBetween(user.getPreferedMinAge(), user.getPreferedMaxAge());
		List<UserDto> userDtos = new ArrayList<>();
		for(int i = 0; i < users.size(); i++) {
			UserDto dto = new UserDto();
			dto = dto.userToUserDto(users.get(i), user, textEncryptor);
			userDtos.add(dto);
		}
		
		//filter users
		List<UserDto> filteredUserDtos = new ArrayList<>();
		for(int i = 0; i < userDtos.size(); i++) {
			UserDto dto = userDtos.get(i);
			if(user.getId() == dto.getId()) {
				continue;
			}	
			if(user.getHiddenUsers().contains(dto)) {
				continue;
			}		
			if(user.getBlockedUsers().contains(dto)) {
				continue;
			}
			if(!user.getPreferedGenders().contains(dto.getGender())) {
				continue;
			}
			if(!dto.getPreferedGenders().contains(user.getGender())) {
				continue;
			}
			if(!user.getIntention().equals(dto.getIntention())) {
				continue;
			}
			if(dto.getDistanceToUser() > distance) {
				continue;
			}
			filteredUserDtos.add(dto);
		}
		
		if(sort == SORT_DISTANCE) {
			Collections.sort(filteredUserDtos,new Comparator<UserDto>() {
			    @Override
			    public int compare(UserDto a, UserDto b) {
			    	return a.getDistanceToUser() < b.getDistanceToUser() ? -1
			    	         : a.getDistanceToUser() > b.getDistanceToUser() ? 1
			    	         : 0;
			    }
			});
		} else if(sort == SORT_ACTIVE_DATE) {
			Collections.sort(filteredUserDtos,new Comparator<UserDto>() {
			    @Override
			    public int compare(UserDto a, UserDto b) {
			    	return a.getActiveDate().compareTo(b.getActiveDate());
			    }
			});
			Collections.reverse(filteredUserDtos);
		}
		
		filteredUserDtos = filteredUserDtos.stream().limit(maxResults).collect(Collectors.toList());
		
		return filteredUserDtos;
	}
	

	
	
	
	
}
