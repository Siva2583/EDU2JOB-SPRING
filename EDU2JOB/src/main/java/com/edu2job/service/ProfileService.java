package com.edu2job.service;

import com.edu2job.dto.ProfileResponse;
import com.edu2job.dto.ProfileUpdateRequest;
import com.edu2job.models.User;
import com.edu2job.models.UserPredictionHistory;
import com.edu2job.repositories.UserPredictionHistoryRepository;
import com.edu2job.util.SkillsJson;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Profile persistence for the nine-field EDU2JOB 2.0 schema.
 *
 * <p>Updates are partial: fields left null in the request keep their stored
 * values, while supplied fields (including an explicitly empty skills list)
 * replace them. Profiles are always read and written for the authenticated
 * user only.</p>
 */
@Service
public class ProfileService {

    private final UserPredictionHistoryRepository profileRepository;

    public ProfileService(UserPredictionHistoryRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
        UserPredictionHistory profile = profileRepository
                .findFirstByUserOrderByCreatedAtDesc(user)
                .orElseGet(() -> {
                    UserPredictionHistory fresh = new UserPredictionHistory();
                    fresh.setUser(user);
                    return fresh;
                });

        if (request.getDegree() != null) {
            profile.setDegree(blankToNull(request.getDegree()));
        }
        if (request.getMajor() != null) {
            profile.setMajor(blankToNull(request.getMajor()));
        }
        if (request.getCgpa() != null) {
            profile.setCgpa(request.getCgpa());
        }
        if (request.getSkills() != null) {
            profile.setSkills(SkillsJson.encode(request.getSkills()));
        }
        if (request.getCertificationName() != null) {
            // An empty certification explicitly means "no certification".
            profile.setCertificationName(blankToNull(request.getCertificationName()));
        }
        if (request.getInternshipExperience() != null) {
            profile.setInternshipExperience(blankToNull(request.getInternshipExperience()));
        }
        if (request.getProjectsCount() != null) {
            profile.setProjectsCount(request.getProjectsCount());
        }
        if (request.getCommunicationSkill() != null) {
            profile.setCommunicationSkill(blankToNull(request.getCommunicationSkill()));
        }
        if (request.getExperienceMonths() != null) {
            profile.setExperienceMonths(request.getExperienceMonths());
        }

        UserPredictionHistory saved = profileRepository.save(profile);
        return ProfileResponse.from(saved, user.getUsername());
    }

    @Transactional(readOnly = true)
    public Optional<ProfileResponse> getLatestProfile(User user) {
        return profileRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(profile -> ProfileResponse.from(profile, user.getUsername()));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
