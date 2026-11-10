package com.dms.deverytime.global.security.jwt;

import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    public String hash(String refreshToken){

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        }

        catch (NoSuchAlgorithmException e) {
            throw new DeveryTimeException(ErrorCode.TOKEN_HASHING_FAILED);
        }
    }
}
