package com.couserver.monster.repository;

import com.couserver.monster.entity.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterRepository extends JpaRepository<Monster, Integer> {


}
