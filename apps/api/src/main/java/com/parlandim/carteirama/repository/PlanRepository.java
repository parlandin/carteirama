package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, String> {

    List<Plan> findAllByActiveTrue();
}
