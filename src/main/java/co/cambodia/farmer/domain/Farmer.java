package co.cambodia.farmer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "farmers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Farmer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

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
