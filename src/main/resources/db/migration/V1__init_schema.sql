CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `email` varchar(255) NOT NULL,
                         `name` varchar(30) NOT NULL,
                         `password_hash` varchar(255) NOT NULL,
                         `profile_image_public_id` varchar(255) DEFAULT NULL,
                         `profile_image_url` varchar(500) DEFAULT NULL,
                         `school_number` varchar(4) NOT NULL,
                         `school_year` int NOT NULL,
                         `username` varchar(10) NOT NULL,
                         `username_updated_at` datetime(6) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_user` (`school_year`,`school_number`),
                         UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
                         UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `category` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `name` varchar(50) NOT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `UK46ccwnsi9409t36lurvtyljak` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `email_verifications` (
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `code` varchar(6) NOT NULL,
                                       `email` varchar(255) NOT NULL,
                                       `expires_at` datetime(6) NOT NULL,
                                       `is_verified` bit(1) NOT NULL,
                                       `request_count` int NOT NULL,
                                       `request_window_started_at` datetime(6) NOT NULL,
                                       `sent_at` datetime(6) NOT NULL,
                                       `verification_attempt_count` int NOT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `UKjs5u5jd7gxiwv9c9ep08ryotb` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_tokens` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `created_at` datetime(6) NOT NULL,
                                  `expires_at` datetime(6) NOT NULL,
                                  `token` varchar(255) NOT NULL,
                                  `user_id` bigint NOT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post` (
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `content` text NOT NULL,
                        `created_at` datetime(6) NOT NULL,
                        `status` enum('DRAFT','PUBLISHED') NOT NULL,
                        `title` varchar(255) NOT NULL,
                        `updated_at` datetime(6) DEFAULT NULL,
                        `view_count` int NOT NULL,
                        `category_id` bigint NOT NULL,
                        `user_id` bigint NOT NULL,
                        `like_count` int NOT NULL,
                        PRIMARY KEY (`id`),
                        KEY `FKg6l1ydp1pwkmyj166teiuov1b` (`category_id`),
                        KEY `FK7ky67sgi7k0ayf22652f7763r` (`user_id`),
                        CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                        CONSTRAINT `FKg6l1ydp1pwkmyj166teiuov1b` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_comments` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `created_at` datetime(6) NOT NULL,
                                 `updated_at` datetime(6) DEFAULT NULL,
                                 `content` varchar(1000) NOT NULL,
                                 `post_id` bigint NOT NULL,
                                 `user_id` bigint NOT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `FKmws3vvhl5o4t7r7sajiqpfe0b` (`post_id`),
                                 KEY `FKsnxoecngu89u3fh4wdrgf0f2g` (`user_id`),
                                 CONSTRAINT `FKmws3vvhl5o4t7r7sajiqpfe0b` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
                                 CONSTRAINT `FKsnxoecngu89u3fh4wdrgf0f2g` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_image` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `image_url` varchar(500) NOT NULL,
                              `sort_order` int NOT NULL,
                              `post_id` bigint NOT NULL,
                              `public_id` varchar(255) NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FKsip7qv57jw2fw50g97t16nrjr` (`post_id`),
                              CONSTRAINT `FKsip7qv57jw2fw50g97t16nrjr` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_like` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `created_at` datetime(6) NOT NULL,
                             `updated_at` datetime(6) DEFAULT NULL,
                             `post_id` bigint NOT NULL,
                             `user_id` bigint NOT NULL,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_post_like` (`post_id`,`user_id`),
                             KEY `FKijnjmw0imnatadr3agtk0udip` (`user_id`),
                             CONSTRAINT `FKijnjmw0imnatadr3agtk0udip` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                             CONSTRAINT `FKj7iy0k7n3d0vkh8o7ibjna884` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_view_log` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `post_id` bigint NOT NULL,
                                 `user_id` bigint NOT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `UKf8s9upat1lqmbjqjsgbjcitps` (`post_id`,`user_id`),
                                 KEY `FKta4qc13rtoslqg92vvmef5q4f` (`user_id`),
                                 CONSTRAINT `FKkxox5pf14a1nd5v4y2qu356dl` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
                                 CONSTRAINT `FKta4qc13rtoslqg92vvmef5q4f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
