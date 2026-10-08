package com.couserver.gamedata.repository;

import com.couserver.gamedata.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Integer> {
}
