package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.PopularDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PopularDestinationRepository extends JpaRepository<PopularDestination, UUID> {

    @Query("SELECT pd FROM PopularDestination pd ORDER BY pd.popularityRank ASC")
    List<PopularDestination> findAllOrderByPopularityRank();

    List<PopularDestination> findByCountryCode(String countryCode);
}