package com.dms.devrytime.global.exception.filter;

import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import com.dms.devrytime.global.exception.response.ErrorData;
import com.dms.devrytime.global.exception.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(request, response);
        } catch (DevryTimeException e) {
            sendErrorMessage(response, e.getErrorCode());
        } catch (Exception e) {
            sendErrorMessage(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }

    }

    private void sendErrorMessage(
            HttpServletResponse response,
            ErrorCode errorCode) throws IOException{
        ErrorResponse errorResponse = ErrorResponse.of(ErrorData.from(errorCode));

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);

    }
}
