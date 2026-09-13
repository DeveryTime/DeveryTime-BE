package com.dms.deverytime.domain.user.service;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPostQueryService {

    private final PostRepository postRepository;

    public Page<PostListResponse> getUserPosts(Long userId, Pageable pageable){
        Page<Post> posts = postRepository.findAllByUser_Id(userId, pageable);

        return posts.map(post -> new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory().getName(),
                post.getCreatedAt()
        ));
    }
}
