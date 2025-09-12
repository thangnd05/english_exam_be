package com.example.test.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "images")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "image_id")  // Thêm ID generator
public class Images {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long image_id;

    private String name;
    private String url;
    private String publicId;

    @Column(name = "post_id")
    private Long postId;


}
