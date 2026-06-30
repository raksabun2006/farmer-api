package co.cambodia.farmer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Entity
@Table(name = "farmers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Farmer {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;
    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;
    private String province;

    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL)
    private List<Product> products;
}