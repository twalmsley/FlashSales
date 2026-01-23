package uk.co.aosd.flash.services;

import java.util.List;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.aosd.flash.domain.RemainingActiveStock;
import uk.co.aosd.flash.dto.ClientActiveSaleDto;
import uk.co.aosd.flash.repository.RemainingActiveStockRepository;

/**
 * A Service for reading active sales.
 */
@Service
@RequiredArgsConstructor
public class ActiveSalesService {

    private static final Logger log = LoggerFactory.getLogger(ActiveSalesService.class);

    private final RemainingActiveStockRepository repository;

    private Function<RemainingActiveStock, ClientActiveSaleDto> toClientActiveSaleDto = stock -> {
        return new ClientActiveSaleDto(
            stock.getSaleId().toString(),
            stock.getItemId().toString(),
            stock.getTitle(),
            stock.getStartTime(),
            stock.getEndTime(),
            stock.getProductId().toString(),
            stock.getAllocatedStock(),
            stock.getSoldCount(),
            stock.getSalePrice());
    };

    /**
     * Get all active sales with remaining stock.
     *
     * @return List of active sales
     */
    @Cacheable(value = "activeSales")
    @Transactional(readOnly = true)
    public List<ClientActiveSaleDto> getActiveSales() {
        log.info("Getting all active sales");
        return repository.findAll().stream().map(toClientActiveSaleDto).toList();
    }
}
