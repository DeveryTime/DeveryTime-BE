package com.dms.deverytime.domain.user.dto.response;

public record ProfileDataResponse(
        Long id,
        String name,
        String schoolNumber,
        String email,
        String username,
        String profileImageUrl
) {
    public static ProfileDataResponse of(Long id, String name, String schoolNumber
                                         ,String email, String username, String profileImageUrl){
        return new ProfileDataResponse(id, name, schoolNumber, email, username, profileImageUrl);
    }
}
