package main.java.com.example.community_service.entity;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Posts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 작성자 ID

    @Column(length = 255, nullable = false)
    private String title;

    @Lob
    private String content;

    private Integer viewCount; // 조회수

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 게시글과 파일은 1:N
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files = new ArrayList<>();

    // 댓글 양방향 매핑
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 좋아요
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> likes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if(viewCount == null) {
            viewCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

