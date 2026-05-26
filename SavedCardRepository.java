package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedCardRepository extends JpaRepository<SavedCard, Integer> {

    List<SavedCard> findByUser_UserId(Integer userId);
}