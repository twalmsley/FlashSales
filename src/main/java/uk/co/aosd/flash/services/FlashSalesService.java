package uk.co.aosd.flash.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uk.co.aosd.flash.dto.CreateSaleDto;
import uk.co.aosd.flash.exc.DuplicateEntityException;

@Service
@RequiredArgsConstructor
public class FlashSalesService {

    @Transactional
    public UUID createFlashSale(@Valid final CreateSaleDto sale) throws DuplicateEntityException {
        return UUID.randomUUID();
    }

}
