package com.dms.deverytime.global.cloudinary.dto;

public record ImageUploadResult(
        String profileImageUrl,
        String profileImagePublicId
) {
    public static ImageUploadResult of(String image, String publicId){
        return new ImageUploadResult(image, publicId);
    }
}
