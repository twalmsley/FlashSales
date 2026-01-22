package uk.co.aosd.flash.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.aosd.flash.domain.FlashSale;
import uk.co.aosd.flash.domain.SaleStatus;
import uk.co.aosd.flash.dto.ClientDraftSaleDto;
import uk.co.aosd.flash.dto.ClientDraftSaleDto.DraftSaleProductDto;
import uk.co.aosd.flash.repository.FlashSaleRepository;

/**
 * A Service for reading draft sales.
 */
@Service
@RequiredArgsConstructor
public class DraftSalesService {

    private static final Logger log = LoggerFactory.getLogger(DraftSalesService.class);

    private final FlashSaleRepository repository;

    private Function<FlashSale, ClientDraftSaleDto> toClientDraftSaleDto = sale -> {
        List<DraftSaleProductDto> products = sale.getItems().stream()
            .map(item -> new DraftSaleProductDto(
                item.getProduct().getId().toString(),
                item.getAllocatedStock(),
                item.getSalePrice()))
            .toList();

        return new ClientDraftSaleDto(
            sale.getId().toString(),
            sale.getTitle(),
            sale.getStartTime(),
            sale.getEndTime(),
            products);
    };

    /**
     * Get all draft sales coming up within the next N days.
     *
     * @param days the number of days to look ahead
     * @return List of draft sales
     */
    @Transactional(readOnly = true)
    public List<ClientDraftSaleDto> getDraftSalesWithinDays(int days) {
        log.info("Getting draft sales within the next {} days", days);
        final OffsetDateTime currentTime = OffsetDateTime.now();
        final OffsetDateTime futureTime = currentTime.plusDays(days);
        final List<FlashSale> draftSales = repository.findDraftSalesWithinDays(SaleStatus.DRAFT, currentTime, futureTime);
        log.info("Found {} draft sales within the next {} days", draftSales.size(), days);
        return draftSales.stream().map(toClientDraftSaleDto).toList();
    }
}
