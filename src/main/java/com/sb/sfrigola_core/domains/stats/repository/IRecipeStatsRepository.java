package com.sb.sfrigola_core.domains.stats.repository;

import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IRecipeStatsRepository extends JpaRepository<RecipeStats, Long> {
}
