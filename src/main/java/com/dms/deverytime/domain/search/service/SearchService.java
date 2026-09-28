package com.dms.deverytime.domain.search.service;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.search.dto.request.PostSearchRequest;
import com.dms.deverytime.domain.search.dto.response.PageResponse;
import com.dms.deverytime.domain.search.dto.response.SearchResponse;
import com.dms.deverytime.domain.search.dto.response.UserInfo;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 다 readonly 이므로 위에서 수식
public class SearchService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public SearchResponse search(PostSearchRequest request) {
        Pageable pageable = request.toPageable();
        String keyword = request.getKeyword().trim(); // trim이 앞뒤 공백 제거

        // username에 keyword가 포함된 유저 전체
        List<User> matchedUsers = userRepository.findByUsernameContaining(keyword);
        List<UserInfo> userInfos = matchedUsers.stream()
                .map(UserInfo::from)
                .toList();

        // 제목 검색
        Page<Post> posts = postRepository.findByTitleContainingWithCategory(keyword, pageable);

        Page<PostListResponse> mappedPosts = posts.map(post -> new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory().getName(),
                post.getCreatedAt()
        ));

        PageResponse<PostListResponse> postPage = new PageResponse<>(mappedPosts);

        return new SearchResponse(userInfos, postPage);
    }




}