package com.dms.deverytime.domain.search.dto.request;

import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;


@Getter
@Setter
public class PostSearchRequest {

    @NotBlank
    private String keyword;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(50)
    private int size = 10;
    private String sort;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");

    public Pageable toPageable() {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }
        String[] parts = sort.split(","); // 콤마 기준으로 쪼갬

        if (!ALLOWED_SORT_FIELDS.contains(parts[0])) {
            throw new DeveryTimeException(ErrorCode.VALIDATION_ERROR);
        }

        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

}


