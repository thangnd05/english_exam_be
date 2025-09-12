package com.example.test.controller;

import com.example.test.models.Categories;
import com.example.test.services.CategoryService;
import com.example.test.services.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }


    @GetMapping("/category")
    public List<Categories> getAll(){
        return categoryService.getAllCategory();
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<Categories> getCategory(@PathVariable Long id) {
        Optional<Categories> category = categoryService.findById(id);
//        if (category.isPresent()) {
            return new ResponseEntity<>(category.get(), HttpStatus.OK);
//        } else {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<Void> deletePostById(@PathVariable Long id) {
        Optional<Categories> category = categoryService.findById(id);
        if (category.isPresent()) {
            categoryService.DeleteCategorybyId(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/categoryName")
    public ResponseEntity<List<Categories>> getCategoryByCategoryName(@RequestParam String categoryName) {
        List<Categories>categoriesList=categoryService.findCategoryBycategoryName(categoryName);
//        if (categoriesList.isEmpty()) {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
        return new ResponseEntity<>(categoriesList, HttpStatus.OK);
    }

    @PostMapping("/category")
    public ResponseEntity<Categories>createCatgeory(
            @RequestParam("categoryName") String categoryName
    ){
        Categories categories =new Categories();
        categories.setCategoryName(categoryName);

        Categories createdCategory = categoryService.saveCategory(categories);
        return new ResponseEntity<>(createdCategory,HttpStatus.CREATED);
    }
    @PutMapping("/category/{id}")
    public ResponseEntity<Categories>UpdateCatgory(@Valid @PathVariable Long id, @RequestBody Categories categories) {
        try {
            Categories update = categoryService.update(id,categories);
            return ResponseEntity.ok(update);
        } catch (Exception e) {
            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
        }
    }


}
