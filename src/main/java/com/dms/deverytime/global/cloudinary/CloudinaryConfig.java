package com.dms.deverytime.global.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final CloudinaryProperties cloudinaryProperties;

    public Cloudinary cloudinary(){
        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", cloudinaryProperties.cloudName(),
                        "api_key", cloudinaryProperties.apiKey(),
                        "api_secret", cloudinaryProperties.apiSecret(),
                        "secure", true
                )
        );
    }
}
