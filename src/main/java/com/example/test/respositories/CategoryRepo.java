package com.example.test.respositories;

import com.example.test.models.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Categories,Long> {


    Optional<Categories> findById(Long categoryId);

    List<Categories>findCategoryByCategoryName(String categoryName);



}
