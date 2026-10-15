package com.couserver.dev.service;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.dev.exception.DevErrorCode;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev-tools.enabled", havingValue = "true")
public class DevService {

    private final StaticDataService staticDataService;
    private final CurrencyRepository currencyRepository;
    private final EquipmentRepository equipmentRepository;
    private final Clock clock;

    @Transactional
    public CurrencyResponse refillStamina(Long playerId) {
        Currency currency = currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
        currency.update(currency.getCurrencyGold(), currency.getCurrencyGem(),
                staticDataService.getAccountConst().maxStamina(), Instant.now(clock));
        return new CurrencyResponse(currency);
    }

    @Transactional
    public List<EquipmentResponse> grantItems(Long playerId, long itemId, int count) {
        ItemData item = staticDataService.getItems().get(itemId);
        if (item == null) {
            throw new BusinessException(DevErrorCode.ITEM_NOT_FOUND);
        }

        List<Equipment> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            created.add(Equipment.create(playerId, item));
        }

        return equipmentRepository.saveAll(created).stream()
                .map(e -> new EquipmentResponse(
                        e.getId(), e.getItemId(), e.getLevel(),
                        e.getGrade().toClientName(), e.isEquipped()))
                .toList();
    }
}
