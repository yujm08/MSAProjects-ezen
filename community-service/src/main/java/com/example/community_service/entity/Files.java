package main.java.com.example.community_service.entity;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Files {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 실제 파일 이름
    private String fileName;

    // 파일 저장 경로 혹은 URL
    private String filePath;

    // 파일 타입 (예: image/png 등)
    private String fileType;

    // 게시글과의 N:1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
