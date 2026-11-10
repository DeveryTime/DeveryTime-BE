UPDATE refresh_tokens
SET token = SHA2(token, 256);