package com.dms.deverytime.domain.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Getter
@Setter
public class PostSearchRequest {

    @NotBlank
    private String keyword;

    private int page = 0;
    private int size = 10;
    private String sort;

    public Pageable toPageable() {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        String[] parts = sort.split(","); // 콤마 기준으로 쪼갬
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

}


